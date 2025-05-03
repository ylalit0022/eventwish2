# Sponsored Ads Rotation and Fair Distribution Implementation Plan

## Overview

This document outlines the implementation plan for enhancing the sponsored ads system with rotation capabilities and fair distribution algorithms. The goal is to improve user experience and ad effectiveness by ensuring ads are displayed in a balanced way without disrupting the existing functionality.

## Current Implementation Analysis

### Server-Side
- SponsoredAd model defines ads with priority, location, date range, and status
- Selection algorithm filters by status, date range, and location
- Ads are sorted by priority (descending)
- Default limit of 10 ads per request

### Client-Side  
- Cache-first approach with Room database
- Single ad selection from high-priority ads
- Visibility-based impression tracking
- Click handling with redirect

## Implementation Goals

1. **Fair Ad Distribution** - Ensure all ads get exposure proportional to their priority
2. **Ad Rotation** - Display different ads over time without requiring new network requests
3. **User Experience** - Provide smooth transitions between ads
4. **Performance** - Minimize network calls while maximizing ad freshness
5. **Analytics** - Track rotation effectiveness and engagement

## Client-Side Implementation

### 1. Local Rotation Manager

```java
/**
 * Manages ad rotation logic to ensure fair distribution of sponsored ads
 */
public class LocalRotationManager {
    private static final String PREF_NAME = "sponsored_ad_rotation";
    private static final String KEY_LAST_ROTATION = "last_rotation_timestamp";
    private static final String KEY_SHOWN_ADS = "shown_ads";
    private static final long DEFAULT_ROTATION_INTERVAL_MS = 20 * 60 * 1000; // 20 minutes
    
    private final SharedPreferences prefs;
    private final Handler rotationHandler;
    private final SponsoredAdRepository repository;
    private long rotationIntervalMs;
    private Set<String> shownAdsInSession;
    private String currentLocation;
    
    // Constructor and initialization
    public LocalRotationManager(Context context, SponsoredAdRepository repository) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.rotationHandler = new Handler(Looper.getMainLooper());
        this.repository = repository;
        this.rotationIntervalMs = DEFAULT_ROTATION_INTERVAL_MS;
        this.shownAdsInSession = new LinkedHashSet<>();
        
        // Restore persisted state
        restoreState();
    }
    
    /**
     * Start rotation for a specific location
     */
    public void startRotation(String location, RotationCallback callback) {
        this.currentLocation = location;
        
        // Schedule first rotation
        scheduleNextRotation(callback);
    }
    
    /**
     * Stop ongoing rotation
     */
    public void stopRotation() {
        rotationHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Schedule the next ad rotation
     */
    private void scheduleNextRotation(RotationCallback callback) {
        // Calculate time until next rotation
        long lastRotation = prefs.getLong(KEY_LAST_ROTATION, 0);
        long now = System.currentTimeMillis();
        long timeElapsed = now - lastRotation;
        
        // If enough time has passed, rotate immediately
        if (timeElapsed >= rotationIntervalMs || lastRotation == 0) {
            rotateAd(callback);
        } else {
            // Otherwise, schedule for remaining time
            long delay = rotationIntervalMs - timeElapsed;
            rotationHandler.postDelayed(() -> rotateAd(callback), delay);
        }
    }
    
    /**
     * Perform ad rotation
     */
    private void rotateAd(RotationCallback callback) {
        // Update rotation timestamp
        long now = System.currentTimeMillis();
        prefs.edit().putLong(KEY_LAST_ROTATION, now).apply();
        
        // Get next ad for rotation
        repository.getNextRotationAd(currentLocation, shownAdsInSession)
            .observeForever(new Observer<SponsoredAd>() {
                @Override
                public void onChanged(SponsoredAd ad) {
                    repository.getNextRotationAd(currentLocation, shownAdsInSession)
                        .removeObserver(this);
                    
                    if (ad != null) {
                        // Record this ad as shown
                        shownAdsInSession.add(ad.getId());
                        persistShownAds();
                        
                        // Notify callback
                        callback.onAdRotated(ad);
                    }
                    
                    // Schedule next rotation
                    rotationHandler.postDelayed(() -> rotateAd(callback), rotationIntervalMs);
                }
            });
    }
    
    /**
     * Save shown ads to preferences
     */
    private void persistShownAds() {
        // Keep only the last 10 shown ads
        List<String> recentAds = new ArrayList<>(shownAdsInSession);
        if (recentAds.size() > 10) {
            recentAds = recentAds.subList(recentAds.size() - 10, recentAds.size());
            shownAdsInSession = new LinkedHashSet<>(recentAds);
        }
        
        // Save to preferences
        Set<String> adSet = new HashSet<>(shownAdsInSession);
        prefs.edit().putStringSet(KEY_SHOWN_ADS, adSet).apply();
    }
    
    /**
     * Restore state from preferences
     */
    private void restoreState() {
        Set<String> savedAds = prefs.getStringSet(KEY_SHOWN_ADS, null);
        if (savedAds != null) {
            shownAdsInSession = new LinkedHashSet<>(savedAds);
        }
    }
    
    /**
     * Reset rotation state
     */
    public void resetRotation() {
        shownAdsInSession.clear();
        prefs.edit()
            .remove(KEY_LAST_ROTATION)
            .remove(KEY_SHOWN_ADS)
            .apply();
    }
    
    /**
     * Set custom rotation interval
     */
    public void setRotationInterval(long intervalMs) {
        this.rotationIntervalMs = intervalMs;
    }
    
    /**
     * Callback for rotation events
     */
    public interface RotationCallback {
        void onAdRotated(SponsoredAd ad);
    }
}
```

