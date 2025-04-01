package com.ds.eventwish.data.remote;

import android.util.Log;
import com.ds.eventwish.config.ApiConfig;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiInterceptor implements Interceptor {
    private static final String TAG = "ApiInterceptor";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second delay between retries
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String apiKey = ApiConfig.getApiKey();
        
        // Log API key (masked for security)
        if (apiKey != null && apiKey.length() > 4) {
            Log.d(TAG, "Using API key: ***" + apiKey.substring(apiKey.length() - 4));
        } else {
            Log.e(TAG, "API key is null or invalid");
            throw new IOException("Invalid API key configuration");
        }
        
        // Add API key to every request
        Request newRequest = originalRequest.newBuilder()
            .header("x-api-key", apiKey)
            .build();
        
        // Implement retry mechanism
        int retryCount = 0;
        Response response = null;
        IOException lastException = null;
        
        while (retryCount < MAX_RETRIES) {
            try {
                response = chain.proceed(newRequest);
                
                // If response is successful, return it
                if (response.isSuccessful()) {
                    return response;
                }
                
                // If we get a server error (5xx), retry
                if (response.code() >= 500) {
                    Log.w(TAG, "Server error: " + response.code() + ", attempt " + (retryCount + 1) + " of " + MAX_RETRIES);
                    response.close();
                    retryCount++;
                    
                    if (retryCount < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                        continue;
                    }
                }
                
                // For other error codes, return the response
                return response;
                
            } catch (IOException e) {
                lastException = e;
                Log.e(TAG, "Network error during attempt " + (retryCount + 1) + ": " + e.getMessage());
                retryCount++;
                
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted", e);
                    }
                    continue;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted during retry delay");
            }
        }
        
        // If we've exhausted all retries, throw the last exception
        if (lastException != null) {
            throw new IOException("Failed after " + MAX_RETRIES + " attempts", lastException);
        }
        
        return response;
    }
}
