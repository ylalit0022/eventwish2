package com.ds.eventwish.ui.base;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.ds.eventwish.R;
import com.ds.eventwish.utils.AnalyticsUtils;

public abstract class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";
    private Snackbar currentSnackbar;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
