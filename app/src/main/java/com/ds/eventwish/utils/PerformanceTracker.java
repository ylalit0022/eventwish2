package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.perf.metrics.HttpMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for tracking app performance with Firebase Performance Monitoring
 */
public class PerformanceTracker {
    private static final String TAG = "PerformanceTracker";
    
    private static boolean isInitialized = false;
    private static FirebasePerformance performance;
    private static Map<String, Trace> activeTraces = new HashMap<>();
    
    /**
     * Initialize Firebase Performance Monitoring
     * @param context Application context
     */
    public static void init(@NonNull Context context) {
        try {
            performance = FirebasePerformance.getInstance();
            
            // Enable performance monitoring data collection
            performance.setPerformanceCollectionEnabled(true);
            
            isInitialized = true;
            Log.d(TAG, "Firebase Performance Monitoring initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Performance Monitoring", e);
            isInitialized = false;
        }
    }
    
    /**
     * Start a trace for a specific operation
     * @param traceName Name of the trace
     * @return True if trace started successfully, false otherwise
     */
    public static boolean startTrace(@NonNull String traceName) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        try {
            if (activeTraces.containsKey(traceName)) {
                Log.w(TAG, "Trace already exists: " + traceName);
                return false;
            }
            
            Trace trace = performance.newTrace(traceName);
            trace.start();
            activeTraces.put(traceName, trace);
            
            Log.d(TAG, "Started trace: " + traceName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start trace: " + traceName, e);
            return false;
        }
    }
    
    /**
     * Stop a trace for a specific operation
     * @param traceName Name of the trace
     * @return True if trace stopped successfully, false otherwise
     */
    public static boolean stopTrace(@NonNull String traceName) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        try {
            Trace trace = activeTraces.get(traceName);
            if (trace == null) {
                Log.w(TAG, "No active trace found with name: " + traceName);
                return false;
            }
            
            trace.stop();
            activeTraces.remove(traceName);
            
            Log.d(TAG, "Stopped trace: " + traceName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop trace: " + traceName, e);
            return false;
        }
    }
    
    /**
     * Add a metric to an active trace
     * @param traceName Name of the trace
     * @param metricName Name of the metric
     * @param value Value of the metric
     * @return True if metric was added successfully, false otherwise
     */
    public static boolean addTraceMetric(@NonNull String traceName, @NonNull String metricName, long value) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        try {
            Trace trace = activeTraces.get(traceName);
            if (trace == null) {
                Log.w(TAG, "No active trace found with name: " + traceName);
                return false;
            }
            
            trace.putMetric(metricName, value);
            
            Log.d(TAG, "Added metric to trace: " + traceName + ", metric: " + metricName + " = " + value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add metric to trace: " + traceName, e);
            return false;
        }
    }
    
