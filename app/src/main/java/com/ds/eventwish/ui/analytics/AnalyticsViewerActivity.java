package com.ds.eventwish.ui.analytics;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ActivityAnalyticsViewerBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Activity for viewing real-time analytics data
 */
public class AnalyticsViewerActivity extends AppCompatActivity {
    private static final String TAG = "AnalyticsViewer";
    
    private ActivityAnalyticsViewerBinding binding;
    private DatabaseReference analyticsRef;
    private ValueEventListener viewerListener;
    private ValueEventListener historyListener;
    private String currentPageId;
    private Map<String, Integer> activeViewers = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyticsViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize Firebase Database
        setupFirebaseReferences();
        
        // Setup UI
        setupUI();
        
        // Load data
        loadTemplateAndWishIds();
    }
    
    private void setupFirebaseReferences() {
        try {
            // Get reference to the analytics node
            analyticsRef = FirebaseDatabase.getInstance().getReference("analytics");
            Log.d(TAG, "Firebase Database reference initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase Database", e);
            showError("Could not connect to analytics database: " + e.getMessage());
        }
    }
    
    private void setupUI() {
        // Setup page selector spinner
        binding.pageSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String pageId = parent.getItemAtPosition(position).toString();
                if (!pageId.equals(currentPageId)) {
                    currentPageId = pageId;
                    loadDataForPage(pageId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // Setup refresh button
        binding.refreshButton.setOnClickListener(v -> {
            if (currentPageId != null) {
                loadDataForPage(currentPageId);
            } else {
                loadTemplateAndWishIds();
            }
        });
    }
    
    private void loadTemplateAndWishIds() {
        showLoading(true);
        
        analyticsRef.child("page_views").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> pageIds = new ArrayList<>();
                
                for (DataSnapshot pageSnapshot : snapshot.getChildren()) {
                    pageIds.add(pageSnapshot.getKey());
                }
                
                if (pageIds.isEmpty()) {
                    showError("No analytics data available");
                    showLoading(false);
                    return;
                }
                
                // Set up spinner with page IDs
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    AnalyticsViewerActivity.this, 
                    android.R.layout.simple_spinner_item, 
                    pageIds
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.pageSelector.setAdapter(adapter);
                
                // Select first item
                if (!pageIds.isEmpty()) {
                    binding.pageSelector.setSelection(0);
                    currentPageId = pageIds.get(0);
                    loadDataForPage(currentPageId);
                }
                
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading page IDs", error.toException());
                showError("Failed to load analytics data: " + error.getMessage());
                showLoading(false);
            }
        });
    }
    
    private void loadDataForPage(String pageId) {
        if (pageId == null || pageId.isEmpty()) {
            showError("Invalid page ID");
            return;
        }
        
        showLoading(true);
        binding.pageIdText.setText(pageId);
        
        // Remove previous listeners
        removeListeners();
        
        // Create new listener for active viewers
        viewerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeCount = 0;
                activeViewers.clear();
                
                for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                    String sessionId = sessionSnapshot.getKey();
                    long lastActiveTimestamp = 0;
                    
                    if (sessionSnapshot.child("last_active").exists()) {
                        lastActiveTimestamp = sessionSnapshot.child("last_active").getValue(Long.class);
                    }
                    
                    // Check if session is active (last active within 2 minutes)
                    long currentTime = System.currentTimeMillis();
                    long timeDiff = currentTime - lastActiveTimestamp;
                    
                    if (timeDiff < TimeUnit.MINUTES.toMillis(2)) {
                        activeCount++;
                        activeViewers.put(sessionId, (int)(lastActiveTimestamp));
                    }
                }
                
                updateActiveViewersUI(activeCount);
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading active viewers", error.toException());
                showError("Failed to load active viewers: " + error.getMessage());
                showLoading(false);
            }
        };
        
        // Create new listener for view history
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ViewSession> sessions = new ArrayList<>();
                
                for (DataSnapshot sessionSnapshot : snapshot.getChildren()) {
                    String sessionId = sessionSnapshot.getKey();
                    long startTime = 0;
                    long endTime = 0;
                    long duration = 0;
                    
                    if (sessionSnapshot.child("start_time").exists()) {
                        startTime = sessionSnapshot.child("start_time").getValue(Long.class);
                    }
                    
                    if (sessionSnapshot.child("end_time").exists()) {
                        endTime = sessionSnapshot.child("end_time").getValue(Long.class);
                    }
                    
                    if (sessionSnapshot.child("duration").exists()) {
                        duration = sessionSnapshot.child("duration").getValue(Long.class);
                    }
                    
                    ViewSession session = new ViewSession(sessionId, startTime, endTime, duration);
                    sessions.add(session);
                }
                
                updateSessionHistoryUI(sessions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading view history", error.toException());
                showError("Failed to load view history: " + error.getMessage());
            }
        };
        
        // Attach listeners
        analyticsRef.child("active_viewers").child(pageId)
            .addValueEventListener(viewerListener);
        
        analyticsRef.child("view_history").child(pageId)
            .addValueEventListener(historyListener);
    }
    
    private void updateActiveViewersUI(int activeCount) {
        binding.activeViewersCount.setText(String.valueOf(activeCount));
        
        StringBuilder viewersText = new StringBuilder();
        for (Map.Entry<String, Integer> entry : activeViewers.entrySet()) {
            String sessionId = entry.getKey();
            int lastActive = entry.getValue();
            
            viewersText.append("Session: ").append(sessionId)
                .append(" (Last active: ").append(formatTimestamp(lastActive))
                .append(")\n");
        }
        
        binding.activeViewersDetails.setText(viewersText.toString());
    }
    
    private void updateSessionHistoryUI(List<ViewSession> sessions) {
        int totalSessions = sessions.size();
        long totalDuration = 0;
        
        for (ViewSession session : sessions) {
            totalDuration += session.getDuration();
        }
        
        double avgDuration = totalSessions > 0 ? (double) totalDuration / totalSessions : 0;
        
        binding.totalSessionsCount.setText(String.valueOf(totalSessions));
        binding.avgDurationText.setText(String.format(Locale.US, "%.1f seconds", avgDuration));
        
        // Show most recent sessions
        StringBuilder historyText = new StringBuilder();
        int count = Math.min(10, sessions.size());
        
        for (int i = 0; i < count; i++) {
            ViewSession session = sessions.get(sessions.size() - 1 - i);
            historyText.append("Session: ").append(session.getSessionId().substring(0, 8))
                .append("..., Duration: ").append(session.getDuration()).append("s")
                .append(" (").append(formatTimestamp(session.getStartTime())).append(")")
                .append("\n");
        }
        
        binding.sessionHistoryText.setText(historyText.toString());
    }
    
    private String formatTimestamp(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showError(String message) {
        binding.errorText.setText(message);
        binding.errorText.setVisibility(View.VISIBLE);
    }
    
    private void removeListeners() {
        if (viewerListener != null && currentPageId != null) {
            analyticsRef.child("active_viewers").child(currentPageId)
                .removeEventListener(viewerListener);
        }
        
        if (historyListener != null && currentPageId != null) {
            analyticsRef.child("view_history").child(currentPageId)
                .removeEventListener(historyListener);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
        binding = null;
    }
    
    /**
     * View session data class
     */
    private static class ViewSession {
        private final String sessionId;
        private final long startTime;
        private final long endTime;
        private final long duration;
        
        public ViewSession(String sessionId, long startTime, long endTime, long duration) {
            this.sessionId = sessionId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public long getDuration() {
            return duration;
        }
    }
} 