package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.Template;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class WishResponse {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("shortCode")
    private String shortCode;
    
    private String message;

    @SerializedName("templateId")
    private Template template;
    
    @SerializedName("recipientName")
    private String recipientName;
    
    @SerializedName("senderName")
    private String senderName;

    @SerializedName("customizedHtml")
    private String customizedHtml;
    
    @SerializedName("cssContent")
    private String cssContent;
    
    @SerializedName("jsContent")
    private String jsContent;
    
    @SerializedName("views")
    private int views;
    
    @SerializedName("lastSharedAt")
    private Date lastSharedAt;
    
    @SerializedName("createdAt")
    private Date createdAt;

    // Getters and Setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Template getTemplate() { return template; }
    public void setTemplate(Template template) { this.template = template; }
    
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getCustomizedHtml() { return customizedHtml; }
    public void setCustomizedHtml(String customizedHtml) { this.customizedHtml = customizedHtml; }
    
    public String getCssContent() { return cssContent; }
    public void setCssContent(String cssContent) { this.cssContent = cssContent; }
    
    public String getJsContent() { return jsContent; }
    public void setJsContent(String jsContent) { this.jsContent = jsContent; }
    
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    
    public Date getLastSharedAt() { return lastSharedAt; }
    public void setLastSharedAt(Date lastSharedAt) { this.lastSharedAt = lastSharedAt; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}