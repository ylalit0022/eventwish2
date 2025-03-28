package com.ds.eventwish.ui.customize;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.data.repository.SharedWishRepository;
import com.ds.eventwish.data.local.entity.SharedWishEntity;
import java.util.HashMap;
import java.util.Map;

public class TemplateCustomizeViewModel extends ViewModel {
    private static TemplateCustomizeViewModel instance;
    private final TemplateRepository templateRepository;
    private final SharedWishRepository sharedWishRepository;
    
    private final MutableLiveData<Template> template = new MutableLiveData<>();
    private final MutableLiveData<String> previewHtml = new MutableLiveData<>();
    private final MutableLiveData<SharedWish> sharedWish = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private String recipientName = "";
    private String senderName = "";
    private String message = "";
    private String templateId;
    
    /**
     * Get singleton instance of TemplateCustomizeViewModel
     * @return the instance
     */
    public static synchronized TemplateCustomizeViewModel getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TemplateCustomizeViewModel must be initialized with init() first");
        }
        return instance;
    }
    
    /**
     * Initialize the ViewModel with application context
     * @param context the application context
     */
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new TemplateCustomizeViewModel(context.getApplicationContext());
        }
    }
    
    private TemplateCustomizeViewModel(Context context) {
        // Use repository instances that take context
        templateRepository = TemplateRepository.getInstance();
        sharedWishRepository = SharedWishRepository.getInstance(context);
    }
    
    public void loadTemplate(String templateId) {
        this.templateId = templateId;
        
        // Use the Resource<Template> LiveData
        templateRepository.getTemplateById(templateId, false).observeForever(resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                Template loadedTemplate = resource.getData();
                this.template.setValue(loadedTemplate);
                updatePreview();
            } else if (resource.isError()) {
                error.setValue(resource.getMessage());
            }
        });
    }
    
    public LiveData<Template> getTemplate() {
        return template;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
        updatePreview();
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
        updatePreview();
    }
    
    public void setMessage(String message) {
        this.message = message;
        updatePreview();
    }
    
    private void updatePreview() {
        if (template.getValue() == null) return;
        
        // Get the HTML template
        String templateHtml = template.getValue().getHtml();
        
        // Replace placeholder values
        templateHtml = templateHtml.replace("{{recipient}}", recipientName)
                                  .replace("{{sender}}", senderName)
                                  .replace("{{message}}", message);
        
        previewHtml.setValue(templateHtml);
    }
    
    public void saveWish() {
        if (template.getValue() == null) {
            error.setValue("No template selected");
            return;
        }
        
        // Create a customization data map
        Map<String, Object> customizationData = new HashMap<>();
        customizationData.put("recipientName", recipientName);
        customizationData.put("senderName", senderName);
        customizationData.put("message", message);
        
        // Get the current template HTML content
        String customizedHtml = previewHtml.getValue();
        
        // Use createSharedWish with the correct parameter types
        sharedWishRepository.createSharedWish(templateId, customizedHtml, customizationData)
            .observeForever(result -> {
                if (result.isSuccess()) {
                    // Create a SharedWish from the SharedWishEntity
                    SharedWishEntity entity = result.getData();
                    
                    // Manual conversion from SharedWishEntity to SharedWish
                    SharedWish wish = new SharedWish();
                    wish.setShortCode(entity.getShortCode());
                    wish.setTemplateId(entity.getTemplateId());
                    wish.setCustomizedHtml(entity.getCustomizedHtml());
                    wish.setPreviewUrl(entity.getShareUrl());
                    
                    sharedWish.setValue(wish);
                } else if (result.isError()) {
                    error.setValue(result.getMessage());
                }
            });
    }
    
    public LiveData<String> getPreviewHtml() {
        return previewHtml;
    }
    
    public LiveData<SharedWish> getSharedWish() {
        return sharedWish;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    // Method to update HTML content directly (for HTML editor)
    public void updateHtmlContent(String updatedHtml) {
        if (template.getValue() == null) return;
        
        // Store the current template
        Template currentTemplate = template.getValue();
        
        // Update the HTML content
        currentTemplate.setHtml(updatedHtml);
        
        // Update the template
        template.setValue(currentTemplate);
        
        // Update the preview with the new HTML
        previewHtml.setValue(updatedHtml);
    }
}
