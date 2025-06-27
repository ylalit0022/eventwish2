package com.ds.eventwish.utils;

/**
 * Utility class for formatting numbers in a human-readable way
 */
public class NumberFormatter {
    
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};
    private static final int MAX_LENGTH = 4;
    
    /**
     * Format a number to a human-readable string with K, M, B suffixes
     * @param number The number to format
     * @return Formatted string (e.g., 1.2K, 3.4M, etc.)
     */
    public static String format(long number) {
        if (number < 0) return "0";
        if (number < 1000) return String.valueOf(number);
        
        int exponent = (int) (Math.log10(number) / 3);
        double value = number / Math.pow(1000, exponent);
        String suffix = SUFFIXES[Math.min(exponent, SUFFIXES.length - 1)];
        
        return String.format("%.1f%s", value, suffix);
    }
    
    /**
     * Format a number to a compact string with maximum length
     * @param number The number to format
     * @return Formatted string
     */
    public static String formatCompact(long number) {
        String formatted = format(number);
        if (formatted.length() > MAX_LENGTH) {
            return formatted.substring(0, MAX_LENGTH);
        }
        return formatted;
    }
} 