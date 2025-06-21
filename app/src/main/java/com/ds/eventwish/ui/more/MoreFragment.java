package com.ds.eventwish.ui.more;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import com.ds.eventwish.BuildConfig;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentMoreBinding;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.splash.SplashActivity;
import com.ds.eventwish.ui.viewmodel.AppUpdateViewModel;
import com.ds.eventwish.utils.AuthStateManager;
import com.google.firebase.auth.FirebaseAuth;

public class MoreFragment extends BaseFragment {
    private FragmentMoreBinding binding;
    private AppUpdateViewModel updateViewModel;
    private static final String TAG = "MoreFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
        setupUpdateChecker();
    }

    private void setupUpdateChecker() {
        updateViewModel = AppUpdateViewModel.getInstance(requireContext());
        updateViewModel.init(requireActivity());
        
        // Verify Remote Config setup
        if (BuildConfig.DEBUG && updateViewModel.getRemoteConfigManager() != null) {
            updateViewModel.getRemoteConfigManager().verifyRemoteConfigSetup();
        }
        
        // Observe update availability for the indicator
        updateViewModel.getIsUpdateAvailable().observe(getViewLifecycleOwner(), isAvailable -> {
            binding.updateIndicator.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
        });
        
        // Observe error messages
        updateViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
        
        // Check silently for updates to update the indicator
        if (BuildConfig.DEBUG) {
            // For debug builds, use Remote Config
            updateViewModel.checkForUpdatesWithRemoteConfigSilently();
        } else {
            // For production builds, use Play Store
            updateViewModel.checkForUpdatesSilently();
        }
    }

    private void setupClickListeners() {
//        binding.settingsCard.setOnClickListener(v -> {
//            // Navigate to settings
//            // Navigation.findNavController(v).navigate(R.id.action_more_to_settings);
//        });

        binding.profileCard.setOnClickListener(v -> {
            // Navigate to profile
            Navigation.findNavController(v).navigate(R.id.action_more_to_profile);
        });

        binding.aboutCard.setOnClickListener(v -> {
            // Navigate to about
            Navigation.findNavController(v).navigate(R.id.action_more_to_about);
        });

        binding.helpCard.setOnClickListener(v -> {
            // Navigate to contact
            Navigation.findNavController(v).navigate(R.id.action_more_to_contact);
        });
        
        binding.updateCard.setOnClickListener(v -> {
            // Force check for updates and show dialog
            if (getActivity() != null) {
                Toast.makeText(requireContext(), "Checking for updates...", Toast.LENGTH_SHORT).show();
                
                if (BuildConfig.DEBUG) {
                    // For debug builds, use Remote Config
                    updateViewModel.forceCheckForUpdatesWithRemoteConfig();
                } else {
                    // For production builds, use Play Store
                    updateViewModel.checkForUpdates(true);
                }
            }
        });
        
        // Set up sign out button click listener
        binding.signOutCard.setOnClickListener(v -> {
            showSignOutConfirmationDialog();
        });
    }
    
    private void showSignOutConfirmationDialog() {
        android.util.Log.d(TAG, "showSignOutConfirmationDialog: Showing sign-out confirmation dialog");
        new AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes", (dialog, which) -> {
                android.util.Log.d(TAG, "showSignOutConfirmationDialog: User confirmed sign-out");
                signOut();
            })
            .setNegativeButton("No", (dialog, which) -> {
                android.util.Log.d(TAG, "showSignOutConfirmationDialog: User cancelled sign-out");
            })
            .show();
    }
    
    private void signOut() {
        android.util.Log.d(TAG, "signOut: Starting sign-out process");
        
        // Show loading indicator
        View loadingOverlay = binding.loadingOverlay;
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
            android.util.Log.d(TAG, "signOut: Showing loading overlay");
        }
        
        // Use AuthManager to sign out and revoke access
        com.ds.eventwish.data.auth.AuthManager authManager = com.ds.eventwish.data.auth.AuthManager.getInstance();
        android.util.Log.d(TAG, "signOut: Calling AuthManager.signOut()");
        
        authManager.signOut(new com.ds.eventwish.data.auth.AuthManager.SignOutCallback() {
            @Override
            public void onSignOutComplete() {
                android.util.Log.d(TAG, "signOut: AuthManager.signOut() completed");
                
                // Clear any local user data
                if (getContext() != null) {
                    // Clear auth_prefs
                    android.util.Log.d(TAG, "signOut: Clearing auth_prefs");
                    getContext().getSharedPreferences("auth_prefs", 0)
                        .edit()
                        .putBoolean("user_authenticated", false)
                        .putBoolean("access_revoked", true) // Mark access as revoked
                        .apply();
                    
                    // Also clear AuthStateManager
                    android.util.Log.d(TAG, "signOut: Clearing AuthStateManager");
                    AuthStateManager.getInstance(requireContext()).clearAuthentication();
                }
                
                // Hide loading indicator
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                    android.util.Log.d(TAG, "signOut: Hiding loading overlay");
                }
                
                // Show success message
                android.util.Log.d(TAG, "signOut: Showing success toast");
                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                
                // Navigate to SplashActivity
                android.util.Log.d(TAG, "signOut: Navigating to SplashActivity");
                Intent intent = new Intent(requireContext(), SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                // Finish current activity
                if (getActivity() != null) {
                    android.util.Log.d(TAG, "signOut: Finishing current activity");
                    getActivity().finish();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
