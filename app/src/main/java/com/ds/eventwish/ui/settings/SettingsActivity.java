package com.ds.eventwish.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.ds.eventwish.R;
import com.ds.eventwish.utils.AnalyticsUtils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            
            // Set up analytics preference toggle
            setupAnalyticsPreference();
        }
        
        private void setupAnalyticsPreference() {
            SwitchPreferenceCompat analyticsPreference = findPreference("enable_analytics");
            if (analyticsPreference != null) {
                // Initialize the switch state based on current analytics status
                analyticsPreference.setChecked(AnalyticsUtils.isAnalyticsEnabled());
                
                // Set up the preference change listener
                analyticsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    // Update analytics settings
                    AnalyticsUtils.setAnalyticsEnabled(enabled);
                    
                    // Show a confirmation toast
                    String message = enabled ? 
                            getString(R.string.settings_analytics_enabled) : 
                            getString(R.string.settings_analytics_disabled);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    
                    return true;
                });
            }
        }
    }
} 