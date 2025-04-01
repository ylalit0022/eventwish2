package com.ds.eventwish.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.ds.eventwish.analytics.AdAnalytics;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAdLoadCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AdMobManagerTest {
    @Mock
    private Activity mockActivity;

    @Mock
    private RewardedAd mockRewardedAd;

    @Mock
    private InterstitialAd mockInterstitialAd;

    @Mock
    private AppOpenAd mockAppOpenAd;

    @Mock
    private OnUserEarnedRewardListener mockRewardListener;

    @Mock
    private AdMobManager.OnAdClosedListener mockClosedListener;

    @Mock
    private AdAnalytics mockAnalytics;

    private AdMobManager adMobManager;
    private Context context;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        adMobManager = AdMobManager.getInstance(context);
    }

    @Test
    public void testLoadRewardedAd() {
        // Setup
        String adUnitId = "test_rewarded_ad_unit";
        RewardedAdLoadCallback loadCallback = new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                // Simulate successful ad load
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Simulate failed ad load
            }
        };

        // Execute
        adMobManager.loadRewardedAd(adUnitId);

        // Verify
        verify(mockAnalytics, never()).trackLoadFailure(anyString(), any(LoadAdError.class));
    }

    @Test
    public void testShowRewardedAd() {
        // Setup
        String adUnitId = "test_rewarded_ad_unit";
        when(mockRewardedAd.isLoaded()).thenReturn(true);

        // Execute
        adMobManager.showRewardedAd(adUnitId, mockRewardListener, mockClosedListener);

        // Verify
        verify(mockRewardedAd).show(any(Activity.class), any(OnUserEarnedRewardListener.class));
        verify(mockAnalytics).trackImpression(adUnitId);
    }

    @Test
    public void testLoadInterstitialAd() {
        // Setup
        String adUnitId = "test_interstitial_ad_unit";
        InterstitialAdLoadCallback loadCallback = new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                // Simulate successful ad load
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Simulate failed ad load
            }
        };

        // Execute
        adMobManager.loadInterstitialAd(adUnitId);

        // Verify
        verify(mockAnalytics, never()).trackLoadFailure(anyString(), any(LoadAdError.class));
    }

    @Test
    public void testShowInterstitialAd() {
        // Setup
        String adUnitId = "test_interstitial_ad_unit";
        when(mockInterstitialAd.isLoaded()).thenReturn(true);

        // Execute
        adMobManager.showInterstitialAd(adUnitId, mockClosedListener);

        // Verify
        verify(mockInterstitialAd).show(any(Activity.class));
        verify(mockAnalytics).trackImpression(adUnitId);
    }

    @Test
    public void testLoadAppOpenAd() {
        // Setup
        AppOpenAdLoadCallback loadCallback = new AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                // Simulate successful ad load
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Simulate failed ad load
            }
        };

        // Execute
        adMobManager.loadAppOpenAd();

        // Verify
        verify(mockAnalytics, never()).trackLoadFailure(anyString(), any(LoadAdError.class));
    }

    @Test
    public void testShowAppOpenAd() {
        // Setup
        when(mockAppOpenAd.isLoaded()).thenReturn(true);

        // Execute
        adMobManager.showAppOpenAdIfAvailable();

        // Verify
        verify(mockAppOpenAd).show(any(Activity.class));
        verify(mockAnalytics).trackImpression("app_open");
    }

    @Test
    public void testAdLoadFailure() {
        // Setup
        String adUnitId = "test_ad_unit";
        LoadAdError mockError = new LoadAdError(1, "test", "test", null, null);

        // Execute
        doAnswer(invocation -> {
            RewardedAdLoadCallback callback = invocation.getArgument(1);
            callback.onAdFailedToLoad(mockError);
            return null;
        }).when(mockRewardedAd).load(any(Context.class), anyString(), any(AdRequest.class), any(RewardedAdLoadCallback.class));

        // Verify
        verify(mockAnalytics).trackLoadFailure(adUnitId, mockError);
    }

    @Test
    public void testAdShowFailure() {
        // Setup
        String adUnitId = "test_ad_unit";
        AdError mockError = new AdError(1, "test", "test");

        // Execute
        doAnswer(invocation -> {
            FullScreenContentCallback callback = invocation.getArgument(0);
            callback.onAdFailedToShowFullScreenContent(mockError);
            return null;
        }).when(mockRewardedAd).setFullScreenContentCallback(any(FullScreenContentCallback.class));

        // Verify
        verify(mockAnalytics).trackShowFailure(adUnitId, mockError);
    }
} 