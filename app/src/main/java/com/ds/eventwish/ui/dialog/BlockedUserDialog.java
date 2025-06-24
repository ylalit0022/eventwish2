package com.ds.eventwish.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.ds.eventwish.R;
import com.ds.eventwish.data.repository.UserRepository;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog for displaying user blocking information
 */
public class BlockedUserDialog extends DialogFragment {
    private static final String TAG = "BlockedUserDialog";
    
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_REASON = "reason";
    private static final String ARG_CONTACT_EMAIL = "contact_email";
    private static final String ARG_BLOCKED_AT = "blocked_at";
    
    private String userId;
    private String reason;
    private String contactEmail;
    private long blockedAt;
    
    private OnDialogDismissedListener listener;
    
    /**
     * Interface for dialog dismissal callback
     */
    public interface OnDialogDismissedListener {
        void onDialogDismissed();
    }
    
    /**
     * Create a new instance of BlockedUserDialog
     */
    public static BlockedUserDialog newInstance(String userId, String reason, String contactEmail, long blockedAt) {
        BlockedUserDialog dialog = new BlockedUserDialog();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_REASON, reason);
        args.putString(ARG_CONTACT_EMAIL, contactEmail);
        args.putLong(ARG_BLOCKED_AT, blockedAt);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            reason = getArguments().getString(ARG_REASON);
            contactEmail = getArguments().getString(ARG_CONTACT_EMAIL);
            blockedAt = getArguments().getLong(ARG_BLOCKED_AT);
        }
        
        Log.d(TAG, "onCreate: Dialog created for user " + userId + " with reason: " + reason);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_blocked_user, null);
        
        // Initialize views
        TextView blockingReasonText = dialogView.findViewById(R.id.blocking_reason_text);
        TextView contactEmailText = dialogView.findViewById(R.id.contact_email_text);
        TextView blockedTimestampText = dialogView.findViewById(R.id.blocked_timestamp_text);
        MaterialButton okButton = dialogView.findViewById(R.id.ok_button);
        
        // Set blocking reason
        if (reason != null && !reason.isEmpty()) {
            blockingReasonText.setText(reason);
        } else {
            blockingReasonText.setText("Account has been blocked");
        }
        
        // Set contact email
        if (contactEmail != null && !contactEmail.isEmpty()) {
            contactEmailText.setText(contactEmail);
        } else {
            contactEmailText.setText("support@eventwish.com");
        }
        
        // Set blocked timestamp
        String formattedTimestamp = formatTimestamp(blockedAt);
        blockedTimestampText.setText("Blocked since: " + formattedTimestamp);
        
        // Set OK button click listener
        okButton.setOnClickListener(v -> {
            Log.d(TAG, "OK button clicked, saving interaction details");
            saveInteractionDetails();
            dismiss();
        });
        
        // Create and configure dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        Log.d(TAG, "onCreateDialog: Dialog created and configured");
        return dialog;
    }
    
    private String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "Unknown date";
        }
    }
    
    private void saveInteractionDetails() {
        try {
            long currentTimestamp = System.currentTimeMillis();
            
            UserRepository userRepository = UserRepository.getInstance(requireContext());
            userRepository.saveBlockingDialogInteraction(userId, reason, contactEmail, currentTimestamp);
            
            Log.d(TAG, "saveInteractionDetails: Saved interaction details for user " + userId + 
                  " at timestamp " + currentTimestamp);
        } catch (Exception e) {
            Log.e(TAG, "Error saving interaction details", e);
        }
    }
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        
        Log.d(TAG, "onDismiss: Dialog dismissed");
        
        if (listener != null) {
            listener.onDialogDismissed();
        }
    }
    
    public void setOnDialogDismissedListener(OnDialogDismissedListener listener) {
        this.listener = listener;
    }
}
