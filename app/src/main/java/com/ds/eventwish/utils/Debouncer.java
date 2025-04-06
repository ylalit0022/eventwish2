package com.ds.eventwish.utils;

import android.os.Handler;
import android.os.Looper;

public class Debouncer {
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private long delay;

    public Debouncer(long delay) {
        this.delay = delay;
    }

    public void call(Runnable action) {
        handler.removeCallbacks(runnable);
        runnable = action;
        handler.postDelayed(runnable, delay);
    }
}