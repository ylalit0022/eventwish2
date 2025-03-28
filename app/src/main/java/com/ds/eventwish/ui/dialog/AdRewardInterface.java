package com.ds.eventwish.ui.dialog;

import android.app.Activity;
import androidx.fragment.app.FragmentManager;

/**
 * Common interface for AdReward dialogs
 * This helps standardize the different implementations
 */
public interface AdRewardInterface {
    /**
     * Show the ad reward dialog
     * @param fragmentManager FragmentManager to use for showing the dialog
     * @param title Custom dialog title (optional)
     */
    void show(FragmentManager fragmentManager, String title);
    
    /**
     * Show the ad reward dialog with default title
     * @param fragmentManager FragmentManager to use for showing the dialog
     */
    void show(FragmentManager fragmentManager);
    
    /**
     * Interface for dialog callbacks
     */
    interface AdRewardCallback {
        /**
         * Called when coins are earned from watching an ad
         * @param amount Amount of coins earned
         */
        void onCoinsEarned(int amount);
        
        /**
         * Called when the feature is unlocked
         * @param durationDays Duration of the unlock in days
         */
        void onFeatureUnlocked(int durationDays);
        
        /**
         * Called when the dialog is dismissed
         */
        void onDismissed();
    }
} 