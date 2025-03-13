package com.ds.eventwish.ui.wish;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.OnBackPressedCallback;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.databinding.FragmentSharedWishBinding;
import com.ds.eventwish.ui.history.HistoryViewModel;
import com.ds.eventwish.ui.history.SharedPrefsManager;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.utils.SocialShareUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;

import android.os.Handler;

public class SharedWishFragment extends Fragment {
    private SharedPrefsManager prefsManager;
    private FragmentSharedWishBinding binding;
    private SharedWishViewModel viewModel;
    private String shortCode;
    private String TAG = "SharedWishFragment";
    private WishResponse currentWish;
    private OnBackPressedCallback backPressCallback;
    private JsonObject analyticsData;

    // Constants for share platforms
    private static final String SHARE_VIA_WHATSAPP = "whatsapp";
    private static final String SHARE_VIA_FACEBOOK = "facebook";
    private static final String SHARE_VIA_TWITTER = "twitter";
    private static final String SHARE_VIA_INSTAGRAM = "instagram";
    private static final String SHARE_VIA_EMAIL = "email";
    private static final String SHARE_VIA_SMS = "sms";
    private static final String SHARE_VIA_OTHER = "other";
    private static final String SHARE_VIA_CLIPBOARD = "clipboard";

    // Base URL for the backend server
    private static final String SERVER_BASE_URL = "https://eventwish2.onrender.com";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new SharedPrefsManager(requireContext());
        viewModel = new ViewModelProvider(this).get(SharedWishViewModel.class);

        // Get the short code from arguments
        if (getArguments() != null) {
            shortCode = SharedWishFragmentArgs.fromBundle(getArguments()).getShortCode();
            Log.d(TAG, "Received shortCode: " + shortCode);
        }

        // Handle back press
        backPressCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Navigate to home instead of just popping back
                navigateToHome();
                Log.d(TAG, "Navigated to home on back press");
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSharedWishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize WebView
        setupWebView();



        // Set up click listeners
        setupClickListeners();

        // Set up observers
        setupObservers();

