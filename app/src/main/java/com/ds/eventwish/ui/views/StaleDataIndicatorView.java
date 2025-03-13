package com.ds.eventwish.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ds.eventwish.R;

/**
 * Custom view for displaying stale data indicator
 */
public class StaleDataIndicatorView extends LinearLayout {
    
    private LinearLayout container;
    private TextView messageView;
    private Button refreshButton;
    private OnRefreshClickListener refreshClickListener;
    
    /**
     * Interface for refresh button click events
     */
    public interface OnRefreshClickListener {
        void onRefreshClick();
    }
    
    /**
     * Interface for refresh button click events
     */
    public interface RefreshListener {
        void onRefreshClick();
    }
    
    private RefreshListener refreshListener;
    
    public StaleDataIndicatorView(Context context) {
        super(context);
        init(context);
    }
    
    public StaleDataIndicatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public StaleDataIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    /**
     * Initialize the view
     * @param context Context
     */
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_stale_data_indicator, this, true);
        
        container = findViewById(R.id.stale_data_container);
        messageView = findViewById(R.id.stale_data_message);
        refreshButton = findViewById(R.id.refresh_button);
        
        refreshButton.setOnClickListener(v -> {
            if (refreshClickListener != null) {
                refreshClickListener.onRefreshClick();
            }
            
            if (refreshListener != null) {
                refreshListener.onRefreshClick();
            }
        });
    }
    
    /**
     * Set the refresh button click listener
     * @param listener OnRefreshClickListener
     */
    public void setOnRefreshClickListener(OnRefreshClickListener listener) {
        this.refreshClickListener = listener;
    }
    
    /**
     * Set the stale data message
     * @param message Message to display
     */
    public void setMessage(String message) {
        messageView.setText(message);
    }
    
    /**
     * Show the stale data indicator
     */
    public void show() {
        container.setVisibility(View.VISIBLE);
    }
    
    /**
     * Hide the stale data indicator
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
     * Set the refresh button text
     * @param text Button text
     */
    public void setRefreshButtonText(String text) {
        refreshButton.setText(text);
    }
    
    /**
     * Show or hide the refresh button
     * @param show true to show, false to hide
     */
    public void showRefreshButton(boolean show) {
        refreshButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Set a listener for the refresh button
     * @param listener The listener to call when the refresh button is clicked
     */
    public void setRefreshListener(RefreshListener listener) {
        this.refreshListener = listener;
    }
} 