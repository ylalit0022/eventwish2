package com.ds.eventwish.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateInteractionRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Worker class to retry failed template operations
 */
public class RetryOperationsWorker extends Worker {
    private static final String TAG = "RetryOperationsWorker";
    
    public RetryOperationsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("failed_operations", Context.MODE_PRIVATE);
        String operations = prefs.getString("pending_operations", "");
        
        if (operations.isEmpty()) {
            return Result.success();
        }
        
        try {
            JSONArray pendingOps = new JSONArray(operations);
            TemplateInteractionRepository repo = TemplateInteractionRepository.getInstance(context);
            
            boolean allSuccess = true;
            JSONArray remainingOps = new JSONArray();
            
            for (int i = 0; i < pendingOps.length(); i++) {
                JSONObject operation = pendingOps.getJSONObject(i);
                String templateId = operation.getString("templateId");
                String type = operation.getString("type");
                
                try {
                    if ("like".equals(type)) {
                        Template template = new Template(templateId, "", "", "", false, false, 0);
                        repo.toggleLike(template);
                    } else if ("favorite".equals(type)) {
                        Template template = new Template(templateId, "", "", "", false, false, 0);
                        repo.toggleFavorite(template);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to retry operation: " + operation.toString(), e);
                    allSuccess = false;
                    remainingOps.put(operation);
                }
            }
            
            // Update pending operations
            if (remainingOps.length() > 0) {
                prefs.edit().putString("pending_operations", remainingOps.toString()).apply();
                return Result.retry();
            } else {
                prefs.edit().remove("pending_operations").apply();
                return Result.success();
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error processing pending operations", e);
            return Result.failure();
        }
    }
} 