        // Load the wish
        if (shortCode != null && !shortCode.isEmpty()) {
            viewModel.loadSharedWish(shortCode);
        } else {
            showError("No short code provided");
        }
    }

    private void setupWebView() {
        WebView webView = binding.webView;
        WebSettings webSettings = webView.getSettings();

        // Enable JavaScript
        webSettings.setJavaScriptEnabled(true);

        // Enable DOM storage for JavaScript localStorage
        webSettings.setDomStorageEnabled(true);

        // Enable zooming and display controls
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Additional settings for better rendering
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);

        // Set default text encoding
        webSettings.setDefaultTextEncodingName("UTF-8");

        // Enable debugging if in debug build
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (0 != (getContext().getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)) {
                android.webkit.WebView.setWebContentsDebuggingEnabled(true);
                Log.d(TAG, "WebView debugging enabled");
            }
        }

        // Set a WebChromeClient to handle JavaScript alerts and console messages
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d(TAG, "WebView Console: " + consoleMessage.message() +
                      " -- From line " + consoleMessage.lineNumber() +
                      " of " + consoleMessage.sourceId());
                return true;
            }
        });

        Log.d(TAG, "WebView setup complete");
    }

    private void setupObservers() {
        viewModel.getSharedWish().observe(getViewLifecycleOwner(), wish -> {
            if (wish != null) {
                Log.d(TAG, "Received Wish JSON: " + new Gson().toJson(wish));
                currentWish = wish;
                loadWishContent(wish);

                // Save to history with preview URL
                try {
                    SharedWish historyWish = new SharedWish();
                    historyWish.setShortCode(wish.getShortCode());
                    historyWish.setRecipientName(wish.getRecipientName());
                    historyWish.setSenderName(wish.getSenderName());
                    historyWish.setTemplate(wish.getTemplate());

                    // Set the preview URL from template or from the wish
                    if (wish.getPreviewUrl() != null && !wish.getPreviewUrl().isEmpty()) {
                        historyWish.setPreviewUrl(wish.getPreviewUrl());
                        Log.d(TAG, "Setting preview URL from wish: " + wish.getPreviewUrl());
                    } else if (wish.getTemplate() != null && wish.getTemplate().getThumbnailUrl() != null) {
                        String previewUrl = wish.getTemplate().getThumbnailUrl();
                        if (!previewUrl.startsWith("http")) {
                            previewUrl = SERVER_BASE_URL + (previewUrl.startsWith("/") ? "" : "/") + previewUrl;
                        }
                        historyWish.setPreviewUrl(previewUrl);
                        Log.d(TAG, "Setting preview URL from template: " + previewUrl);
                    } else {
                        // Use default preview image
                        historyWish.setPreviewUrl(SERVER_BASE_URL + "/images/default-preview.png");
                        Log.d(TAG, "Setting default preview URL");
                    }

                    // Set title and description for social media sharing
                    historyWish.setTitle("EventWish Greeting");
                    historyWish.setDescription("A special wish from " + wish.getSenderName() + " to " + wish.getRecipientName());

                    // Set deep link
                    historyWish.setDeepLink("eventwish://wish/" + wish.getShortCode());

                    // Set content fields
                    historyWish.setCustomizedHtml(wish.getCustomizedHtml());
                    historyWish.setCssContent(wish.getCssContent());
                    historyWish.setJsContent(wish.getJsContent());

                    // Log content lengths for debugging
                    Log.d(TAG, "Saving to history - customizedHtml length: " +
                          (historyWish.getCustomizedHtml() != null ? historyWish.getCustomizedHtml().length() : 0) +
                          ", cssContent length: " +
                          (historyWish.getCssContent() != null ? historyWish.getCssContent().length() : 0) +
                          ", jsContent length: " +
                          (historyWish.getJsContent() != null ? historyWish.getJsContent().length() : 0));

                    // Use HistoryViewModel instead of SharedPrefsManager
                    ViewModelProvider provider = new ViewModelProvider(requireActivity());
                    HistoryViewModel historyViewModel = provider.get(HistoryViewModel.class);
                    historyViewModel.addToHistory(historyWish);

                    Log.d(TAG, "Saved wish to history: " + wish.getShortCode());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save to history", e);
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.d("SharedWishFragment", error);
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Observe analytics data
        viewModel.getAnalytics().observe(getViewLifecycleOwner(), analytics -> {
            if (analytics != null) {
                Log.d(TAG, "Received analytics: " + analytics);
                updateAnalyticsDisplay(analytics);
            }
        });
    }

    private void setupClickListeners() {
        binding.shareButton.setOnClickListener(v -> shareWish());

        // Add analytics button click listener
        binding.analyticsButton.setOnClickListener(v -> showAnalyticsBottomSheet());
    }

    private void loadWishContent(WishResponse wish) {
        Log.d(TAG, "Loading wish content, wish: " + (wish != null ? "not null" : "null"));

        if (wish == null) {
            Log.e(TAG, "Wish is null");
            showError("Unable to load wish: No data available");
            return;
        }

        if (wish.getCustomizedHtml() == null || wish.getCustomizedHtml().trim().isEmpty()) {
            Log.e(TAG, "HTML content is null or empty for wish: " + wish.getShortCode());
            showError("Unable to load wish: No content available");
            return;
        }

        // Get template for fallback CSS/JS if needed
        Template template = wish.getTemplate();
        Log.d(TAG, "Template: " + (template != null ? "found" : "null"));

        // Construct the full HTML content
        String htmlContent = wish.getCustomizedHtml();
        String cssContent = wish.getCssContent();
        String jsContent = wish.getJsContent();

        // Use template CSS/JS as fallback if needed
        if ((cssContent == null || cssContent.isEmpty()) && template != null) {
            cssContent = template.getCssContent();
            Log.d(TAG, "Using template CSS as fallback");
        }

        if ((jsContent == null || jsContent.isEmpty()) && template != null) {
            jsContent = template.getJsContent();
            Log.d(TAG, "Using template JS as fallback");
        }

        // Ensure we have non-null values for string formatting
        if (cssContent == null) cssContent = "";
        if (jsContent == null) jsContent = "";

        // Log content lengths for debugging
        Log.d(TAG, "HTML Content length: " + htmlContent.length());
        Log.d(TAG, "CSS Content length: " + cssContent.length());
        Log.d(TAG, "JS Content length: " + jsContent.length());

        // Construct the full HTML document
        String fullHtml = String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<meta charset='UTF-8'>" +
            "<style>%s</style>" +
            "</head>" +
            "<body>" +
            "%s" +
            "<script>%s</script>" +
            "</body>" +
            "</html>",
            cssContent, htmlContent, jsContent
        );

        // Log a sample of the full HTML for debugging
        Log.d(TAG, "Full HTML sample: " + (fullHtml.length() > 100 ? fullHtml.substring(0, 100) + "..." : fullHtml));

        // Set up WebView client to capture errors
        binding.webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + errorCode + " - " + description);
                showError("Error loading content: " + description);
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                Log.d(TAG, "WebView page finished loading");
                // Hide loading indicator if needed
                showLoading(false);
            }
        });

        // Show loading indicator
        showLoading(true);

        // Load the HTML content into the WebView with a base URL
        try {
            binding.webView.loadDataWithBaseURL(
                "https://eventwish2.onrender.com/",
                fullHtml,
                "text/html",
                "UTF-8",
                null
            );
        } catch (Exception e) {
            Log.e(TAG, "Error loading HTML into WebView", e);
            showError("Error displaying content: " + e.getMessage());
        }
    }

    private void shareWish() {
        if (currentWish == null) {
            Toast.makeText(requireContext(), "Cannot share wish: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_share, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Get share URL
        String shareUrl = getString(R.string.share_url_format, currentWish.getShortCode());

        // Set up click listeners for share options
        bottomSheetView.findViewById(R.id.whatsappShare).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text_whatsapp, shareUrl);
            handleShareVia(SHARE_VIA_WHATSAPP);
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.facebookShare).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text_facebook, shareUrl);
            handleShareVia(SHARE_VIA_FACEBOOK);
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.twitterShare).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text_twitter, shareUrl);
            handleShareVia(SHARE_VIA_TWITTER);
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.emailShare).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text_email, shareUrl);
            handleShareVia(SHARE_VIA_EMAIL);
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.smsShare).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text_sms, shareUrl);
            handleShareVia(SHARE_VIA_SMS);
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.moreOptions).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text, shareUrl);
            handleShareVia(SHARE_VIA_OTHER);
            bottomSheetDialog.dismiss();
        });

        bottomSheetView.findViewById(R.id.copyLink).setOnClickListener(v -> {
            copyLinkToClipboard(shareUrl);
            bottomSheetDialog.dismiss();
        });

        // Add a home button to navigate directly to home
