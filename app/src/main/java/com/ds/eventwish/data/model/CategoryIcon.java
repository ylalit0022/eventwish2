package com.ds.eventwish.data.model;

import android.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@JsonAdapter(CategoryIcon.CategoryIconTypeAdapter.class)
public class CategoryIcon {
    private static final String TAG = "CategoryIcon";
    
    @SerializedName("_id")
    private String id;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("categoryIcon")
    private String categoryIcon;

    public CategoryIcon(String id, String category, String categoryIcon) {
        this.id = id;
        this.category = category;
        this.categoryIcon = categoryIcon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    @Override
    public String toString() {
        return "CategoryIcon{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", categoryIcon='" + categoryIcon + '\'' +
                '}';
    }

    public static class CategoryIconTypeAdapter extends com.google.gson.TypeAdapter<CategoryIcon> {
        @Override
        public void write(JsonWriter out, CategoryIcon value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name("_id").value(value.getId());
            out.name("category").value(value.getCategory());
            out.name("categoryIcon").value(value.getCategoryIcon());
            out.endObject();
        }

        @Override
        public CategoryIcon read(JsonReader in) throws IOException {
            // Handle null input
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                Log.d(TAG, "Received NULL value in JSON");
                return null;
            }
            
            // Handle string input (direct URL)
            if (in.peek() == JsonToken.STRING) {
                String iconUrl = in.nextString();
                Log.d(TAG, "Parsed direct string URL: " + iconUrl);
                return new CategoryIcon(null, null, iconUrl);
            }
            
            // Handle boolean or number (invalid but we'll handle gracefully)
            if (in.peek() == JsonToken.BOOLEAN || in.peek() == JsonToken.NUMBER) {
                String value = in.nextString();
                Log.w(TAG, "Received unexpected value type (boolean/number): " + value);
                return new CategoryIcon(null, null, null);
            }
            
            // Handle array format (take first item)
            if (in.peek() == JsonToken.BEGIN_ARRAY) {
                try {
                    return handleArrayFormat(in);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing JSON array: " + e.getMessage(), e);
                    return new CategoryIcon(null, null, null);
                }
            }
            
            // Handle regular object format
            if (in.peek() == JsonToken.BEGIN_OBJECT) {
                try {
                    return handleObjectFormat(in);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing JSON object: " + e.getMessage(), e);
                    // Try to skip the object to prevent parsing issues
                    try {
                        skipObject(in);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to skip object: " + ex.getMessage(), ex);
                    }
                    return new CategoryIcon(null, null, null);
                }
            }
            
            // Default fallback for unknown types
            Log.w(TAG, "Unknown JSON token: " + in.peek());
            try {
                in.skipValue();
            } catch (Exception e) {
                Log.e(TAG, "Error skipping unknown value: " + e.getMessage(), e);
            }
            return new CategoryIcon(null, null, null);
        }
        
        // Helper method to skip an object
        private void skipObject(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.BEGIN_OBJECT) {
                in.beginObject();
                while (in.hasNext()) {
                    in.nextName();
                    in.skipValue();
                }
                in.endObject();
            }
        }
        
        private CategoryIcon handleArrayFormat(JsonReader in) throws IOException {
            in.beginArray();
            
            // If array is empty, return null object
            if (in.peek() == JsonToken.END_ARRAY) {
                in.endArray();
                Log.d(TAG, "Empty JSON array");
                return new CategoryIcon(null, null, null);
            }
            
            CategoryIcon result = null;
            
            // Check type of first array element
            if (in.peek() == JsonToken.STRING) {
                // Array of strings, use first as URL
                String iconUrl = in.nextString();
                result = new CategoryIcon(null, null, iconUrl);
                Log.d(TAG, "Using first string from array as URL: " + iconUrl);
            } else if (in.peek() == JsonToken.BEGIN_OBJECT) {
                // Array of objects, parse first object
                result = handleObjectFormat(in);
            } else {
                // Skip unknown element
                in.skipValue();
                result = new CategoryIcon(null, null, null);
                Log.w(TAG, "Skipped unknown array element type: " + in.peek());
            }
            
            // Skip rest of array
            while (in.peek() != JsonToken.END_ARRAY) {
                try {
                    in.skipValue();
                } catch (Exception e) {
                    Log.e(TAG, "Error skipping array value: " + e.getMessage(), e);
                    break;
                }
            }
            in.endArray();
            
            return result;
        }
        
