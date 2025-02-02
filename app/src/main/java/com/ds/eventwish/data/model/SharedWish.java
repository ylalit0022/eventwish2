package com.ds.eventwish.data.model;

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
    
    @SerializedName("customizedHtml")
    private String customizedHtml;
    
    @SerializedName("views")
    private int views;
    
    @SerializedName("lastSharedAt")
    private Date lastSharedAt;
    
    @SerializedName("createdAt")
    private Date createdAt;
    
    @SerializedName("updatedAt")
    private Date updatedAt;

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
               Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shortCode, templateId);
    }
}
