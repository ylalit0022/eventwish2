package com.ds.eventwish.data.remote;

import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;

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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
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
     * Initialize the API client with the application context
     * @param appContext Application context
     */
    public static void init(Context appContext) {
        if (appContext == null) {
            Log.e(TAG, "Cannot initialize ApiClient with null context");
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        context = appContext.getApplicationContext();
        
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

        // Create OkHttp client with interceptors
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(chain -> {
                Request original = chain.request();
                
                // Add API key header to every request
                Request.Builder requestBuilder = original.newBuilder()
                    .header("x-api-key", apiKey) // Using lowercase header name for consistency
                    .method(original.method(), original.body());
                
                // Add auth token if available
                SecureTokenManager tokenManager = SecureTokenManager.getInstance();
                if (tokenManager != null && tokenManager.hasTokens()) {
                    String token = tokenManager.getAccessToken();
                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }
                }
                
                Request request = requestBuilder.build();
                
                // Log request details in debug mode
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "API Request: " + request.url() +
                          "\nAPI Key: " + (apiKey != null ? apiKey.substring(0, 10) + "..." : "null") +
                          "\nHeaders: " + request.headers());
                }
                
                return chain.proceed(request);
            })
            .addInterceptor(getLoggingInterceptor())
            .build();

        // Create Retrofit with Gson converter
        retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
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
    private static OkHttpClient getHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS));

        // Add API key header interceptor
        builder.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                .header("x-api-key", BuildConfig.API_KEY);
                
            Request request = requestBuilder.build();
            Log.d(TAG, "Sending request with API key: " + request.url());
            
            return chain.proceed(request);
        });

        // Add cache if application context is available
        if (context != null) {
            File httpCacheDirectory = new File(context.getCacheDir(), "http-cache");
            int cacheSize = 10 * 1024 * 1024; // 10 MB
            Cache cache = new Cache(httpCacheDirectory, cacheSize);
            builder.cache(cache);
            
            // Add network cache interceptor
            builder.addNetworkInterceptor(chain -> {
                Request request = chain.request();
                Response response = chain.proceed(request);
                
                // Cache for 5 minutes
                return response.newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build();
            });
        }

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
            
            // Add debug interceptor to log connection issues
            builder.addInterceptor(chain -> {
                Request request = chain.request();
                long startTime = System.currentTimeMillis();
                Log.d(TAG, "Sending request: " + request.url());
                
                try {
                    Response response = chain.proceed(request);
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "Received response for " + request.url() + " in " + (endTime - startTime) + "ms");
                    return response;
                } catch (IOException e) {
                    Log.e(TAG, "Error during API call to " + request.url() + ": " + e.getMessage(), e);
                    throw e;
                }
            });
        }

        return builder.build();
    }
}