package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.ResourceDatabase;
import com.ds.eventwish.data.local.dao.ResourceDao;
import com.ds.eventwish.data.local.entity.ResourceEntity;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.model.ResourceType;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiResponse;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.ErrorHandler;
import com.ds.eventwish.utils.NetworkErrorHandler;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.data.cache.ResourceCache;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for loading resources with caching and offline support
 */
public class ResourceRepository {
    private static final String TAG = "ResourceRepository";
    
    // Cache expiration times
    private static final long DEFAULT_CACHE_EXPIRATION = TimeUnit.HOURS.toMillis(1); // 1 hour
    private static final long LONG_CACHE_EXPIRATION = TimeUnit.DAYS.toMillis(1); // 1 day
    
    // ETag header
    private static final String HEADER_ETAG = "ETag";
    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    
    // Resource types
    public static final String RESOURCE_TYPE_TEMPLATE = "template";
    public static final String RESOURCE_TYPE_CATEGORY = "category";
    public static final String RESOURCE_TYPE_ICON = "icon";
    public static final String RESOURCE_TYPE_CATEGORY_ICON = "category_icon";
    
    // Singleton instance
    private static volatile ResourceRepository instance;
    
    // Dependencies
    private final ResourceDao resourceDao;
    private final ApiService apiService;
    private final AppExecutors appExecutors;
    private final NetworkUtils networkUtils;
    private final ResourceCache resourceCache;
    private final Gson gson;
    private final ErrorHandler errorHandler;
    private final Context context;
    
    // ETag cache
    private final Map<String, String> etagCache = new HashMap<>();
    
