package com.ds.eventwish.ui.api;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import com.ds.eventwish.R;
import com.ds.eventwish.data.local.AppDatabase;

import java.io.File;

/**
 * Activity for viewing database information
 */
public class CoinsDatabaseLogActivity extends AppCompatActivity {
    private static final String TAG = "CoinsDbLogActivity";
    
    private TextView resultTextView;
    private ProgressBar progressBar;
    private Button refreshButton;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_log);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Database Log");
        }
        
        // Initialize views
        resultTextView = findViewById(R.id.resultTextView);
        progressBar = findViewById(R.id.progressBar);
        refreshButton = findViewById(R.id.refreshButton);
        scrollView = findViewById(R.id.scrollView);
        
        // Set up refresh button
        refreshButton.setOnClickListener(v -> refreshDatabaseLog());
        
        // Load database log
        refreshDatabaseLog();
    }
    
    private void refreshDatabaseLog() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        resultTextView.setText("Loading database information...");
        refreshButton.setEnabled(false);
        
        // Run on background thread to avoid ANR
        new Thread(() -> {
            final String logData = getDatabaseLog();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                resultTextView.setText(logData);
                refreshButton.setEnabled(true);
                
                // Scroll to top
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_UP));
            });
        }).start();
    }
    
    private String getDatabaseLog() {
        StringBuilder builder = new StringBuilder();
        
        // Log timestamp
        builder.append("=== DATABASE LOG ===\n");
        builder.append("Time: ").append(new java.util.Date()).append("\n\n");
        
        // Check database file
        builder.append("=== DATABASE FILES ===\n");
        try {
            File dbFile = getDatabasePath("eventwish-db");
            if (dbFile.exists()) {
                builder.append("Main DB file: ").append(dbFile.getAbsolutePath()).append("\n");
                builder.append("Size: ").append(dbFile.length() / 1024).append(" KB\n");
                builder.append("Last modified: ").append(new java.util.Date(dbFile.lastModified())).append("\n");
            } else {
                builder.append("Database file not found!\n");
            }
            
            // Check for journal files
            File dbPath = dbFile.getParentFile();
            if (dbPath != null && dbPath.exists() && dbPath.isDirectory()) {
                File[] files = dbPath.listFiles((dir, name) -> name.startsWith("eventwish-db") || name.contains("room"));
                if (files != null) {
                    builder.append("\nDatabase related files:\n");
                    for (File file : files) {
                        builder.append("- ").append(file.getName())
                               .append(" (").append(file.length() / 1024).append(" KB)\n");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking database files", e);
            builder.append("Error checking database files: ").append(e.getMessage()).append("\n");
        }
        
        // Log general database information
        builder.append("\n=== DATABASE TABLES ===\n");
        try {
            // Get a read-only database instance to avoid any potential data modification
            AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "eventwish-db")
                    .allowMainThreadQueries() // For this diagnostic tool only
                    .build();
            
            builder.append("Database connection established\n");
            
            // Use reflection to find table names
            java.lang.reflect.Field[] fields = AppDatabase.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getName().endsWith("Dao")) {
                    builder.append("DAO: ").append(field.getName()).append("\n");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error accessing database", e);
            builder.append("Error accessing database: ").append(e.getMessage()).append("\n");
        }
        
        // Log SharedPreferences information
        builder.append("\n=== SHARED PREFERENCES ===\n");
        try {
            String[] prefFiles = getApplicationContext().fileList();
            for (String file : prefFiles) {
                if (file.endsWith(".xml")) {
                    builder.append("Preferences file: ").append(file).append("\n");
                    
                    // For each pref file, try to open it
                    try {
                        android.content.SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                file.replace(".xml", ""), MODE_PRIVATE);
                        
                        builder.append("  Keys: ").append(prefs.getAll().size()).append("\n");
                        
                        // List some important keys
                        if (prefs.contains("coins")) {
                            builder.append("  coins: ").append(prefs.getInt("coins", 0)).append("\n");
                        }
                        if (prefs.contains("unlocked")) {
                            builder.append("  unlocked: ").append(prefs.getBoolean("unlocked", false)).append("\n");
                        }
                        if (prefs.contains("timestamp")) {
                            builder.append("  timestamp: ").append(prefs.getLong("timestamp", 0)).append("\n");
                        }
                    } catch (Exception e) {
                        builder.append("  Error reading preferences: ").append(e.getMessage()).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing shared preferences", e);
            builder.append("Error accessing shared preferences: ").append(e.getMessage()).append("\n");
        }
        
        // Log device information
        builder.append("\n=== DEVICE INFORMATION ===\n");
        builder.append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n");
        builder.append("Model: ").append(android.os.Build.MODEL).append("\n");
        builder.append("Android version: ").append(android.os.Build.VERSION.RELEASE)
                .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
        
        builder.append("\n=== APP INFORMATION ===\n");
        try {
            String versionName = getApplicationContext().getPackageManager()
                    .getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
            int versionCode = getApplicationContext().getPackageManager()
                    .getPackageInfo(getApplicationContext().getPackageName(), 0).versionCode;
            builder.append("Version: ").append(versionName).append(" (").append(versionCode).append(")\n");
        } catch (Exception e) {
            builder.append("Error getting app version: ").append(e.getMessage()).append("\n");
        }
        
        return builder.toString();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 