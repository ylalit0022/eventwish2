package com.ds.eventwish.analytics;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import java.util.HashMap;
import java.util.Map;

public class AdAnalytics {
    private static final String TAG = "AdAnalytics";
    private static AdAnalytics instance;
    private final Context context;
    private final Map<String, Integer> impressionCounts;
    private final Map<String, Integer> clickCounts;
    private final Map<String, Integer> loadFailures;
    private final Map<String, Integer> showFailures;

    private AdAnalytics(Context context) {
        this.context = context.getApplicationContext();
        this.impressionCounts = new HashMap<>();
        this.clickCounts = new HashMap<>();
        this.loadFailures = new HashMap<>();
        this.showFailures = new HashMap<>();
    }

    public static AdAnalytics getInstance(Context context) {
        if (instance == null) {
            synchronized (AdAnalytics.class) {
                if (instance == null) {
                    instance = new AdAnalytics(context);
                }
            }
        }
        return instance;
    }

    public void trackImpression(String adUnitId) {
        int count = impressionCounts.getOrDefault(adUnitId, 0);
        impressionCounts.put(adUnitId, count + 1);
        Log.d(TAG, "Ad impression tracked for " + adUnitId + ": " + (count + 1));
    }

    public void trackClick(String adUnitId) {
        int count = clickCounts.getOrDefault(adUnitId, 0);
        clickCounts.put(adUnitId, count + 1);
        Log.d(TAG, "Ad click tracked for " + adUnitId + ": " + (count + 1));
    }

    public void trackLoadFailure(String adUnitId, LoadAdError error) {
        int count = loadFailures.getOrDefault(adUnitId, 0);
        loadFailures.put(adUnitId, count + 1);
        Log.e(TAG, "Ad load failure for " + adUnitId + ": " + error.getMessage());
    }

    public void trackShowFailure(String adUnitId, AdError error) {
        int count = showFailures.getOrDefault(adUnitId, 0);
        showFailures.put(adUnitId, count + 1);
        Log.e(TAG, "Ad show failure for " + adUnitId + ": " + error.getMessage());
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("impressions", impressionCounts);
        analytics.put("clicks", clickCounts);
        analytics.put("loadFailures", loadFailures);
        analytics.put("showFailures", showFailures);
        return analytics;
    }

    public void reset() {
        impressionCounts.clear();
        clickCounts.clear();
        loadFailures.clear();
        showFailures.clear();
        Log.d(TAG, "Analytics reset");
    }
} 