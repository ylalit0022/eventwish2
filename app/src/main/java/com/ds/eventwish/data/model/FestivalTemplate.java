package com.ds.eventwish.data.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class FestivalTemplate {
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName(value = "imageUrl", alternate = {"previewUrl"})
    
    private String imageUrl;    
    @SerializedName("category")
    private String category;
    
    @SerializedName("htmlContent")
    private String htmlContent;
    
    @SerializedName("cssContent")
    private String cssContent;
    
    @SerializedName("jsContent")
    private String jsContent;
    
    public FestivalTemplate() {
        // Required for Gson
    }
    
    public FestivalTemplate(String id, String title, String content, String imageUrl, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.category = category;
    }
    
    public FestivalTemplate(String id, String title, String content, String imageUrl, String category,
                           String htmlContent, String cssContent, String jsContent) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.category = category;
        this.htmlContent = htmlContent;
        this.cssContent = cssContent;
        this.jsContent = jsContent;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getHtmlContent() {
        return htmlContent;
    }
    
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
    
    public String getCssContent() {
        return cssContent;
    }
    
    public void setCssContent(String cssContent) {
        this.cssContent = cssContent;
    }
    
    public String getJsContent() {
        return jsContent;
    }
    
    public void setJsContent(String jsContent) {
        this.jsContent = jsContent;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FestivalTemplate that = (FestivalTemplate) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @NonNull
    @Override
    public String toString() {
        return "FestivalTemplate{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
