package com.ds.eventwish.ui.template;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.eventwish.data.model.Template;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lazy loader for WebView templates based on visibility
 * Only renders templates that are currently visible to optimize performance
 */
public class WebViewLazyLoader {
    private static final String TAG = "WebViewLazyLoader";
    private static final int LOAD_DELAY_MS = 150; // Delay for smoother scrolling
    private static final int PRELOAD_BUFFER = 1; // Number of items to preload ahead
    
    // Dependencies
    private final WeakReference<Context> contextRef;
    private final TemplateWebViewRenderer renderer;
    
    // Threading
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // State management
    private final Map<String, LoadState> loadStates = new HashMap<>();
    private final Set<String> visibleItems = new HashSet<>();
    private final Set<String> loadingItems = new HashSet<>();
    
    // Scroll handling
    private Handler scrollHandler = new Handler(Looper.getMainLooper());
    private Runnable scrollRunnable;
    private boolean isScrolling = false;
    
    /**
     * Load state for templates
     */
    private enum LoadState {
        NOT_LOADED,
        LOADING,
        LOADED,
        ERROR
    }
    
    /**
     * Callback interface for load events
     */
    public interface LoadCallback {
        void onLoadStart(String templateId);
        void onLoadComplete(String templateId);
        void onLoadError(String templateId, String error);
    }
    
    public WebViewLazyLoader(Context context) {
        this.contextRef = new WeakReference<>(context);
        this.renderer = new TemplateWebViewRenderer(context);
    }
    