    /**
     * Get the singleton instance of ResourceRepository
     * @param context Application context
     * @return ResourceRepository instance
     */
    public static ResourceRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ResourceRepository.class) {
                if (instance == null) {
                    instance = new ResourceRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor to initialize dependencies
     * @param context Application context
     */
    private ResourceRepository(Context context) {
        this.context = context.getApplicationContext();
        ResourceDatabase database = ResourceDatabase.getInstance(context);
        resourceDao = database.resourceDao();
        apiService = ApiClient.getClient();
        appExecutors = AppExecutors.getInstance();
        networkUtils = NetworkUtils.getInstance(context);
        resourceCache = ResourceCache.getInstance(context);
        gson = new Gson();
        errorHandler = ErrorHandler.getInstance(context);
        
        // Clean up expired resources periodically
        cleanupExpiredResources();
        
        Log.d(TAG, "ResourceRepository initialized");
    }
    
    /**
     * Load a resource with caching and offline support
     * @param type Resource type
     * @param id Resource ID
     * @param forceRefresh Whether to force a refresh from network
     * @return LiveData with Resource containing the data
     */
    public LiveData<Resource<JsonObject>> loadResource(ResourceType type, String id, boolean forceRefresh) {
        MutableLiveData<Resource<JsonObject>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        String cacheKey = getCacheKey(type, id);
        
        // First try to get from cache
        appExecutors.diskIO().execute(() -> {
            JsonObject cachedData = resourceCache.get(cacheKey);
            
            if (cachedData != null && !forceRefresh) {
                Log.d(TAG, "Loaded resource from cache: " + cacheKey);
                result.postValue(Resource.success(cachedData));
                
                // If network is available and not metered, check for updates in background
                if (networkUtils.isConnected() && !networkUtils.isConnectionMetered()) {
                    refreshResourceInBackground(type, id, result);
                }
            } else {
                // If not in cache or force refresh, load from network
                if (networkUtils.isConnected()) {
                    loadResourceFromNetwork(type, id, result);
                } else {
                    // If offline and no cache, return error
                    if (cachedData != null) {
                        Log.d(TAG, "Offline, using stale cached data: " + cacheKey);
                        result.postValue(Resource.success(cachedData, true));
                    } else {
                        Log.e(TAG, "Offline and no cached data available: " + cacheKey);
                        result.postValue(Resource.error("No internet connection and no cached data available.", null));
                        errorHandler.handleError(
                                ErrorHandler.ErrorType.OFFLINE,
                                "No internet connection and no cached data available for " + type.getKey() + " " + id,
                                ErrorHandler.ErrorSeverity.MEDIUM);
                    }
                }
            }
        });
        
        return result;
    }
    
    /**
     * Load a resource from network
     * @param type Resource type
     * @param id Resource ID
     * @param result LiveData to update with the result
     */
    private void loadResourceFromNetwork(ResourceType type, String id, MutableLiveData<Resource<JsonObject>> result) {
        String cacheKey = getCacheKey(type, id);
        String etag = etagCache.get(cacheKey);
        
        // Build headers with ETag if available
        Map<String, String> headers = new HashMap<>();
        if (etag != null) {
            headers.put(HEADER_IF_NONE_MATCH, etag);
        }
        
        Call<JsonObject> call = apiService.getResource(type.getApiPath(), id, headers);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    
                    // Cache the response
                    appExecutors.diskIO().execute(() -> {
                        resourceCache.put(cacheKey, data, getCacheExpiration(type));
                        
                        // Save ETag if available
                        Headers responseHeaders = response.headers();
                        String newEtag = responseHeaders.get(HEADER_ETAG);
                        if (newEtag != null) {
                            etagCache.put(cacheKey, newEtag);
                        }
                    });
                    
                    result.postValue(Resource.success(data));
                } else if (response.code() == 304) {
                    // Not modified, use cached data
                    Log.d(TAG, "Resource not modified (304): " + cacheKey);
                    appExecutors.diskIO().execute(() -> {
                        JsonObject cachedData = resourceCache.get(cacheKey);
                        if (cachedData != null) {
                            // Cast the data to JsonObject if needed
                            Object data = cachedData;
                            JsonObject jsonData = (data instanceof JsonObject) ? (JsonObject) data : null;
                            result.postValue(Resource.success(jsonData));
                        } else {
                            // This shouldn't happen, but just in case
                            result.postValue(Resource.error("Resource not modified but not in cache", null));
                        }
                    });
                } else {
                    String errorMsg = "Failed to load resource";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    
                    // Try to get from cache as fallback
                    final String finalErrorMsg = errorMsg;
                    appExecutors.diskIO().execute(() -> {
                        JsonObject cachedData = resourceCache.get(cacheKey);
                        if (cachedData != null) {
                            Log.d(TAG, "API error, using cache: " + cacheKey);
                            // Cast the data to JsonObject if needed
                            Object data = cachedData;
                            JsonObject jsonData = (data instanceof JsonObject) ? (JsonObject) data : null;
                            result.postValue(Resource.success(jsonData, true));
                        } else {
                            result.postValue(Resource.error(finalErrorMsg, null));
                            errorHandler.handleError(
                                    ErrorHandler.ErrorType.SERVER_ERROR,
                                    finalErrorMsg,
                                    ErrorHandler.ErrorSeverity.MEDIUM);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // Try to get from cache as fallback
                appExecutors.diskIO().execute(() -> {
                    JsonObject cachedData = resourceCache.get(cacheKey);
                    if (cachedData != null) {
                        Log.d(TAG, "Network request failed, using cache: " + cacheKey);
                        // Cast the data to JsonObject if needed
                        Object data = cachedData;
                        JsonObject jsonData = (data instanceof JsonObject) ? (JsonObject) data : null;
                        result.postValue(Resource.success(jsonData, true));
                    } else {
                        String errorMsg = NetworkErrorHandler.getErrorMessage(context, t);
                        result.postValue(Resource.error(errorMsg, null));
                        NetworkErrorHandler.handleNetworkError(context, t, errorHandler);
                    }
                });
            }
        });
    }
    
    /**
     * Refresh a resource in background without updating loading state
     * @param type Resource type
     * @param id Resource ID
     * @param result LiveData to update with the result
     */
    private void refreshResourceInBackground(ResourceType type, String id, MutableLiveData<Resource<JsonObject>> result) {
        String cacheKey = getCacheKey(type, id);
        String etag = etagCache.get(cacheKey);
        
        // Build headers with ETag if available
        Map<String, String> headers = new HashMap<>();
        if (etag != null) {
            headers.put(HEADER_IF_NONE_MATCH, etag);
        }
        
        Call<JsonObject> call = apiService.getResource(type.getApiPath(), id, headers);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    
                    // Cache the response
                    appExecutors.diskIO().execute(() -> {
                        resourceCache.put(cacheKey, data, getCacheExpiration(type));
                        
                        // Save ETag if available
                        Headers responseHeaders = response.headers();
                        String newEtag = responseHeaders.get(HEADER_ETAG);
                        if (newEtag != null) {
                            etagCache.put(cacheKey, newEtag);
                        }
                        
                        // Only update LiveData if it's not in loading state
                        if (result.getValue() != null && !result.getValue().isLoading()) {
                            // Cast the data to JsonObject if needed
                            Object dataToPost = data;
                            JsonObject jsonData = (dataToPost instanceof JsonObject) ? (JsonObject) dataToPost : null;
                            result.postValue(Resource.success(jsonData));
                        }
                    });
                } else if (response.code() != 304) {
                    // Only log errors, don't update LiveData
                    Log.e(TAG, "Background refresh failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Background refresh failed", t);
            }
        });
    }
    
    /**
     * Get cache key for a resource
     * @param type Resource type
     * @param id Resource ID
     * @return Cache key
     */
    private String getCacheKey(ResourceType type, String id) {
        return type.getCachePrefix() + id;
    }
    
    /**
     * Get cache key for a resource
     * @param resourceType Resource type as string
     * @param id Resource ID
     * @return Cache key
     */
    private String getCacheKey(String resourceType, String id) {
        return ResourceType.fromString(resourceType).getCachePrefix() + id;
    }
    
    /**
     * Get cache expiration time for a resource type
     * @param type Resource type
     * @return Cache expiration time in milliseconds
     */
    private long getCacheExpiration(ResourceType type) {
        switch (type) {
            case TEMPLATE:
            case CATEGORY:
            case ICON:
                return LONG_CACHE_EXPIRATION;
            default:
                return DEFAULT_CACHE_EXPIRATION;
        }
    }
    
    /**
     * Clear the cache for a specific resource
     * @param type Resource type
     * @param id Resource ID
     */
    public void clearCache(ResourceType type, String id) {
        String cacheKey = getCacheKey(type, id);
        appExecutors.diskIO().execute(() -> {
            resourceCache.remove(cacheKey);
            etagCache.remove(cacheKey);
        });
    }
    
    /**
     * Clear the cache for all resources of a specific type
     * @param type Resource type
     */
    public void clearCache(ResourceType type) {
        appExecutors.diskIO().execute(() -> {
            resourceCache.removeByPrefix(type.getCachePrefix());
            
            // Remove ETags for this type
            synchronized (etagCache) {
                String prefix = type.getCachePrefix();
                etagCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
            }
        });
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        appExecutors.diskIO().execute(() -> {
            resourceCache.clear();
            etagCache.clear();
        });
    }
    
    /**
     * Get a resource by type and key with offline-first approach
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @param forceRefresh Whether to force a refresh from network
     * @return LiveData with resource data
     */
    public LiveData<Resource<JsonObject>> getResource(
            final String resourceType,
            final String resourceKey,
            final boolean forceRefresh) {
        
        final MediatorLiveData<Resource<JsonObject>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading(null));
        
        final String cacheKey = getCacheKey(resourceType, resourceKey);
        
        try {
            // Check memory cache first
            JsonObject cachedData = null;
            Object cachedObject = resourceCache.get(cacheKey);
            
            if (cachedObject != null) {
                try {
                    if (cachedObject instanceof JsonObject) {
                        cachedData = (JsonObject) cachedObject;
                        Log.d(TAG, "Found JsonObject in memory cache for: " + cacheKey);
                    } else if (cachedObject instanceof LinkedTreeMap) {
                        // Convert LinkedTreeMap to JsonObject
                        Log.d(TAG, "Converting LinkedTreeMap to JsonObject in memory cache for: " + cacheKey);
                        @SuppressWarnings("unchecked")
                        LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) cachedObject;
                        cachedData = convertMapToJsonObject(map);
                        // Update cache with converted object for future use
                        resourceCache.put(cacheKey, cachedData, getCacheExpiration(ResourceType.fromString(resourceType)));
                    } else if (cachedObject instanceof String) {
                        // Try to parse string as JSON
                        Log.d(TAG, "Converting String to JsonObject in memory cache for: " + cacheKey);
                        String jsonString = (String) cachedObject;
                        cachedData = JsonParser.parseString(jsonString).getAsJsonObject();
                        // Update cache with converted object for future use
                        resourceCache.put(cacheKey, cachedData, getCacheExpiration(ResourceType.fromString(resourceType)));
                    } else {
                        // For other types, try to convert via Gson
                        Log.d(TAG, "Converting unknown type to JsonObject in memory cache for: " + cacheKey + ", type: " + cachedObject.getClass().getName());
                        String json = gson.toJson(cachedObject);
                        cachedData = JsonParser.parseString(json).getAsJsonObject();
                        // Update cache with converted object for future use
                        resourceCache.put(cacheKey, cachedData, getCacheExpiration(ResourceType.fromString(resourceType)));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error converting cached object to JsonObject: " + e.getMessage(), e);
                    // Continue with database lookup
                }
            }
            
            if (cachedData != null && !forceRefresh) {
                result.setValue(Resource.success(cachedData));
                Log.d(TAG, "Resource found in memory cache: " + cacheKey);
                return result;
            }
            
            // Observe database for changes
            final LiveData<ResourceEntity> dbSource = resourceDao.getResourceLiveData(resourceType, resourceKey);
            result.addSource(dbSource, resourceEntity -> {
                if (resourceEntity != null && !forceRefresh && !resourceEntity.isStale() && !resourceEntity.isExpired()) {
                    try {
                        // Handle potential LinkedTreeMap to JsonObject conversion
                        Object rawData = resourceEntity.getData();
                        JsonObject data = null;
                        
                        if (rawData == null) {
                            Log.e(TAG, "Error: Data is null for resource: " + cacheKey);
                            result.setValue(Resource.error("Error loading resource data: Data is null", null));
                            fetchFromNetwork(resourceType, resourceKey, result, dbSource);
                            return;
                        }
                        
                        try {
                            if (rawData instanceof JsonObject) {
                                // If it's already a JsonObject, use it directly
                                data = (JsonObject) rawData;
                                Log.d(TAG, "Found JsonObject in database for: " + cacheKey);
                            } else if (rawData instanceof LinkedTreeMap) {
                                // If it's a LinkedTreeMap, convert it to JsonObject
                                Log.d(TAG, "Converting LinkedTreeMap to JsonObject in database for: " + cacheKey);
                                @SuppressWarnings("unchecked")
                                LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) rawData;
                                data = convertMapToJsonObject(map);
                                
                                // Update the entity with the converted JsonObject for future use
                                resourceEntity.setData(data);
                                appExecutors.diskIO().execute(() -> {
                                    resourceDao.update(resourceEntity);
                                });
                            } else if (rawData instanceof String) {
                                // Try to parse string as JSON
                                Log.d(TAG, "Converting String to JsonObject in database for: " + cacheKey);
                                String jsonString = (String) rawData;
                                data = JsonParser.parseString(jsonString).getAsJsonObject();
                                
                                // Update the entity with the converted JsonObject for future use
                                resourceEntity.setData(data);
                                appExecutors.diskIO().execute(() -> {
                                    resourceDao.update(resourceEntity);
                                });
                            } else {
                                // For other types, try to convert via Gson
                                Log.d(TAG, "Converting unknown type to JsonObject in database for: " + cacheKey + ", type: " + rawData.getClass().getName());
                                String json = gson.toJson(rawData);
                                data = JsonParser.parseString(json).getAsJsonObject();
                                
                                // Update the entity with the converted JsonObject for future use
                                resourceEntity.setData(data);
                                appExecutors.diskIO().execute(() -> {
                                    resourceDao.update(resourceEntity);
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting database object to JsonObject: " + e.getMessage(), e);
                            result.setValue(Resource.error("Error processing resource data: " + e.getMessage(), null));
                            fetchFromNetwork(resourceType, resourceKey, result, dbSource);
                            return;
                        }
                        
                        if (data != null) {
                            result.setValue(Resource.success(data));
                            Log.d(TAG, "Resource found in database: " + cacheKey);
                            
                            // Cache in memory
                            resourceCache.put(cacheKey, data, getCacheExpiration(ResourceType.fromString(resourceType)));
                            
                            // If we don't need to force refresh, we're done
                            if (!forceRefresh) {
                                return;
                            }
                        } else {
                            // If data is null, it might be due to a conversion issue
                            Log.e(TAG, "Error: Data is null or could not be converted for resource: " + cacheKey);
                            result.setValue(Resource.error("Error loading resource data", null));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing resource data: " + e.getMessage(), e);
                        result.setValue(Resource.error("Error processing resource data: " + e.getMessage(), null));
                    }
                }
                
                // If we got here, we need to fetch from network
                fetchFromNetwork(resourceType, resourceKey, result, dbSource);
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in getResource: " + e.getMessage(), e);
            result.setValue(Resource.error("Unexpected error: " + e.getMessage(), null));
        }
        
        return result;
    }
    
    /**
     * Convert a LinkedTreeMap to JsonObject
     * @param map LinkedTreeMap to convert
     * @return Converted JsonObject
     */
    private JsonObject convertMapToJsonObject(LinkedTreeMap<String, Object> map) {
        if (map == null) {
            Log.e(TAG, "Cannot convert null map to JsonObject");
            return new JsonObject();
        }
        
        JsonObject jsonObject = new JsonObject();
        
        try {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (key == null) {
                    Log.w(TAG, "Skipping null key in map conversion");
                    continue;
                }
                
                try {
                    if (value == null) {
                        jsonObject.add(key, null);
                    } else if (value instanceof String) {
                        jsonObject.addProperty(key, (String) value);
                    } else if (value instanceof Number) {
                        jsonObject.addProperty(key, (Number) value);
                    } else if (value instanceof Boolean) {
                        jsonObject.addProperty(key, (Boolean) value);
                    } else if (value instanceof LinkedTreeMap) {
                        // Recursively convert nested maps
                        @SuppressWarnings("unchecked")
                        LinkedTreeMap<String, Object> nestedMap = (LinkedTreeMap<String, Object>) value;
                        jsonObject.add(key, convertMapToJsonObject(nestedMap));
                    } else if (value instanceof List) {
                        // Convert list to JsonArray
                        jsonObject.add(key, convertListToJsonArray((List<?>) value));
                    } else {
                        // For other types, try to convert via Gson
                        try {
                            String json = gson.toJson(value);
                            if (json.startsWith("{") && json.endsWith("}")) {
                                jsonObject.add(key, JsonParser.parseString(json).getAsJsonObject());
                            } else if (json.startsWith("[") && json.endsWith("]")) {
                                jsonObject.add(key, JsonParser.parseString(json).getAsJsonArray());
                            } else {
                                // For simple values, convert to string
                                jsonObject.addProperty(key, json);
                            }
                        } catch (Exception e) {
                            // If all else fails, use toString
                            Log.w(TAG, "Using toString() for complex value in key: " + key + ", type: " + value.getClass().getName());
                            jsonObject.addProperty(key, value.toString());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error converting value for key: " + key + ", error: " + e.getMessage(), e);
                    // Skip this entry but continue with others
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in convertMapToJsonObject: " + e.getMessage(), e);
            // Return whatever we've built so far
        }
        
        return jsonObject;
    }
    
    /**
     * Convert a List to JsonArray
     * @param list List to convert
     * @return Converted JsonArray
     */
    private com.google.gson.JsonArray convertListToJsonArray(List<?> list) {
        if (list == null) {
            Log.e(TAG, "Cannot convert null list to JsonArray");
            return new com.google.gson.JsonArray();
        }
        
        com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
        
        try {
            for (Object item : list) {
                try {
                    if (item == null) {
                        jsonArray.add((String) null);
                    } else if (item instanceof String) {
                        jsonArray.add((String) item);
                    } else if (item instanceof Number) {
                        jsonArray.add((Number) item);
                    } else if (item instanceof Boolean) {
                        jsonArray.add((Boolean) item);
                    } else if (item instanceof LinkedTreeMap) {
                        // Recursively convert nested maps
                        @SuppressWarnings("unchecked")
                        LinkedTreeMap<String, Object> map = (LinkedTreeMap<String, Object>) item;
                        jsonArray.add(convertMapToJsonObject(map));
                    } else if (item instanceof List) {
                        // Recursively convert nested lists
                        jsonArray.add(convertListToJsonArray((List<?>) item));
                    } else {
                        // For other types, try to convert via Gson
                        try {
                            String json = gson.toJson(item);
                            if (json.startsWith("{") && json.endsWith("}")) {
                                jsonArray.add(JsonParser.parseString(json).getAsJsonObject());
                            } else if (json.startsWith("[") && json.endsWith("]")) {
                                jsonArray.add(JsonParser.parseString(json).getAsJsonArray());
                            } else {
                                // For simple values, convert to string
                                jsonArray.add(json);
                            }
                        } catch (Exception e) {
                            // If all else fails, use toString
                            Log.w(TAG, "Using toString() for complex value in array, type: " + item.getClass().getName());
                            jsonArray.add(item.toString());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error converting list item: " + e.getMessage(), e);
                    // Skip this item but continue with others
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in convertListToJsonArray: " + e.getMessage(), e);
            // Return whatever we've built so far
        }
        
        return jsonArray;
    }
    
    /**
     * Get all resources of a specific type
     * @param resourceType Resource type
     * @param forceRefresh Whether to force a refresh from network
     * @return LiveData with list of resources
     */
    public LiveData<Resource<List<ResourceEntity>>> getResourcesByType(
            final String resourceType,
            final boolean forceRefresh) {
        
        final MediatorLiveData<Resource<List<ResourceEntity>>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading(null));
        
        // Observe database for changes
        final LiveData<List<ResourceEntity>> dbSource = resourceDao.getResourcesByTypeLiveData(resourceType);
        result.addSource(dbSource, resourceEntities -> {
            if (resourceEntities != null && !resourceEntities.isEmpty() && !forceRefresh) {
                result.setValue(Resource.success(resourceEntities));
                Log.d(TAG, "Resources found in database: " + resourceType + " (count: " + resourceEntities.size() + ")");
                
                // If we don't need to force refresh, we're done
                if (!forceRefresh) {
                    return;
                }
            }
            
            // If we got here, we need to fetch from network
            fetchAllFromNetwork(resourceType, result, dbSource);
        });
        
        return result;
    }
    
    /**
     * Save a resource to local storage
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @param data Resource data
     * @param metadata Additional metadata
     * @param etag ETag for HTTP caching
     * @param expirationTime Expiration time
     */
    public void saveResource(
            final String resourceType,
            final String resourceKey,
            final JsonObject data,
            final Map<String, String> metadata,
            final String etag,
            final Date expirationTime) {
        
        appExecutors.diskIO().execute(() -> {
            try {
                // Create or update resource entity
                ResourceEntity existingResource = resourceDao.getResource(resourceType, resourceKey);
                
                if (existingResource != null) {
                    // Update existing resource
                    existingResource.update(data, etag, expirationTime);
                    existingResource.setMetadata(metadata);
                    resourceDao.update(existingResource);
                    Log.d(TAG, "Resource updated in database: " + resourceType + ":" + resourceKey);
                } else {
                    // Create new resource
                    ResourceEntity newResource = new ResourceEntity(
                            resourceType,
                            resourceKey,
                            data,
                            metadata,
                            expirationTime,
                            etag
                    );
                    resourceDao.insert(newResource);
                    Log.d(TAG, "Resource inserted in database: " + resourceType + ":" + resourceKey);
                }
                
                // Cache in memory
                final String cacheKey = getCacheKey(resourceType, resourceKey);
                resourceCache.put(cacheKey, data, getCacheExpiration(ResourceType.fromString(resourceType)));
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving resource: " + resourceType + ":" + resourceKey, e);
            }
        });
    }
    
    /**
     * Mark resources as stale by type
     * @param resourceType Resource type
     */
    public void markResourcesAsStale(final String resourceType) {
        appExecutors.diskIO().execute(() -> {
            try {
                int count = resourceDao.markResourcesAsStaleByType(resourceType);
                Log.d(TAG, "Marked " + count + " resources as stale: " + resourceType);
            } catch (Exception e) {
                Log.e(TAG, "Error marking resources as stale: " + resourceType, e);
            }
        });
    }
    
    /**
     * Delete resources by type
     * @param resourceType Resource type
     */
    public void deleteResourcesByType(final String resourceType) {
        appExecutors.diskIO().execute(() -> {
            try {
                int count = resourceDao.deleteResourcesByType(resourceType);
                Log.d(TAG, "Deleted " + count + " resources: " + resourceType);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting resources: " + resourceType, e);
            }
        });
    }
    
    /**
     * Clean up expired resources
     */
    private void cleanupExpiredResources() {
        appExecutors.diskIO().execute(() -> {
            try {
                int count = resourceDao.deleteExpiredResources(new Date());
                Log.d(TAG, "Cleaned up " + count + " expired resources");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up expired resources", e);
            }
        });
    }
    
    /**
     * Fetch a resource from network
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @param result MediatorLiveData to update with result
     * @param dbSource Database source to remove when done
     */
    private void fetchFromNetwork(
            final String resourceType,
            final String resourceKey,
            final MediatorLiveData<Resource<JsonObject>> result,
            final LiveData<ResourceEntity> dbSource) {
        
        // Get existing resource for ETag
        appExecutors.diskIO().execute(() -> {
            ResourceEntity existingResource = resourceDao.getResource(resourceType, resourceKey);
            String etag = existingResource != null ? existingResource.getEtag() : null;
            
            // Build headers
            Map<String, String> headers = new HashMap<>();
            if (etag != null) {
                headers.put("If-None-Match", etag);
            }
            
            // Add cache control header based on network conditions
            headers.put("Cache-Control", networkUtils.getCacheControlHeaderValue((int) TimeUnit.MILLISECONDS.toSeconds(DEFAULT_CACHE_EXPIRATION)));
            
            // Make API call
            Call<JsonObject> call = getApiCall(resourceType, resourceKey, headers);
            if (call == null) {
                result.removeSource(dbSource);
                result.setValue(Resource.error("Unsupported resource type: " + resourceType, null));
                return;
            }
            
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    result.removeSource(dbSource);
                    
                    if (response.isSuccessful()) {
                        JsonObject body = response.body();
                        Headers responseHeaders = response.headers();
                        String newEtag = responseHeaders.get("ETag");
                        
                        // Calculate expiration time
                        long expirationMillis = DEFAULT_CACHE_EXPIRATION;
                        String cacheControl = responseHeaders.get("Cache-Control");
                        if (cacheControl != null && cacheControl.contains("max-age=")) {
                            try {
                                String maxAge = cacheControl.split("max-age=")[1].split(",")[0];
                                expirationMillis = TimeUnit.SECONDS.toMillis(Long.parseLong(maxAge));
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing Cache-Control header", e);
                            }
                        }
                        
                        Date expirationTime = new Date(System.currentTimeMillis() + expirationMillis);
                        
                        // Save to database
                        if (body != null) {
                            saveResource(resourceType, resourceKey, body, null, newEtag, expirationTime);
                            result.setValue(Resource.success(body));
                            Log.d(TAG, "Resource fetched from network: " + resourceType + ":" + resourceKey);
                        } else {
                            result.setValue(Resource.error("Empty response body", null));
                            Log.e(TAG, "Empty response body for resource: " + resourceType + ":" + resourceKey);
                        }
                    } else if (response.code() == 304) {
                        // Not modified, use cached data
                        if (existingResource != null) {
                            // Update last updated time
                            existingResource.setLastUpdated(new Date());
                            existingResource.setStale(false);
                            appExecutors.diskIO().execute(() -> resourceDao.update(existingResource));
                            
                            // Cast the data to JsonObject if needed
                            Object data = existingResource.getData();
                            JsonObject jsonData = (data instanceof JsonObject) ? (JsonObject) data : null;
                            result.setValue(Resource.success(jsonData));
                            Log.d(TAG, "Resource not modified (304): " + resourceType + ":" + resourceKey);
                        } else {
                            result.setValue(Resource.error("Resource not found in cache", null));
                            Log.e(TAG, "Resource not found in cache after 304: " + resourceType + ":" + resourceKey);
                        }
                    } else {
                        // Error response
                        ApiResponse<JsonObject> apiResponse = new ApiResponse<>(response);
                        result.setValue(Resource.error(apiResponse.getErrorMessage(), null));
                        Log.e(TAG, "Error fetching resource: " + resourceType + ":" + resourceKey + " - " + apiResponse.getErrorMessage());
                    }
                }
                
                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    result.removeSource(dbSource);
                    
                    // If we have cached data, use it
                    if (existingResource != null) {
                        // Cast the data to JsonObject if needed
                        Object data = existingResource.getData();
                        JsonObject jsonData = (data instanceof JsonObject) ? (JsonObject) data : null;
                        result.setValue(Resource.error("Network error: " + t.getMessage(), jsonData));
                        Log.e(TAG, "Network error, using cached data: " + resourceType + ":" + resourceKey, t);
                    } else {
                        result.setValue(Resource.error("Network error: " + t.getMessage(), null));
                        Log.e(TAG, "Network error, no cached data: " + resourceType + ":" + resourceKey, t);
                    }
                }
            });
        });
    }
    
    /**
     * Fetch all resources of a type from network
     * @param resourceType Resource type
     * @param result MediatorLiveData to update with result
     * @param dbSource Database source to remove when done
     */
    private void fetchAllFromNetwork(
            final String resourceType,
            final MediatorLiveData<Resource<List<ResourceEntity>>> result,
            final LiveData<List<ResourceEntity>> dbSource) {
        
        // Build headers
        Map<String, String> headers = new HashMap<>();
        
        // Add cache control header based on network conditions
        headers.put("Cache-Control", networkUtils.getCacheControlHeaderValue((int) TimeUnit.MILLISECONDS.toSeconds(DEFAULT_CACHE_EXPIRATION)));
        
        // Make API call
        Call<List<JsonObject>> call = getApiCallForAll(resourceType, headers);
        if (call == null) {
            result.removeSource(dbSource);
            result.setValue(Resource.error("Unsupported resource type: " + resourceType, null));
            return;
        }
        
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                result.removeSource(dbSource);
                
                if (response.isSuccessful()) {
                    List<JsonObject> body = response.body();
                    Headers responseHeaders = response.headers();
                    String newEtag = responseHeaders.get("ETag");
                    
                    // Calculate expiration time
                    long expirationMillis = DEFAULT_CACHE_EXPIRATION;
                    String cacheControl = responseHeaders.get("Cache-Control");
                    if (cacheControl != null && cacheControl.contains("max-age=")) {
                        try {
                            String maxAge = cacheControl.split("max-age=")[1].split(",")[0];
                            expirationMillis = TimeUnit.SECONDS.toMillis(Long.parseLong(maxAge));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing Cache-Control header", e);
                        }
                    }
                    
                    Date expirationTime = new Date(System.currentTimeMillis() + expirationMillis);
                    
                    // Save to database
                    if (body != null && !body.isEmpty()) {
                        appExecutors.diskIO().execute(() -> {
                            try {
                                // Mark all existing resources as stale
                                resourceDao.markResourcesAsStaleByType(resourceType);
                                
                                // Process each resource
                                for (JsonObject item : body) {
                                    String resourceKey = getResourceKeyFromItem(resourceType, item);
                                    if (resourceKey != null) {
                                        saveResource(resourceType, resourceKey, item, null, newEtag, expirationTime);
                                    }
                                }
                                
                                // Query updated resources
                                List<ResourceEntity> updatedResources = resourceDao.getResourcesByType(resourceType);
                                appExecutors.mainThread().execute(() -> {
                                    result.setValue(Resource.success(updatedResources));
                                });
                                
                                Log.d(TAG, "Resources fetched from network: " + resourceType + " (count: " + body.size() + ")");
                            } catch (Exception e) {
                                Log.e(TAG, "Error saving resources: " + resourceType, e);
                                appExecutors.mainThread().execute(() -> {
                                    result.setValue(Resource.error("Error saving resources: " + e.getMessage(), null));
                                });
                            }
                        });
                    } else {
                        result.setValue(Resource.error("Empty response body", null));
                        Log.e(TAG, "Empty response body for resources: " + resourceType);
                    }
                } else {
                    // Error response
                    ApiResponse<List<JsonObject>> apiResponse = new ApiResponse<>(response);
                    result.setValue(Resource.error(apiResponse.getErrorMessage(), null));
                    Log.e(TAG, "Error fetching resources: " + resourceType + " - " + apiResponse.getErrorMessage());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                result.removeSource(dbSource);
                
                // If we have cached data, use it
                appExecutors.diskIO().execute(() -> {
                    List<ResourceEntity> cachedResources = resourceDao.getResourcesByType(resourceType);
                    appExecutors.mainThread().execute(() -> {
                        if (cachedResources != null && !cachedResources.isEmpty()) {
                            result.setValue(Resource.error("Network error: " + t.getMessage(), cachedResources));
                            Log.e(TAG, "Network error, using cached data: " + resourceType, t);
                        } else {
                            result.setValue(Resource.error("Network error: " + t.getMessage(), null));
                            Log.e(TAG, "Network error, no cached data: " + resourceType, t);
                        }
                    });
                });
            }
        });
    }
    
    /**
     * Get API call for a specific resource
     * @param resourceType Resource type
     * @param resourceKey Resource key
     * @param headers HTTP headers
     * @return API call or null if unsupported
     */
    @Nullable
    private Call<JsonObject> getApiCall(String resourceType, String resourceKey, Map<String, String> headers) {
        switch (resourceType) {
            case RESOURCE_TYPE_TEMPLATE:
                return apiService.getTemplate(resourceKey, headers);
            case RESOURCE_TYPE_CATEGORY:
                return apiService.getCategory(resourceKey, headers);
            case RESOURCE_TYPE_ICON:
                return apiService.getIcon(resourceKey, headers);
            default:
                return null;
        }
    }
    
    /**
     * Get API call for all resources of a type
     * @param resourceType Resource type
     * @param headers HTTP headers
     * @return API call or null if unsupported
     */
    @Nullable
    private Call<List<JsonObject>> getApiCallForAll(String resourceType, Map<String, String> headers) {
        switch (resourceType) {
            case RESOURCE_TYPE_TEMPLATE:
                return apiService.getTemplatesJson(headers);
            case RESOURCE_TYPE_CATEGORY:
                return apiService.getCategories(headers);
            case RESOURCE_TYPE_ICON:
                return apiService.getIcons(headers);
            default:
                return null;
        }
    }
    
    /**
     * Get resource key from an item
     * @param resourceType Resource type
     * @param item Item data
     * @return Resource key or null if not found
     */
    @Nullable
    private String getResourceKeyFromItem(String resourceType, JsonObject item) {
        try {
            switch (resourceType) {
                case RESOURCE_TYPE_TEMPLATE:
                case RESOURCE_TYPE_CATEGORY:
                case RESOURCE_TYPE_ICON:
                    return item.has("id") ? item.get("id").getAsString() : null;
                default:
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting resource key from item", e);
            return null;
        }
    }
    
    /**
     * Get a resource synchronously from the cache
     * This method is for internal use only and should be used sparingly
     * @param resourceType The resource type
     * @param resourceKey The resource key
     * @return The resource data as JsonObject, or null if not found
     */
    public JsonObject getResourceSync(String resourceType, String resourceKey) {
        String cacheKey = getCacheKey(resourceType, resourceKey);
        return resourceCache.get(cacheKey);
    }
} 