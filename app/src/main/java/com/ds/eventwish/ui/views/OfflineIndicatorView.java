package com.ds.eventwish.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.ds.eventwish.R;
import com.ds.eventwish.utils.NetworkUtils;

/**
 * Custom view for displaying offline mode indicator
 */
public class OfflineIndicatorView extends LinearLayout {
    
    private LinearLayout container;
    private TextView messageView;
    private Button retryButton;
    private OnRetryClickListener retryClickListener;
    private RetryListener retryListener;
    
    /**
     * Interface for retry button click events
     */
    public interface OnRetryClickListener {
        void onRetryClick();
    }
    
    /**
     * Interface for retry button click events
     */
    public interface RetryListener {
        void onRetryClick();
    }
    
    public OfflineIndicatorView(Context context) {
        super(context);
        init(context);
    }
    
    public OfflineIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public OfflineIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    /**
     * Initialize the view
     * @param context Context
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_offline_indicator, this, true);
        
        container = findViewById(R.id.offline_container);
        messageView = findViewById(R.id.offline_message);
        retryButton = findViewById(R.id.retry_button);
        
        retryButton.setOnClickListener(v -> {
            if (retryClickListener != null) {
                retryClickListener.onRetryClick();
            }
            
            if (retryListener != null) {
                retryListener.onRetryClick();
            }
        });
    }
    
    /**
     * Set the retry button click listener
     * @param listener OnRetryClickListener
     */
    public void setOnRetryClickListener(OnRetryClickListener listener) {
        this.retryClickListener = listener;
    }
    
    /**
     * Set the offline message
     * @param message Message to display
     */
    public void setMessage(String message) {
        messageView.setText(message);
    }
    
    /**
     * Show the offline indicator
     */
    public void show() {
        container.setVisibility(View.VISIBLE);
    }
    
    /**
     * Hide the offline indicator
     */
    public void hide() {
        container.setVisibility(View.GONE);
    }
    
    /**
     * Check if the indicator is visible
     * @return true if visible, false otherwise
     */
    public boolean isShowing() {
        return container.getVisibility() == View.VISIBLE;
    }
    
    /**
     * Start monitoring network state and automatically show/hide the indicator
     * @param lifecycleOwner LifecycleOwner for LiveData observation
     */
    public void startMonitoring(LifecycleOwner lifecycleOwner) {
        NetworkUtils.getInstance(getContext()).getNetworkAvailability().observe(lifecycleOwner, isConnected -> {
            if (isConnected) {
                hide();
            } else {
                show();
            }
        });
    }
    
    /**
     * Set the retry button text
     * @param text Button text
     */
    public void setRetryButtonText(String text) {
        retryButton.setText(text);
    }
    
    /**
     * Show or hide the retry button
     * @param show true to show, false to hide
     */
    public void showRetryButton(boolean show) {
        retryButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Set a listener for the retry button
     * @param listener The listener to call when the retry button is clicked
     */
    public void setRetryListener(RetryListener listener) {
        this.retryListener = listener;
    }
} 