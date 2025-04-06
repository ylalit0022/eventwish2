package com.ds.eventwish.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Worker to check for template updates every 15 minutes
 */
public class TemplateUpdateWorker extends Worker {
    private static final String TAG = "TemplateUpdateWorker";
    private static final String PREF_NAME = "template_updates";
    private static final String KEY_TEMPLATE_IDS = "template_ids";
    private static final String KEY_HAS_UPDATES = "has_updates";

    public TemplateUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting template update check");
        
        try {
            // Get the API service
            ApiService apiService = ApiClient.getClient();
            
            // Get the current template IDs
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> savedTemplateIds = prefs.getStringSet(KEY_TEMPLATE_IDS, new HashSet<>());
            
            // Fetch the latest templates
            Call<TemplateResponse> call = apiService.getTemplates(1, 20);
            Response<TemplateResponse> response = call.execute();
            
            if (response.isSuccessful() && response.body() != null) {
                TemplateResponse templateResponse = response.body();
                List<Template> templates = templateResponse.getTemplates();
                
                if (templates != null && !templates.isEmpty()) {
                    // Check if there are new templates
                    Set<String> newTemplateIds = new HashSet<>();
                    for (Template template : templates) {
                        newTemplateIds.add(template.getId());
                    }
                    
                    // Check if there are any new templates
                    boolean hasNewTemplates = false;
                    for (String id : newTemplateIds) {
                        if (!savedTemplateIds.contains(id)) {
                            hasNewTemplates = true;
                            break;
                        }
                    }
                    
                    // Save the new template IDs
                    prefs.edit()
                        .putStringSet(KEY_TEMPLATE_IDS, newTemplateIds)
                        .putBoolean(KEY_HAS_UPDATES, hasNewTemplates)
                        .apply();
                    
                    Log.d(TAG, "Template update check completed. Has new templates: " + hasNewTemplates);
                }
            }
            
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error checking for template updates", e);
            return Result.retry();
        }
    }
    
    /**
     * Check if there are new templates
     * @param context The context
     * @return true if there are new templates
     */
    public static boolean hasNewTemplates(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_HAS_UPDATES, false);
    }
    
    /**
     * Reset the new templates flag
     * @param context The context
     */
    public static void resetNewTemplatesFlag(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_HAS_UPDATES, false)
            .apply();
    }
} 