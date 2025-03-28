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
import com.ds.eventwish.ui.viewmodel.AdViewModel;
import com.ds.eventwish.ui.viewmodel.CoinsViewModel;
import java.util.concurrent.TimeUnit;

/**
 * Unified implementation of AdRewardDialog that combines the best of both existing implementations
 */
public class UnifiedAdRewardDialog extends DialogFragment implements AdRewardInterface {
    private static final String TAG = "UnifiedAdRewardDialog";
    private static final String ARG_TITLE = "title";
    
    // Constants
    private static final int REWARD_AMOUNT = 10;    // Coins per ad view
    private static final int REQUIRED_COINS = 50;   // Coins needed to unlock feature
    private static final int UNLOCK_DURATION = 30;  // Days to unlock feature
    
    // ViewModels
    private AdViewModel adViewModel;
    private CoinsViewModel coinsViewModel;
    
    // UI elements
    private TextView coinsText;
    private TextView remainingTimeText;
    private Button watchAdButton;
    private Button unlockFeatureButton;
    private ProgressBar progressBar;
    private TextView errorText;
    
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
        adViewModel = new ViewModelProvider(requireActivity()).get(AdViewModel.class);
        coinsViewModel = new ViewModelProvider(requireActivity()).get(CoinsViewModel.class);
        
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
        adViewModel.loadRewardedAd();
        
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
        
        // Observe ad loading state
        adViewModel.getIsRewardedAdLoading().observe(this, isLoading -> {
            if (progressBar != null && watchAdButton != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                watchAdButton.setEnabled(!isLoading);
                watchAdButton.setText(isLoading ? R.string.loading : R.string.watch_ad);
                
                Log.d(TAG, "Ad loading state changed: " + (isLoading ? "loading" : "not loading"));
            }
        });
        
        // Observe ad ready state
        adViewModel.getIsRewardedAdReady().observe(this, isReady -> {
            if (watchAdButton != null) {
                watchAdButton.setEnabled(isReady);
                Log.d(TAG, "Ad ready state changed: " + (isReady ? "ready" : "not ready"));
            }
        });
        
        // Observe ad error
        adViewModel.getAdError().observe(this, error -> {
            if (errorText != null) {
                if (error != null && !error.isEmpty()) {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText(error);
                    Log.e(TAG, "Ad error: " + error);
                } else {
                    errorText.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        
        watchAdButton.setOnClickListener(v -> {
            Log.d(TAG, "Watch ad button clicked");
            
            // Show progress and disable buttons
            setLoading(true);
            
            // Show the rewarded ad
            adViewModel.showRewardedAd(requireActivity(), success -> {
                // Execute on main thread
                requireActivity().runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "Ad reward result: " + (success ? "Success" : "Failed"));
                        setLoading(false);
                        
                        if (success) {
                            // Show success message
                            showTemporaryMessage("Ad watched successfully! Earning coins...");
                            
                            // Introduce delay to ensure server registers the reward
                            new Handler().postDelayed(() -> {
                                // Explicitly refresh coins with verification
                                verifyCoinsUpdated();
                                
                                // Notify callback
                                if (callback != null) {
                                    callback.onCoinsEarned(REWARD_AMOUNT);
                                }
                            }, 1000);
                        } else {
                            // Show error message
                            errorText.setText(R.string.ad_failed);
                            errorText.setVisibility(View.VISIBLE);
                            
                            // Reload ad after failure
                            adViewModel.loadRewardedAd();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in ad reward callback", e);
                        setLoading(false);
                        errorText.setText(R.string.ad_error);
                        errorText.setVisibility(View.VISIBLE);
                    }
                });
            });
        });
        
        unlockFeatureButton.setOnClickListener(v -> {
            Log.d(TAG, "Unlock feature button clicked");
            
            // Show progress and disable buttons
            setLoading(true);
            
            // Check if user has enough coins
            int currentCoins = coinsViewModel.getCurrentCoins();
            if (currentCoins < REQUIRED_COINS) {
                errorText.setText(getString(R.string.not_enough_coins, REQUIRED_COINS));
                errorText.setVisibility(View.VISIBLE);
                setLoading(false);
                return;
            }
            
            // Unlock the feature
            coinsViewModel.unlockFeature(UNLOCK_DURATION);
            
            // Introduce delay to ensure UI updates
            new Handler().postDelayed(() -> {
                // Explicitly refresh to make sure UI updates
                coinsViewModel.refreshCoinsCount();
                
                // Show success message
                showTemporaryMessage("Feature unlocked successfully!");
                
                // Notify callback
                if (callback != null) {
                    callback.onFeatureUnlocked(UNLOCK_DURATION);
                }
                
                // Dismiss the dialog after a short delay
                new Handler().postDelayed(() -> {
                    dismissAllowingStateLoss();
                }, 1200);
            }, 800);
        });
    }
    
