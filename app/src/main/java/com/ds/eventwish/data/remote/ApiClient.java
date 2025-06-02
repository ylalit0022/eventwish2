package com.ds.eventwish.data.remote;

import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.config.ApiConfig;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.utils.DeviceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.LinkedTreeMap;
import com.ds.eventwish.util.SecureTokenManager;
import com.ds.eventwish.utils.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API Client for the EventWish API
 */
public class ApiClient {
    // Static members
    private static final String TAG = "ApiClient";
    
    // Base URL of the API
    private static final String BASE_URL = "https://eventwish2.onrender.com/api/";
    
    // API service
    private static ApiService apiService;
    
    // Retrofit instance
    private static Retrofit retrofit;
    
    // Context
    private static Context context;

    /**
     * Exception thrown when there is no network connectivity
     */
    public static class NoConnectivityException extends IOException {
        public NoConnectivityException() {
            super("No network connectivity");
        }
    }

    /**
     * Initialize the API client with the application context
     * @param appContext Application context
     */
    public static void init(Context appContext) {
        if (appContext == null) {
            Log.e(TAG, "Cannot initialize ApiClient with null context");
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        context = appContext.getApplicationContext();
        
        // Try to ensure SecureTokenManager is initialized
        try {
            SecureTokenManager.init(context);
            Log.d(TAG, "SecureTokenManager initialized during ApiClient init");
        } catch (Exception e) {
            Log.w(TAG, "Could not initialize SecureTokenManager during ApiClient init: " + e.getMessage());
            // Continue without secure token manager - will use fallback methods
        }
        
        // Log for debugging
        Log.d(TAG, "ApiClient initialized with API URL: " + BASE_URL);
    }

    /**
     * Get the API service
     * @return API service
     */
    public static ApiService getClient() {
        if (context == null) {
            Log.e(TAG, "ApiClient not initialized. Call ApiClient.init() first");
            throw new IllegalStateException("ApiClient not initialized. Call ApiClient.init() first");
        }
        
        if (apiService == null) {
            // Create a new API service
            apiService = createApiService();
            Log.d(TAG, "Created new ApiService instance");
        }
        
        return apiService;
    }
    
    /**
     * Get API service instance (singleton)
     * @return API service instance
     */
    public static ApiService getInstance() {
        return getClient();
    }

    /**
     * Create the API service
     * @return API service
     */
    private static ApiService createApiService() {
        // Get API key from BuildConfig or secure storage
        String apiKey = getApiKey();
        Log.d(TAG, "Creating API service with API key: " + (apiKey != null ? "valid key" : "null key"));

        // Create Gson converter that properly handles empty arrays
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(List.class, new EmptyListDeserializer())
            .create();

        // Create OkHttp client with interceptors
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
            .addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                
                // Build new request with API key
                okhttp3.Request.Builder requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .method(original.method(), original.body());

                // Add API key and device ID if available
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("x-api-key", apiKey);
                }
                
                // Add device ID for tracking and authentication
                String deviceId = DeviceUtils.getDeviceId(context);
                if (deviceId != null && !deviceId.isEmpty()) {
                    requestBuilder.header("x-device-id", deviceId);
                }
                
                // Add authentication token if available
                // Safely check if SecureTokenManager is initialized
                String authToken = null;
                try {
                    SecureTokenManager tokenManager = SecureTokenManager.getInstance();
                    if (tokenManager != null) {
                        authToken = tokenManager.getAccessToken();
                    }
                } catch (IllegalStateException e) {
                    // SecureTokenManager not initialized yet
                    Log.w(TAG, "SecureTokenManager not initialized yet: " + e.getMessage());
                    // Continue without auth token
                }
                
                if (authToken != null && !authToken.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + authToken);
                }
                
                okhttp3.Request request = requestBuilder.build();
                
                // Offline mode handling - serve cached responses if available
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    Log.d(TAG, "Device is offline, trying to serve from cache: " + request.url());
                    CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build();
                    
                    okhttp3.Request cachedRequest = request.newBuilder()
                        .cacheControl(cacheControl)
                        .build();
                        
