package com.ds.eventwish.config;

import com.ds.eventwish.BuildConfig;

public class ApiConfig {
    public static final String API_KEY = BuildConfig.API_KEY;
    public static final String BASE_URL = BuildConfig.BASE_URL;
    
    public static String getApiKey() {
        return API_KEY;
    }
    
    public static String getBaseUrl() {
        return BASE_URL;
    }
}
