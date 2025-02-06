package com.ds.eventwish.resourse;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.databinding.FragmentResourceBinding;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

public class ResourceFragment extends Fragment {
    private static final String TAG = "ResourceFragment";
    private FragmentResourceBinding binding;
    private ResourceViewModel viewModel;
    private String shortCode;
    private BottomNavigationView bottomNav;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ResourceFragment created");
        viewModel = new ViewModelProvider(this).get(ResourceViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view");
        binding = FragmentResourceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: View created");

        // Get bottom navigation from activity
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setVisibility(View.VISIBLE);
        }

        // Get shortCode from deep link
        if (getArguments() != null) {
            shortCode = getArguments().getString("shortCode");
            Log.d(TAG, "onViewCreated: Received shortCode=" + shortCode);
            if (shortCode != null) {
                Log.d(TAG, "onViewCreated: Loading wish with shortCode=" + shortCode);
                viewModel.loadWish(shortCode);
            } else {
                Log.e(TAG, "onViewCreated: shortCode is null in arguments");
                showError("Invalid wish code");
            }
        } else {
            Log.e(TAG, "onViewCreated: No arguments received");
            showError("No wish code provided");
        }

        setupWebView();
        setupObservers();
        setupRetryButton();
    }

    private void setupWebView() {
        Log.d(TAG, "setupWebView: Configuring WebView");
        if (binding != null && binding.webView != null) {
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setDomStorageEnabled(true);
            binding.webView.setWebViewClient(new SafeWebViewClient());
            Log.d(TAG, "setupWebView: WebView configured successfully");
        } else {
            Log.e(TAG, "setupWebView: binding or webView is null");
        }
    }

    private void setupRetryButton() {

        if (binding != null && binding.retryLayout != null) {
            binding.retryLayout.retryButton.setOnClickListener(v -> {
                if (shortCode != null) {
                    binding.retryLayout.getRoot().setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.VISIBLE);
                    viewModel.loadWish(shortCode);
                }
            });
        }
    }

    private void setupObservers() {
        Log.d(TAG, "setupObservers: Setting up observers");
        viewModel.getWish().observe(getViewLifecycleOwner(), wishResponse -> {
            Log.d(TAG, "Wish response received: " + (wishResponse != null ? "not null" : "null"));
            if (wishResponse != null) {
                binding.progressBar.setVisibility(View.GONE);
                binding.retryLayout.getRoot().setVisibility(View.GONE);
                if (wishResponse.getTemplate() != null) {
                    Log.d(TAG, "Setting up WebView content for wish");
                    setupWebViewContent(wishResponse);
                } else {
                    Log.e(TAG, "Template is null in wish response");
                    showError("Invalid wish template");
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                binding.progressBar.setVisibility(View.GONE);
                binding.retryLayout.getRoot().setVisibility(View.VISIBLE);
                binding.retryLayout.errorText.setText(error);
                showError(error);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && isAdded()) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    binding.retryLayout.getRoot().setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupWebViewContent(WishResponse wishResponse) {
        if (binding != null && binding.webView != null && wishResponse != null) {
            binding.contentLayout.setVisibility(View.VISIBLE);
            String html = wishResponse.getCustomizedHtml();
            String css = wishResponse.getTemplate().getCssContent();
            String js = wishResponse.getTemplate().getJsContent();

            String fullHtml = String.format(
                "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\">" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=2.0, minimum-scale=0.5\">" +
                        "<style>%s</style>" +
                        "</head>" +
                        "<body>%s<script>%s</script></body>" +
                "</html>",
                css, html, js
            );
            binding.webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
        }
    }

    private void showError(String message) {
        if (isAdded() && binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && binding.webView != null) {
            binding.webView.stopLoading();
            binding.webView.clearCache(true);
            binding.webView.clearHistory();
            binding.webView.destroy();
        }
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.getWish().removeObservers(this);
        viewModel.getError().removeObservers(this);
        viewModel.isLoading().removeObservers(this);
    }

    private static class SafeWebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return true; // Prevent navigation
        }
    }
}