package com.ds.eventwish.ui.detail;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonObject;

public class TemplateDetailViewModel extends ViewModel {
    private final ApiService apiService;
    String TAG = "TemplateDetailsViewModel";
    private final MutableLiveData<Template> template = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> wishSaved = new MutableLiveData<>();

    private String recipientName = "";
    private String senderName = "";
    private String templateId;
    private String customizedHtml = null;
    
    // Base URL for the backend server
    private static final String SERVER_BASE_URL = "https://eventwish2.onrender.com";

    public TemplateDetailViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<Template> getTemplate() {
        return template;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getWishSaved() {
        return wishSaved;
    }

    public void setRecipientName(String name) {
        this.recipientName = name != null ? name.trim() : "";
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setSenderName(String name) {
        this.senderName = name != null ? name.trim() : "";
    }

    public String getSenderName() {
        return senderName;
    }
    
    public void setCustomizedHtml(String html) {
        this.customizedHtml = html;
        Log.d(TAG, "CustomizedHtml updated: " + (html != null ? html.substring(0, Math.min(50, html.length())) + "..." : "null"));
    }

    public void loadTemplate(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            error.setValue("Invalid template ID");
            return;
        }

        this.templateId = templateId;
        isLoading.setValue(true);
        
        apiService.getTemplateById(templateId).enqueue(new Callback<Template>() {
            @Override
            public void onResponse(Call<Template> call, Response<Template> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    template.setValue(response.body());
                } else {
                    error.setValue("Failed to load template");
                }
            }

            @Override
            public void onFailure(Call<Template> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void saveWish() {
        Log.d(TAG, "Saving wish");
        if (templateId == null || templateId.isEmpty()) {
            Log.e(TAG, "Cannot save wish - template is null");
            error.setValue("Template not loaded");
            return;
        }

        if (recipientName.isEmpty() || senderName.isEmpty()) {
            error.setValue("Please enter both recipient and sender names");
            return;
        }

        Log.d(TAG, "Creating wish with recipient: " + recipientName +
                ", sender: " + senderName +
                ", templateId: " + templateId);

        Template currentTemplate = template.getValue();
        if (currentTemplate == null) {
            error.setValue("Template not available");
            return;
        }

        isLoading.setValue(true);
        SharedWish wish = new SharedWish();
        wish.setTemplateId(templateId);
        wish.setRecipientName(recipientName.trim());
        wish.setSenderName(senderName.trim());
        wish.setSharedVia("LINK");
        
        // Set title and description for social media sharing
        wish.setTitle("EventWish Greeting");
        wish.setDescription("A special wish from " + senderName.trim() + " to " + recipientName.trim());

        // Debug log for API call
        Log.d(TAG, "Making API call to create shared wish");
        Log.d(TAG, "Request body: templateId=" + templateId + 
              ", recipientName=" + recipientName + 
              ", senderName=" + senderName);

        // Use the customizedHtml if it was set by the WebView
        if (customizedHtml != null && !customizedHtml.isEmpty()) {
            Log.d(TAG, "Using customizedHtml from WebView");
            wish.setCustomizedHtml(customizedHtml);
        } else {
            // Fallback to creating customized HTML with replaced names
            Log.d(TAG, "Creating customizedHtml from template");
            String customHtml = currentTemplate.getHtmlContent();
            if (customHtml != null) {
                customHtml = customHtml.replace("[Recipient]", 
                    "<span class=\"recipient-name\">" + recipientName + "</span>");
                customHtml = customHtml.replace("{recipient}", 
                    "<span class=\"recipient-name\">" + recipientName + "</span>");
                customHtml = customHtml.replace("[Your Name]", 
                    "<span class=\"sender-name\">" + senderName + "</span>");
                customHtml = customHtml.replace("{sender}", 
                    "<span class=\"sender-name\">" + senderName + "</span>");
                wish.setCustomizedHtml(customHtml);
            }
        }

        // Always set CSS and JS content
        wish.setCssContent(currentTemplate.getCssContent());
        wish.setJsContent(currentTemplate.getJsContent());
        
        // Set preview URL from template thumbnail if available
        if (currentTemplate.getThumbnailUrl() != null && !currentTemplate.getThumbnailUrl().isEmpty()) {
            // If the thumbnail URL is already a full URL, use it directly
            if (currentTemplate.getThumbnailUrl().startsWith("http")) {
                wish.setPreviewUrl(currentTemplate.getThumbnailUrl());
            } else {
                // Otherwise, construct a full URL using the server base URL
                wish.setPreviewUrl(SERVER_BASE_URL + currentTemplate.getThumbnailUrl());
            }
            Log.d(TAG, "Set preview URL: " + wish.getPreviewUrl());
        }

        // Convert SharedWish to Map<String, Object> for the API call
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("templateId", wish.getTemplateId());
        requestBody.put("customizedHtml", wish.getCustomizedHtml());
        requestBody.put("senderName", wish.getSenderName());
        requestBody.put("recipientName", wish.getRecipientName());
        requestBody.put("message", wish.getMessage());
        requestBody.put("previewUrl", wish.getPreviewUrl());
        
        apiService.createSharedWish(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d(TAG, "API Response Code: " + response.code());
                Log.d(TAG, "API URL Called: " + call.request().url());
                isLoading.setValue(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    try {
                        // Try to get shortCode directly from the response
                        String shortCode = null;
                        if (responseBody.has("shortCode")) {
                            shortCode = responseBody.get("shortCode").getAsString();
                        }
                        
                        Log.d(TAG, "Final shortCode: " + shortCode);
                        
                        // If we have a valid shortCode, save it
                        if (shortCode != null && !shortCode.isEmpty()) {
                            // Update wish object with the shortCode
                            wish.setShortCode(shortCode);
                            wish.setDeepLink("eventwish://wish/" + shortCode);
                            
                            // Set shareUrl if available
                            if (responseBody.has("shareUrl")) {
                                String shareUrl = responseBody.get("shareUrl").getAsString();
                                // Use preview URL as the share URL if available
                                wish.setPreviewUrl(shareUrl);
                            }
                            
                            wishSaved.setValue(shortCode);
                        } else {
                            Log.e(TAG, "API returned null or empty shortCode");
                            error.setValue("Failed to get wish code from server");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response JSON", e);
                        error.setValue("Failed to save wish: " + response.code());
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                            response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
                        error.setValue("Failed to save wish: " + response.code());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                        error.setValue("Failed to save wish: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "API Call Failed", t);
                Log.e(TAG, "Failed URL: " + call.request().url());
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
