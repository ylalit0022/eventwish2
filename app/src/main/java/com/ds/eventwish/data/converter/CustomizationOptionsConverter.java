package com.ds.eventwish.data.converter;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Type converter for CustomizationOptions objects in Room database
 * Handles conversion between JSON string and CustomizationOptions object
 */
public class CustomizationOptionsConverter {
    private static final Gson gson = new Gson();

    /**
     * Convert CustomizationOptions object to JSON string for storage
     */
    @TypeConverter
    public static String fromCustomizationOptions(CustomizationOptions options) {
        return options == null ? null : gson.toJson(options);
    }

    /**
     * Convert JSON string to CustomizationOptions object
     */
    @TypeConverter
    public static CustomizationOptions toCustomizationOptions(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(value, CustomizationOptions.class);
        } catch (JsonSyntaxException e) {
            // Return null if JSON is invalid
            return null;
        }
    }

    /**
     * CustomizationOptions data class for Template customization settings
     */
    public static class CustomizationOptions {
        private boolean allowNameEdit;
        private boolean allowPhotoEdit;
        private boolean allowColorEdit;
        private boolean allowTextEdit;
        private boolean allowLayoutEdit;

        // Default constructor
        public CustomizationOptions() {
            this.allowNameEdit = true;
            this.allowPhotoEdit = true;
            this.allowColorEdit = true;
            this.allowTextEdit = true;
            this.allowLayoutEdit = false;
        }

        // Constructor with parameters
        public CustomizationOptions(boolean allowNameEdit, boolean allowPhotoEdit, 
                                  boolean allowColorEdit, boolean allowTextEdit, 
                                  boolean allowLayoutEdit) {
            this.allowNameEdit = allowNameEdit;
            this.allowPhotoEdit = allowPhotoEdit;
            this.allowColorEdit = allowColorEdit;
            this.allowTextEdit = allowTextEdit;
            this.allowLayoutEdit = allowLayoutEdit;
        }

        // Getters
        public boolean isAllowNameEdit() { return allowNameEdit; }
        public boolean isAllowPhotoEdit() { return allowPhotoEdit; }
        public boolean isAllowColorEdit() { return allowColorEdit; }
        public boolean isAllowTextEdit() { return allowTextEdit; }
        public boolean isAllowLayoutEdit() { return allowLayoutEdit; }

        // Setters
        public void setAllowNameEdit(boolean allowNameEdit) { this.allowNameEdit = allowNameEdit; }
        public void setAllowPhotoEdit(boolean allowPhotoEdit) { this.allowPhotoEdit = allowPhotoEdit; }
        public void setAllowColorEdit(boolean allowColorEdit) { this.allowColorEdit = allowColorEdit; }
        public void setAllowTextEdit(boolean allowTextEdit) { this.allowTextEdit = allowTextEdit; }
        public void setAllowLayoutEdit(boolean allowLayoutEdit) { this.allowLayoutEdit = allowLayoutEdit; }

        @Override
        public String toString() {
            return "CustomizationOptions{" +
                    "allowNameEdit=" + allowNameEdit +
                    ", allowPhotoEdit=" + allowPhotoEdit +
                    ", allowColorEdit=" + allowColorEdit +
                    ", allowTextEdit=" + allowTextEdit +
                    ", allowLayoutEdit=" + allowLayoutEdit +
                    '}';
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CustomizationOptions that = (CustomizationOptions) obj;
            return allowNameEdit == that.allowNameEdit &&
                    allowPhotoEdit == that.allowPhotoEdit &&
                    allowColorEdit == that.allowColorEdit &&
                    allowTextEdit == that.allowTextEdit &&
                    allowLayoutEdit == that.allowLayoutEdit;
        }

        @Override
        public int hashCode() {
            int result = Boolean.hashCode(allowNameEdit);
            result = 31 * result + Boolean.hashCode(allowPhotoEdit);
            result = 31 * result + Boolean.hashCode(allowColorEdit);
            result = 31 * result + Boolean.hashCode(allowTextEdit);
            result = 31 * result + Boolean.hashCode(allowLayoutEdit);
            return result;
        }
    }
} 