package com.ds.eventwish.data.model.response;

import com.ds.eventwish.data.model.CategoryIcon;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for category icons API
 * Can handle both object and array responses
 */
public class CategoryIconResponse {
    @SerializedName("data")
    private List<CategoryIcon> data;
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("message")
    private String message;
    
    public CategoryIconResponse() {
        this.data = new ArrayList<>();
    }
    
    public List<CategoryIcon> getData() {
        return data != null ? data : new ArrayList<>();
    }
    
    public void setData(List<CategoryIcon> data) {
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
} 