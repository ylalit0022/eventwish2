package com.ds.eventwish.data.model;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.Objects;

public class SharedWish {
    @SerializedName("_id")
    private String id;
    
    private String shortCode;
    private String message;
    
    @SerializedName("templateId")
    private String templateId;

    @SerializedName("template")
    private Template template;
    
    @SerializedName("recipientName")
    private String recipientName;
    
    @SerializedName("senderName")
    private String senderName;
    
    @SerializedName("htmlContent")
    private String customizedHtml;
    
    @SerializedName("views")
    private int views;
    
    @SerializedName("lastSharedAt")
    private Date lastSharedAt;
    
    @SerializedName("createdAt")
    private Date createdAt;
    
    @SerializedName("updatedAt")
    private Date updatedAt;

    @SerializedName("cssContent")
    private String cssContent;

    @SerializedName("jsContent")
    private String jsContent;

    @SerializedName("previewUrl")
    private String previewUrl;

    @SerializedName("sharedVia")
    private String sharedVia = "LINK";

    public SharedWish() {
    }

    public SharedWish(String senderName, String recipientName, String message, String templateId, String previewUrl) {
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.message = message;
        this.templateId = templateId;
        this.previewUrl = previewUrl;
        Log.d("SharedWish", "Created with previewUrl: " + previewUrl);
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
        Log.d("SharedWish", "Preview URL set to: " + previewUrl);
    }

    public String getJsContent() {
        return jsContent;
    }

    public void setJsContent(String jsContent) {
        this.jsContent = jsContent;
    }

    public String getCssContent() {
        return cssContent;
    }

    public void setCssContent(String cssContent) {
        this.cssContent = cssContent;
    }

    public String getSharedVia() {
        return sharedVia;
    }

    public void setSharedVia(String sharedVia) {
        this.sharedVia = sharedVia;
    }

    // Getters
    public String getId() { 
        return id; 
    }

    public String getShortCode() { 
        return shortCode; 
    }

    public String getMessage() { 
        return message; 
    }

    public String getTemplateId() {
        return templateId; 
    }

    public Template getTemplate() { 
        return template; 
    }

    public String getRecipientName() { 
        return recipientName != null ? recipientName : ""; 
    }

    public String getSenderName() { 
        return senderName != null ? senderName : ""; 
    }

    public String getCustomizedHtml() { 
        return customizedHtml; 
    }

    public int getViews() { 
        return views; 
    }

    public Date getLastSharedAt() { 
        return lastSharedAt; 
    }

    public Date getCreatedAt() { 
        return createdAt; 
    }

    public Date getUpdatedAt() { 
        return updatedAt; 
    }

    // Setters
    public void setId(String id) { 
        this.id = id; 
    }

    public void setShortCode(String shortCode) { 
        this.shortCode = shortCode; 
    }

    public void setMessage(String message) { 
        this.message = message; 
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId; 
    }

    public void setTemplate(Template template) { 
        this.template = template; 
    }

    public void setRecipientName(String recipientName) { 
        this.recipientName = recipientName; 
    }

    public void setSenderName(String senderName) { 
        this.senderName = senderName; 
    }

    public void setCustomizedHtml(String customizedHtml) { 
        this.customizedHtml = customizedHtml; 
    }

    public void setViews(int views) { 
        this.views = views; 
    }

    public void setLastSharedAt(Date lastSharedAt) { 
        this.lastSharedAt = lastSharedAt; 
    }

    public void setCreatedAt(Date createdAt) { 
        this.createdAt = createdAt; 
    }

    public void setUpdatedAt(Date updatedAt) { 
        this.updatedAt = updatedAt; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedWish that = (SharedWish) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(shortCode, that.shortCode) &&
               Objects.equals(previewUrl, that.previewUrl) &&
               Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortCode, templateId);
    }
}
