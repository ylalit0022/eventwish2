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
        new AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes", (dialog, which) -> {
                signOut();
            })
            .setNegativeButton("No", null)
            .show();
    }
    
    private void signOut() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Clear any local user data
        if (getContext() != null) {
            // Clear auth_prefs
            getContext().getSharedPreferences("auth_prefs", 0)
                .edit()
                .putBoolean("user_authenticated", false)
                .apply();
            
            // Also clear AuthStateManager
            AuthStateManager.getInstance(requireContext()).clearAuthentication();
        }
        
        // Show success message
        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to SplashActivity
        Intent intent = new Intent(requireContext(), SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        // Finish current activity
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
