package com.ds.eventwish;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.utils.FirebaseInAppMessagingHandler;
import com.ds.eventwish.utils.FirebaseTokenLogger;
import com.ds.eventwish.utils.FlashyMessageDebugger;
import com.ds.eventwish.utils.FlashyMessageTestUtil;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;

/**
 * Activity for testing flashy messages without using Firebase
 */
public class TestFlashyMessageActivity extends AppCompatActivity {
    
    private EditText titleEditText;
    private EditText messageEditText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_flashy_message);
        
        titleEditText = findViewById(R.id.edit_text_title);
        messageEditText = findViewById(R.id.edit_text_message);
        
        Button createButton = findViewById(R.id.button_create);
        Button createDefaultButton = findViewById(R.id.button_create_default);
        Button clearButton = findViewById(R.id.button_clear);
        Button goToMainButton = findViewById(R.id.button_go_to_main);
        
        // Add Firebase verification buttons
        Button showTokenButton = findViewById(R.id.button_show_token);
        Button showInstallIdButton = findViewById(R.id.button_show_install_id);
        Button triggerInAppButton = findViewById(R.id.button_trigger_in_app);
        
        // Add debugging buttons
        Button dumpMessagesButton = findViewById(R.id.button_dump_messages);
        Button forceRefreshButton = findViewById(R.id.button_force_refresh);
        Button resetStateButton = findViewById(R.id.button_reset_state);
        
        createButton.setOnClickListener(v -> createFlashyMessage());
        createDefaultButton.setOnClickListener(v -> createDefaultFlashyMessage());
        clearButton.setOnClickListener(v -> clearFlashyMessages());
        goToMainButton.setOnClickListener(v -> goToMainActivity());
        
        // Set up Firebase verification button listeners
        if (showTokenButton != null) {
            showTokenButton.setOnClickListener(v -> showFirebaseToken());
        }
        
        if (showInstallIdButton != null) {
            showInstallIdButton.setOnClickListener(v -> showFirebaseInstallId());
        }
        
        if (triggerInAppButton != null) {
            triggerInAppButton.setOnClickListener(v -> triggerInAppMessage());
        }
        
        // Set up debugging button listeners
        if (dumpMessagesButton != null) {
            dumpMessagesButton.setOnClickListener(v -> dumpFlashyMessages());
        }
        
        if (forceRefreshButton != null) {
            forceRefreshButton.setOnClickListener(v -> forceRefreshFlashyMessages());
        }
        
        if (resetStateButton != null) {
            resetStateButton.setOnClickListener(v -> resetFlashyMessageState());
        }
    }
    
    private void createFlashyMessage() {
        String title = titleEditText.getText().toString().trim();
        String message = messageEditText.getText().toString().trim();
        
        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FlashyMessageTestUtil.createTestFlashyMessage(this, title, message);
        Toast.makeText(this, "Flashy message created! Go to main activity to see it.", Toast.LENGTH_LONG).show();
    }
    
    private void createDefaultFlashyMessage() {
        FlashyMessageTestUtil.createDefaultTestFlashyMessage(this);
        Toast.makeText(this, "Default flashy message created! Go to main activity to see it.", Toast.LENGTH_LONG).show();
    }
    
    private void clearFlashyMessages() {
        FlashyMessageTestUtil.clearAllFlashyMessages(this);
        Toast.makeText(this, "All flashy messages cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void goToMainActivity() {
        finish();
    }
    
    /**
     * Show Firebase FCM token for verification
     */
    private void showFirebaseToken() {
        FirebaseTokenLogger.logFirebaseToken(this, true);
    }
    
    /**
     * Show Firebase Installation ID for verification
     */
    private void showFirebaseInstallId() {
        FirebaseTokenLogger.logFirebaseInstallationId(this);
    }
    
    /**
     * Trigger Firebase In-App Messaging for testing
     */
    private void triggerInAppMessage() {
        // Use our custom handler to trigger test events
        FirebaseInAppMessagingHandler.triggerEvent(this, "test_event");
        
        // Also try triggering some common event names that might be configured in the Firebase Console
        FirebaseInAppMessagingHandler.triggerEvent(this, "app_open");
        FirebaseInAppMessagingHandler.triggerEvent(this, "user_engagement");
    }
    
    /**
     * Dump all flashy messages to logcat for debugging
     */
    private void dumpFlashyMessages() {
        FlashyMessageDebugger.dumpFlashyMessages(this);
    }
    
    /**
     * Force refresh flashy messages display
     */
    private void forceRefreshFlashyMessages() {
        FlashyMessageDebugger.forceRefreshFlashyMessages(this);
        Toast.makeText(this, "Forced refresh of flashy messages", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Reset flashy message display state
     */
    private void resetFlashyMessageState() {
        FlashyMessageDebugger.resetFlashyMessageDisplayState(this);
        Toast.makeText(this, "Reset flashy message display state", Toast.LENGTH_SHORT).show();
    }
} 