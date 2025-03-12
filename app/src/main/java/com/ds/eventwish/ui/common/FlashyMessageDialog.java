package com.ds.eventwish.ui.common;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ds.eventwish.R;
import com.ds.eventwish.utils.FlashyMessageManager;

public class FlashyMessageDialog extends DialogFragment {
    private static final String TAG = "FlashyMessageDialog";
    private static final String ARG_MESSAGE_ID = "message_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    
    private String messageId;
    private String title;
    private String message;
    
    public static FlashyMessageDialog newInstance(String messageId, String title, String message) {
        FlashyMessageDialog fragment = new FlashyMessageDialog();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE_ID, messageId);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog_Alert);
        
        if (getArguments() != null) {
            messageId = getArguments().getString(ARG_MESSAGE_ID);
            title = getArguments().getString(ARG_TITLE);
            message = getArguments().getString(ARG_MESSAGE);
        }
        
        Log.d(TAG, "Created dialog for message: " + messageId);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.flashy_message_layout, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView titleTextView = view.findViewById(R.id.flashy_message_title);
        TextView messageTextView = view.findViewById(R.id.flashy_message_content);
        Button closeButton = view.findViewById(R.id.flashy_message_close_button);
        
        titleTextView.setText(title);
        messageTextView.setText(message);
        
        closeButton.setOnClickListener(v -> {
            // Mark the message as shown so it won't be displayed again
            if (messageId != null && getContext() != null) {
                FlashyMessageManager.markMessageAsShown(getContext(), messageId);
                Log.d(TAG, "Marked message as shown: " + messageId);
            }
            dismiss();
        });
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
    
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        
        // Ensure we reset the display state when the dialog is dismissed
        if (getContext() != null) {
            FlashyMessageManager.resetDisplayState(getContext());
            Log.d(TAG, "Reset display state on dismiss");
        }
    }
    
    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        
        // Mark the message as shown if the dialog is canceled
        if (messageId != null && getContext() != null) {
            FlashyMessageManager.markMessageAsShown(getContext(), messageId);
            Log.d(TAG, "Marked message as shown on cancel: " + messageId);
        }
    }
} 