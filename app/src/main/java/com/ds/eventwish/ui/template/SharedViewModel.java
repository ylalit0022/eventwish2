package com.ds.eventwish.ui.template;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel {
    
    private static final String TAG = "SharedViewModel";
    private final MutableLiveData<List<Object>> templates = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Object> selectedTemplate = new MutableLiveData<>();

    public LiveData<List<Object>> getTemplates() {
        return templates;
    }

    public LiveData<Object> getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(Object template) {
        Log.d(TAG, "Setting selected template");
        selectedTemplate.setValue(template);
    }

    public void loadTemplates() {
        Log.d(TAG, "Loading templates");
        // Stub implementation - would normally load from repository
        templates.setValue(new ArrayList<>());
    }

    public void refreshTemplates() {
        Log.d(TAG, "Refreshing templates");
        loadTemplates();
    }
} 