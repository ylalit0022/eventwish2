package com.ds.eventwish.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.ds.eventwish.R;
import com.ds.eventwish.data.repository.CoinsRepository;
import com.ds.eventwish.ui.dialog.UnifiedAdRewardDialog;
import com.ds.eventwish.ui.viewmodel.CoinsViewModel;

/**
 * Central manager for premium features in the app.
 * This class provides methods to check if specific premium features are unlocked.
 */
public class FeatureManager {
    private static final String TAG = "FeatureManager";
    
    // Singleton instance
    private static FeatureManager instance;
    
    // Dependencies
    private final Context context;
    private final CoinsRepository coinsRepository;
    
    // Feature constants
    public static final String HTML_EDITING = "html_editing";
    public static final String TEMPLATE_DOWNLOAD = "template_download";
    public static final String CUSTOM_FONTS = "custom_fonts";
    public static final String PRIORITY_SUPPORT = "priority_support";
    
    /**
     * Private constructor to initialize manager
     * @param context Application context
     */
    private FeatureManager(Context context) {
        this.context = context.getApplicationContext();
        this.coinsRepository = CoinsRepository.getInstance(context);
        
        // Validate unlock status on initialization
        validateUnlockStatus();
    }
    
    /**
     * Get singleton instance
     * @param context Context
     * @return FeatureManager instance
     */
    public static synchronized FeatureManager getInstance(Context context) {
        if (instance == null) {
            instance = new FeatureManager(context);
        }
        return instance;
    }
    
    /**
     * Check if a specific feature is unlocked
     * @param featureKey The feature key to check
     * @return true if the feature is unlocked, false otherwise
     */
    public boolean isFeatureUnlocked(String featureKey) {
        boolean isUnlocked = coinsRepository.isFeatureUnlocked();
        
        // Add extra logic for different feature types if needed
        switch (featureKey) {
            case HTML_EDITING:
                // HTML editing available if any premium is unlocked
                return isUnlocked;
                
            case TEMPLATE_DOWNLOAD:
                // Template download might have additional criteria
                return isUnlocked;
                
            case CUSTOM_FONTS:
                // Custom fonts might be a separate feature
                return isUnlocked;
                
            case PRIORITY_SUPPORT:
                // Priority support might be a separate feature
                return isUnlocked;
                
            default:
                Log.w(TAG, "Unknown feature key: " + featureKey);
                return false;
        }
    }
    
    /**
     * Get remaining time for premium features in milliseconds
     * @return Remaining time in milliseconds
     */
    public long getRemainingTime() {
        return coinsRepository.getRemainingTime();
    }
    
    /**
     * Check if any premium features are unlocked
     * @return true if any premium features are unlocked, false otherwise
     */
    public boolean isPremium() {
        return coinsRepository.isFeatureUnlocked();
    }
    
    /**
     * Get current coins count
     * @return Current coins count
     */
    public int getCoinsCount() {
        return coinsRepository.getCurrentCoins();
    }
    
    /**
     * Validate unlock status to ensure integrity
     */
    public void validateUnlockStatus() {
        coinsRepository.validateUnlockStatus();
    }
    
    /**
     * Check if user has enough coins for a feature
     * @param requiredCoins The number of coins required
     * @return true if user has enough coins, false otherwise
     */
    public boolean hasEnoughCoins(int requiredCoins) {
        return getCoinsCount() >= requiredCoins;
    }
    
    /**
     * Check if a feature is accessible and show unlock dialog if not
     * @param activity Activity context
     * @param featureKey Feature key to check
     * @return true if the feature is accessible, false otherwise
     */
    public boolean checkFeatureAccess(FragmentActivity activity, String featureKey) {
        // First check if feature is already unlocked
        if (isFeatureUnlocked(featureKey)) {
            return true;
        }
        
        // Feature is locked, show dialog to unlock
        showUnlockDialog(activity, featureKey);
        return false;
    }
    
    /**
     * Show dialog to unlock a feature
     * @param activity Activity context
     * @param featureKey Feature key to unlock
     */
    private void showUnlockDialog(FragmentActivity activity, String featureKey) {
        // Show toast with feature-specific message
        String message = getLockedFeatureMessage(featureKey);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        
        // Show UnifiedAdRewardDialog with proper initialization
        UnifiedAdRewardDialog dialog = UnifiedAdRewardDialog.newInstance("Unlock Premium Features")
            .setCallback(new UnifiedAdRewardDialog.AdRewardCallback() {
                @Override
                public void onCoinsEarned(int amount) {
                    Toast.makeText(context, "You earned " + amount + " coins!", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFeatureUnlocked(int durationDays) {
                    Toast.makeText(context, "Feature unlocked for " + durationDays + " days!", Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onDismissed() {
                    // Nothing to do here
                }
            });
        
        dialog.show(activity.getSupportFragmentManager());
    }
    
    /**
     * Get the locked feature message for a specific feature
     * @param featureKey The feature key
     * @return A human-readable message about the locked feature
     */
    public String getLockedFeatureMessage(String featureKey) {
        String message;
        switch (featureKey) {
            case HTML_EDITING:
                message = context.getString(R.string.html_editing_locked);
                break;
            case TEMPLATE_DOWNLOAD:
                message = context.getString(R.string.template_download_locked);
                break;
            case CUSTOM_FONTS:
                message = context.getString(R.string.custom_fonts_locked);
                break;
            case PRIORITY_SUPPORT:
                message = context.getString(R.string.priority_support_locked);
                break;
            default:
                message = context.getString(R.string.premium_feature_locked);
        }
        return message;
    }
    
    /**
     * Show a simple explanation dialog about premium features
     * @param activity The activity context
     */
    public void showPremiumFeaturesExplanation(FragmentActivity activity) {
        // Create builder for the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.premium_features);
        
        // Create message with HTML formatting for better readability
        String message = "EventWish offers premium features that can be unlocked with coins:\n\n" +
                         "• <b>HTML Editing</b>: Customize templates with HTML code\n" +
                         "• <b>Template Downloads</b>: Save templates for offline use\n" +
                         "• <b>Custom Fonts</b>: Access premium font options\n" +
                         "• <b>Priority Support</b>: Get faster support responses\n\n" +
                         "You can earn coins by watching rewarded ads and use them to unlock features for 30 days.";
        
        // Set the message with HTML formatting
        TextView messageView = new TextView(activity);
        messageView.setPadding(48, 32, 48, 32);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            messageView.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT));
        } else {
            messageView.setText(Html.fromHtml(message));
        }
        builder.setView(messageView);
        
        // Add a button to get coins
        builder.setPositiveButton(R.string.watch_ad, (dialog, which) -> {
            // Show the ad reward dialog with our new unified implementation
            UnifiedAdRewardDialog adDialog = UnifiedAdRewardDialog.newInstance("Earn Coins")
                .setCallback(new UnifiedAdRewardDialog.AdRewardCallback() {
                    @Override
                    public void onCoinsEarned(int amount) {
                        Toast.makeText(context, "You earned " + amount + " coins!", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onFeatureUnlocked(int durationDays) {
                        Toast.makeText(context, "Feature unlocked for " + durationDays + " days!", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onDismissed() {
                        // Nothing to do here
                    }
                });
            
            adDialog.show(activity.getSupportFragmentManager());
        });
        
        // Add a cancel button
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Show the dialog
        builder.show();
    }
} 