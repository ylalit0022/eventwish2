package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.Template;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class TemplateResponse {
    @SerializedName("data")
    private List<Template> templates;
    
    private int page;
    private int totalPages;
    private int totalItems;
    private boolean hasMore;
    private Map<String, Integer> categories;
    private int totalTemplates;

    // Getters
    public List<Template> getTemplates() { return templates; }
    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }
    public int getTotalItems() { return totalItems; }
    public boolean isHasMore() { return hasMore; }
    public Map<String, Integer> getCategories() { return categories; }
    public int getTotalTemplates() { return totalTemplates; }

    // Setters
    public void setTemplates(List<Template> templates) { this.templates = templates; }
    public void setPage(int page) { this.page = page; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public void setCategories(Map<String, Integer> categories) { this.categories = categories; }
    public void setTotalTemplates(int totalTemplates) { this.totalTemplates = totalTemplates; }
}
