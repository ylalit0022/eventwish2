package com.ds.eventwish.data.remote;

import android.content.Context;
import android.util.Log;

import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.utils.NetworkUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.LinkedTreeMap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    
    // Network constants
    private static final int CONNECT_TIMEOUT = 15;
    private static final int READ_TIMEOUT = 30;
    private static final int WRITE_TIMEOUT = 30;
    private static final int MAX_RETRIES = 3;
    private static final String BASE_URL = "https://eventwish2.onrender.com/api/";
    
    // Cache constants
    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int CACHE_MAX_AGE = 60 * 60; // 1 hour
    private static final int CACHE_MAX_STALE = 60 * 60 * 24 * 7; // 1 week
    
    // Singleton instances
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static OkHttpClient okHttpClient = null;
    private static Context applicationContext = null;
    private static volatile ApiClient instance = null;
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private ApiClient() {
        // Private constructor to enforce singleton pattern
    }
    
    /**
     * Initialize the ApiClient with application context
     * @param context Application context
     */
    public static void init(Context context) {
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();
            Log.d(TAG, "ApiClient initialized with application context");
        }
    }
    
    /**
     * Get the ApiClient singleton instance
     * @return ApiClient instance
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }
    
    /**
     * Get the ApiService instance
     * @return ApiService instance
     */
    public ApiService getApiService() {
        return getClient();
    }
    
    /**
     * Get the ApiService instance (static method for backward compatibility)
     * @return ApiService instance
     */
    public static ApiService getClient() {
        if (apiService == null) {
            if (applicationContext == null) {
                throw new IllegalStateException("ApiClient must be initialized with context before use");
            }
            
            // Create OkHttpClient
            OkHttpClient client = getOkHttpClient();
            
            // Create custom Gson instance
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(JsonObject.class, new JsonObjectDeserializer())
                .create();
            
            // Create Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
            
            apiService = retrofit.create(ApiService.class);
            Log.d(TAG, "ApiService created");
        }
        return apiService;
    }
    
    /**
     * Custom deserializer for JsonObject to handle LinkedTreeMap conversion
     */
    private static class JsonObjectDeserializer implements JsonDeserializer<JsonObject> {
        @Override
        public JsonObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonObject()) {
                return json.getAsJsonObject();
            } else if (json.isJsonArray()) {
                // If it's an array, create an empty object
                return new JsonObject();
            } else if (json.isJsonNull()) {
                // If it's null, create an empty object
                return new JsonObject();
            } else {
                // For primitive values, create an object with a "value" property
                JsonObject obj = new JsonObject();
                obj.add("value", json);
                return obj;
            }
        }
    }
    
    /**
     * Get the OkHttpClient instance with caching and retry mechanisms
     * @return OkHttpClient instance
     */
    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            // Create logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG ? 
                HttpLoggingInterceptor.Level.BODY : 
                HttpLoggingInterceptor.Level.NONE);
            
            // Create cache
            File cacheDir = new File(applicationContext.getCacheDir(), "http_cache");
            Cache cache = new Cache(cacheDir, CACHE_SIZE);
            
            // Create OkHttpClient
            okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(logging)
                .addInterceptor(provideOfflineCacheInterceptor())
                .addNetworkInterceptor(provideCacheInterceptor())
                .addInterceptor(provideRetryInterceptor())
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS)) // Disable connection pooling
                .protocols(Arrays.asList(Protocol.HTTP_1_1)) // Use only HTTP/1.1
                .retryOnConnectionFailure(true)
                .build();
            
            Log.d(TAG, "OkHttpClient created with cache at: " + cacheDir.getAbsolutePath());
        }
        return okHttpClient;
    }
    
    /**
     * Provide cache interceptor for network requests
     * @return Interceptor for caching
     */
    private static Interceptor provideCacheInterceptor() {
        return chain -> {
            Response response = chain.proceed(chain.request());
            
            CacheControl cacheControl;
            
            // Cache based on network type
            if (NetworkUtils.getInstance(applicationContext).isConnectionUnmetered()) {
                // Unmetered connection (WiFi) - cache for 1 hour
                cacheControl = new CacheControl.Builder()
                    .maxAge(CACHE_MAX_AGE, TimeUnit.SECONDS)
                    .build();
            } else {
                // Metered connection (Cellular) - cache for 5 minutes
                cacheControl = new CacheControl.Builder()
                    .maxAge(5 * 60, TimeUnit.SECONDS)
                    .build();
            }
            
            return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build();
        };
    }
    
    /**
     * Provide offline cache interceptor for when the device is offline
     * @return Interceptor for offline caching
     */
    private static Interceptor provideOfflineCacheInterceptor() {
        return chain -> {
            Request request = chain.request();
            
            if (!NetworkUtils.getInstance(applicationContext).isConnected()) {
                Log.d(TAG, "Device is offline, using cached data");
                
                CacheControl cacheControl = new CacheControl.Builder()
                    .maxStale(CACHE_MAX_STALE, TimeUnit.SECONDS)
                    .build();
                
                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build();
            }
            
            return chain.proceed(request);
        };
    }
    
    /**
     * Provide retry interceptor for failed requests
     * @return Interceptor for retrying requests
     */
    private static Interceptor provideRetryInterceptor() {
        return chain -> {
            Request request = chain.request();
            
            // Don't retry POST, PUT, DELETE requests
            if (!request.method().equals("GET")) {
                return chain.proceed(request);
            }
            
            int retryCount = 0;
            Response response = null;
            IOException ioException = null;
            
            while (retryCount < MAX_RETRIES) {
                try {
                    if (response != null) {
                        response.close();
                    }
                    
                    // Log retry attempt
                    if (retryCount > 0) {
                        Log.d(TAG, "Retry attempt " + retryCount + " for: " + request.url());
                    }
                    
                    response = chain.proceed(request);
                    
                    if (response.isSuccessful()) {
                        return response;
                    }
                } catch (IOException e) {
                    ioException = e;
                    Log.e(TAG, "Attempt " + (retryCount + 1) + " failed: " + e.getMessage());
                }
                
                retryCount++;
                
                if (retryCount < MAX_RETRIES) {
                    // Exponential backoff
                    try {
                        long sleepTime = (long) (1000 * Math.pow(2, retryCount - 1));
                        Log.d(TAG, "Waiting " + sleepTime + "ms before retry");
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", e);
                    }
                }
            }
            
            // If we got a response, return it even if it's not successful
            if (response != null) {
                return response;
            }
            
            // Otherwise, throw the last exception
            throw ioException != null ? ioException : new IOException("Request failed after " + MAX_RETRIES + " retries");
        };
    }
    
    /**
     * Clear the HTTP cache
     */
    public static void clearCache() {
        if (okHttpClient != null && okHttpClient.cache() != null) {
            try {
                okHttpClient.cache().evictAll();
                Log.d(TAG, "HTTP cache cleared");
            } catch (IOException e) {
                Log.e(TAG, "Failed to clear HTTP cache", e);
            }
        }
    }
}
