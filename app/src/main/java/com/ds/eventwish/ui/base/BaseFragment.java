package com.ds.eventwish.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.ds.eventwish.R;
import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.ui.splash.SplashActivity;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.AuthStateManager;
import com.ds.eventwish.utils.NetworkUtils;

public abstract class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";
    private Snackbar currentSnackbar;
    
    // Flag to determine if this fragment requires authentication
    private boolean requiresAuthentication = true;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // AUTHENTICATION CHECK: This is where we enforce Google Sign-In for all fragments
        // Protected fragments include: HomeFragment, TemplateDetailFragment, ReminderFragment, HistoryFragment, NotificationFragment
        // ResourceFragment is explicitly excluded from this check
        if (requiresAuthentication) {
            // Check if user is authenticated
            if (!isUserAuthenticated()) {
                Log.d(TAG, "User not authenticated, redirecting to SplashActivity");
                redirectToSignIn();
                return;
            }
        }
    }
    
    /**
     * Check if the user is authenticated
     * @return true if authenticated, false otherwise
     */
    protected boolean isUserAuthenticated() {
        try {
            // Use AuthManager to check if user is signed in
            boolean isSignedIn = AuthManager.getInstance().isSignedIn();
            
            // If user is signed in but offline, consider them authenticated
            if (!isSignedIn && !NetworkUtils.isNetworkAvailable(requireContext())) {
                // Fall back to AuthStateManager for offline authentication check
                return AuthStateManager.getInstance(requireContext()).isAuthenticated();
            }
            
            return isSignedIn;
        } catch (Exception e) {
            Log.e(TAG, "Error checking authentication status", e);
            return false;
        }
    }
    
    /**
     * Redirect to sign-in screen
     */
    protected void redirectToSignIn() {
        try {
            Intent intent = new Intent(requireActivity(), SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to sign-in", e);
        }
    }
    
    /**
     * Set whether this fragment requires authentication
     * @param requiresAuth true if authentication is required, false otherwise
     */
    protected void setRequiresAuthentication(boolean requiresAuth) {
        this.requiresAuthentication = requiresAuth;
    }

    protected void showError(String message) {
        if (getView() != null) {
            currentSnackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
            currentSnackbar.show();
        }
    }

    protected void showError(int messageResId) {
        if (getView() != null) {
            showError(getString(messageResId));
        }
    }
    
    protected void hideError() {
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
            currentSnackbar = null;
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Track screen view for all fragments automatically
        try {
            if (getActivity() != null) {
                String screenName = this.getClass().getSimpleName();
                // Use activity context for reliable screen tracking
                AnalyticsUtils.trackScreenView(
                    getActivity(),
                    screenName, 
                    this.getClass().getName()
                );
                Log.d(TAG, screenName + " view tracked via BaseFragment");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking screen view in BaseFragment", e);
        }
    }

    @Override
    public void onDestroyView() {
        hideError();
        super.onDestroyView();
    }
}
