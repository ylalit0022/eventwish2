package com.ds.eventwish;

import android.app.Application;

public class EventWishApplication extends Application {
    private static EventWishApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static EventWishApplication getInstance() {
        return instance;
    }
}
