package com.ds.eventwish.ui.detail;

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

public class TemplateDetailViewModel extends ViewModel {
    private final ApiService apiService;
    private final MutableLiveData<Template> template = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> wishSaved = new MutableLiveData<>();

    private String recipientName = "";
    private String senderName = "";
    private String templateId;

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
        if (templateId == null || templateId.isEmpty()) {
            error.setValue("Template not loaded");
            return;
        }

        if (recipientName.isEmpty() || senderName.isEmpty()) {
            error.setValue("Please enter both recipient and sender names");
            return;
        }

        Template currentTemplate = template.getValue();
        if (currentTemplate == null) {
            error.setValue("Template not available");
            return;
        }

        isLoading.setValue(true);
        SharedWish wish = new SharedWish();
        wish.setTemplateId(templateId);
        wish.setRecipientName(recipientName);
        wish.setSenderName(senderName);

        // Create customized HTML with replaced names
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

        apiService.createSharedWish(wish).enqueue(new Callback<SharedWish>() {
            @Override
            public void onResponse(Call<SharedWish> call, Response<SharedWish> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    wishSaved.setValue(response.body().getShortCode());
                } else {
                    error.setValue("Failed to save wish");
                }
            }

            @Override
            public void onFailure(Call<SharedWish> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
