package com.ds.eventwish.ui.contact;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentContactBinding;
import com.ds.eventwish.ui.base.BaseFragment;

public class ContactFragment extends BaseFragment {
    private static final String TAG = "ContactFragment";
    
    private ContactViewModel viewModel;
    private FragmentContactBinding binding;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorView;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentContactBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        webView = binding.webView;
        progressBar = binding.progressBar;
        errorView = binding.errorView;
        swipeRefresh = binding.swipeRefresh;
        
        // Setup WebView
        setupWebView();
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        
        // Observe data changes
        observeViewModel();
        
        // Setup SwipeRefreshLayout
        setupSwipeRefresh();
        
        // Load data
        viewModel.loadContactContent();
    }
    
    private void setupSwipeRefresh() {
        // Set refresh colors
        swipeRefresh.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.colorPrimaryDark
        );
        
        // Set refresh listener
        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG, "Refreshing contact content via pull-to-refresh");
            viewModel.refreshContactContent();
        });
        
        // Disable refresh when WebView is scrolled
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            swipeRefresh.setEnabled(scrollY == 0);
        });
    }
    
    private void setupWebView() {
        // Enable JavaScript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        // Set WebViewClient to handle links
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Handling URL: " + url);
                
                if (url.startsWith("mailto:")) {
                    // Handle email links
                    try {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling mailto link: " + e.getMessage());
                        return false;
                    }
                } else if (url.startsWith("tel:")) {
                    // Handle phone links
                    try {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling tel link: " + e.getMessage());
                        return false;
                    }
                } else if (url.startsWith("http:") || url.startsWith("https:")) {
                    // Handle web links - open in external browser
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling http link: " + e.getMessage());
                        return false;
                    }
                }
                return false;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page loaded successfully");
            }
        });
    }
    
    private void observeViewModel() {
        // Observe contact content
        viewModel.getContactContent().observe(getViewLifecycleOwner(), contact -> {
            if (contact != null && contact.getHtmlCode() != null) {
                Log.d(TAG, "Contact content received, loading into WebView");
                webView.loadDataWithBaseURL(null, contact.getHtmlCode(), "text/html", "UTF-8", null);
                webView.setVisibility(View.VISIBLE); // Always make WebView visible when content is available
                
                // Only hide error view if there's no error message
                if (errorView.getVisibility() == View.VISIBLE && 
                    !errorView.getText().toString().startsWith(getString(R.string.using_offline_content))) {
                    errorView.setVisibility(View.GONE);
                }
            }
        });
        
        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading && webView.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
            swipeRefresh.setRefreshing(isLoading);
        });
        
        // Observe error state
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error loading contact content: " + error);
                
                // If using offline content, show a small banner but keep the webview visible
                if (error.startsWith("Using offline content")) {
                    errorView.setText(getString(R.string.using_offline_content));
                    errorView.setVisibility(View.VISIBLE);
                    // WebView should already be visible from the content observer
                } else {
                    // For other errors, show the full error and hide webview
                    errorView.setText(error);
                    errorView.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            } else {
                errorView.setVisibility(View.GONE);
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up WebView
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        binding = null;
    }
} 