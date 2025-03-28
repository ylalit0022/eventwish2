package com.ds.eventwish.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.ds.eventwish.R;
import com.ds.eventwish.ui.viewmodel.AdViewModel;
import com.ds.eventwish.ui.viewmodel.CoinsViewModel;
import java.util.concurrent.TimeUnit;

public class AdRewardDialog extends DialogFragment {
    private static final String TAG = "AdRewardDialog";
    private AdViewModel adViewModel;
    private CoinsViewModel coinsViewModel;
    private TextView coinsText;
    private TextView remainingTimeText;
    private Button watchAdButton;
    private Button unlockFeatureButton;
    private ProgressBar progressBar;
    private TextView errorText;
    private static final int REQUIRED_COINS = 50; // Required coins to unlock feature
    private static final int UNLOCK_DURATION = 30; // Duration in days
    private static final int REWARD_AMOUNT = 10; // Coins rewarded per ad view

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
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

        return new AlertDialog.Builder(context)
            .setTitle(R.string.reward_dialog_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create();
    }

    private void setupObservers() {
        // Observe coins
        coinsViewModel.getCoinsLiveData().observe(this, coins -> {
            coinsText.setText(getString(R.string.coins_format, coins));
            updateUnlockButtonState(coins);
            Log.d(TAG, "Coins updated: " + coins);
        });

        // Observe remaining time
        coinsViewModel.getRemainingTimeLiveData().observe(this, remainingTime -> {
            if (remainingTime > 0) {
                long days = TimeUnit.MILLISECONDS.toDays(remainingTime);
                long hours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24;
                remainingTimeText.setText(getString(R.string.remaining_time_format, days, hours));
                remainingTimeText.setVisibility(View.VISIBLE);
                // Hide buttons if feature is already unlocked
                watchAdButton.setVisibility(View.GONE);
                unlockFeatureButton.setVisibility(View.GONE);
            } else {
                remainingTimeText.setVisibility(View.GONE);
                watchAdButton.setVisibility(View.VISIBLE);
                unlockFeatureButton.setVisibility(View.VISIBLE);
            }
        });

        // Observe ad loading state
        adViewModel.getIsRewardedAdLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            watchAdButton.setEnabled(!isLoading);
            watchAdButton.setText(isLoading ? R.string.loading : R.string.watch_ad);
        });

        // Observe ad ready state
        adViewModel.getIsRewardedAdReady().observe(this, isReady -> {
            watchAdButton.setEnabled(isReady);
        });
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners");
        
        watchAdButton.setOnClickListener(v -> {
            Log.d(TAG, "Watch ad button clicked");
            if (adViewModel.getIsRewardedAdReady().getValue() == Boolean.TRUE) {
                watchAdButton.setEnabled(false);
                watchAdButton.setText(R.string.loading);
                
                adViewModel.showRewardedAd(requireActivity(), success -> {
                    Log.d(TAG, "Ad reward callback: success=" + success);
                    if (success) {
                        // Add coins when ad is successfully viewed
                        coinsViewModel.addCoins(REWARD_AMOUNT);
                        
                        // Force refresh coins to ensure UI updates
                        coinsViewModel.refreshCoinsCount();
                        
                        // Show success message
                        showMessage(R.string.reward_success);
                    } else {
                        // Show error message
                        showMessage(R.string.reward_error);
                        // Re-enable the button
                        watchAdButton.setEnabled(true);
                        watchAdButton.setText(R.string.watch_ad);
                    }
                });
            } else {
                // Ad not ready, try to load it
                Log.d(TAG, "Ad not ready, attempting to load one");
                watchAdButton.setEnabled(false);
                watchAdButton.setText(R.string.loading);
                adViewModel.loadRewardedAd();
            }
        });

        unlockFeatureButton.setOnClickListener(v -> {
            int currentCoins = coinsViewModel.getCurrentCoins();
            Log.d(TAG, "Unlock feature button clicked, current coins: " + currentCoins);
            
            if (currentCoins >= REQUIRED_COINS) {
                unlockFeatureButton.setEnabled(false);
                coinsViewModel.unlockFeature(UNLOCK_DURATION);
                
                // Force refresh coins to ensure UI updates after spending coins
                coinsViewModel.refreshCoinsCount();
                
                showMessage(R.string.html_editing_unlocked);
                dismiss();
            } else {
                // Not enough coins
                showMessage("Not Enough Coins", 
                    getString(R.string.not_enough_coins, REQUIRED_COINS - currentCoins));
            }
        });
    }

    private void updateUnlockButtonState(int coins) {
        boolean canUnlock = coins >= REQUIRED_COINS;
        unlockFeatureButton.setEnabled(canUnlock);
        unlockFeatureButton.setText(getString(R.string.unlock_feature_format, REQUIRED_COINS));
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
        // Verify unlock status when dialog is shown
        coinsViewModel.verifyUnlockStatus();
        // Force refresh coins count when dialog is shown
        coinsViewModel.refreshCoinsCount();
    }
}