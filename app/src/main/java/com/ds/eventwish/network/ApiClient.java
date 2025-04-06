package com.ds.eventwish.network;

import android.content.Context;
import android.util.Log;

/**
 * @deprecated This class is deprecated and will be removed in a future version.
 * Use {@link com.ds.eventwish.data.remote.ApiClient} instead.
 */
@Deprecated
public class ApiClient {
    private static final String TAG = "ApiClient_Deprecated";
    
    /**
     * Initialize the API client with context
     * @param appContext Application context
     * @deprecated Use {@link com.ds.eventwish.data.remote.ApiClient#init(Context)} instead.
     */
    @Deprecated
    public static void init(Context appContext) {
        Log.w(TAG, "Using deprecated ApiClient.init(), please update to com.ds.eventwish.data.remote.ApiClient");
        // Forward to the correct implementation
        com.ds.eventwish.data.remote.ApiClient.init(appContext);
    }
    
    /**
     * Get the API service instance
     * @return API service
     * @deprecated Use {@link com.ds.eventwish.data.remote.ApiClient#getClient()} instead.
     */
    @Deprecated
    public static com.ds.eventwish.data.remote.ApiService getClient() {
        Log.w(TAG, "Using deprecated ApiClient.getClient(), please update to com.ds.eventwish.data.remote.ApiClient");
        // Forward to the correct implementation
        return com.ds.eventwish.data.remote.ApiClient.getClient();
    }
} 