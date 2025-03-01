package com.ds.eventwish.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ds.eventwish.data.model.Template;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to manage template update timestamps and check for new templates
 */
public class TemplateUpdateManager {
    private static final String TAG = "TemplateUpdateManager";
    private static final String PREF_NAME = "template_updates";
    private static final String KEY_LAST_UPDATE_TIME = "last_update_time";
    private static final String KEY_LAST_TEMPLATE_COUNT = "last_template_count";
    private static final String KEY_HAS_NEW_TEMPLATES = "has_new_templates";
    private static final String KEY_TEMPLATE_IDS = "template_ids";
    private static final String KEY_VIEWED_TEMPLATE_IDS = "viewed_template_ids";

    private static TemplateUpdateManager instance;
    private final SharedPreferences preferences;

    private TemplateUpdateManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized TemplateUpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new TemplateUpdateManager(context);
        }
        return instance;
    }

    /**
     * Check if there are new templates by comparing with previous data
     * @param templates The current list of templates
     * @return true if new templates are detected
     */
    public boolean checkForNewTemplates(List<Template> templates) {
        if (templates == null || templates.isEmpty()) {
            Log.d(TAG, "No templates to check");
            return false;
        }

        // If already showing new templates indicator, don't check again
        if (hasNewTemplates()) {
            Log.d(TAG, "Already showing new templates indicator");
            return true;
        }

        // Get the current list of template IDs
        Set<String> currentTemplateIds = new HashSet<>();
        for (Template template : templates) {
            if (template.getId() != null) {
                currentTemplateIds.add(template.getId());
            }
        }

        // Get the previous list of template IDs
        Set<String> previousTemplateIds = getPreviousTemplateIds();
        
        // Get the viewed template IDs
        Set<String> viewedTemplateIds = getViewedTemplateIds();

        // Check if there are any new templates
        boolean hasNewTemplates = false;
        for (String id : currentTemplateIds) {
            // If the template is new and hasn't been viewed yet
            if (!previousTemplateIds.contains(id) && !viewedTemplateIds.contains(id)) {
                hasNewTemplates = true;
                Log.d(TAG, "New template detected: " + id);
                break;
            }
        }
        
        if (hasNewTemplates) {
            Log.d(TAG, "New templates detected!");
            // Store the new template IDs but keep the flag
            preferences.edit()
                .putBoolean(KEY_HAS_NEW_TEMPLATES, true)
                .apply();
        }
        
        // Always update the stored template IDs
        storeTemplateIds(currentTemplateIds);
        
        return hasNewTemplates;
    }
    
    /**
     * Store the current set of template IDs
     */
    private void storeTemplateIds(Set<String> templateIds) {
        preferences.edit()
            .putStringSet(KEY_TEMPLATE_IDS, templateIds)
            .apply();
        Log.d(TAG, "Stored " + templateIds.size() + " template IDs");
    }
    
    /**
     * Get the previously stored template IDs
     */
    private Set<String> getPreviousTemplateIds() {
        return preferences.getStringSet(KEY_TEMPLATE_IDS, new HashSet<>());
    }

    /**
     * Reset the new templates flag
     */
    public void resetNewTemplatesFlag() {
        Log.d(TAG, "Resetting new templates flag");
        preferences.edit()
            .putBoolean(KEY_HAS_NEW_TEMPLATES, false)
            .apply();
            
        // Force a commit to ensure the change is persisted immediately
        preferences.edit().commit();
    }

    /**
     * Check if there are new templates that the user hasn't seen yet
     */
    public boolean hasNewTemplates() {
        return preferences.getBoolean(KEY_HAS_NEW_TEMPLATES, false);
    }

    /**
     * Mark a template as viewed to avoid showing the indicator for it
     * @param templateId The ID of the template that was viewed
     */
    public void markTemplateAsViewed(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Marking template as viewed: " + templateId);
        
        // Get the current set of viewed template IDs
        Set<String> viewedTemplateIds = getViewedTemplateIds();
        
        // Add the new template ID
        viewedTemplateIds.add(templateId);
        
        // Save the updated set
        saveViewedTemplateIds(viewedTemplateIds);
    }
    
    /**
     * Get the set of viewed template IDs
     */
    private Set<String> getViewedTemplateIds() {
        return preferences.getStringSet(KEY_VIEWED_TEMPLATE_IDS, new HashSet<>());
    }
    
    /**
     * Save the set of viewed template IDs
     */
    private void saveViewedTemplateIds(Set<String> viewedTemplateIds) {
        preferences.edit()
            .putStringSet(KEY_VIEWED_TEMPLATE_IDS, viewedTemplateIds)
            .apply();
            
        // Force a commit to ensure the change is persisted immediately
        preferences.edit().commit();
    }
}
