package com.ds.eventwish.data.remote;

import android.util.Log;
import com.ds.eventwish.config.ApiConfig;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiInterceptor implements Interceptor {
    private static final String TAG = "ApiInterceptor";
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String apiKey = ApiConfig.getApiKey();
        
        // Log API key (masked for security)
        if (apiKey != null && apiKey.length() > 4) {
            Log.d(TAG, "Using API key: ***" + apiKey.substring(apiKey.length() - 4));
        } else {
            Log.e(TAG, "API key is null or invalid");
        }
        
        // Add API key to every request
        Request newRequest = originalRequest.newBuilder()
            .header("x-api-key", apiKey)
            .build();
            
        return chain.proceed(newRequest);
    }
}
