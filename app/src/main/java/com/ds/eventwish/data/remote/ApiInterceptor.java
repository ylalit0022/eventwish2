package com.ds.eventwish.data.remote;

import com.ds.eventwish.config.ApiConfig;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ApiInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Add API key to every request
        Request newRequest = originalRequest.newBuilder()
            .header("x-api-key", ApiConfig.getApiKey())
            .build();
            
        return chain.proceed(newRequest);
    }
}
