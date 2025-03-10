package com.ds.eventwish.data.model;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

@JsonAdapter(CategoryIcon.CategoryIconTypeAdapter.class)
public class CategoryIcon {
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

    public static class CategoryIconTypeAdapter extends TypeAdapter<CategoryIcon> {
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
            if (in.peek() == com.google.gson.stream.JsonToken.STRING) {
                // If the input is a string, treat it as the categoryIcon URL
                String iconUrl = in.nextString();
                return new CategoryIcon(null, null, iconUrl);
            }

            in.beginObject();
            String id = null;
            String category = null;
            String categoryIcon = null;

            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "_id":
                        id = in.nextString();
                        break;
                    case "category":
                        category = in.nextString();
                        break;
                    case "categoryIcon":
                        categoryIcon = in.nextString();
                        break;
                    default:
                        in.skipValue();
                        break;
                }
            }
            in.endObject();

            return new CategoryIcon(id, category, categoryIcon);
        }
    }
}