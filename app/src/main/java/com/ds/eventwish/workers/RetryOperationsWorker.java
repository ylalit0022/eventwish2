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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class to retry failed template operations
 */
public class RetryOperationsWorker extends Worker {
    private static final String TAG = "RetryOperationsWorker";
    private final TemplateInteractionRepository repo;
    
    public RetryOperationsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        repo = TemplateInteractionRepository.getInstance(context);
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
            boolean allSuccess = true;
            JSONArray remainingOps = new JSONArray();
            
            for (int i = 0; i < pendingOps.length(); i++) {
                JSONObject operation = pendingOps.getJSONObject(i);
                String templateId = operation.getString("templateId");
                String type = operation.getString("type");
                
                try {
                    processFailedOperation(templateId, type);
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

    private void processFailedOperation(String templateId, String type) {
        Log.d(TAG, "Processing failed operation - Template: " + templateId + ", Type: " + type);
        Template template = new Template(templateId, "", "", "");
        retryOperation(template, type);
    }

    private void retryOperation(Template template, String type) {
        AtomicBoolean retrySuccessful = new AtomicBoolean(false);
        int retryCount = 0;
        final int maxRetries = 3;

        while (!retrySuccessful.get() && retryCount < maxRetries) {
            try {
                if ("like".equals(type)) {
                    repo.toggleLike(template.getId());
                } else if ("favorite".equals(type)) {
                    repo.toggleFavorite(template.getId());
                }
                retrySuccessful.set(true);
            } catch (Exception e) {
                Log.e(TAG, String.format("Retry attempt %d failed for %s operation on template %s", 
                    retryCount + 1, type, template.getId()), e);
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!retrySuccessful.get()) {
            Log.e(TAG, String.format("All retry attempts failed for %s operation on template %s", 
                type, template.getId()));
        }
    }
} 