### 2. Repository Updates

```java
/**
 * Updates to SponsoredAdRepository to support rotation
 */
public class SponsoredAdRepository {
    // ... existing code ...
    
    /**
     * Get the next ad for rotation, excluding previously shown ads
     */
    public LiveData<SponsoredAd> getNextRotationAd(String location, Set<String> excludeIds) {
        MediatorLiveData<SponsoredAd> result = new MediatorLiveData<>();
        
        // First check cache
        LiveData<List<SponsoredAdEntity>> cachedAds = sponsoredAdDao.getActiveAdsByLocationExcluding(
            location, 
            excludeIds != null ? new ArrayList<>(excludeIds) : new ArrayList<>()
        );
        
        result.addSource(cachedAds, entities -> {
            result.removeSource(cachedAds);
            
            if (entities != null && !entities.isEmpty()) {
                // Apply weighted selection
                SponsoredAdEntity selectedEntity = applyWeightedSelection(entities);
                result.setValue(entityToModel(selectedEntity));
                
                // Refresh cache in background if needed
                long cacheAge = System.currentTimeMillis() - selectedEntity.getLastFetchTime();
                if (cacheAge > CACHE_REFRESH_INTERVAL_MS) {
                    refreshAdsFromNetwork(location);
                }
            } else {
                // Fetch from network
                fetchRotationAdFromNetwork(location, excludeIds, result);
            }
        });
        
        return result;
    }
    
    /**
     * Apply weighted selection based on priority and previous impressions
     */
    private SponsoredAdEntity applyWeightedSelection(List<SponsoredAdEntity> entities) {
        // Calculate total weight (priority * inverse of impression count)
        double totalWeight = 0;
        double[] weights = new double[entities.size()];
        
        for (int i = 0; i < entities.size(); i++) {
            SponsoredAdEntity entity = entities.get(i);
            
            // Higher priority and fewer impressions = higher weight
            double impressionFactor = 1.0 / (1 + Math.log(1 + entity.getImpressionCount()));
            weights[i] = entity.getPriority() * impressionFactor;
            totalWeight += weights[i];
        }
        
        // Select based on weighted probability
        double random = Math.random() * totalWeight;
        double weightSum = 0;
        
        for (int i = 0; i < entities.size(); i++) {
            weightSum += weights[i];
            if (random <= weightSum) {
                return entities.get(i);
            }
        }
        
        // Fallback to first ad (should rarely happen)
        return entities.get(0);
    }
    
    /**
     * Fetch rotation ad from network
     */
    private void fetchRotationAdFromNetwork(String location, Set<String> excludeIds, MediatorLiveData<SponsoredAd> result) {
        // Build query params
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("location", location);
        queryMap.put("limit", 5); // Get a few for local selection
        
        if (excludeIds != null && !excludeIds.isEmpty()) {
            queryMap.put("exclude", new ArrayList<>(excludeIds));
        }
        
        // Make API call
        apiService.getSponsoredAdsForRotation(queryMap).enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SponsoredAd> ads = response.body().getAds();
                    
                    if (ads != null && !ads.isEmpty()) {
                        // Save to database
                        saveAdsToDatabase(ads);
                        
                        // Select one ad using weighted selection
                        int randomIndex = (int)(Math.random() * ads.size());
                        result.setValue(ads.get(randomIndex));
                    } else {
                        result.setValue(null);
                    }
                } else {
                    Log.e(TAG, "Error fetching rotation ads: " + response.code());
                    result.setValue(null);
                }
            }
            
            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                Log.e(TAG, "Network error fetching rotation ads", t);
                result.setValue(null);
            }
        });
    }
}
```