                    return chain.proceed(cachedRequest);
                }
                
                // Log request details in debug mode
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "API Request: " + request.url() +
                          "\nAPI Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "null") +
                          "\nHeaders: " + request.headers());
                }
                
                try {
                    // Attempt the request
                    okhttp3.Response response = chain.proceed(request);
                    
                    // Handle rate limiting or server errors
                    if (response.code() == 429) {
                        Log.w(TAG, "Rate limited by API, waiting before retry");
                        try {
                            Thread.sleep(2000); // Wait 2 seconds
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            // Don't throw exception on interruption - just continue
                            Log.w(TAG, "Thread interrupted during rate limit wait", ie);
                        }
                        return chain.proceed(request); // Retry once
                    } else if (response.code() >= 500) {
                        Log.w(TAG, "Server error " + response.code() + ", attempting retry");
                        response.close();
                        try {
                            Thread.sleep(1000); // Wait 1 second
                        } catch (InterruptedException ie2) {
                            Thread.currentThread().interrupt();
                            // Don't throw exception on interruption - just continue
                            Log.w(TAG, "Thread interrupted during server error wait", ie2);
                        }
                        return chain.proceed(request); // Retry once
                    }
                    
                    // Check for empty responses
                    if (response.code() == 200 && response.body() != null) {
                        String contentType = response.header("Content-Type");
                        if (contentType != null && contentType.contains("application/json")) {
                            // If this is a GET request for templates, check if it's empty
                            if (request.url().toString().contains("/templates") && "GET".equals(request.method())) {
                                try {
                                    // Peek at the response body without consuming it
                                    String responseBody = response.peekBody(Long.MAX_VALUE).string();
                                    if (responseBody.contains("\"data\":[]") || responseBody.contains("\"templates\":[]")) {
                                        Log.d(TAG, "Empty templates response detected: " + responseBody);
                                        // This is a valid empty response, not an error
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Error peeking at response body", e);
                                }
                            }
                        }
                    }
                    
                    return response;
                } catch (IOException e) {
                    // Handle canceled requests gracefully
                    if (e.getMessage() != null && (e.getMessage().contains("Canceled") || 
                        e.getMessage().contains("Socket closed") || 
                        e.getMessage().contains("Connection reset") ||
                        "canceled".equalsIgnoreCase(e.getMessage()))) {
                        
                        Log.d(TAG, "Request was intentionally canceled: " + request.url());
                        
                        // For canceled GET requests, try to return cached data
                        if (request.method().equals("GET")) {
                            try {
                                okhttp3.Request cachedRequest = request.newBuilder()
                                    .cacheControl(CacheControl.FORCE_CACHE)
                                    .build();
                                return chain.proceed(cachedRequest);
                            } catch (Exception cacheEx) {
                                Log.w(TAG, "Failed to get cached data after cancellation: " + cacheEx.getMessage());
                                // Fall through to throw the original exception
                            }
                        }
                        
                        // Rethrow with a more descriptive message
                        throw new IOException("Request canceled: " + request.url(), e);
                    }
                    
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        Log.e(TAG, "Network error despite having connection: " + e.getMessage(), e);
                        // Try once more after a short delay
                        try {
                            Thread.sleep(1000);
                            return chain.proceed(request);
                        } catch (InterruptedException ie3) {
                            Thread.currentThread().interrupt();
                            // Don't throw exception on interruption - just continue
                            Log.w(TAG, "Thread interrupted during retry", ie3);
                            
                            // Try to proceed anyway
                            try {
                                return chain.proceed(request);
                            } catch (Exception proceedEx) {
                                // If that fails, try cached data for GET requests
                                if (request.method().equals("GET")) {
                                    try {
                                        okhttp3.Request cachedRequest = request.newBuilder()
                                            .cacheControl(CacheControl.FORCE_CACHE)
                                            .build();
                                        return chain.proceed(cachedRequest);
                                    } catch (Exception cacheEx) {
                                        Log.w(TAG, "Failed to get cached data: " + cacheEx.getMessage());
                                    }
                                }
                                throw new IOException("Failed to proceed with request after interruption", proceedEx);
                            }
                        } catch (Exception retryException) {
                            Log.e(TAG, "Retry failed: " + retryException.getMessage(), retryException);
                            
                            // If retry fails, try cached data for GET requests
                            if (request.method().equals("GET")) {
                                try {
                                    okhttp3.Request cachedRequest = request.newBuilder()
                                        .cacheControl(CacheControl.FORCE_CACHE)
                                        .build();
                                    return chain.proceed(cachedRequest);
                                } catch (Exception cacheEx) {
                                    Log.w(TAG, "Failed to get cached data after retry failure: " + cacheEx.getMessage());
                                }
                            }
                            
                            throw new IOException("Retry failed", retryException);
                        }
                    } else {
                        Log.w(TAG, "Network unavailable, using cached data if available");
                        okhttp3.Request offlineRequest = request.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=86400")
                            .build();
                        try {
                            return chain.proceed(offlineRequest);
                        } catch (Exception cacheException) {
                            // If cache retrieval fails, throw the original exception
                            throw new IOException("Cache retrieval failed", e);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during API request: " + e.getMessage(), e);
                    throw new IOException("Error during API request", e);
                }
            })
            .addInterceptor(getLoggingInterceptor())
            .build();

        // Create Retrofit with Gson converter
        retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

        return retrofit.create(ApiService.class);
    }

    /**
     * Get API key from the most reliable source
     * @return API key
     */
    private static String getApiKey() {
        // First try to get from SecureTokenManager if available
        try {
            if (context != null) {
                SecureTokenManager tokenManager = SecureTokenManager.getInstance();
                if (tokenManager != null) {
                    String secureApiKey = tokenManager.getApiKey();
                    if (secureApiKey != null && !secureApiKey.isEmpty()) {
                        Log.d(TAG, "Using API key from SecureTokenManager");
                        return secureApiKey;
                    }
                }
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "SecureTokenManager not initialized yet: " + e.getMessage());
            // Continue with fallback
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key from SecureTokenManager", e);
        }
        
        // Fallback to BuildConfig
        if (BuildConfig.API_KEY != null && !BuildConfig.API_KEY.isEmpty()) {
            Log.d(TAG, "Using API key from BuildConfig");
            return BuildConfig.API_KEY;
        }
        
        // Final fallback to hardcoded key (not recommended)
        Log.w(TAG, "No API key found in SecureTokenManager or BuildConfig, using fallback key");
        return "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915"; // This is a fallback that should be replaced
    }

    /**
     * Update the API key (used when the key changes or is refreshed)
     * @param newApiKey The new API key to use
     */
    public static void updateApiKey(String newApiKey) {
        if (newApiKey == null || newApiKey.isEmpty()) {
            Log.e(TAG, "Cannot update API key: new key is null or empty");
            return;
        }
        
        // Store in SecureTokenManager if possible
        try {
            if (context != null) {
                SecureTokenManager tokenManager = SecureTokenManager.getInstance();
                if (tokenManager != null) {
                    tokenManager.saveApiKey(newApiKey);
                    Log.d(TAG, "Saved new API key to SecureTokenManager");
                }
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "SecureTokenManager not initialized yet: " + e.getMessage());
            // Cannot save API key now, will be handled later when SecureTokenManager is initialized
        } catch (Exception e) {
            Log.e(TAG, "Error saving API key to SecureTokenManager", e);
        }
        
        // Recreate API service with new key
        apiService = null;
        getClient(); // This will recreate the service with the new key
        
        Log.d(TAG, "API service recreated with updated API key");
    }

    /**
     * Get a logging interceptor for debugging
     * @return Logging interceptor
     */
    private static HttpLoggingInterceptor getLoggingInterceptor() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        
        // Only log in debug mode
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        
        return loggingInterceptor;
    }

    /**
     * Get the API service
     * @return API service
     */
    public ApiService getApiService() {
        return apiService;
    }

    /**
     * Clean up resources
     */
    public static void cleanup() {
        apiService = null;
        retrofit = null;
    }

    /**
     * Get the HTTP client
     * @return OkHttp client
     */
    private OkHttpClient getHttpClient() {
        // Cache configuration
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        int cacheSize = 10 * 1024 * 1024; // 10 MB
        Cache cache = new Cache(cacheDir, cacheSize);

        // Debug interceptor for logging request/response times
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        }

        // Create OkHttpClient with API key and logging interceptors
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
                .cache(cache);
        
        // Add retry mechanism for server errors and rate limiting
        addRetryInterceptor(httpClient);
                
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                long requestStartTime = System.currentTimeMillis();
                okhttp3.Request original = chain.request();
                
                // Build new request with API key
                okhttp3.Request.Builder requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .method(original.method(), original.body());

                // Add API key and device ID if available
                String apiKey = getApiKey();
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.header("x-api-key", apiKey);
                }
                
                // Add device ID for tracking and authentication
                String deviceId = DeviceUtils.getDeviceId(context);
                if (deviceId != null && !deviceId.isEmpty()) {
                    requestBuilder.header("x-device-id", deviceId);
                }
                
                // Add authentication token if available
                String authToken = null;
                try {
                    SecureTokenManager tokenManager = SecureTokenManager.getInstance();
                    if (tokenManager != null) {
                        authToken = tokenManager.getAccessToken();
                    }
                } catch (IllegalStateException e) {
                    // SecureTokenManager not initialized yet
                    Log.w(TAG, "SecureTokenManager not initialized yet: " + e.getMessage());
                    // Continue without auth token
                }
                
                if (authToken != null && !authToken.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + authToken);
                }
                
                okhttp3.Request request = requestBuilder.build();
                
                // Offline mode handling - serve cached responses if available
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    Log.d(TAG, "Device is offline, trying to serve from cache: " + request.url());
                    CacheControl cacheControl = new CacheControl.Builder()
                        .maxStale(7, TimeUnit.DAYS)
                        .build();
                    
                    okhttp3.Request cachedRequest = request.newBuilder()
                        .cacheControl(cacheControl)
                        .build();
                        
                    return chain.proceed(cachedRequest);
                }
                
                // Log request details
                Log.d(TAG, String.format("Sending request to: %s (%s)", 
                        request.url(), request.method()));
                
                try {
                    okhttp3.Response response = chain.proceed(request);
                    long requestEndTime = System.currentTimeMillis();
                    long duration = requestEndTime - requestStartTime;
                    
                    // Log response details
                    int code = response.code();
                    String message = response.message();
                    Log.d(TAG, String.format("Received response from %s: %d %s in %dms", 
                            request.url(), code, message, duration));
                    
                    // Debug headers
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "Response headers: " + response.headers());
                    }
                    
                    return response;
                } catch (IOException e) {
                    long requestEndTime = System.currentTimeMillis();
                    long duration = requestEndTime - requestStartTime;
                    Log.e(TAG, String.format("Network error requesting %s after %dms: %s", 
                            request.url(), duration, e.getMessage()), e);
                    throw e;
                }
            }
        })
        .addNetworkInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Request request = chain.request();
                NetworkUtils networkUtils = NetworkUtils.getInstance(context);
                
                // Add cache control based on network state
                if (request.method().equalsIgnoreCase("GET")) {
                    okhttp3.Request.Builder builder = request.newBuilder();
                    
                    if (networkUtils.isNetworkAvailable()) {
                        // Online - use network but cache for a short period
                        builder.header("Cache-Control", "public, max-age=60"); // 1 minute
                    } else {
                        // Offline - use cache if available, for a longer time
                        builder.header("Cache-Control", "public, only-if-cached, max-stale=86400"); // 1 day
                        Log.d(TAG, "Offline mode: using cached response for: " + request.url());
                    }
                    
                    request = builder.build();
                }
                
                return chain.proceed(request);
            }
        })
        .addInterceptor(loggingInterceptor);

        return httpClient.build();
    }

    /**
     * Add a retry interceptor to handle rate limiting and server errors
     * @param builder OkHttpClient builder to add the interceptor to
     */
    private void addRetryInterceptor(OkHttpClient.Builder builder) {
        builder.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                okhttp3.Request request = chain.request();
                int maxRetries = 3;
                int retryCount = 0;
                                
                okhttp3.Response response = null;
                IOException exception = null;
                
                while (retryCount < maxRetries) {
                    try {
                        if (retryCount > 0) {
                            // Exponential backoff - wait longer between each retry
                            long waitTime = (long) Math.pow(2, retryCount) * 1000;
                            Log.d(TAG, String.format("Retry attempt %d for %s, waiting %dms", 
                                    retryCount, request.url(), waitTime));
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new IOException("Retry interrupted", ie);
                            }
                        }
                        
                        // Make the request
                        response = chain.proceed(request);
                        
                        // Check if we should retry based on response code
                        int responseCode = response.code();
                        if (responseCode == 429) { // Too Many Requests
                            Log.w(TAG, "Rate limited (429) by API for " + request.url());
                            
                            // Extract retry-after header if available
                            String retryAfter = response.header("Retry-After");
                            if (retryAfter != null) {
                                try {
                                    long retryAfterSeconds = Long.parseLong(retryAfter);
                                    long waitTime = retryAfterSeconds * 1000;
                                    Log.d(TAG, "Waiting " + waitTime + "ms as specified by Retry-After header");
                                    try {
                                        Thread.sleep(waitTime);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new IOException("Retry interrupted", ie);
                                    }
                                } catch (NumberFormatException e) {
                                    Log.w(TAG, "Invalid Retry-After header: " + retryAfter);
                                }
                            }
                            
                            // Close response and retry
                            response.close();
                            retryCount++;
                            continue;
                        } else if (responseCode >= 500) { // Server errors
                            Log.w(TAG, "Server error (" + responseCode + ") for " + request.url());
                            // Close response and retry
                            response.close();
                            retryCount++;
                            continue;
                        }
                        
                        // No need to retry for 2xx, 3xx, or 4xx (except 429)
                        return response;
                        
                    } catch (IOException e) {
                        // Network issue occurred
                        exception = e;
                        Log.w(TAG, "Network error during API call to " + request.url() + 
                                ": " + e.getMessage() + ", retry: " + retryCount);
                        retryCount++;
                        
                        // If it's the last retry, throw the exception
                        if (retryCount >= maxRetries) {
                            throw exception;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during API request: " + e.getMessage(), e);
                        throw new IOException("Error during API request", e);
                    }
                }
                
                // This should only happen if we've exhausted retries on a response with status 429 or 5xx
                if (response != null) {
                    return response;
                }
                
                // This should only happen if we've exhausted retries with an IOException
                throw exception != null ? exception : new IOException("Unknown error after retries");
            }
        });
    }

    /**
     * Custom deserializer for handling empty array responses
     * This avoids "Expected BEGIN_ARRAY but was BEGIN_OBJECT" errors
     */
    private static class EmptyListDeserializer implements JsonDeserializer<List<?>> {
        @Override
        public List<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // Log the typeOfT for debugging
            Log.d(TAG, "Deserializing List with type: " + typeOfT.toString());
            
            // Handle arrays directly
            if (json.isJsonArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonElement element : json.getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        list.add(element.getAsString());
                    } else if (element.isJsonObject()) {
                        // Use context to properly deserialize objects
                        // Extract the type parameter from the List type
                        Type elementType = ((java.lang.reflect.ParameterizedType) typeOfT).getActualTypeArguments()[0];
                        try {
                            list.add(context.deserialize(element, elementType));
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing object: " + e.getMessage(), e);
                            // Fallback to JsonObject if deserialization fails
                            list.add(element.getAsJsonObject());
                        }
                    } else if (element.isJsonArray()) {
                        list.add(element.getAsJsonArray());
                    } else if (element.isJsonNull()) {
                        list.add(null);
                    }
                }
                return list;
            } else if (json.isJsonObject()) {
                // If we got an object when expecting an array, check for empty data field
                JsonObject jsonObject = json.getAsJsonObject();
                
                // Extract the element type from the List type parameter
                Type elementType = null;
                try {
                    elementType = ((java.lang.reflect.ParameterizedType) typeOfT).getActualTypeArguments()[0];
                    Log.d(TAG, "Element type: " + elementType.toString());
                } catch (Exception e) {
                    Log.e(TAG, "Error getting element type: " + e.getMessage(), e);
                }
                
                if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                    Log.d(TAG, "Found data array in object, returning properly deserialized objects");
                    List<Object> dataList = new ArrayList<>();
                    for (JsonElement element : jsonObject.getAsJsonArray("data")) {
                        if (element.isJsonObject()) {
                            if (elementType != null) {
                                try {
                                    dataList.add(context.deserialize(element, elementType));
                                } catch (Exception e) {
                                    Log.e(TAG, "Error deserializing data element: " + e.getMessage(), e);
                                    dataList.add(element.getAsJsonObject());
                                }
                            } else {
                                dataList.add(element.getAsJsonObject());
                            }
                        }
                    }
                    return dataList;
                } else if (jsonObject.has("templates") && jsonObject.get("templates").isJsonArray()) {
                    Log.d(TAG, "Found templates array in object, returning properly deserialized objects");
                    List<Object> templatesList = new ArrayList<>();
                    for (JsonElement element : jsonObject.getAsJsonArray("templates")) {
                        if (element.isJsonObject()) {
                            if (elementType != null) {
                                try {
                                    templatesList.add(context.deserialize(element, elementType));
                                } catch (Exception e) {
                                    Log.e(TAG, "Error deserializing template element: " + e.getMessage(), e);
                                    templatesList.add(element.getAsJsonObject());
                                }
                            } else {
                                templatesList.add(element.getAsJsonObject());
                            }
                        }
                    }
                    return templatesList;
                }
                // Return empty list for other object types
                Log.d(TAG, "Object doesn't contain expected array field, returning empty list");
                return new ArrayList<>();
            }
            
            // For other cases, return empty list
            Log.d(TAG, "Unexpected JSON type, returning empty list");
            return new ArrayList<>();
        }
    }
}