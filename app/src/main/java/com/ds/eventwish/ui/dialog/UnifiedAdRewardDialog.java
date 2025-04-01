package com.ds.eventwish.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.ds.eventwish.R;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.ui.viewmodel.CoinsViewModel;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import java.util.concurrent.TimeUnit;

/**
 * Unified implementation of AdRewardDialog that combines the best of both existing implementations
 */
public class UnifiedAdRewardDialog extends DialogFragment implements AdRewardInterface {
    private static final String TAG = "UnifiedAdRewardDialog";
    private static final String ARG_TITLE = "title";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ad unit ID
    
    // Constants
    private static final int REWARD_AMOUNT = 10;    // Coins per ad view
    private static final int REQUIRED_COINS = 50;   // Coins needed to unlock feature
    private static final int UNLOCK_DURATION = 30;  // Days to unlock feature
    
    // ViewModels
    private CoinsViewModel coinsViewModel;
    
    // UI elements
    private TextView coinsText;
    private TextView remainingTimeText;
    private Button watchAdButton;
    private Button unlockFeatureButton;
    private ProgressBar progressBar;
    private TextView errorText;
    
    // AdMobManager
    private AdMobManager adMobManager;
    
    // Callback
    private AdRewardCallback callback;
    
    /**
     * Factory method to create a new instance of UnifiedAdRewardDialog
     * @param title Dialog title (optional)
     * @return A new instance of UnifiedAdRewardDialog
     */
    public static UnifiedAdRewardDialog newInstance(@Nullable String title) {
        UnifiedAdRewardDialog dialog = new UnifiedAdRewardDialog();
        Bundle args = new Bundle();
        if (title != null) {
            args.putString(ARG_TITLE, title);
        }
        dialog.setArguments(args);
        return dialog;
    }
    
    /**
     * Set the callback for dialog events
     * @param callback AdRewardCallback instance
     * @return This dialog for method chaining
     */
    public UnifiedAdRewardDialog setCallback(AdRewardCallback callback) {
        this.callback = callback;
        return this;
    }
    