    private void updateUnlockButtonState(int coins) {
        if (unlockFeatureButton != null) {
            boolean canUnlock = coins >= REQUIRED_COINS;
            unlockFeatureButton.setEnabled(canUnlock);
            unlockFeatureButton.setText(getString(R.string.unlock_feature_format, REQUIRED_COINS));
        }
    }
    
    private void showMessage(int messageResId) {
        showMessage(getString(R.string.app_name), getString(messageResId));
    }
    
    private void showMessage(String title, String message) {
        if (getContext() != null) {
            new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Add null checks to prevent crashes
        try {
            // Only verify unlock status if the ViewModel is initialized
            if (coinsViewModel != null) {
                // Verify unlock status when dialog is shown
                coinsViewModel.verifyUnlockStatus();
                
                // Force full background refresh when dialog is shown
                Log.d(TAG, "Performing full background refresh in onResume");
                showTemporaryMessage("Loading latest coin balance...");
                
                coinsViewModel.forceBackgroundRefresh(success -> {
                    Log.d(TAG, "Background refresh completed in onResume: " + (success ? "success" : "failed"));
                    if (!success) {
                        // Fallback to local refresh if server refresh fails
                        coinsViewModel.refreshCoinsCount();
                    }
                });
            } else {
                Log.e(TAG, "coinsViewModel is null in onResume");
                // Try to initialize ViewModels if they're null
                try {
                    coinsViewModel = new ViewModelProvider(requireActivity()).get(CoinsViewModel.class);
                    adViewModel = new ViewModelProvider(requireActivity()).get(AdViewModel.class);
                    
                    if (coinsViewModel != null) {
                        // Now we can safely call methods
                        coinsViewModel.verifyUnlockStatus();
                        
                        // Use improved refresh method
                        Log.d(TAG, "Performing background refresh after ViewModel initialization");
                        coinsViewModel.forceBackgroundRefresh();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to initialize ViewModels in onResume", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called - cleaning up resources");
        
        // Clean up any observers that might cause memory leaks
        if (adViewModel != null) {
            adViewModel.getIsRewardedAdReady().removeObservers(this);
            adViewModel.getIsRewardedAdLoading().removeObservers(this);
            adViewModel.getAdError().removeObservers(this);
        }
        
        if (coinsViewModel != null) {
            coinsViewModel.getCoinsLiveData().removeObservers(this);
            coinsViewModel.getIsUnlockedLiveData().removeObservers(this);
            coinsViewModel.getRemainingTimeLiveData().removeObservers(this);
        }
    }
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(TAG, "onDismiss called");
        
        // Force a final background refresh to ensure UI is updated
        if (coinsViewModel != null) {
            Log.d(TAG, "Final refresh in onDismiss");
            
            // Use the callback implementation to ensure we don't block the UI
            coinsViewModel.forceBackgroundRefresh(success -> {
                Log.d(TAG, "Final refresh in onDismiss completed: " + (success ? "success" : "failed"));
                
                // Notify callback if set
                if (callback != null) {
                    callback.onDismissed();
                }
            });
        } else {
            // Call callback directly if ViewModel is unavailable
            if (callback != null) {
                callback.onDismissed();
            }
        }
    }
    
    /**
     * Verify that coins were actually updated and retry if needed
     */
    private void verifyCoinsUpdated() {
        // Get initial coins count for comparison
        final int initialCoins = coinsViewModel.getCurrentCoins();
        Log.d(TAG, "Verifying coins update. Initial count: " + initialCoins);
        
        // Show temporary loading message
        showTemporaryMessage("Refreshing coin balance...");
        
        // Use improved background refresh with callback
        coinsViewModel.forceBackgroundRefresh(success -> {
            if (success) {
                // Get updated coin count after refresh
                int currentCoins = coinsViewModel.getCurrentCoins();
                Log.d(TAG, "Coin update after server refresh: " + initialCoins + " -> " + currentCoins);
                
                if (currentCoins > initialCoins) {
                    // Success - coins updated
                    Log.d(TAG, "Coin update verified: " + initialCoins + " -> " + currentCoins);
                    
                    // Update UI with animation
                    if (coinsText != null) {
                        updateCoinsDisplay(currentCoins, true);
                    }
                    
                    // Update button state
                    updateUnlockButtonState(currentCoins);
                    
                    // Show success message
                    showTemporaryMessage("+" + (currentCoins - initialCoins) + " coins earned!");
                } else {
                    // Coins didn't increase - retry with delay
                    Log.d(TAG, "Coin update not verified, scheduling retry");
                    verifyCoinsWithRetry(initialCoins, 1);
                }
            } else {
                // Server refresh failed - fall back to local refresh
                Log.d(TAG, "Server refresh failed, trying local refresh with retry");
                coinsViewModel.refreshCoinsCount();
                verifyCoinsWithRetry(initialCoins, 1);
            }
        });
    }
    
    /**
     * Recursively verify coins with increasing retry count and delays
     */
    private void verifyCoinsWithRetry(int initialCoins, int retryCount) {
        if (retryCount > 3) {
            Log.w(TAG, "Giving up on coin verification after 3 retries");
            if (isAdded()) {
                showTemporaryMessage("Coins may take a moment to update");
                coinsViewModel.refreshCoinsCount();
            }
            return;
        }

        // Use exponential backoff for retries
        int delay = retryCount * 800;

        new Handler().postDelayed(() -> {
            // Check if fragment is still attached
            if (!isAdded()) {
                Log.d(TAG, "Fragment no longer attached, abandoning coin verification");
                return;
            }

            // Force refresh again
            coinsViewModel.refreshCoinsCount(success -> {
                // Verify fragment is still attached before proceeding
                if (!isAdded()) {
                    Log.d(TAG, "Fragment no longer attached during refresh callback");
                    return;
                }

                // Rest of the verification logic
                int currentCoins = coinsViewModel.getCurrentCoins();
                Log.d(TAG, "Verification attempt " + retryCount + ": Current coins: " + currentCoins);

                if (currentCoins > initialCoins) {
                    Log.d(TAG, "Coin update verified on retry: " + initialCoins + " -> " + currentCoins);
                    updateCoinsDisplay(currentCoins, true);
                    updateUnlockButtonState(currentCoins);
                    showTemporaryMessage("+" + (currentCoins - initialCoins) + " coins earned!");
                } else {
                    verifyCoinsWithRetry(initialCoins, retryCount + 1);
                }
            });
        }, delay);
    }

    /**
     * Show a temporary message and then hide it
     */
    private void showTemporaryMessage(String message) {
        // First check if fragment is attached and errorText exists
        if (!isAdded() || errorText == null) {
            Log.d(TAG, "Cannot show message - fragment detached or view null");
            return;
        }

        try {
            errorText.setText(message);
            errorText.setTextColor(requireContext().getResources().getColor(R.color.colorSuccess));
            errorText.setVisibility(View.VISIBLE);

            // Hide the message after a delay
            new Handler().postDelayed(() -> {
                // Check again if still attached when hiding
                if (isAdded() && errorText != null) {
                    errorText.setVisibility(View.GONE);
                    errorText.setTextColor(requireContext().getResources().getColor(R.color.colorError));
                }
            }, 3000);
        } catch (Exception e) {
            Log.e(TAG, "Error showing temporary message", e);
        }
    }

    /**
     * Helper method to update the coins display with optional animation
     */
    private void updateCoinsDisplay(int coins, boolean animate) {
        if (!isAdded() || coinsText == null) {
            Log.d(TAG, "Cannot update coins display - fragment detached or view null");
            return;
        }

        try {
            if (animate) {
                coinsText.setAlpha(0.3f);
                coinsText.setText(getString(R.string.coins_format, coins));
                coinsText.animate().alpha(1.0f).setDuration(500).start();
            } else {
                coinsText.setText(getString(R.string.coins_format, coins));
            }
            coinsText.setTag(coins);
        } catch (Exception e) {
            Log.e(TAG, "Error updating coins display", e);
        }
    }

    private void setLoading(boolean isLoading) {
        if (!isAdded()) {
            Log.d(TAG, "Cannot set loading state - fragment detached");
            return;
        }

        if (progressBar != null && watchAdButton != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            watchAdButton.setEnabled(!isLoading);
            watchAdButton.setText(isLoading ? R.string.loading : R.string.watch_ad);
        }
    }
}