//        bottomSheetView.findViewById(R.id.homeButton).setOnClickListener(v -> {
//            // Navigate to home
//            navigateToHome();
//            bottomSheetDialog.dismiss();
//            Log.d(TAG, "Navigated to home from share bottom sheet");
//        });
//
//        bottomSheetDialog.show();
    }

    /**
     * Handle sharing via a specific platform
     * @param platform The platform to share on
     */
    private void handleShareVia(String platform) {
        if (currentWish == null) {
            Toast.makeText(requireContext(), "Cannot share wish: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the wish's sharedVia field
        currentWish.setSharedVia(platform);

        // Track the share
        trackShare(platform);

        // Create a SharedWish object from the WishResponse
        SharedWish sharedWish = new SharedWish();

        // Set basic information
        sharedWish.setShortCode(currentWish.getShortCode());
        sharedWish.setSenderName(currentWish.getSenderName());
        sharedWish.setRecipientName(currentWish.getRecipientName());
        sharedWish.setSharedVia(platform);

        // Set content fields
        sharedWish.setCustomizedHtml(currentWish.getCustomizedHtml());
        sharedWish.setCssContent(currentWish.getCssContent());
        sharedWish.setJsContent(currentWish.getJsContent());

        // Set preview URL from template or from the wish
        if (currentWish.getPreviewUrl() != null && !currentWish.getPreviewUrl().isEmpty()) {
            sharedWish.setPreviewUrl(currentWish.getPreviewUrl());
            Log.d(TAG, "Setting preview URL from wish: " + currentWish.getPreviewUrl());
        } else if (currentWish.getTemplate() != null && currentWish.getTemplate().getThumbnailUrl() != null) {
            String previewUrl = currentWish.getTemplate().getThumbnailUrl();
            if (!previewUrl.startsWith("http")) {
                previewUrl = SERVER_BASE_URL + (previewUrl.startsWith("/") ? "" : "/") + previewUrl;
            }
            sharedWish.setPreviewUrl(previewUrl);
            Log.d(TAG, "Setting preview URL from template: " + previewUrl);
        } else {
            // Use default preview image
            sharedWish.setPreviewUrl(SERVER_BASE_URL + "/images/default-preview.png");
            Log.d(TAG, "Setting default preview URL");
        }

        // Set title and description for social media sharing
        String title = "EventWish Greeting";
        String description = "A special wish from " + currentWish.getSenderName() + " to " + currentWish.getRecipientName();
        sharedWish.setTitle(title);
        sharedWish.setDescription(description);

        // Set deep link
        String deepLink = "eventwish://wish/" + currentWish.getShortCode();
        sharedWish.setDeepLink(deepLink);

        Log.d(TAG, "Sharing wish with title: " + title +
              ", description: " + description +
              ", deepLink: " + deepLink +
              ", customizedHtml length: " + (sharedWish.getCustomizedHtml() != null ? sharedWish.getCustomizedHtml().length() : 0) +
              ", cssContent length: " + (sharedWish.getCssContent() != null ? sharedWish.getCssContent().length() : 0) +
              ", jsContent length: " + (sharedWish.getJsContent() != null ? sharedWish.getJsContent().length() : 0));

        // Save the wish to history
        try {
            ViewModelProvider provider = new ViewModelProvider(requireActivity());
            HistoryViewModel historyViewModel = provider.get(HistoryViewModel.class);
            historyViewModel.addToHistory(sharedWish);
            Log.d(TAG, "Saved wish to history from handleShareVia: " + sharedWish.getShortCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save to history from handleShareVia", e);
        }

        // Create the share text
        String shareText = getString(R.string.share_wish_text, getString(R.string.share_url_format, currentWish.getShortCode()));

        // Create a simple share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        // Start the share activity
        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));

            // Navigate to home after sharing
            new Handler().postDelayed(() -> {
                try {
                    if (isAdded() && getView() != null) {
                        // Navigate to home instead of just popping back
                        navigateToHome();
                        Log.d(TAG, "Navigated to home after sharing");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to home after sharing", e);
                }
            }, 500); // Small delay to ensure the share dialog appears first

        } catch (Exception e) {
            Log.e(TAG, "Error launching share intent", e);
            Toast.makeText(requireContext(), "Error launching share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigate to the home fragment and select the home tab in bottom navigation
     */
    private void navigateToHome() {
        try {
            // Get NavController
            NavController navController = Navigation.findNavController(requireView());

            // Navigate to home using the action defined in nav_graph.xml
            navController.navigate(R.id.action_shared_wish_to_home);

            // Set the bottom navigation to home
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.navigation_home);
                    Log.d(TAG, "Set bottom navigation to home");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to home", e);
            // Fallback to popping back stack if navigation fails
            try {
                NavController navController = Navigation.findNavController(requireView());
                navController.popBackStack();
                Log.d(TAG, "Fallback: Popped back stack");
            } catch (Exception ex) {
                Log.e(TAG, "Error popping back stack", ex);
            }
        }
    }

    /**
     * Show or hide loading indicator
     * @param show True to show, false to hide
     */
    private void showLoading(boolean show) {
        if (binding != null) {
            binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Copy text to clipboard
     * @param text The text to copy
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Wish Link", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), getString(R.string.link_copied), Toast.LENGTH_SHORT).show();
    }

    /**
     * Copy link to clipboard
     * @param shareUrl The URL to copy
     */
    private void copyLinkToClipboard(String shareUrl) {
        copyToClipboard(shareUrl);

        // Track the share
        trackShare(SHARE_VIA_CLIPBOARD);
    }

    /**
     * Track a share event
     * @param platform The platform used for sharing
     */
    private void trackShare(String platform) {
        if (shortCode == null || shortCode.isEmpty()) return;

        try {
            // Create a JSON object with the platform
            JsonObject platformJson = new JsonObject();
            platformJson.addProperty("platform", platform);

            // Get the API service
            ApiService apiService = ApiClient.getClient();

            // Make the API call
            apiService.updateSharedWishPlatform(shortCode, platformJson).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Successfully tracked share via " + platform);
                    } else {
                        Log.e(TAG, "Failed to track share: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e(TAG, "Error tracking share: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error tracking share: " + e.getMessage());
        }
    }

    /**
     * Show analytics bottom sheet
     */
    private void showAnalyticsBottomSheet() {
        if (analyticsData == null) {
            Toast.makeText(requireContext(), "No analytics data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show the bottom sheet
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_analytics, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);

        // Set up the analytics data
        TextView viewsText = bottomSheetView.findViewById(R.id.viewsText);
        TextView uniqueViewsText = bottomSheetView.findViewById(R.id.uniqueViewsText);
        TextView shareCountText = bottomSheetView.findViewById(R.id.shareCountText);
        LinearLayout platformBreakdownLayout = bottomSheetView.findViewById(R.id.platformBreakdownLayout);

        // Set the values
        int views = analyticsData.has("views") ? analyticsData.get("views").getAsInt() : 0;
        int uniqueViews = analyticsData.has("uniqueViews") ? analyticsData.get("uniqueViews").getAsInt() : 0;
        int shareCount = analyticsData.has("shareCount") ? analyticsData.get("shareCount").getAsInt() : 0;

        viewsText.setText(String.valueOf(views));
        uniqueViewsText.setText(String.valueOf(uniqueViews));
        shareCountText.setText(String.valueOf(shareCount));

        // Add platform breakdown
        if (analyticsData.has("platformBreakdown") && analyticsData.get("platformBreakdown").isJsonObject()) {
            JsonObject platformBreakdown = analyticsData.getAsJsonObject("platformBreakdown");

            // Clear existing views
            platformBreakdownLayout.removeAllViews();

            // Add a view for each platform
            for (Map.Entry<String, com.google.gson.JsonElement> entry : platformBreakdown.entrySet()) {
                String platform = entry.getKey();
                int count = entry.getValue().getAsInt();

                // Create a new row
                View rowView = getLayoutInflater().inflate(R.layout.item_platform_breakdown, null);
                TextView platformText = rowView.findViewById(R.id.platformText);
                TextView countText = rowView.findViewById(R.id.countText);

                // Set the values
                platformText.setText(getPlatformDisplayName(platform));
                countText.setText(String.valueOf(count));

                // Add the row to the layout
                platformBreakdownLayout.addView(rowView);
            }
        }

        bottomSheetDialog.show();
    }

    /**
     * Get a display name for a platform
     * @param platform The platform code
     * @return The display name
     */
    private String getPlatformDisplayName(String platform) {
        switch (platform) {
            case SHARE_VIA_WHATSAPP:
                return "WhatsApp";
            case SHARE_VIA_FACEBOOK:
                return "Facebook";
            case SHARE_VIA_TWITTER:
                return "Twitter";
            case SHARE_VIA_INSTAGRAM:
                return "Instagram";
            case SHARE_VIA_EMAIL:
                return "Email";
            case SHARE_VIA_SMS:
                return "SMS";
            case SHARE_VIA_CLIPBOARD:
                return "Copied Link";
            case SHARE_VIA_OTHER:
                return "Other";
            case "LINK":
                return "Direct Link";
            default:
                return platform;
        }
    }

    /**
     * Update analytics display
     * @param analytics The analytics data
     */
    private void updateAnalyticsDisplay(JsonObject analytics) {
        this.analyticsData = analytics;

        // Update the analytics button visibility
        if (binding != null && binding.analyticsButton != null) {
            binding.analyticsButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backPressCallback.remove();
    }

    /**
     * Show an error message to the user
     * @param message The error message
     */
    private void showError(String message) {
        if (binding != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            // You could also show an error view in the UI
        }
    }

    /**
     * Set up the toolbar with navigation icon and title
     */
//    private void setupToolbar() {
//        if (binding.toolbar != null) {
//            // Set navigation icon (back button)
//            binding.toolbar.setNavigationIcon(R.drawable.ic_back);
//
//            // Set title
//            binding.toolbar.setTitle("Shared Wish");
//
//            // Set navigation click listener
//            binding.toolbar.setNavigationOnClickListener(v -> {
//                // Navigate to home
//                navigateToHome();
//                Log.d(TAG, "Navigated to home from toolbar back button");
//            });
//        }
//    }
}
