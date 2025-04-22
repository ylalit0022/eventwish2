package com.ds.eventwish.data.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.ds.eventwish.data.converter.DateConverter;
import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.Objects;

@Entity(
    tableName = "shared_wish",
    foreignKeys = @ForeignKey(
        entity = Template.class,
        parentColumns = "id",
        childColumns = "template_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"template_id"}),
        @Index(value = {"short_code"}, unique = true),
        @Index(value = {"created_at"})
    }
)
public class SharedWish {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    @SerializedName("_id")
    private String id;
    
    @ColumnInfo(name = "short_code")
    private String shortCode;

    @ColumnInfo(name = "message")
    private String message;
    
    @ColumnInfo(name = "template_id")
    @SerializedName("templateId")
    private String templateId;

    @Ignore
    @SerializedName("template")
    private Template template;
    
    @ColumnInfo(name = "recipient_name")
    @SerializedName("recipientName")
    private String recipientName;
    
    @ColumnInfo(name = "sender_name")
    @SerializedName("senderName")
    private String senderName;
    
    @ColumnInfo(name = "customized_html")
    @SerializedName("customizedHtml")
    private String customizedHtml;
    
    @ColumnInfo(name = "views")
    @SerializedName("views")
    private int views;
    
    @ColumnInfo(name = "last_shared_at")
    @SerializedName("lastSharedAt")
    @TypeConverters(DateConverter.class)
    private Date lastSharedAt;
    
    @ColumnInfo(name = "created_at")
    @SerializedName("createdAt")
    @TypeConverters(DateConverter.class)
    private Date createdAt;
    
    @ColumnInfo(name = "updated_at")
    @SerializedName("updatedAt")
    @TypeConverters(DateConverter.class)
    private Date updatedAt;

    @ColumnInfo(name = "css_content")
    @SerializedName("cssContent")
    private String cssContent;

    @ColumnInfo(name = "js_content")
    @SerializedName("jsContent")
    private String jsContent;

    @ColumnInfo(name = "preview_url")
    @SerializedName("previewUrl")
    private String previewUrl;

    @ColumnInfo(name = "shared_via")
    @SerializedName("sharedVia")
    private String sharedVia;

    @ColumnInfo(name = "title")
    @SerializedName("title")
    private String title;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "deep_link")
    @SerializedName("deepLink")
    private String deepLink;

    // Required by Room
    public SharedWish() {
    }

    @Ignore
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
        return sharedVia != null ? sharedVia : "LINK";
    }

    public void setSharedVia(String sharedVia) {
        this.sharedVia = sharedVia;
        Log.d("SharedWish", "SharedVia set to: " + sharedVia);
    }

    public String getTitle() {
        return title != null ? title : "";

    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeepLink() {
        return deepLink != null ? deepLink : "";
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
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

