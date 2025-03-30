package com.ds.eventwish.data.remote;

import android.content.Context;
import android.util.Log;
import android.content.SharedPreferences;

import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.config.ApiConfig;
import com.ds.eventwish.utils.ApiConstants;
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

public class ApiClient {
    // Static members
    private static final String TAG = "ApiClient";
    private static final String API_BASE_URL = "https://eventwish2.onrender.com/api/";
    public static final String BASE_URL = API_BASE_URL;
    private static Retrofit retrofit = null;
    private static Context applicationContext = null;
    private static ApiClient instance;
    private static OkHttpClient okHttpClient = null;
    
    /**
     * Initialize ApiClient with application context
     * @param context Application context
     */
    public static void init(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        applicationContext = context.getApplicationContext();
        
        // Clean up any existing client to avoid leaks
        if (okHttpClient != null) {
            cleanupOkHttpClient();
        }
        
        // Create new OkHttpClient
        okHttpClient = createOkHttpClient(applicationContext);
        
        Log.d(TAG, "ApiClient initialized with application context");
    }
    
    /**
     * Get the API service instance
     * @return API service
     */
    public static ApiService getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new ApiInterceptor())
                .build();
                
            retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.getBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit.create(ApiService.class);
    }
    
    /**
     * Create a new OkHttpClient instance
     * @param context Application context
     * @return New OkHttpClient instance
     */
    private static OkHttpClient createOkHttpClient(Context context) {
        // Create logging interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? 
            HttpLoggingInterceptor.Level.BODY : 
            HttpLoggingInterceptor.Level.BASIC);
        
        // Create connection error interceptor
        Interceptor connectionErrorInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                try {
                    return chain.proceed(request);
                } catch (IOException e) {
                    Log.e(TAG, "Connection error: " + e.getMessage(), e);
                    if (e.getMessage() != null && e.getMessage().contains("closed")) {
                        Log.e(TAG, "Connection was closed, creating new request");
                        // Try to create a new request to avoid closed connection issues
                        Request newRequest = request.newBuilder().build();
                        return chain.proceed(newRequest);
                    }
                    throw e;
                }
            }
        };
        
        // Create dispatcher with custom settings
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(64); // Increase from 20 to default 64
        dispatcher.setMaxRequestsPerHost(10); // Increase from 5 to 10
        
        // Create OkHttpClient
        return new OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(connectionErrorInterceptor)
            .addInterceptor(provideNoCacheInterceptor())
            .addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    // Use constant API key
                    .header("x-api-key", ApiConstants.API_KEY)
                    .method(original.method(), original.body());
                
                // Add auth token if available
                String authToken = getStoredAuthToken(context);
                if (authToken != null) {
                    requestBuilder.header("Authorization", "Bearer " + authToken);
                }
                
                return chain.proceed(requestBuilder.build());
            })
            .connectTimeout(30, TimeUnit.SECONDS) // Increase from 15 to 30
            .readTimeout(45, TimeUnit.SECONDS) // Increase from 30 to 45
            .writeTimeout(45, TimeUnit.SECONDS) // Increase from 30 to 45
            .retryOnConnectionFailure(true)
            .dispatcher(dispatcher)
            .connectionPool(new ConnectionPool(10, 60, TimeUnit.SECONDS)) // Increase pool size and idle time
            .protocols(Arrays.asList(Protocol.HTTP_1_1)) // Use only HTTP/1.1
            .build();
    }
    
    /**
     * Clean up OkHttpClient resources
     */
    private static void cleanupOkHttpClient() {
        if (okHttpClient != null) {
            try {
                // Only cleanup if there are no active calls
                if (okHttpClient.dispatcher().runningCallsCount() == 0) {
                    // Clear connection pool
                    okHttpClient.connectionPool().evictAll();
                    
                    // Shutdown executor service only if no active calls
                    if (!okHttpClient.dispatcher().executorService().isShutdown()) {
                        okHttpClient.dispatcher().executorService().shutdown();
                    }
                    
                    Log.d(TAG, "OkHttpClient resources cleaned up");
                } else {
                    Log.d(TAG, "Skipping cleanup due to active calls: " + 
                          okHttpClient.dispatcher().runningCallsCount());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up OkHttpClient", e);
            }
        }
    }
    
    /**
     * Provide a no-cache interceptor to force network requests
     * @return Interceptor for preventing caching
     */
    private static Interceptor provideNoCacheInterceptor() {
        return chain -> {
            Request request = chain.request();
            
            // Add no-cache headers to force network requests
            request = request.newBuilder()
                .header("Cache-Control", "no-cache")
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
            
            // Execute the request with retry logic
            Response response = null;
            IOException lastException = null;
            int maxRetries = 3;
            int retryCount = 0;
            
            while (retryCount < maxRetries && response == null) {
                try {
                    response = chain.proceed(request);
                } catch (IOException e) {
                    lastException = e;
                    
                    // Don't retry if the request was explicitly canceled
                    if (e.getMessage() != null && e.getMessage().contains("Canceled")) {
                        Log.d(TAG, "Request was canceled, not retrying");
                        throw e;
                    }
                    
                    // Retry on connection issues
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("closed") || 
                         e.getMessage().contains("timeout") || 
                         e.getMessage().contains("connection"))) {
                        
                        retryCount++;
                        Log.w(TAG, "Retry attempt " + retryCount + " of " + maxRetries);
                        
                        if (retryCount < maxRetries) {
                            // Wait before retrying
                            try {
                                Thread.sleep(1000 * retryCount);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw e;
                            }
                            
                            // Create a new request for retry
                            request = request.newBuilder().build();
                            continue;
                        }
                    }
                    
                    // If we get here, either we've exhausted retries or hit a non-retryable error
                    throw lastException;
                }
            }
            
            if (response == null && lastException != null) {
                throw lastException;
            }
            
            // Add no-cache headers to response
            return response.newBuilder()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
        };
    }

    private static String getStoredAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("coins_secure_prefs", Context.MODE_PRIVATE);
        return prefs.getString("auth_token", null);
    }

    /**
     * Get singleton instance of ApiClient (for backward compatibility)
     * @return ApiClient instance
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Get ApiService instance (for backward compatibility)
     * @return ApiService instance
     */
    public ApiService getApiService() {
        return getClient();
    }
    
    /**
     * Clean up resources when the app is closing
     * Should be called from Application.onTerminate or similar lifecycle method
     */
    public static void cleanup() {
        cleanupOkHttpClient();
        okHttpClient = null;
        retrofit = null;
        instance = null;
        Log.d(TAG, "ApiClient cleaned up");
    }
}
