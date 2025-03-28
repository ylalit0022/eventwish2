package com.ds.eventwish.data.repository;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.FestivalDao;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.ServerTimeResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.CacheManager;
import com.google.gson.reflect.TypeToken;
import com.ds.eventwish.utils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FestivalRepository {
    private static final String TAG = "FestivalRepository";
    private static FestivalRepository instance;
    private static final String CACHE_KEY_FESTIVALS = "festivals";
    private static final String CACHE_KEY_FESTIVALS_BY_CATEGORY = "festivals_category_";
    private static final long BACKGROUND_CHECK_INTERVAL = 15; // minutes
    private static final String PREF_NOTIFICATION_HISTORY = "notification_history";

    private final FestivalDao festivalDao;
    private final ApiService apiService;
    private final Executor executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isFromCache = new MutableLiveData<>(false);
    private final CacheManager cacheManager;
    private final Context context;

    private FestivalRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        festivalDao = database.festivalDao();
        apiService = ApiClient.getClient();
        executor = Executors.newFixedThreadPool(4);
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        cacheManager = CacheManager.getInstance(context);
        
        // Initialize the unread count on a background thread
        executor.execute(this::refreshUnreadCount);
        
        // Start background check for database changes
        startBackgroundDatabaseCheck();
    }

    public static synchronized FestivalRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FestivalRepository(context);
        }
        return instance;
    }

    // For testing purposes only
    public static void setTestInstance(FestivalRepository testInstance) {
        instance = testInstance;
    }

    /**
     * Get upcoming festivals from the database
     * @return LiveData containing a list of upcoming festivals
     */
    public LiveData<List<Festival>> getUpcomingFestivals() {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 3); // Get festivals for the next 3 months
        Date endDate = calendar.getTime();
        return festivalDao.getUpcomingFestivals(now, endDate);
    }

    /**
     * Get upcoming festivals by category from the database
     * @param category The category to filter by
     * @return LiveData containing a list of upcoming festivals for the given category
     */
    public LiveData<List<Festival>> getUpcomingFestivalsByCategory(String category) {
        return festivalDao.getFestivalsByCategory(category);
    }

    /**
     * Get a festival by ID from the database
     * @param festivalId The ID of the festival to retrieve
     * @return LiveData containing the festival with the given ID
     */
    public LiveData<Festival> getFestivalById(String festivalId) {
        return festivalDao.getFestivalById(festivalId);
    }

    /**
     * Get the count of unread festivals
     * @return LiveData containing the count of unread festivals
     */
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    /**
     * Get the loading state
     * @return LiveData containing the loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Get the error message
     * @return LiveData containing the error message
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get whether the data is from cache
     * @return LiveData containing whether the data is from cache
     */
    public LiveData<Boolean> getIsFromCache() {
        return isFromCache;
    }

    /**
     * Mark a festival as read
     * @param festivalId The ID of the festival to mark as read
     */
    public void markAsRead(String festivalId) {
        executor.execute(() -> {
            festivalDao.markAsRead(festivalId);
            refreshUnreadCount();
        });
    }

    /**
     * Mark a festival as notified
     * @param festivalId The ID of the festival to mark as notified
     */
    public void markAsNotified(String festivalId) {
        executor.execute(() -> {
            festivalDao.markAsNotified(festivalId);
        });
    }

    /**
     * Mark all festivals as read
     */
    public void markAllAsRead() {
        executor.execute(() -> {
            festivalDao.markAllAsRead();
            refreshUnreadCount();
        });
    }

    /**
     * Get unnotified upcoming festivals
     * @return List of unnotified upcoming festivals
     */
    public List<Festival> getUnnotifiedUpcomingFestivals() {
        // Ensure we're not on the main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(TAG, "getUnnotifiedUpcomingFestivals called on main thread, this is not allowed");
            throw new IllegalStateException("Cannot access database on the main thread");
        }
        
        Date now = TimeUtils.getCurrentServerTime();
        Date endDate = TimeUtils.addMonthsToServerTime(1); // Get festivals for the next month
        
        return festivalDao.getUnnotifiedUpcomingFestivals(now, endDate);
    }

    /**
     * Refresh the unread count from the database
     */
    private void refreshUnreadCount() {
        // Ensure we're on a background thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.d(TAG, "refreshUnreadCount called on main thread, moving to background thread");
            executor.execute(this::refreshUnreadCount);
            return;
        }
        
        // Get the unread count directly without observing
        try {
            int count = festivalDao.getUnreadCountSync();
            unreadCount.postValue(count);
            Log.d(TAG, "Updated unread count: " + count);
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing unread count", e);
        }
    }

    /**
     * Force refresh upcoming festivals from the server
     * This method will clear the local database first
     */
    public void refreshUpcomingFestivals() {
        executor.execute(() -> {
            // First sync server time
            try {
                Call<ServerTimeResponse> timeCall = apiService.getServerTime();
                retrofit2.Response<ServerTimeResponse> timeResponse = timeCall.execute();
                if (timeResponse.isSuccessful() && timeResponse.body() != null) {
                    ServerTimeResponse timeData = timeResponse.body();
                    if (timeData != null) {
                        TimeUtils.syncWithServerTime(timeData.getTimestamp(), timeData.getFormatted());
                    }
                } else {
                    Log.e(TAG, "Server time sync failed: success is false");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing server time", e);
            }

            // Then fetch festivals
            try {
                Call<List<Festival>> call = apiService.getFestivals();
                retrofit2.Response<List<Festival>> response = call.execute();
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    
                    // Update database
                    festivalDao.insertAll(festivals);
                    
                    // Update cache timestamp
                    updateCacheTimestamp();
                    
                    // Set error message to null since request was successful
                    errorMessage.postValue(null);
                    
                    // Refresh unread count
                    refreshUnreadCount();
                } else {
                    errorMessage.postValue("Failed to fetch festivals from server");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching festivals", e);
                errorMessage.postValue("Network error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Force refresh upcoming festivals by category from the server
     * @param category The category to refresh
     */
    public void refreshUpcomingFestivalsByCategory(String category) {
        isLoading.setValue(true);
        isFromCache.setValue(false);
        
        // Clear cache for this category
        cacheManager.clearCache(CACHE_KEY_FESTIVALS_BY_CATEGORY + category);
        
        // Fetch from server
        fetchUpcomingFestivalsByCategoryFromServer(category);
    }
    
    /**
     * Load upcoming festivals with cache support
     */
    public void loadUpcomingFestivals() {
        isLoading.setValue(true);
        
        // Check cache first
        TypeToken<List<Festival>> typeToken = new TypeToken<List<Festival>>() {};
        List<Festival> cachedFestivals = cacheManager.getFromCache(CACHE_KEY_FESTIVALS, typeToken);
        
        if (cachedFestivals != null && !cacheManager.isCacheExpired(CACHE_KEY_FESTIVALS)) {
            // Use cached data
            Log.d(TAG, "Using cached festivals: " + cachedFestivals.size() + " festivals");
            executor.execute(() -> {
                festivalDao.insertAllFestivals(cachedFestivals);
                isLoading.postValue(false);
                isFromCache.postValue(true);
            });
        } else {
            // Cache expired or not available, fetch from server
            Log.d(TAG, "Cache expired or not available, fetching from server");
            isFromCache.setValue(false);
            fetchUpcomingFestivalsFromServer();
        }
    }
    
    /**
     * Load upcoming festivals by category with cache support
     * @param category The category to load
     */
    public void loadUpcomingFestivalsByCategory(String category) {
        isLoading.setValue(true);
        
        // Check cache first
        String cacheKey = CACHE_KEY_FESTIVALS_BY_CATEGORY + category;
        TypeToken<List<Festival>> typeToken = new TypeToken<List<Festival>>() {};
        List<Festival> cachedFestivals = cacheManager.getFromCache(cacheKey, typeToken);
        
        if (cachedFestivals != null && !cacheManager.isCacheExpired(cacheKey)) {
            // Use cached data
            Log.d(TAG, "Using cached festivals for category " + category + ": " + cachedFestivals.size() + " festivals");
            executor.execute(() -> {
                festivalDao.deleteFestivalsByCategory(category);
                festivalDao.insertAllFestivals(cachedFestivals);
                isLoading.postValue(false);
                isFromCache.postValue(true);
            });
        } else {
            // Cache expired or not available, fetch from server
            Log.d(TAG, "Cache expired or not available for category " + category + ", fetching from server");
            isFromCache.setValue(false);
            fetchUpcomingFestivalsByCategoryFromServer(category);
        }
    }

    private void fetchUpcomingFestivalsFromServer() {
        apiService.getUpcomingFestivals().enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    Log.d(TAG, "Fetched " + festivals.size() + " festivals from server");
                    
                    // Save to database
                    executor.execute(() -> {
                        try {
                            // Clear existing festivals
                            festivalDao.deleteAllFestivals();
                            
                            // Insert new festivals
                            festivalDao.insertAllFestivals(festivals);
                            
                            // Update unread count
                            refreshUnreadCount();
                            
                            // Save to cache
                            cacheManager.saveToCache(CACHE_KEY_FESTIVALS, festivals);
                            
                            isLoading.postValue(false);
                            errorMessage.postValue(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving festivals to database", e);
                            isLoading.postValue(false);
                            errorMessage.postValue("Error saving festivals: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Error fetching festivals: " + response.code());
                    isLoading.postValue(false);
                    errorMessage.postValue("Error fetching festivals: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "Error fetching festivals", t);
                isLoading.postValue(false);
                errorMessage.postValue("Error fetching festivals: " + t.getMessage());
                
                // Try to load from database as fallback
                loadFromDatabaseAsFallback();
            }
        });
    }
    
    private void fetchUpcomingFestivalsByCategoryFromServer(String category) {
        apiService.getFestivalsByCategory(category).enqueue(new Callback<List<Festival>>() {
            @Override
            public void onResponse(Call<List<Festival>> call, Response<List<Festival>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Festival> festivals = response.body();
                    Log.d(TAG, "Fetched " + festivals.size() + " festivals for category " + category + " from server");
                    
                    // Save to database
                    executor.execute(() -> {
                        try {
                            // Clear existing festivals for this category
                            festivalDao.deleteFestivalsByCategory(category);
                            
                            // Insert new festivals
                            festivalDao.insertAllFestivals(festivals);
                            
                            // Update unread count
                            refreshUnreadCount();
                            
                            // Save to cache
                            cacheManager.saveToCache(CACHE_KEY_FESTIVALS_BY_CATEGORY + category, festivals);
                            
                            isLoading.postValue(false);
                            errorMessage.postValue(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving festivals to database", e);
                            isLoading.postValue(false);
                            errorMessage.postValue("Error saving festivals: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Error fetching festivals for category " + category + ": " + response.code());
                    isLoading.postValue(false);
                    errorMessage.postValue("Error fetching festivals: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<Festival>> call, Throwable t) {
                Log.e(TAG, "Error fetching festivals for category " + category, t);
                isLoading.postValue(false);
                errorMessage.postValue("Error fetching festivals: " + t.getMessage());
                
                // Try to load from database as fallback
                loadFromDatabaseAsFallback(category);
            }
        });
    }
    
    /**
     * Load festivals from database as fallback when network request fails
     */
    private void loadFromDatabaseAsFallback() {
        executor.execute(() -> {
            try {
                Date now = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, 3);
                Date endDate = calendar.getTime();
                
                List<Festival> festivals = festivalDao.getUpcomingFestivalsSync(now, endDate);
                if (festivals != null && !festivals.isEmpty()) {
                    Log.d(TAG, "Loaded " + festivals.size() + " festivals from database as fallback");
                    
                    // Save to cache and mark as from database
                    cacheManager.saveToCache(CACHE_KEY_FESTIVALS, festivals);
                    cacheManager.setDataSource(CACHE_KEY_FESTIVALS, CacheManager.DataSource.DATABASE);
                    
                    isFromCache.postValue(true);
                } else {
                    Log.d(TAG, "No festivals found in database as fallback");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading festivals from database as fallback", e);
            }
        });
    }
    
    /**
     * Load festivals by category from database as fallback when network request fails
     * @param category The category to load
     */
    private void loadFromDatabaseAsFallback(String category) {
        executor.execute(() -> {
            try {
                List<Festival> festivals = festivalDao.getFestivalsByCategorySync(category);
                if (festivals != null && !festivals.isEmpty()) {
                    Log.d(TAG, "Loaded " + festivals.size() + " festivals for category " + category + " from database as fallback");
                    
                    // Save to cache and mark as from database
                    cacheManager.saveToCache(CACHE_KEY_FESTIVALS_BY_CATEGORY + category, festivals);
                    cacheManager.setDataSource(CACHE_KEY_FESTIVALS_BY_CATEGORY + category, CacheManager.DataSource.DATABASE);
                    
                    isFromCache.postValue(true);
                } else {
                    Log.d(TAG, "No festivals found in database for category " + category + " as fallback");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading festivals from database for category " + category + " as fallback", e);
            }
        });
    }
    
    /**
     * Start background check for database changes
     */
    private void startBackgroundDatabaseCheck() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (isAppInForeground()) {
                Log.d(TAG, "Performing background check for database changes");
                checkForDatabaseChanges();
            }
        }, BACKGROUND_CHECK_INTERVAL, BACKGROUND_CHECK_INTERVAL, TimeUnit.MINUTES);
    }
    
    /**
     * Check if the app is in the foreground
     * @return true if the app is in the foreground
     */
    private boolean isAppInForeground() {
        // This is a simplified check - in a real app, you would use ProcessLifecycleOwner
        // or a similar mechanism to determine if the app is in the foreground
        return true;
    }
    
    /**
     * Check for database changes
     */
    private void checkForDatabaseChanges() {
        executor.execute(() -> {
            try {
                // Check for new unnotified festivals
                List<Festival> unnotifiedFestivals = getUnnotifiedUpcomingFestivals();
                if (unnotifiedFestivals != null && !unnotifiedFestivals.isEmpty()) {
                    Log.d(TAG, "Found " + unnotifiedFestivals.size() + " unnotified festivals during background check");
                    // You could trigger notifications here if needed
                }
                
                // Refresh unread count
                refreshUnreadCount();
            } catch (Exception e) {
                Log.e(TAG, "Error checking for database changes", e);
            }
        });
    }
    
    /**
     * Clear memory cache but keep disk cache
     * Call this when the app is sent to the background
     */
    public void clearMemoryCache() {
        cacheManager.clearMemoryCache();
    }
    
    /**
     * Clear all cache data
     * Call this when the app is first launched
     */
    public void clearAllCache() {
        cacheManager.clearAllCache();
    }

    /**
     * Schedule countdown notifications for upcoming festivals
     */
    public void scheduleCountdownNotifications() {
        Log.d(TAG, "Scheduling countdown notifications for upcoming festivals");
        
        // Ensure we're not on the main thread for database access
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.d(TAG, "scheduleCountdownNotifications called on main thread, moving to background thread");
            executor.execute(this::scheduleCountdownNotifications);
            return;
        }
        
        // Get upcoming festivals that are within the next 30 days
        try {
            Date now = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30); // Get festivals for the next 30 days
            Date endDate = calendar.getTime();
            
            List<Festival> upcomingFestivals = festivalDao.getUpcomingFestivalsSync(now, endDate);
            
            if (upcomingFestivals != null && !upcomingFestivals.isEmpty()) {
                Log.d(TAG, "Found " + upcomingFestivals.size() + " upcoming festivals for countdown notifications");
                
                for (Festival festival : upcomingFestivals) {
                    scheduleCountdownNotification(festival);
                }
            } else {
                Log.d(TAG, "No upcoming festivals found for countdown notifications");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling countdown notifications", e);
        }
    }
    
    /**
     * Schedule countdown notifications for a specific festival
     * @param festival The festival to schedule notifications for
     */
    private void scheduleCountdownNotification(Festival festival) {
        // Calculate days until festival using server time
        Calendar festivalDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        festivalDate.setTime(festival.getDate());
        
        Calendar today = TimeUtils.getServerCalendar();
        
        // Clear time portion for accurate day calculation
        clearTimeFields(festivalDate);
        clearTimeFields(today);
        
        // Calculate days difference
        long diffInMillis = festivalDate.getTimeInMillis() - today.getTimeInMillis();
        int daysUntil = (int) (diffInMillis / (24 * 60 * 60 * 1000));
        
        Log.d(TAG, "Festival: " + festival.getName() + " is in " + daysUntil + " days (using server time)");
        
        // Schedule notifications at 7 days, 3 days, and 1 day before the festival
        scheduleNotificationIfNeeded(festival, daysUntil, 7);
        scheduleNotificationIfNeeded(festival, daysUntil, 3);
        scheduleNotificationIfNeeded(festival, daysUntil, 1);
    }
    
    /**
     * Schedule a notification if the festival is exactly at the specified countdown day
     * @param festival The festival to schedule a notification for
     * @param daysUntil Days until the festival
     * @param countdownDay The specific countdown day to check (e.g., 7 for 7 days before)
     */
    private void scheduleNotificationIfNeeded(Festival festival, int daysUntil, int countdownDay) {
        // Skip Father's Day test festival
        if (festival.getName().equals("Father's Day") && festival.getCategory().equals("Cultural")) {
            Log.d(TAG, "Skipping test Father's Day festival notification");
            return;
        }
        
        if (daysUntil == countdownDay) {
            Log.d(TAG, "Scheduling " + countdownDay + "-day countdown notification for: " + festival.getName());
            
            // Check if we've already scheduled this notification today
            String notificationKey = "festival_notification_" + festival.getId() + "_" + countdownDay;
            SharedPreferences prefs = context.getSharedPreferences("festival_notifications", Context.MODE_PRIVATE);
            
            // Get today's date as a string (yyyy-MM-dd)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String today = dateFormat.format(new Date());
            
            // Check if we've already scheduled this notification today
            String lastScheduled = prefs.getString(notificationKey, "");
            if (lastScheduled.equals(today)) {
                Log.d(TAG, "Notification for " + festival.getName() + " already scheduled today, skipping");
                return;
            }
            
            // Create intent for the CountdownNotificationReceiver
            Intent intent = new Intent(context, com.ds.eventwish.receivers.CountdownNotificationReceiver.class);
            intent.setAction("com.ds.eventwish.COUNTDOWN_NOTIFICATION");
            intent.putExtra("festivalId", festival.getId());
            intent.putExtra("title", festival.getName());
            intent.putExtra("daysLeft", countdownDay);
            
            // Create a unique request code based on festival ID and countdown day
            int requestCode = (festival.getId().hashCode() * 10) + countdownDay;
            
            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Get current time
            Calendar calendar = Calendar.getInstance();
            // Set notification time to 9:00 AM
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            
            // If current time is past 9:00 AM, schedule for tomorrow
            Calendar now = Calendar.getInstance();
            if (now.get(Calendar.HOUR_OF_DAY) >= 9) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Log.d(TAG, "Current time is past 9:00 AM, scheduling for tomorrow");
            }
            
            // Schedule the notification
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) 
                    context.getSystemService(Context.ALARM_SERVICE);
            
            if (alarmManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
                
                // Save that we've scheduled this notification today
                prefs.edit().putString(notificationKey, today).apply();
                
                Log.d(TAG, "Countdown notification scheduled for " + festival.getName() + 
                        " at " + calendar.getTime() + " (" + countdownDay + " days before)");
            } else {
                Log.e(TAG, "AlarmManager is null, could not schedule countdown notification");
            }
        }
    }
    
    /**
     * Clear time fields from a calendar for accurate day calculation
     * @param calendar The calendar to clear time fields from
     */
    private void clearTimeFields(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Update the cache timestamp for festivals
     */
    private void updateCacheTimestamp() {
        if (cacheManager != null) {
            // Save current time as cache timestamp
            cacheManager.saveToCache(CACHE_KEY_FESTIVALS + "_timestamp", new Date().getTime());
            Log.d(TAG, "Updated cache timestamp for festivals");
        }
    }

    /**
     * Get upcoming festivals synchronously (for use in workers)
     * @return List of upcoming festivals
     */
    public List<Festival> getUpcomingFestivalsSync() {
        Log.d(TAG, "Getting upcoming festivals synchronously");
        
        try {
            // Get current date at midnight
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date today = calendar.getTime();
            
            // Get festivals from database
            List<Festival> festivals = festivalDao.getUpcomingFestivalsSync(today);
            
            if (festivals != null && !festivals.isEmpty()) {
                Log.d(TAG, "Found " + festivals.size() + " upcoming festivals in database");
                return festivals;
            } else {
                Log.d(TAG, "No upcoming festivals found in database");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting upcoming festivals synchronously", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Mark a festival as notified for a specific day threshold
     * @param festivalId Festival ID
     * @param daysUntil Days until the festival
     */
    public void markAsNotified(String festivalId, int daysUntil) {
        Log.d(TAG, "Marking festival " + festivalId + " as notified for " + daysUntil + " days threshold");
        
        try {
            // Get shared preferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NOTIFICATION_HISTORY, Context.MODE_PRIVATE);
            
            // Create a key for this festival and day threshold
            String key = festivalId + "_" + daysUntil;
            
            // Mark as notified
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
            
            Log.d(TAG, "Festival " + festivalId + " marked as notified for " + daysUntil + " days threshold");
        } catch (Exception e) {
            Log.e(TAG, "Error marking festival as notified", e);
        }
    }
    
    /**
     * Check if a festival has been notified for a specific day threshold
     * @param festivalId Festival ID
     * @param daysUntil Days until the festival
     * @return true if notified, false otherwise
     */
    public boolean isNotified(String festivalId, int daysUntil) {
        try {
            // Get shared preferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NOTIFICATION_HISTORY, Context.MODE_PRIVATE);
            
            // Create a key for this festival and day threshold
            String key = festivalId + "_" + daysUntil;
            
            // Check if notified
            return prefs.contains(key);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if festival is notified", e);
            return false;
        }
    }
    
    /**
     * Reset notification history for a festival
     * @param festivalId Festival ID
     */
    public void resetNotificationHistory(String festivalId) {
        Log.d(TAG, "Resetting notification history for festival " + festivalId);
        
        try {
            // Get shared preferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NOTIFICATION_HISTORY, Context.MODE_PRIVATE);
            
            // Get all keys
            SharedPreferences.Editor editor = prefs.edit();
            
            // Remove all keys for this festival
            for (String key : prefs.getAll().keySet()) {
                if (key.startsWith(festivalId + "_")) {
                    editor.remove(key);
                }
            }
            
            // Apply changes
            editor.apply();
            
            Log.d(TAG, "Notification history reset for festival " + festivalId);
        } catch (Exception e) {
            Log.e(TAG, "Error resetting notification history", e);
        }
    }
}