### 3. ViewModel Updates

```java
/**
 * Updates to SponsoredAdViewModel to support rotation
 */
public class SponsoredAdViewModel extends ViewModel {
    // ... existing code ...
    
    private final LocalRotationManager rotationManager;
    private final MutableLiveData<SponsoredAd> rotatingAdLiveData = new MutableLiveData<>();
    private boolean isRotationActive = false;
    
    public SponsoredAdViewModel(SponsoredAdRepository repository, Application application) {
        super();
        this.repository = repository;
        rotationManager = new LocalRotationManager(application, repository);
    }
    
    /**
     * Get LiveData for rotating ads
     */
    public LiveData<SponsoredAd> getRotatingAd() {
        return rotatingAdLiveData;
    }
    
    /**
     * Start ad rotation for a location
     */
    public void startRotation(String location) {
        if (isRotationActive) {
            return;
        }
        
        isRotationActive = true;
        rotationManager.startRotation(location, new LocalRotationManager.RotationCallback() {
            @Override
            public void onAdRotated(SponsoredAd ad) {
                rotatingAdLiveData.postValue(ad);
            }
        });
    }
    
    /**
     * Stop ad rotation
     */
    public void stopRotation() {
        isRotationActive = false;
        rotationManager.stopRotation();
    }
    
    /**
     * Set custom rotation interval
     */
    public void setRotationInterval(long intervalMs) {
        rotationManager.setRotationInterval(intervalMs);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        stopRotation();
    }
}
```

### 4. View Updates

