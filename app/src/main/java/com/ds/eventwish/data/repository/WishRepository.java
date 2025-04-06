package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.NetworkResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WishRepository {
    private static final String PREF_NAME = "wish_preferences";
    private static final String KEY_HISTORY = "history_data";
    
    private final ApiService apiService;
    private final SharedPreferences preferences;
    private final Gson gson;
    
    private static WishRepository instance;
    private static final Object lock = new Object();

    public static WishRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new WishRepository(context, ApiClient.getClient());
                }
            }
        }
        return instance;
    }
    
    public WishRepository(Context context, ApiService apiService) {
        this.apiService = apiService;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }
    
    public void loadHistory(MutableLiveData<NetworkResult<List<SharedWish>>> result) {
        result.setValue(NetworkResult.loading());
        
        apiService.getMyWishes().enqueue(new Callback<List<SharedWish>>() {
            @Override
            public void onResponse(Call<List<SharedWish>> call, Response<List<SharedWish>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SharedWish> wishes = response.body();
                    saveHistoryToCache(wishes);
                    result.setValue(NetworkResult.success(wishes));
                } else {
                    result.setValue(NetworkResult.error("Failed to load history"));
                }
            }
            
            @Override
            public void onFailure(Call<List<SharedWish>> call, Throwable t) {
                result.setValue(NetworkResult.error(t.getMessage()));
            }
        });
    }
    
    public List<SharedWish> loadCachedHistory() {
        String jsonHistory = preferences.getString(KEY_HISTORY, null);
        if (jsonHistory != null) {
            Type type = new TypeToken<List<SharedWish>>(){}.getType();
            return gson.fromJson(jsonHistory, type);
        }
        return new ArrayList<>();
    }
    
    private void saveHistoryToCache(List<SharedWish> wishes) {
        String jsonHistory = gson.toJson(wishes);
        preferences.edit().putString(KEY_HISTORY, jsonHistory).apply();
    }
    
    public void clearHistory(MutableLiveData<NetworkResult<Void>> result) {
        result.setValue(NetworkResult.loading());
        
        apiService.clearHistory().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    preferences.edit().remove(KEY_HISTORY).apply();
                    result.setValue(NetworkResult.success(null));
                } else {
                    result.setValue(NetworkResult.error("Failed to clear history"));
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue(NetworkResult.error(t.getMessage()));
            }
        });
    }
    
    public void addToHistory(SharedWish wish) {
        List<SharedWish> cachedWishes = loadCachedHistory();
        // Add new wish at the beginning of the list
        cachedWishes.add(0, wish);
        saveHistoryToCache(cachedWishes);
    }
    
    /**
     * Get a wish by short code from the cached history
     * @param shortCode The short code of the wish
     * @return The wish with the given short code, or null if not found
     */
    public SharedWish getWishByShortCode(String shortCode) {
        if (shortCode == null) return null;
        
        List<SharedWish> cachedWishes = loadCachedHistory();
        for (SharedWish wish : cachedWishes) {
            if (shortCode.equals(wish.getShortCode())) {
                return wish;
            }
        }
        
        return null;
    }
    
    /**
     * Save a wish to the history
     * @param wish The wish to save
     */
    public void saveWish(SharedWish wish) {
        if (wish == null) return;
        
        List<SharedWish> cachedWishes = loadCachedHistory();
        
        // Check if wish already exists
        for (int i = 0; i < cachedWishes.size(); i++) {
            if (cachedWishes.get(i).getShortCode().equals(wish.getShortCode())) {
                // Update existing wish
                SharedWish existingWish = cachedWishes.get(i);
                existingWish.setSharedVia(wish.getSharedVia());
                existingWish.setLastSharedAt(new Date());
                cachedWishes.set(i, existingWish);
                break;
            }
        }
        
        // Add new wish if not found
        if (!cachedWishes.contains(wish)) {
            cachedWishes.add(0, wish);
        }
        
        // Keep only the last 50 wishes
        if (cachedWishes.size() > 50) {
            cachedWishes = cachedWishes.subList(0, 50);
        }
        
        saveHistoryToCache(cachedWishes);
    }
}