        private CategoryIcon handleObjectFormat(JsonReader in) throws IOException {
            in.beginObject();
            String id = null;
            String category = null;
            String categoryIcon = null;

            try {
                while (in.hasNext()) {
                    String name = in.nextName();
                    switch (name) {
                        case "_id":
                            id = readStringValue(in);
                            break;
                        case "id":
                            if (id == null) { // Only set if not already set from _id
                                id = readStringValue(in);
                            } else {
                                in.skipValue();
                            }
                            break;
                        case "category":
                            category = readStringValue(in);
                            break;
                        case "categoryIcon":
                            categoryIcon = extractIconUrl(in);
                            break;
                        case "icon":
                        case "url":
                        case "src":
                        case "uri":
                        case "imageUrl":
                        case "image":
                            // Only set if main categoryIcon not set
                            if (categoryIcon == null || categoryIcon.isEmpty()) {
                                categoryIcon = extractIconUrl(in);
                            } else {
                                in.skipValue();
                            }
                            break;
                        default:
                            in.skipValue();
                            break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing object fields: " + e.getMessage(), e);
                // Continue parsing to ensure we properly close the object
            }
            
            in.endObject();
            
            // Ensure we have a category if we have an ID
            if (category == null && id != null) {
                // Try to extract category from ID if possible
                category = extractCategoryFromId(id);
            }
            
            CategoryIcon result = new CategoryIcon(id, category, categoryIcon);
            Log.d(TAG, "Parsed object: " + result);
            return result;
        }
        
        // Helper to extract category from ID if possible
        private String extractCategoryFromId(String id) {
            if (id == null || id.isEmpty()) {
                return null;
            }
            
            // Some IDs might contain category information (implementation-specific)
            // This is a placeholder for any logic you might want to add
            return null;
        }
        
        // Helper to safely read string values, handling nulls
        private String readStringValue(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else if (in.peek() == JsonToken.STRING) {
                return in.nextString();
            } else if (in.peek() == JsonToken.BEGIN_OBJECT || in.peek() == JsonToken.BEGIN_ARRAY) {
                // Skip complex types and return null
                in.skipValue();
                Log.w(TAG, "Expected string but got complex type (object/array)");
                return null;
            } else {
                // Handle unexpected types by converting to string
                try {
                    String value = in.nextString();
                    Log.w(TAG, "Expected string but got: " + value);
                    return value;
                } catch (Exception e) {
                    Log.e(TAG, "Error reading non-string value: " + e.getMessage(), e);
                    in.skipValue();
                    return null;
                }
            }
        }
        
        // Extract icon URL from potentially nested objects or direct values
        private String extractIconUrl(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else if (in.peek() == JsonToken.STRING) {
                String url = in.nextString();
                // Validate URL format
                if (url != null && !url.isEmpty() && !url.trim().isEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Log.w(TAG, "Icon URL doesn't start with http(s)://: " + url);
                        // Try to fix common URL issues
                        if (url.startsWith("//")) {
                            url = "https:" + url;
                            Log.d(TAG, "Fixed protocol-relative URL: " + url);
                        } else if (!url.contains("://")) {
                            url = "https://" + url;
                            Log.d(TAG, "Added https:// prefix to URL: " + url);
                        }
                    }
                    return url;
                } else {
                    Log.w(TAG, "Empty icon URL");
                    return null;
                }
            } else if (in.peek() == JsonToken.BEGIN_OBJECT) {
                // Handle nested objects with URL fields
                in.beginObject();
                String url = null;
                
                while (in.hasNext()) {
                    String fieldName = in.nextName();
                    if (fieldName.equals("url") || 
                        fieldName.equals("src") || 
                        fieldName.equals("uri") ||
                        fieldName.equals("href") ||
                        fieldName.equals("imageUrl")) {
                        url = readStringValue(in);
                    } else {
                        in.skipValue();
                    }
                }
                
                in.endObject();
                return url;
            } else {
                // Skip other types (arrays, etc.)
                in.skipValue();
                return null;
            }
        }
    }
}