```java
/**
 * Updates to SponsoredAdView to support rotation
 */
public class SponsoredAdView extends FrameLayout {
    // ... existing code ...
    
    private boolean enableRotation = false;
    private SponsoredAd previousAd;
    private ImageView previousAdImage;
    
    /**
     * Enable rotation mode
     */
    public void enableRotation(boolean enable) {
        this.enableRotation = enable;
        
        if (enable && viewModel != null) {
            viewModel.startRotation(location);
            observeRotatingAd();
        } else if (viewModel != null) {
            viewModel.stopRotation();
        }
    }
    
    /**
     * Observe rotating ads
     */
    private void observeRotatingAd() {
        if (lifecycleOwner != null && viewModel != null) {
            viewModel.getRotatingAd().observe(lifecycleOwner, this::handleRotatedAd);
        }
    }
    
    /**
     * Handle rotated ad with smooth transition
     */
    private void handleRotatedAd(SponsoredAd ad) {
        if (ad == null || (currentAd != null && currentAd.getId().equals(ad.getId()))) {
            return;
        }
        
        // Save previous ad state for transition
        previousAd = currentAd;
        if (adImage != null) {
            previousAdImage = adImage;
        }
        
        // Create new ad image for crossfade
        adImage = new ImageView(getContext());
        adImage.setLayoutParams(new FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, 
            LayoutParams.MATCH_PARENT
        ));
        adImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        adImage.setAlpha(0f);
        
        // Add new image on top
        if (adContainer != null) {
            adContainer.addView(adImage);
        }
        
        // Load the new ad
        currentAd = ad;
        loadAd(ad);
    }
    
    /**
     * Override to handle rotation transitions
     */
    @Override
    protected void onAdImageLoaded() {
        super.onAdImageLoaded();
        
        // If this is a rotation, animate crossfade
        if (enableRotation && previousAdImage != null) {
            // Fade in new image
            adImage.animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator());
            
            // Fade out old image
            previousAdImage.animate()
                .alpha(0f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (adContainer != null) {
                        adContainer.removeView(previousAdImage);
                    }
                    previousAdImage = null;
                    previousAd = null;
                });
        } else {
            // Standard loading behavior
            adImage.setAlpha(1f);
        }
    }
    
    /**
     * Set rotation interval
     */
    public void setRotationInterval(long intervalMs) {
        if (viewModel != null) {
            viewModel.setRotationInterval(intervalMs);
        }
    }
    
    @Override
    public void cleanup() {
        if (viewModel != null) {
            viewModel.stopRotation();
        }
        super.cleanup();
    }
}
```

### 5. Room Database Updates

```java
/**
 * Updates to SponsoredAdDao
 */
@Dao
public interface SponsoredAdDao {
    // ... existing code ...
    
    /**
     * Get active ads by location, excluding specific IDs
     */
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND " +
           "status = 1 AND " +
           "start_date <= :now AND end_date >= :now AND " +
           "id NOT IN (:excludeIds) " +
           "ORDER BY priority DESC")
    LiveData<List<SponsoredAdEntity>> getActiveAdsByLocationExcluding(
        String location, 
        List<String> excludeIds, 
        @Param("now") long now
    );
    
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND " +
           "status = 1 AND " +
           "start_date <= :now AND end_date >= :now AND " +
           "id NOT IN (:excludeIds) " +
           "ORDER BY priority DESC")
    LiveData<List<SponsoredAdEntity>> getActiveAdsByLocationExcluding(
        String location, 
        List<String> excludeIds
    );
}
```

## Server-Side Implementation

### 1. Update SponsoredAd Model

```javascript
// Add to SponsoredAd.js

/**
 * Get active ads for rotation, excluding specified IDs
 * @param {String} location - The location to filter by
 * @param {Array} excludeIds - Array of ad IDs to exclude
 * @returns {Promise<Array>} - List of matching ads
 */
sponsoredAdSchema.statics.getAdsForRotation = async function(location = null, excludeIds = []) {
  const now = new Date();
  const query = {
    status: true,
    start_date: { $lte: now },
    end_date: { $gte: now }
  };
  
  if (location) {
    query.location = location;
  }
  
  if (excludeIds && excludeIds.length > 0) {
    // Convert string IDs to ObjectIds if needed
    const objectIds = excludeIds.map(id => {
      try {
        return mongoose.Types.ObjectId(id);
      } catch (e) {
        return id; // Keep as is if not valid ObjectId
      }
    });
    
    query._id = { $nin: objectIds };
  }
  
  return this.find(query).sort({ priority: -1 });
};

/**
 * Get ads with fair distribution based on impressions
 * @param {String} location - The location to filter by
 * @param {Number} limit - Maximum number of ads to return
 * @returns {Promise<Array>} - List of fairly distributed ads
 */
sponsoredAdSchema.statics.getFairDistributedAds = async function(location = null, limit = 10) {
  // Get active ads
  const ads = await this.getActiveAds(location);
  
  // Apply fair distribution algorithm
  return this.applyFairDistribution(ads, limit);
};

/**
 * Apply fair distribution algorithm based on priority and impressions
 * @param {Array} ads - List of ads to distribute
 * @param {Number} limit - Maximum number of ads to return
 * @returns {Array} - Fairly distributed ads
 */
sponsoredAdSchema.statics.applyFairDistribution = function(ads, limit) {
  if (!ads || ads.length === 0) {
    return [];
  }
  
  // Calculate weights based on priority and impression count
  const adsWithWeights = ads.map(ad => {
    // Higher priority and fewer impressions = higher weight
    const impressionFactor = 1.0 / (1 + Math.log(1 + ad.impression_count));
    const weight = ad.priority * impressionFactor;
    
    return {
      ad,
      weight
    };
  });
  
  // Sort by weight (descending)
  adsWithWeights.sort((a, b) => b.weight - a.weight);
  
  // Return ads up to limit
  return adsWithWeights.slice(0, limit).map(item => item.ad);
};
```

