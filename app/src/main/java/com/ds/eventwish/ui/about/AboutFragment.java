package com.ds.eventwish.ui.about;

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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.About;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AboutFragment extends Fragment {
    private static final String TAG = "AboutFragment";
    
    private AboutViewModel viewModel;
    private WebView webView;
    private ProgressBar progressBar;
    private TextView errorText;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton backButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        webView = view.findViewById(R.id.webView);
        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        backButton = view.findViewById(R.id.backButton);

        //setup backpress
        backButtonFun();

        // Setup WebView
        setupWebView();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AboutViewModel.class);

        // Observe data
        observeViewModel();

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Load data
        viewModel.loadAboutContent();
    }

    private void backButtonFun() {
        backButton.setOnClickListener(v -> {

            Navigation.findNavController(v).navigate(R.id.navigation_more);

        });
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
            Log.d(TAG, "Refreshing about content via pull-to-refresh");
            viewModel.refreshAboutContent();
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
        viewModel.getAboutContent().observe(getViewLifecycleOwner(), about -> {
            if (about != null && about.getHtmlCode() != null) {
                Log.d(TAG, "About content received, loading into WebView");
                webView.loadDataWithBaseURL(null, about.getHtmlCode(), "text/html", "UTF-8", null);
                webView.setVisibility(View.VISIBLE);
                
                // Only hide error text if there's no error message
                if (errorText.getVisibility() == View.VISIBLE && 
                    !errorText.getText().toString().startsWith(getString(R.string.using_offline_content))) {
                    errorText.setVisibility(View.GONE);
                }
            }
        });
        
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading && webView.getVisibility() != View.VISIBLE ? View.VISIBLE : View.GONE);
            swipeRefresh.setRefreshing(isLoading);
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error loading about content: " + error);
                
                // If using offline content, show a small banner but keep the webview visible
                if (error.startsWith("Using offline content")) {
                    errorText.setText(getString(R.string.using_offline_content));
                    errorText.setVisibility(View.VISIBLE);
                    // WebView should already be visible from the content observer
                } else {
                    // For other errors, show the full error and hide webview
                    errorText.setText(error);
                    errorText.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            } else {
                errorText.setVisibility(View.GONE);
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
    }
}