    /**
     * Increment a counter metric on an active trace
     * @param traceName Name of the trace
     * @param counterName Name of the counter
     * @param incrementBy Value to increment by
     * @return True if counter was incremented successfully, false otherwise
     */
    public static boolean incrementTraceCounter(@NonNull String traceName, @NonNull String counterName, long incrementBy) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        try {
            Trace trace = activeTraces.get(traceName);
            if (trace == null) {
                Log.w(TAG, "No active trace found with name: " + traceName);
                return false;
            }
            
            trace.incrementMetric(counterName, incrementBy);
            
            Log.d(TAG, "Incremented counter on trace: " + traceName + ", counter: " + counterName + " by " + incrementBy);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to increment counter on trace: " + traceName, e);
            return false;
        }
    }
    
    /**
     * Add an attribute to an active trace
     * @param traceName Name of the trace
     * @param attributeName Name of the attribute
     * @param value Value of the attribute
     * @return True if attribute was added successfully, false otherwise
     */
    public static boolean addTraceAttribute(@NonNull String traceName, @NonNull String attributeName, @NonNull String value) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        try {
            Trace trace = activeTraces.get(traceName);
            if (trace == null) {
                Log.w(TAG, "No active trace found with name: " + traceName);
                return false;
            }
            
            trace.putAttribute(attributeName, value);
            
            Log.d(TAG, "Added attribute to trace: " + traceName + ", attribute: " + attributeName + " = " + value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add attribute to trace: " + traceName, e);
            return false;
        }
    }
    
    /**
     * Create an HTTP metric for a network request
     * @param url URL of the request
     * @param httpMethod HTTP method (GET, POST, etc.)
     * @return HttpMetric object or null if creation failed
     */
    public static HttpMetric createHttpMetric(@NonNull String url, @NonNull String httpMethod) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return null;
        }
        
        try {
            HttpMetric metric = performance.newHttpMetric(url, httpMethod);
            Log.d(TAG, "Created HTTP metric for: " + httpMethod + " " + url);
            return metric;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create HTTP metric", e);
            return null;
        }
    }
    
    /**
     * Start an HTTP metric for a network request
     * @param metric HttpMetric object to start
     * @return True if metric started successfully, false otherwise
     */
    public static boolean startHttpMetric(@Nullable HttpMetric metric) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        if (metric == null) {
            Log.e(TAG, "Cannot start null HTTP metric");
            return false;
        }
        
        try {
            metric.start();
            Log.d(TAG, "Started HTTP metric");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start HTTP metric", e);
            return false;
        }
    }
    
    /**
     * Stop an HTTP metric for a network request
     * @param metric HttpMetric object to stop
     * @param responseCode HTTP response code
     * @param responseSize Response size in bytes
     * @return True if metric stopped successfully, false otherwise
     */
    public static boolean stopHttpMetric(@Nullable HttpMetric metric, int responseCode, long responseSize) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        if (metric == null) {
            Log.e(TAG, "Cannot stop null HTTP metric");
            return false;
        }
        
        try {
            metric.setHttpResponseCode(responseCode);
            metric.setResponsePayloadSize(responseSize);
            metric.stop();
            
            Log.d(TAG, "Stopped HTTP metric, response code: " + responseCode + ", size: " + responseSize);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop HTTP metric", e);
            return false;
        }
    }
    
    /**
     * Add an attribute to an HTTP metric
     * @param metric HttpMetric object
     * @param attributeName Name of the attribute
     * @param value Value of the attribute
     * @return True if attribute was added successfully, false otherwise
     */
    public static boolean addHttpMetricAttribute(@Nullable HttpMetric metric, @NonNull String attributeName, @NonNull String value) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return false;
        }
        
        if (metric == null) {
            Log.e(TAG, "Cannot add attribute to null HTTP metric");
            return false;
        }
        
        try {
            metric.putAttribute(attributeName, value);
            
            Log.d(TAG, "Added attribute to HTTP metric: " + attributeName + " = " + value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add attribute to HTTP metric", e);
            return false;
        }
    }
    
    /**
     * Create and start a performance trace for a code block
     * Should be paired with stopPerformanceTrace() in a try/finally block
     * @param traceName Name of the trace
     * @return The trace object that was started, or null if it failed
     */
    public static Trace startPerformanceTrace(@NonNull String traceName) {
        if (!isInitialized || performance == null) {
            Log.e(TAG, "Performance monitoring not initialized. Call init() first.");
            return null;
        }
        
        try {
            Trace trace = performance.newTrace(traceName);
            trace.start();
            
            Log.d(TAG, "Started performance trace: " + traceName);
            return trace;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start performance trace: " + traceName, e);
            return null;
        }
    }
    
    /**
     * Stop a performance trace
     * @param trace The trace to stop
     * @param traceName Name of the trace (for logging)
     */
    public static void stopPerformanceTrace(@Nullable Trace trace, @NonNull String traceName) {
        if (trace == null) {
            Log.e(TAG, "Cannot stop null trace: " + traceName);
            return;
        }
        
        try {
            trace.stop();
            Log.d(TAG, "Stopped performance trace: " + traceName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop performance trace: " + traceName, e);
        }
    }
} 