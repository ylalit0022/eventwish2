package com.ds.eventwish.utils;

import java.text.DecimalFormat;

/**
 * Utility class for formatting numbers in a social media style (1K, 10K, 1M, etc.)
 */
public class NumberFormatter {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.#");
    
    /**
     * Format a number in social media style (1K, 10K, 1M, etc.)
     * 
     * @param count The number to format
     * @return Formatted string (e.g., 1K, 10K, 1M)
     */
    public static String formatCount(long count) {
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 10000) {
            // For numbers between 1,000 and 9,999, format as #.#K (e.g., 1.2K)
            double value = count / 1000.0;
            return DECIMAL_FORMAT.format(value) + "K";
        } else if (count < 1000000) {
            // For numbers between 10,000 and 999,999, format as #K (e.g., 10K, 999K)
            return (count / 1000) + "K";
        } else if (count < 10000000) {
            // For numbers between 1,000,000 and 9,999,999, format as #.#M (e.g., 1.2M)
            double value = count / 1000000.0;
            return DECIMAL_FORMAT.format(value) + "M";
        } else {
            // For numbers 10,000,000 and above, format as #M (e.g., 10M, 100M)
            return (count / 1000000) + "M";
        }
    }
} 