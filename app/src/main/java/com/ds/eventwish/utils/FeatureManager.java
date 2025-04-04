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

/**
 * Central manager for premium features in the app.
 * This class provides methods to check if specific premium features are unlocked.
 */
public class FeatureManager {
    private static final String TAG = "FeatureManager";
    
    // Singleton instance
    private static FeatureManager instance;
    
    // Feature keys
    public static final String FEATURE_HTML_EDITING = "html_editing";
    public static final String FEATURE_PREMIUM_TEMPLATES = "premium_templates";
    public static final String FEATURE_REMOVE_ADS = "remove_ads";
    public static final String FEATURE_ADVANCED_COLORS = "advanced_colors";
    
    // Context reference
    private final Context context;
    
    // Private constructor
    private FeatureManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Get singleton instance of FeatureManager
     * @param context Application context
     * @return FeatureManager instance
     */
    public static synchronized FeatureManager getInstance(Context context) {
        if (instance == null) {
            instance = new FeatureManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize ViewModel for the manager
     * This should be called from an activity or fragment
     * @param activity Activity to get ViewModelProvider
     */
    public void initializeViewModel(FragmentActivity activity) {
        // Removed CoinsViewModel initialization
    }
    
    /**
     * Check if a feature is unlocked
     * @param featureKey The key of the feature to check
     * @return true if the feature is unlocked, false otherwise
     */
    public boolean isFeatureUnlocked(String featureKey) {
        // For now, return based on the feature key
        switch (featureKey) {
            case FEATURE_HTML_EDITING:
                return isHtmlEditingUnlocked();
            case FEATURE_PREMIUM_TEMPLATES:
                return isPremiumTemplatesUnlocked();
            case FEATURE_REMOVE_ADS:
                return isAdsRemoved();
            case FEATURE_ADVANCED_COLORS:
                return areAdvancedColorsUnlocked();
            default:
                return false;
        }
    }
    
    /**
     * Check if HTML editing feature is unlocked
     * @return true if HTML editing is unlocked
     */
    public boolean isHtmlEditingUnlocked() {
        // Default to unlocked since coins functionality is removed
        return true;
    }
    
    /**
     * Check if premium templates are unlocked
     * @return true if premium templates are unlocked
     */
    public boolean isPremiumTemplatesUnlocked() {
        // Default to unlocked since coins functionality is removed
        return true;
    }
    
    /**
     * Check if ads are removed
     * @return true if ads are removed
     */
    public boolean isAdsRemoved() {
        // Default to true (ads removed) since coins functionality is removed
        return true;
    }
    
    /**
     * Check if advanced colors are unlocked
     * @return true if advanced colors are unlocked
     */
    public boolean areAdvancedColorsUnlocked() {
        // Default to unlocked since coins functionality is removed
        return true;
    }
    
    /**
     * Get the current coins count
     * @return The number of coins the user has
     */
    public int getCoinsCount() {
        // Default to a large number since coins functionality is removed
        return 9999;
    }
    
    /**
     * Check if the user has enough coins for a purchase
     * @param requiredCoins The number of coins required
     * @return true if the user has enough coins
     */
    public boolean hasEnoughCoins(int requiredCoins) {
        // Default to true since coins functionality is removed
        return true;
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
    }
    
    /**
     * Get the locked feature message for a specific feature
     * @param featureKey The feature key
     * @return A human-readable message about the locked feature
     */
    private String getLockedFeatureMessage(String featureKey) {
        switch (featureKey) {
            case FEATURE_HTML_EDITING:
                return context.getString(R.string.html_editing_locked);
            case FEATURE_PREMIUM_TEMPLATES:
                return context.getString(R.string.premium_templates_locked);
            case FEATURE_REMOVE_ADS:
                return context.getString(R.string.remove_ads_locked);
            case FEATURE_ADVANCED_COLORS:
                return context.getString(R.string.advanced_colors_locked);
            default:
                return context.getString(R.string.feature_locked);
        }
    }
    
    /**
     * Show a premium features dialog
     * @param activity The activity context
     */
    public void showPremiumFeaturesDialog(FragmentActivity activity) {
        if (activity == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.premium_features);
        
        // Inflate custom layout
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_premium_features, null);
        builder.setView(dialogView);
        
        // Set up feature descriptions
        TextView htmlEditingDesc = dialogView.findViewById(R.id.html_editing_desc);
        TextView premiumTemplatesDesc = dialogView.findViewById(R.id.premium_templates_desc);
        TextView removeAdsDesc = dialogView.findViewById(R.id.remove_ads_desc);
        TextView advancedColorsDesc = dialogView.findViewById(R.id.advanced_colors_desc);
        TextView premiumNote = dialogView.findViewById(R.id.premium_note);
        
        // Handle HTML links in the descriptions
        if (htmlEditingDesc != null) {
            htmlEditingDesc.setText(Html.fromHtml(
                    activity.getString(R.string.html_editing_description),
                    Html.FROM_HTML_MODE_COMPACT));
            htmlEditingDesc.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        if (premiumTemplatesDesc != null) {
            premiumTemplatesDesc.setText(Html.fromHtml(
                    activity.getString(R.string.premium_templates_description),
                    Html.FROM_HTML_MODE_COMPACT));
            premiumTemplatesDesc.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        if (removeAdsDesc != null) {
            removeAdsDesc.setText(Html.fromHtml(
                    activity.getString(R.string.remove_ads_description),
                    Html.FROM_HTML_MODE_COMPACT));
            removeAdsDesc.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        if (advancedColorsDesc != null) {
            advancedColorsDesc.setText(Html.fromHtml(
                    activity.getString(R.string.advanced_colors_description),
                    Html.FROM_HTML_MODE_COMPACT));
            advancedColorsDesc.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        if (premiumNote != null) {
            premiumNote.setText(activity.getString(R.string.ads_removed_from_app));
        }
        
        // Set up dialog buttons
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        
        // Optional: set dialog background to be transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        dialog.show();
    }

    /**
     * Show a dialog for feature access when not unlocked
     * @param activity The activity context
     * @param featureKey The feature key
     */
    public void showFeatureLockedDialog(FragmentActivity activity, String featureKey) {
        if (activity == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.feature_locked);
        
        String message = context.getString(R.string.feature_requires_points);
        builder.setMessage(message);
        
        // Set up dialog buttons
        builder.setPositiveButton(R.string.unlock, (dialog, which) -> {
            // Logic to unlock feature with points
            unlockFeature(featureKey);
            dialog.dismiss();
        });
        
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
        });
        
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Unlock a specific feature
     * @param featureKey The feature to unlock
     * @return True if successfully unlocked
     */
    public boolean unlockFeature(String featureKey) {
        // Implement feature unlocking logic
        // For now, just return true to simulate successful unlock
        Toast.makeText(context, R.string.feature_unlocked_message, Toast.LENGTH_SHORT).show();
        return true;
    }

    public void showAdRewardDialog(FragmentActivity activity) {
        if (activity == null) return;
        
        // Show a message to the user that ads have been removed from the app
        Toast.makeText(context, R.string.ads_removed_from_app, Toast.LENGTH_LONG).show();
    }

    public void showFeatureAdRewardDialog(FragmentActivity activity, String featureId) {
        if (activity == null) return;
        
        // Show a message to the user that ads have been removed from the app
        Toast.makeText(context, R.string.feature_requires_points, Toast.LENGTH_LONG).show();
    }

    public void showTemplateAdRewardDialog(FragmentActivity activity, String templateId) {
        if (activity == null) return;
        
        // Show a message to the user that ads have been removed from the app
        Toast.makeText(context, R.string.template_requires_points, Toast.LENGTH_LONG).show();
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void unlockFeature(String featureId, int days) {
        // Removed CoinsViewModel reference - all features are now unlocked by default
        Toast.makeText(context, R.string.feature_unlocked_message, Toast.LENGTH_SHORT).show();
    }

    private void unlockTemplate(String templateId) {
        // Implement template unlocking logic
        Toast.makeText(context, R.string.template_unlocked_message, Toast.LENGTH_SHORT).show();
    }
} 