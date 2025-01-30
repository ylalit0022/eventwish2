package com.ds.eventwish.ui.customize;

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

public class TemplateCustomizeViewModel extends ViewModel {
    private final ApiService apiService;
    private final MutableLiveData<Template> template = new MutableLiveData<>();
    private final MutableLiveData<String> previewHtml = new MutableLiveData<>();
    private final MutableLiveData<SharedWish> sharedWish = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String recipientName = "";
    private String senderName = "";
    private String message = "";
    private String templateId;

    public TemplateCustomizeViewModel() {
        apiService = ApiClient.getClient();
    }

    public LiveData<Template> getTemplate() {
        return template;
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

    public void setRecipientName(String name) {
        this.recipientName = name;
        generatePreview();
    }

    public void setSenderName(String name) {
        this.senderName = name;
        generatePreview();
    }

    public void setMessage(String message) {
        this.message = message;
        generatePreview();
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public void loadTemplate(String templateId) {
        this.templateId = templateId;
        
        apiService.getTemplateById(templateId).enqueue(new Callback<Template>() {
            @Override
            public void onResponse(Call<Template> call, Response<Template> response) {
                if (response.isSuccessful() && response.body() != null) {
                    template.setValue(response.body());
                    generatePreview();
                } else {
                    error.setValue("Failed to load template");
                }
            }

            @Override
            public void onFailure(Call<Template> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }

    private void generatePreview() {
        Template currentTemplate = template.getValue();
        if (currentTemplate == null) return;

        String html = currentTemplate.getHtmlContent();
        if (html == null) return;

        // Replace placeholders with actual values
        html = html.replace("{{recipientName}}", recipientName)
                  .replace("{{senderName}}", senderName)
                  .replace("{{message}}", message);

        // Add CSS and JS
        String css = currentTemplate.getCssContent();
        String js = currentTemplate.getJsContent();
        
        StringBuilder fullHtml = new StringBuilder();
        fullHtml.append("<!DOCTYPE html><html><head><style>");
        if (css != null) fullHtml.append(css);
        fullHtml.append("</style></head><body>");
        fullHtml.append(html);
        fullHtml.append("<script>");
        if (js != null) fullHtml.append(js);
        fullHtml.append("</script></body></html>");

        previewHtml.setValue(fullHtml.toString());
    }

    public void saveWish() {
        Template currentTemplate = template.getValue();
        if (currentTemplate == null) return;

        SharedWish wish = new SharedWish();
        wish.setTemplateId(templateId);
        wish.setRecipientName(recipientName);
        wish.setSenderName(senderName);
        wish.setMessage(message);

        apiService.createSharedWish(wish).enqueue(new Callback<SharedWish>() {
            @Override
            public void onResponse(Call<SharedWish> call, Response<SharedWish> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sharedWish.setValue(response.body());
                } else {
                    error.setValue("Failed to save wish");
                }
            }

            @Override
            public void onFailure(Call<SharedWish> call, Throwable t) {
                error.setValue("Network error: " + t.getMessage());
            }
        });
    }
}