    /**
     * Set up lazy loading for RecyclerView
     */
    public void setupLazyLoading(@NonNull RecyclerView recyclerView, LoadCallback callback) {
        if (recyclerView.getLayoutManager() == null) {
            Log.w(TAG, "RecyclerView LayoutManager is null, cannot setup lazy loading");
            return;
        }
        
        // Add scroll listener for visibility detection
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Load visible items after scrolling stops
                    scheduleVisibilityCheck(recyclerView, callback);
                }
            }
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Update visibility during scroll
                if (!isScrolling) {
                    scheduleVisibilityCheck(recyclerView, callback);
                }
            }
        });
        
        // Initial load
        scheduleVisibilityCheck(recyclerView, callback);
    }
    
    /**
     * Schedule visibility check with delay to avoid excessive calls
     */
    private void scheduleVisibilityCheck(RecyclerView recyclerView, LoadCallback callback) {
        if (scrollRunnable != null) {
            scrollHandler.removeCallbacks(scrollRunnable);
        }
        
        scrollRunnable = () -> checkVisibleItems(recyclerView, callback);
        scrollHandler.postDelayed(scrollRunnable, LOAD_DELAY_MS);
    }
    
    /**
     * Check which items are visible and load them
     */
    private void checkVisibleItems(RecyclerView recyclerView, LoadCallback callback) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) return;
        
        int firstVisible = -1;
        int lastVisible = -1;
        
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
            lastVisible = linearLayoutManager.findLastVisibleItemPosition();
        }
        
        if (firstVisible == -1 || lastVisible == -1) {
            Log.w(TAG, "Could not determine visible item positions");
            return;
        }
        
        // Add preload buffer
        int startPos = Math.max(0, firstVisible - PRELOAD_BUFFER);
        int endPos = lastVisible + PRELOAD_BUFFER;
        
        Log.d(TAG, "Checking visibility for positions " + startPos + " to " + endPos);
        
        // Process visible items
        backgroundExecutor.execute(() -> {
            Set<String> currentlyVisible = new HashSet<>();
            
            for (int position = startPos; position <= endPos; position++) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                
                if (viewHolder != null && viewHolder.itemView != null) {
                    WebView webView = findWebViewInItem(viewHolder.itemView);
                    Template template = getTemplateFromViewHolder(viewHolder);
                    
                    if (webView != null && template != null) {
                        String templateId = template.getId();
                        currentlyVisible.add(templateId);
                        
                        // Load if not already loaded or loading
                        LoadState currentState = loadStates.get(templateId);
                        if (currentState == null || currentState == LoadState.NOT_LOADED) {
                            loadTemplate(webView, template, callback);
                        }
                    }
                }
            }
            
            // Update visible items set
            synchronized (visibleItems) {
                visibleItems.clear();
                visibleItems.addAll(currentlyVisible);
            }
        });
    }
    
    /**
     * Find WebView in item view
     */
    private WebView findWebViewInItem(View itemView) {
        if (itemView instanceof WebView) {
            return (WebView) itemView;
        }
        
        // Look for WebView with template_image ID
        View webView = itemView.findViewById(com.ds.eventwish.R.id.template_image);
        if (webView instanceof WebView) {
            return (WebView) webView;
        }
        
        // Recursively search for WebView
        if (itemView instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) itemView;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebViewInItem(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get template from ViewHolder
     */
    private Template getTemplateFromViewHolder(RecyclerView.ViewHolder viewHolder) {
        try {
            // Try to get template from ViewHolder if it has a getTemplate method
            if (viewHolder instanceof TemplateViewHolderInterface) {
                return ((TemplateViewHolderInterface) viewHolder).getTemplate();
            }
            
            // Try reflection as fallback
            try {
                java.lang.reflect.Field templateField = viewHolder.getClass().getDeclaredField("template");
                templateField.setAccessible(true);
                Object template = templateField.get(viewHolder);
                if (template instanceof Template) {
                    return (Template) template;
                }
            } catch (Exception e) {
                // Reflection failed, try other approaches
            }
            
            // Try to get template from item view tag
            Object tag = viewHolder.itemView.getTag();
            if (tag instanceof Template) {
                return (Template) tag;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting template from ViewHolder", e);
        }
        
        return null;
    }
    
    /**
     * Interface for ViewHolders that can provide template
     */
    public interface TemplateViewHolderInterface {
        Template getTemplate();
    }
    
    /**
     * Load template in WebView
     */
    public void loadTemplate(WebView webView, Template template, LoadCallback callback) {
        if (webView == null || template == null) {
            Log.w(TAG, "WebView or Template is null, cannot load");
            return;
        }
        
        String templateId = template.getId();
        
        synchronized (loadingItems) {
            if (loadingItems.contains(templateId)) {
                Log.d(TAG, "Template already loading: " + templateId);
                return;
            }
            loadingItems.add(templateId);
        }
        
        loadStates.put(templateId, LoadState.LOADING);
        
        if (callback != null) {
            mainHandler.post(() -> callback.onLoadStart(templateId));
        }
        
        Log.d(TAG, "Loading template: " + templateId);
        
        // Set WebView visibility and prepare for loading
        mainHandler.post(() -> {
            webView.setVisibility(View.INVISIBLE);
            
            // Render template
            renderer.renderTemplate(webView, template, new TemplateWebViewRenderer.RenderCallback() {
                @Override
                public void onRenderComplete() {
                    mainHandler.post(() -> {
                        webView.setVisibility(View.VISIBLE);
                        loadStates.put(templateId, LoadState.LOADED);
                        
                        synchronized (loadingItems) {
                            loadingItems.remove(templateId);
                        }
                        
                        if (callback != null) {
                            callback.onLoadComplete(templateId);
                        }
                        
                        Log.d(TAG, "Template loaded successfully: " + templateId);
                    });
                }
                
                @Override
                public void onRenderError(String error) {
                    mainHandler.post(() -> {
                        loadStates.put(templateId, LoadState.ERROR);
                        
                        synchronized (loadingItems) {
                            loadingItems.remove(templateId);
                        }
                        
                        if (callback != null) {
                            callback.onLoadError(templateId, error);
                        }
                        
                        Log.e(TAG, "Template load error: " + templateId + " - " + error);
                    });
                }
                
                @Override
                public void onLoadingStateChanged(boolean isLoading) {
                    // Handle loading state changes if needed
                }
            });
        });
    }
    
    /**
     * Force load a specific template regardless of visibility
     */
    public void forceLoadTemplate(WebView webView, Template template, LoadCallback callback) {
        if (webView == null || template == null) return;
        
        String templateId = template.getId();
        loadStates.put(templateId, LoadState.NOT_LOADED);
        
        synchronized (loadingItems) {
            loadingItems.remove(templateId);
        }
        
        loadTemplate(webView, template, callback);
    }
    
    /**
     * Check if template is loaded
     */
    public boolean isTemplateLoaded(String templateId) {
        LoadState state = loadStates.get(templateId);
        return state == LoadState.LOADED;
    }
    
    /**
     * Check if template is loading
     */
    public boolean isTemplateLoading(String templateId) {
        LoadState state = loadStates.get(templateId);
        return state == LoadState.LOADING;
    }
    
    /**
     * Get load state for template
     */
    public LoadState getLoadState(String templateId) {
        return loadStates.getOrDefault(templateId, LoadState.NOT_LOADED);
    }
    
    /**
     * Clear WebView content and reset state
     */
    public void clearTemplate(WebView webView, String templateId) {
        if (webView != null) {
            mainHandler.post(() -> {
                webView.loadUrl("about:blank");
                webView.setVisibility(View.INVISIBLE);
            });
        }
        
        if (templateId != null) {
            loadStates.put(templateId, LoadState.NOT_LOADED);
            synchronized (loadingItems) {
                loadingItems.remove(templateId);
            }
            synchronized (visibleItems) {
                visibleItems.remove(templateId);
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up WebViewLazyLoader");
        
        if (scrollRunnable != null) {
            scrollHandler.removeCallbacks(scrollRunnable);
            scrollRunnable = null;
        }
        
        synchronized (loadingItems) {
            loadingItems.clear();
        }
        
        synchronized (visibleItems) {
            visibleItems.clear();
        }
        
        loadStates.clear();
        
        if (!backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        
        if (renderer != null) {
            renderer.cleanup();
        }
        
        contextRef.clear();
    }
    
    /**
     * Get debug information
     */
    public String getDebugInfo() {
        return "WebViewLazyLoader{" +
                "loadStates=" + loadStates.size() +
                ", visibleItems=" + visibleItems.size() +
                ", loadingItems=" + loadingItems.size() +
                ", isScrolling=" + isScrolling +
                '}';
    }
} 