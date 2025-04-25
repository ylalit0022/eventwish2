package com.ds.eventwish.ads;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.R;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.ApiClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class AdDemoActivity extends AppCompatActivity {
    private static final String TAG = "AdDemoActivity";
    
    private Spinner adTypeSpinner;
    private Button fetchButton;
    private TextView responseText;
    private AdMobRepository adMobRepository;
    private String selectedAdType = "interstitial"; // Default ad type
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_demo);
        
        // Initialize views
        adTypeSpinner = findViewById(R.id.adTypeSpinner);
        fetchButton = findViewById(R.id.fetchButton);
        responseText = findViewById(R.id.responseText);
        
        // Setup spinner
        String[] adTypes = {"app_open", "banner", "interstitial", "rewarded"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, adTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adTypeSpinner.setAdapter(adapter);
        
        // Initialize API client and repository
        ApiClient.init(this);
        ApiService apiService = ApiClient.getInstance();
        adMobRepository = new AdMobRepository(this, apiService);
        
        // Setup listeners
        adTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAdType = adTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedAdType = "interstitial";
            }
        });
        
        fetchButton.setOnClickListener(v -> fetchAdUnit());
    }
    
    private void fetchAdUnit() {
        responseText.setText(getString(R.string.log_admob_request, selectedAdType));
        
        adMobRepository.fetchAdUnit(selectedAdType, new AdMobRepository.AdUnitCallback() {
            @Override
            public void onSuccess(JsonObject response) {
                // Pretty print the JSON response
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyJson = gson.toJson(response);
                
                runOnUiThread(() -> {
                    String displayText = getString(R.string.raw_response_title) + "\n\n" + prettyJson;
                    responseText.setText(displayText);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> responseText.setText(error));
            }
        });
    }
} 