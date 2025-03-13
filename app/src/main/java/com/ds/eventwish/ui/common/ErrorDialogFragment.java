package com.ds.eventwish.ui.common;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.ds.eventwish.R;
import com.ds.eventwish.utils.ErrorHandler;
import com.ds.eventwish.utils.NetworkUtils;

/**
 * Dialog fragment for displaying critical errors to the user
 */
public class ErrorDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_ERROR_TYPE = "error_type";
    
    private String message;
    private ErrorHandler.ErrorType errorType;
    
    /**
     * Create a new instance of ErrorDialogFragment
     * @param message Error message to display
     * @param errorType Type of error
     * @return ErrorDialogFragment instance
     */
    public static ErrorDialogFragment newInstance(String message, ErrorHandler.ErrorType errorType) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putSerializable(ARG_ERROR_TYPE, errorType);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            message = getArguments().getString(ARG_MESSAGE);
            errorType = (ErrorHandler.ErrorType) getArguments().getSerializable(ARG_ERROR_TYPE);
        }
        
        // Use a style that allows for a larger dialog
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Dialog);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        // Inflate custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_error, null);
        
        // Set up views
        TextView titleView = view.findViewById(R.id.error_title);
        TextView messageView = view.findViewById(R.id.error_message);
        ImageView iconView = view.findViewById(R.id.error_icon);
        Button primaryButton = view.findViewById(R.id.error_primary_button);
        Button secondaryButton = view.findViewById(R.id.error_secondary_button);
        
        // Set error message
        messageView.setText(message);
        
        // Configure dialog based on error type
        configureDialogForErrorType(titleView, iconView, primaryButton, secondaryButton);
        
        // Build dialog
        builder.setView(view);
        
        // Create dialog
        AlertDialog dialog = builder.create();
        
        // Prevent dialog from being dismissed on outside touch for critical errors
        dialog.setCanceledOnTouchOutside(false);
        
        return dialog;
    }
    
    /**
     * Configure dialog UI based on error type
     * @param titleView Title TextView
     * @param iconView Icon ImageView
     * @param primaryButton Primary action button
     * @param secondaryButton Secondary action button
     */
    private void configureDialogForErrorType(
            TextView titleView,
            ImageView iconView,
            Button primaryButton,
            Button secondaryButton) {
        
        // Default configuration
        String title = getString(R.string.error_title_default);
        int iconResId = R.drawable.ic_error;
        String primaryButtonText = getString(R.string.error_button_ok);
        String secondaryButtonText = null;
        
        // Configure based on error type
        switch (errorType) {
            case OFFLINE:
                title = getString(R.string.error_title_offline);
                iconResId = R.drawable.ic_offline;
                primaryButtonText = getString(R.string.error_button_retry);
                secondaryButtonText = getString(R.string.error_button_settings);
                
                // Set up retry button
                primaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogRetry(errorType);
                    }
                });
                
                // Set up settings button
                secondaryButton.setVisibility(View.VISIBLE);
                secondaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogSettings(errorType);
                    }
                });
                break;
                
            case NETWORK:
            case TIMEOUT:
                title = getString(R.string.error_title_network);
                iconResId = R.drawable.ic_network_error;
                primaryButtonText = getString(R.string.error_button_retry);
                
                // Set up retry button
                primaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogRetry(errorType);
                    }
                });
                break;
                
            case SERVER:
                title = getString(R.string.error_title_server);
                iconResId = R.drawable.ic_server_error;
                primaryButtonText = getString(R.string.error_button_retry);
                
                // Set up retry button
                primaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogRetry(errorType);
                    }
                });
                break;
                
            case AUTHENTICATION_REQUIRED:
                title = getString(R.string.error_title_auth);
                iconResId = R.drawable.ic_auth_error;
                primaryButtonText = getString(R.string.error_button_login);
                
                // Set up login button
                primaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogLogin(errorType);
                    }
                });
                break;
                
            case RESOURCE_NOT_FOUND:
            case RESOURCE_EXPIRED:
            case RESOURCE_INVALID:
                title = getString(R.string.error_title_resource);
                iconResId = R.drawable.ic_resource_error;
                primaryButtonText = getString(R.string.error_button_retry);
                
                // Set up retry button
                primaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogRetry(errorType);
                    }
                });
                break;
                
            case PERMISSION_DENIED:
                title = getString(R.string.error_title_permission);
                iconResId = R.drawable.ic_permission_error;
                primaryButtonText = getString(R.string.error_button_settings);
                
                // Set up settings button
                primaryButton.setOnClickListener(v -> {
                    dismiss();
                    if (getActivity() instanceof ErrorDialogListener) {
                        ((ErrorDialogListener) getActivity()).onErrorDialogSettings(errorType);
                    }
                });
                break;
                
            case UNKNOWN:
            case CLIENT:
            default:
                // Default configuration
                primaryButton.setOnClickListener(v -> dismiss());
                break;
        }
        
        // Set title and icon
        titleView.setText(title);
        iconView.setImageResource(iconResId);
        
        // Set button text
        primaryButton.setText(primaryButtonText);
        
        // Configure secondary button visibility
        if (secondaryButtonText != null) {
            secondaryButton.setText(secondaryButtonText);
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof ErrorDialogListener) {
            ((ErrorDialogListener) getActivity()).onErrorDialogDismissed(errorType);
        }
    }
    
    /**
     * Interface for error dialog callbacks
     */
    public interface ErrorDialogListener {
        /**
         * Called when the retry button is clicked
         * @param errorType Type of error
         */
        void onErrorDialogRetry(ErrorHandler.ErrorType errorType);
        
        /**
         * Called when the settings button is clicked
         * @param errorType Type of error
         */
        void onErrorDialogSettings(ErrorHandler.ErrorType errorType);
        
        /**
         * Called when the login button is clicked
         * @param errorType Type of error
         */
        void onErrorDialogLogin(ErrorHandler.ErrorType errorType);
        
        /**
         * Called when the dialog is dismissed
         * @param errorType Type of error
         */
        void onErrorDialogDismissed(ErrorHandler.ErrorType errorType);
    }
} 