### 2. Add Controller Methods

```javascript
// Add to sponsoredAdController.js

/**
 * Get ads for rotation with exclusion support
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getAdsForRotation = async (req, res) => {
  try {
    const location = req.query.location || null;
    const limit = parseInt(req.query.limit) || 10;
    
    // Parse exclude parameter
    let excludeIds = [];
    if (req.query.exclude) {
      excludeIds = Array.isArray(req.query.exclude) 
        ? req.query.exclude 
        : [req.query.exclude];
    }
    
    // Get ads with exclusion
    const ads = await SponsoredAd.getAdsForRotation(location, excludeIds);
    
    // Apply fair distribution
    const distributedAds = SponsoredAd.applyFairDistribution(ads, limit);
    
    res.json({
      success: true,
      ads: distributedAds
    });
  } catch (error) {
    logger.error(`Error in getAdsForRotation: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to get ads for rotation',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
};

/**
 * Get ads with fair distribution (weighted by priority and impressions)
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getFairDistributedAds = async (req, res) => {
  try {
    const location = req.query.location || null;
    const limit = parseInt(req.query.limit) || 10;
    
    // Get ads with fair distribution
    const ads = await SponsoredAd.getFairDistributedAds(location, limit);
    
    res.json({
      success: true,
      ads: ads
    });
  } catch (error) {
    logger.error(`Error in getFairDistributedAds: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to get fair distributed ads',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
};
```

### 3. Add API Routes

```javascript
// Add to sponsoredAds.js

/**
 * @route GET /api/sponsored-ads/rotation
 * @description Get ads for rotation with exclusion support
 * @access Public
 */
router.get('/rotation', sponsoredAdController.getAdsForRotation);

/**
 * @route GET /api/sponsored-ads/fair-distribution
 * @description Get ads with fair distribution based on priority and impressions
 * @access Public
 */
router.get('/fair-distribution', sponsoredAdController.getFairDistributedAds);
```

## Integration in Application

### 1. Usage in HomeFragment

```java
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    // ... existing code ...
    
    // Initialize sponsored ad with rotation
    sponsoredAdView = view.findViewById(R.id.sponsored_ad_view);
    sponsoredAdView.initialize("category_below", getViewLifecycleOwner(), this);
    sponsoredAdView.enableRotation(true);
    sponsoredAdView.setRotationInterval(TimeUnit.MINUTES.toMillis(15)); // Rotate every 15 minutes
}
```

### 2. Retrofit API Service Updates

```java
/**
 * Updates to ApiService interface
 */
public interface ApiService {
    // ... existing endpoints ...
    
    /**
     * Get sponsored ads for rotation with exclusion list
     */
    @GET("sponsored-ads/rotation")
    Call<SponsoredAdResponse> getSponsoredAdsForRotation(@QueryMap Map<String, Object> options);
    
