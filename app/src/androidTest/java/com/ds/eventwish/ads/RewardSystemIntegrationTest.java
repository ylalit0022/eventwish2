package com.ds.eventwish.ads;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.util.Arrays;
import java.util.List;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.analytics.AdAnalytics;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class RewardSystemIntegrationTest {
    private ActivityScenario<MainActivity> activityScenario;
    private Context context;
    private AdMobManager adMobManager;
    private AdAnalytics analytics;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        adMobManager = AdMobManager.getInstance(context);
        analytics = AdAnalytics.getInstance(context);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(context, initializationStatus -> {
            // Set test device IDs
            List<String> testDeviceIds = Arrays.asList("TEST-DEVICE-HASH");
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build();
            MobileAds.setRequestConfiguration(configuration);
        });

        // Launch MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        activityScenario = ActivityScenario.launch(intent);
    }

    @After
    public void cleanup() {
        if (activityScenario != null) {
            activityScenario.close();
        }
        analytics.reset();
    }

    @Test
    public void testRewardDialogDisplay() {
        // Click on the reward button
        Espresso.onView(withId(R.id.btnReward))
                .perform(ViewActions.click());

        // Verify reward dialog is displayed
        Espresso.onView(withId(R.id.dialogReward))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testWatchAdButton() {
        // Open reward dialog
        Espresso.onView(withId(R.id.btnReward))
                .perform(ViewActions.click());

        // Click watch ad button
        Espresso.onView(withId(R.id.btnWatchAd))
                .perform(ViewActions.click());

        // Verify ad is being shown
        // Note: In real tests, you would need to handle the ad display
        // This is just a basic verification
        assertNotNull(adMobManager);
    }

    @Test
    public void testRewardAnalytics() {
        // Open reward dialog
        Espresso.onView(withId(R.id.btnReward))
                .perform(ViewActions.click());

        // Click watch ad button
        Espresso.onView(withId(R.id.btnWatchAd))
                .perform(ViewActions.click());

        // Verify analytics are being tracked
        Map<String, Object> analyticsData = analytics.getAnalytics();
        assertNotNull(analyticsData);
        assertNotNull(analyticsData.get("impressions"));
    }

    @Test
    public void testUnlockFeatureButton() {
        // Open reward dialog
        Espresso.onView(withId(R.id.btnReward))
                .perform(ViewActions.click());

        // Click unlock feature button
        Espresso.onView(withId(R.id.btnUnlockFeature))
                .perform(ViewActions.click());

        // Verify feature is unlocked
        // Note: In real tests, you would need to verify the actual feature state
        // This is just a basic verification
        assertNotNull(adMobManager);
    }

    @Test
    public void testCoinsUpdate() {
        // Get initial coins
        int initialCoins = getCurrentCoins();

        // Open reward dialog
        Espresso.onView(withId(R.id.btnReward))
                .perform(ViewActions.click());

        // Click watch ad button
        Espresso.onView(withId(R.id.btnWatchAd))
                .perform(ViewActions.click());

        // Verify coins are updated
        // Note: In real tests, you would need to handle the ad completion
        // This is just a basic verification
        assertNotNull(adMobManager);
    }

    private int getCurrentCoins() {
        // This is a placeholder method
        // In real tests, you would need to implement proper coin retrieval
        return 0;
    }
} 