package com.ds.eventwish.data.remote;

import com.ds.eventwish.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.Protocol;
import java.util.Arrays;

public class ApiClient {
    private static final int CONNECT_TIMEOUT = 15;
    private static final int READ_TIMEOUT = 15;
    private static final int WRITE_TIMEOUT = 15;
    private static final int MAX_RETRIES = 3;
    private static Retrofit retrofit = null;
    private static final String TAG = "ApiClient";

    public static ApiService getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? 
            HttpLoggingInterceptor.Level.BODY : 
            HttpLoggingInterceptor.Level.NONE);

        return new OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(chain -> {
                okhttp3.Request request = chain.request();
                okhttp3.Request.Builder builder = request.newBuilder();
                
                // Add cache control headers
                builder.addHeader("Cache-Control", "no-cache");
                builder.addHeader("Connection", "close"); // Disable keep-alive
                
                request = builder.build();
                
                int retryCount = 0;
                okhttp3.Response response = null;
                
                while (retryCount < MAX_RETRIES) {
                    try {
                        if (response != null) {
                            response.close();
                        }
                        response = chain.proceed(request);
                        if (response.isSuccessful()) {
                            return response;
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Attempt " + (retryCount + 1) + " failed: " + e.getMessage());
                        if (retryCount == MAX_RETRIES - 1) throw e;
                    }
                    retryCount++;
                    try {
                        Thread.sleep(1000L * retryCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                
                if (response != null) {
                    return response;
                }
                
                return chain.proceed(request);
            })
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(0, 1, TimeUnit.MILLISECONDS)) // Disable connection pooling
            .protocols(Arrays.asList(Protocol.HTTP_1_1)) // Use only HTTP/1.1
            .retryOnConnectionFailure(true)
            .build();
    }
}
