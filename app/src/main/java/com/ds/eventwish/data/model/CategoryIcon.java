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
                iconUrl = validateAndFixUrl(iconUrl);
                if (iconUrl != null) {
                    return new CategoryIcon(null, null, iconUrl);
                } else {
                    Log.w(TAG, "Invalid direct string URL, returning empty icon");
                    return new CategoryIcon(null, null, null);
                }
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
                // Validate URL before using it
                iconUrl = validateAndFixUrl(iconUrl);
                if (iconUrl != null) {
                    result = new CategoryIcon(null, null, iconUrl);
                    Log.d(TAG, "Using first string from array as URL: " + iconUrl);
                } else {
                    Log.w(TAG, "Invalid URL in array, returning empty icon");
                    result = new CategoryIcon(null, null, null);
                }
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
                            // Check if this is an ObjectId string or a nested object
                            if (in.peek() == JsonToken.STRING) {
                                String value = in.nextString();
                                // If this looks like an ObjectId, convert it to an API URL
                                if (value != null && value.matches("^[0-9a-fA-F]{24}$")) {
                                    Log.d(TAG, "Found categoryIcon as ObjectId reference: " + value);
                                    categoryIcon = createApiUrlFromId(value);
                                } else {
                                    categoryIcon = validateAndFixUrl(value);
                                }
                            } else {
                                categoryIcon = extractIconUrl(in);
                            }
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
            
            // If we have an ID but no icon URL, try to construct a URL from the ID
            if ((categoryIcon == null || categoryIcon.isEmpty()) && id != null && id.matches("^[0-9a-fA-F]{24}$")) {
                Log.d(TAG, "Using object ID to construct icon URL: " + id);
                categoryIcon = createApiUrlFromId(id);
            }
            
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
                return validateAndFixUrl(url);
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
                        if (url != null && !url.isEmpty()) {
                            // Found a valid URL field, no need to check other fields
                            break;
                        }
                    } else {
                        in.skipValue();
                    }
                }
                
                in.endObject();
                return validateAndFixUrl(url);
            } else {
                // Skip other types (arrays, etc.)
                in.skipValue();
                return null;
            }
        }
        
        /**
         * Validate and fix URL with improved handling of common issues
         */
        private String validateAndFixUrl(String url) {
            if (url == null) {
                return null;
            }
            
            // Trim the URL
            url = url.trim();
            
            // Empty URL check
            if (url.isEmpty()) {
                Log.w(TAG, "Empty icon URL after trim");
                return null;
            }
            
            // Validate common problematic values
            if (url.equalsIgnoreCase("null") || 
                url.equalsIgnoreCase("undefined") ||
                url.equalsIgnoreCase("n/a") ||
                url.equalsIgnoreCase("unknown")) {
                Log.w(TAG, "Icon URL contains invalid value: " + url);
                return null;
            }
            
            // Check if this is a MongoDB ObjectId (24 hex chars)
            if (url.matches("^[0-9a-fA-F]{24}$")) {
                Log.d(TAG, "Detected MongoDB ObjectId instead of URL: " + url);
                // Make sure it's a valid ObjectId as per MongoDB rules
                try {
                    // Simple validation to match backend validator
                    if (url.length() == 24) {
                        // Convert ObjectId to API URL to fetch the actual icon
                        return createApiUrlFromId(url);
                    } else {
                        Log.w(TAG, "Invalid MongoDB ObjectId format: " + url);
                        return null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error validating ObjectId: " + url, e);
                    return null;
                }
            }
            
            // Fix common URL issues
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Log.w(TAG, "Icon URL doesn't start with http(s)://: " + url);
                
                // Handle various protocol-relative or missing protocol cases
                if (url.startsWith("//")) {
                    url = "https:" + url;
                    Log.d(TAG, "Fixed protocol-relative URL: " + url);
                } else if (url.startsWith("www.")) {
                    url = "https://" + url;
                    Log.d(TAG, "Added https:// prefix to www URL: " + url);
                } else if (!url.contains("://")) {
                    url = "https://" + url;
                    Log.d(TAG, "Added https:// prefix to URL: " + url);
                }
            }
            
            // Fix double protocol issues
            if (url.contains("http://http://")) {
                url = url.replace("http://http://", "http://");
                Log.d(TAG, "Fixed double http:// in URL: " + url);
            }
            if (url.contains("https://http://")) {
                url = url.replace("https://http://", "http://");
                Log.d(TAG, "Fixed https://http:// in URL: " + url);
            }
            if (url.contains("http://https://")) {
                url = url.replace("http://https://", "https://");
                Log.d(TAG, "Fixed http://https:// in URL: " + url);
            }
            if (url.contains("https://https://")) {
                url = url.replace("https://https://", "https://");
                Log.d(TAG, "Fixed double https:// in URL: " + url);
            }
            
            // Try to validate URL structure
            try {
                java.net.URL parsedUrl = new java.net.URL(url);
                String host = parsedUrl.getHost();
                if (host == null || host.isEmpty()) {
                    Log.w(TAG, "URL has no host: " + url);
                    return null;
                }
            } catch (Exception e) {
                Log.w(TAG, "Invalid URL format: " + url, e);
                return null;
            }
            
            return url;
        }

        /**
         * Create an API URL to fetch a CategoryIcon by its ID
         * @param id The MongoDB ObjectId
         * @return The API URL for the icon
         */
        public static String createApiUrlFromId(String id) {
            if (id == null || id.isEmpty() || !id.matches("^[0-9a-fA-F]{24}$")) {
                Log.w(TAG, "Invalid ObjectId for API URL creation: " + id);
                return null;
            }
            return "https://eventwish2.onrender.com/api/categoryIcons/" + id;
        }
    }
}