    @Override
    public void show(FragmentManager fragmentManager, String title) {
        try {
            // Ensure we have valid arguments
            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
                setArguments(args);
            }
            
            if (title != null) {
                args.putString(ARG_TITLE, title);
            }
            
            // Check if the fragment manager is valid and the dialog isn't already added
            if (fragmentManager != null && !isAdded()) {
                // Use a safe way to show the dialog
                String tag = "UnifiedAdRewardDialog";
                fragmentManager.beginTransaction()
                    .add(this, tag)
                    .commitAllowingStateLoss();
            } else {
                Log.e(TAG, "Cannot show dialog: " + 
                    (fragmentManager == null ? "Fragment manager is null" : "Dialog already added"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog with title", e);
        }
    }
    
    @Override
    public void show(FragmentManager fragmentManager) {
        try {
            if (fragmentManager != null && !isAdded()) {
                show(fragmentManager, null);
            } else {
                Log.e(TAG, "Cannot show dialog");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog", e);
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_ad_reward, null);
        
        // Initialize ViewModels using activity scope to share data
        coinsViewModel = new ViewModelProvider(requireActivity()).get(CoinsViewModel.class);
        
        // Initialize AdMobManager
        adMobManager = AdMobManager.getInstance(context);
        
        // Initialize views
        coinsText = view.findViewById(R.id.coins_text);
        remainingTimeText = view.findViewById(R.id.remaining_time_text);
        watchAdButton = view.findViewById(R.id.watch_ad_button);
        unlockFeatureButton = view.findViewById(R.id.unlock_feature_button);
        progressBar = view.findViewById(R.id.progress_bar);
        errorText = view.findViewById(R.id.error_text);
        
        // Set up observers
        setupObservers();
        
        // Set up click listeners
        setupClickListeners();
        
        // Load rewarded ad
        adMobManager.loadRewardedAd(REWARDED_AD_UNIT_ID);
        
        // Get dialog title from arguments
        String title = getArguments() != null 
            ? getArguments().getString(ARG_TITLE, getString(R.string.reward_dialog_title)) 
            : getString(R.string.reward_dialog_title);
        
        // Create and return the dialog
        return new AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                if (callback != null) {
                    callback.onDismissed();
                }
            })
            .create();
    }
    
    private void setupObservers() {
        // Observe coins with better logging and UI feedback
        coinsViewModel.getCoinsLiveData().observe(this, coins -> {
            if (coinsText != null) {
                Log.d(TAG, "Coins observer triggered with value: " + coins);
                
                // Add animation effect for coin updates
                if (coinsText.getTag() instanceof Integer) {
                    int oldValue = (Integer) coinsText.getTag();
                    if (oldValue != coins) {
                        // Animate the text change if the value has changed
                        coinsText.setAlpha(0.3f);
                        coinsText.setText(getString(R.string.coins_format, coins));
                        coinsText.animate().alpha(1.0f).setDuration(500).start();
                        
                        // Log the change
                        Log.d(TAG, "Coin value changed from " + oldValue + " to " + coins);
                    } else {
                        // Just update the text without animation if value is the same
                        coinsText.setText(getString(R.string.coins_format, coins));
                    }
                } else {
                    // First time setting the value
                    coinsText.setText(getString(R.string.coins_format, coins));
                }
                
                // Save the current value as tag for future comparison
                coinsText.setTag(coins);
                
                // Update button state
                updateUnlockButtonState(coins);
                
                // Make sure the button states are updated correctly
                if (unlockFeatureButton != null) {
                    boolean canUnlock = coins >= REQUIRED_COINS;
                    unlockFeatureButton.setEnabled(canUnlock);
                    
                    if (canUnlock) {
                        Log.d(TAG, "User now has enough coins to unlock features");
                    }
                }
            }
        });
        
        // Also observe coin update events for guaranteed UI updates
        coinsViewModel.getCoinUpdateEvent().observe(this, coins -> {
            Log.d(TAG, "Coin update event received with value: " + coins);
            // Just update the button state and don't animate since this is a forced update
            if (coinsText != null) {
                coinsText.setText(getString(R.string.coins_format, coins));
                coinsText.setTag(coins);
            }
            updateUnlockButtonState(coins);
        });
        
        // Observe remaining time with improved formatting
        coinsViewModel.getRemainingTimeLiveData().observe(this, remainingTime -> {
            if (remainingTimeText != null && watchAdButton != null && unlockFeatureButton != null) {
                if (remainingTime > 0) {
                    // Use improved formatting from ViewModel
                    String formattedTime = coinsViewModel.formatRemainingTime(remainingTime);
                    
                    // Set the formatted text
                    remainingTimeText.setText(formattedTime);
                    remainingTimeText.setVisibility(View.VISIBLE);
                    
                    // Hide buttons if feature is already unlocked
                    watchAdButton.setVisibility(View.GONE);
                    unlockFeatureButton.setVisibility(View.GONE);
                    
                    // Log the time for debugging
                    Log.d(TAG, "Feature is unlocked with remaining time: " + formattedTime);
                } else {
                    remainingTimeText.setVisibility(View.GONE);
                    watchAdButton.setVisibility(View.VISIBLE);
                    unlockFeatureButton.setVisibility(View.VISIBLE);
                    
                    Log.d(TAG, "Feature is not unlocked, showing buttons");
                }
            }
        });
    }
    
    private void setupClickListeners() {
        if (watchAdButton != null) {
            watchAdButton.setOnClickListener(v -> {
                if (adMobManager != null && adMobManager.canShowAd(REWARDED_AD_UNIT_ID)) {
                    setLoading(true);
                    adMobManager.showRewardedAd(REWARDED_AD_UNIT_ID, new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                            // Handle reward earned
                            if (coinsViewModel != null) {
                                coinsViewModel.addCoins(REWARD_AMOUNT);
                                if (callback != null) {
                                    callback.onCoinsEarned(REWARD_AMOUNT);
                                }
                            }
                        }
                    }, new AdMobManager.OnAdClosedListener() {
                        @Override
                        public void onAdClosed() {
                            setLoading(false);
                            // Reload the ad for next time
                            adMobManager.loadRewardedAd(REWARDED_AD_UNIT_ID);
                        }
                    });
                } else {
                    showMessage(R.string.ad_not_ready);
                }
            });
        }
        
        if (unlockFeatureButton != null) {
            unlockFeatureButton.setOnClickListener(v -> {
                if (coinsViewModel != null) {
                    int currentCoins = coinsViewModel.getCoinsLiveData().getValue();
                    if (currentCoins >= REQUIRED_COINS) {
                        // Deduct coins and unlock feature
                        coinsViewModel.deductCoins(REQUIRED_COINS);
                        coinsViewModel.unlockFeature(UNLOCK_DURATION);
                        
                        if (callback != null) {
                            callback.onFeatureUnlocked(UNLOCK_DURATION);
                        }
                        
                        // Dismiss dialog
                        dismiss();
                    } else {
                        showMessage(R.string.not_enough_coins);
                    }
                }
            });
        }
    }
    
    private void updateUnlockButtonState(int coins) {
        if (unlockFeatureButton != null) {
            boolean canUnlock = coins >= REQUIRED_COINS;
            unlockFeatureButton.setEnabled(canUnlock);
            unlockFeatureButton.setAlpha(canUnlock ? 1.0f : 0.5f);
        }
    }
    
    private void showMessage(int messageResId) {
        if (errorText != null) {
            errorText.setText(messageResId);
            errorText.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                if (errorText != null) {
                    errorText.setVisibility(View.GONE);
                }
            }, 3000);
        }
    }
    
    private void showMessage(String title, String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload rewarded ad if needed
        if (adMobManager != null) {
            adMobManager.loadRewardedAd(REWARDED_AD_UNIT_ID);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up any resources
        if (callback != null) {
            callback.onDismissed();
        }
    }
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (callback != null) {
            callback.onDismissed();
        }
    }
    
    private void setLoading(boolean isLoading) {
        if (progressBar != null && watchAdButton != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            watchAdButton.setEnabled(!isLoading);
            watchAdButton.setText(isLoading ? R.string.loading : R.string.watch_ad);
        }
    }
}