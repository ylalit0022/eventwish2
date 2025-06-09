package com.ds.eventwish.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StringUtils {
    
    /**
     * Converts a string to camelCase format.
     * Example: "john doe" becomes "John Doe"
     *
     * @param input The input string to convert
     * @return The camelCase formatted string, or null if input is null
     */
    @Nullable
    public static String toCamelCase(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char ch : input.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
                result.append(ch);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(ch));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(ch));
            }
        }
        
        return result.toString();
    }
} 