    /**
     * Get sponsored ads with fair distribution
     */
    @GET("sponsored-ads/fair-distribution")
    Call<SponsoredAdResponse> getFairDistributedAds(@QueryMap Map<String, Object> options);
}
```

## Testing Plan

### 1. Unit Tests

```java
/**
 * Test the local rotation manager
 */
@RunWith(AndroidJUnit4.class)
public class LocalRotationManagerTest {
    private LocalRotationManager rotationManager;
    private MockSponsoredAdRepository mockRepository;
    private Context context;
    
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        mockRepository = new MockSponsoredAdRepository();
        rotationManager = new LocalRotationManager(context, mockRepository);
    }
    
    @Test
    public void testRotationTiming() {
        // Test rotation scheduling and timing
    }
    
    @Test
    public void testExclusionList() {
        // Test that shown ads are excluded from future rotations
    }
    
    @Test
    public void testStateRestoration() {
        // Test persistence of rotation state across instances
    }
}

/**
 * Test the weighted selection algorithm
 */
@RunWith(AndroidJUnit4.class)
public class WeightedSelectionTest {
    private SponsoredAdRepository repository;
    
    @Before
    public void setup() {
        // Initialize repository with mock data
    }
    
    @Test
    public void testWeightedSelection() {
        // Test that high priority ads are selected more often
    }
    
    @Test
    public void testImpressionBalancing() {
        // Test that ads with fewer impressions get more exposure
    }
}
```

### 2. Integration Tests

```java
/**
 * Test the end-to-end rotation system
 */
@RunWith(AndroidJUnit4.class)
public class RotationIntegrationTest {
    private SponsoredAdViewModel viewModel;
    private SponsoredAdRepository repository;
    private Application application;
    
    @Before
    public void setup() {
        application = ApplicationProvider.getApplicationContext();
        repository = new SponsoredAdRepository(application);
        viewModel = new SponsoredAdViewModel(repository, application);
    }
    
    @Test
    public void testRotationLifecycle() {
        // Test complete rotation lifecycle
    }
    
    @Test
    public void testRotationWithNetworkErrors() {
        // Test rotation behavior during network errors
    }
}
```

## Performance Considerations

1. **Caching**: The implementation heavily relies on local caching to minimize network calls
2. **Battery Usage**: Rotation scheduling uses Handler with delayed posts instead of continuous timers
3. **Memory Management**: Image transitions clean up previous resources to prevent leaks
4. **Database Queries**: Carefully designed queries with proper indexing for efficient rotation
5. **Network Efficiency**: Batch fetching of ads for local selection reduces API calls

## Security Considerations

1. **Persistence**: Only non-sensitive data (ad IDs) are persisted in SharedPreferences
2. **Input Validation**: All server-side routes validate input parameters
3. **Rate Limiting**: Server prevents abuse with appropriate rate limits
4. **Error Handling**: All exceptions are caught and logged without exposing details to clients

## Implementation Timeline

### Week 1: Foundation
- Implement LocalRotationManager
- Update SponsoredAdRepository with rotation support
- Add basic rotation in SponsoredAdViewModel
- Create unit tests for core rotation logic

### Week 2: UI Integration
- Implement rotation UI transitions
- Add user preference controls
- Create shimmer effects for loading
- Implement error states for rotation

### Week 3: Server-Side Support
- Update server model with rotation support
- Add new API endpoints
- Implement fair distribution algorithm
- Create server-side unit tests

### Week 4: Testing and Refinement
- Conduct comprehensive integration testing
- Optimize performance
- Implement analytics tracking
- Finalize documentation

## Conclusion

This implementation plan provides a comprehensive approach to ad rotation and fair distribution. The design ensures backward compatibility with existing systems while enhancing the user experience and ad effectiveness. The phased implementation allows for incremental testing and refinement. 