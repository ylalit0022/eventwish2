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
    
    // Production API URL
    public static final String BASE_URL = "https://eventwish2.onrender.com/api/";
    
    private static Retrofit retrofit = null;
    private static Context applicationContext = null;
    private static ApiClient instance;
    private static ApiService apiService = null;

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
        if (apiService != null) {
            cleanupApiService();
        }

        // Create new ApiService
        apiService = createApiService(context);

        Log.d(TAG, "ApiClient initialized with application context");
        Log.d(TAG, "Using API URL: " + BASE_URL);
    }

    /**
     * Get the API service instance
     * @return API service
     */
    public static ApiService getClient() {
        if (apiService == null) {
            synchronized (ApiClient.class) {
                if (apiService == null) {
                    OkHttpClient client = getHttpClient();
                    
                    Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
                    
                    apiService = retrofit.create(ApiService.class);
                }
            }
        }
        return apiService;
    }

    /**
     * Create a new ApiService instance
     * @param context Application context
     * @return New ApiService instance
     */
    private static ApiService createApiService(Context context) {
        // Create Gson converter with pretty printing for debugging
        Gson gson = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .create();
        
        // Create OkHttp client with logging and timeouts
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);
        
        // Add common headers interceptor
        httpClientBuilder.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-API-Key", BuildConfig.API_KEY)
                .method(original.method(), original.body());
            return chain.proceed(requestBuilder.build());
        });
        
        // Add logging interceptor in debug mode
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClientBuilder.addInterceptor(loggingInterceptor);
        }
        
        // Build OkHttp client
        OkHttpClient httpClient = httpClientBuilder.build();
        
        // Create Retrofit instance
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build();
        }
        
        // Create and return API service
        return retrofit.create(ApiService.class);
    }

    /**
     * Clean up the API service
     */
    private static void cleanupApiService() {
        if (apiService != null) {
            apiService = null;
        }
        if (retrofit != null) {
            retrofit = null;
        }
    }

    /**
     * Get the singleton instance
     * @return ApiClient instance
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
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
        cleanupApiService();
        instance = null;
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

        // Add cache if application context is available
        if (applicationContext != null) {
            File httpCacheDirectory = new File(applicationContext.getCacheDir(), "http-cache");
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