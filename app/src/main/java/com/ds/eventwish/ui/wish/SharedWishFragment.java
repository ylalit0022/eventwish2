package com.ds.eventwish.ui.wish;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.navigation.NavDirections;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.databinding.FragmentSharedWishBinding;
import com.ds.eventwish.ui.history.HistoryViewModel;
import com.ds.eventwish.ui.history.SharedPrefsManager;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.utils.SocialShareUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.ads.RewardedAdManager;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import android.os.CountDownTimer;
import com.google.android.material.snackbar.Snackbar;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;

public class SharedWishFragment extends Fragment {
    private SharedPrefsManager prefsManager;
    private FragmentSharedWishBinding binding;
    private SharedWishViewModel viewModel;
    private String shortCode;
    private String TAG = "SharedWishFragment";
    private WishResponse currentWish;
    private OnBackPressedCallback backPressCallback;
    private JsonObject analyticsData;
    
    // Rewarded ad manager
    private RewardedAdManager rewardedAdManager;
    private boolean isRewardedAdLoading = false;
    private boolean rewardedAdWatched = false;
    private CountDownTimer cooldownTimer;
    
    // Analytics tracking
    private static final long ANALYTICS_HEARTBEAT_INTERVAL = 30000; // 30 seconds
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable analyticsHeartbeatRunnable;
    private long viewStartTime;
    private boolean isTracking = false;

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

    private static final int MAX_AD_FAILURE_COUNT = 3; // After 3 failures, enable sharing without ads
    private int adFailureCount = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new SharedPrefsManager(requireContext());
        viewModel = new ViewModelProvider(this).get(SharedWishViewModel.class);

        // Reset the rewarded ad watched flag when fragment is created
        rewardedAdWatched = false;
        Log.d(TAG, "📱🚨 onCreate: Reset rewardedAdWatched flag to false");

        // Initialize rewarded ad manager
        rewardedAdManager = new RewardedAdManager(requireContext());
        
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

        // Track screen view for analytics
        AnalyticsUtils.trackScreenView("SharedWishFragment", SharedWishFragment.class.getName());

        // Initialize WebView
        setupWebView();

        // Set up click listeners
        setupClickListeners();

        // Set up observers
        setupObservers();
        
        // Restore rewarded ad state if app was closed and reopened
        if (rewardedAdManager != null && !rewardedAdWatched) {
            // Make sure watch ad button is visible
            if (binding != null && binding.watchAdButton != null) {
                binding.watchAdButton.setVisibility(View.VISIBLE);
                Log.d(TAG, "📱🔄 Ensuring watch ad button is visible on resume");
            }
            
            // Check if ad is loaded
            if (rewardedAdManager.isAdLoaded()) {
                Log.d(TAG, "📱✅ Ad already loaded on view created, updating button state");
                updateRewardedAdButtonState();
            } else {
                // Try to load ad
                Log.d(TAG, "📱🔄 No ad loaded on view created, attempting to load");
                loadRewardedAd();
            }
        }
        
        // UNCOMMENT THE AD CODE - Important for fixing visibility issues
        
        // Check if we should skip ads - can be controlled by server config
        checkShouldSkipAds();
        
        // Preload rewarded ad if we're not skipping ads
        if (!rewardedAdWatched) {
            loadRewardedAd();
        }
        
        // Show the watch ad button if needed - FORCE THIS
        if (binding != null && binding.watchAdButton != null && !rewardedAdWatched) {
            binding.watchAdButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "📱🚨 FORCING watch ad button visibility to VISIBLE");
        }
        
        // Skip ads completely for now - COMMENT THIS OUT to enable ads
        // enableShareWithoutAd();

        // Load the wish
        if (shortCode != null && !shortCode.isEmpty()) {
            viewModel.loadSharedWish(shortCode);
        } else {
            showError("No short code provided");
        }
        
        // Run a delayed check to verify button visibility after layout
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding != null && binding.watchAdButton != null && !rewardedAdWatched) {
                int visibility = binding.watchAdButton.getVisibility();
                Log.d(TAG, "📱🚨 DELAYED CHECK: Watch ad button visibility = " + 
                    (visibility == View.VISIBLE ? "VISIBLE" : 
                    (visibility == View.GONE ? "GONE" : "INVISIBLE")));
                
                // Force visibility again if not visible
                if (visibility != View.VISIBLE) {
                    binding.watchAdButton.setVisibility(View.VISIBLE);
                    Log.d(TAG, "📱🚨 DELAYED VISIBILITY FORCE: Setting to VISIBLE");
                }
            }
        }, 500); // Check after 500ms
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
                    
                    // Set templateId if available
                    if (wish.getTemplateId() != null) {
                        historyWish.setTemplateId(wish.getTemplateId());
                        Log.d(TAG, "Setting templateId: " + wish.getTemplateId());
                    } else if (wish.getTemplate() != null && wish.getTemplate().getId() != null) {
                        historyWish.setTemplateId(wish.getTemplate().getId());
                        Log.d(TAG, "Setting templateId from template: " + wish.getTemplate().getId());
                    }

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
                    Log.d(TAG, "Saving to history - " +
                          "previewUrl: " + historyWish.getPreviewUrl() +
                          ", templateId: " + historyWish.getTemplateId() +
                          ", customizedHtml length: " +
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
        // Configure share button to be initially disabled
        binding.shareButton.setEnabled(false);
        // Set initial gray background color to indicate disabled state
        binding.shareButton.setBackgroundTintList(
            ColorStateList.valueOf(getResources().getColor(R.color.share_button_disabled)));
        binding.shareLockIcon.setVisibility(View.VISIBLE);
        Log.d(TAG, "📱 Initial setup: Share button disabled and locked with custom color");
        
        // Ensure watch ad button is visible initially if not watched yet
        if (!rewardedAdWatched && binding.watchAdButton != null) {
            binding.watchAdButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "📱 Initial setup: Watch ad button made visible");
        }
        
        // Set up click listeners
        binding.shareButton.setOnClickListener(v -> shareWish());
        
        // Add watch rewarded ad button click listener
        binding.watchAdButton.setOnClickListener(v -> {
            Log.d(TAG, "📱 Watch Ad button clicked");
            showRewardedAd();
        });
        Log.d(TAG, "📱 Watch Ad button click listener set up");

        // Add analytics button click listener
        binding.analyticsButton.setOnClickListener(v -> showAnalyticsBottomSheet());
        
        // Add reuse template button click listener
        binding.reuseTemplateButton.setOnClickListener(v -> reuseTemplate());
        
        // Log initial state of buttons
        Log.d(TAG, "📱 Initial button states - Share: disabled, Analytics: " + 
            (binding.analyticsButton.getVisibility() == View.VISIBLE ? "visible" : "hidden") + 
            ", Watch Ad: " + (binding.watchAdButton.isEnabled() ? "enabled" : "disabled") + 
            ", Watch Ad visibility: " + (binding.watchAdButton.getVisibility() == View.VISIBLE ? "visible" : "hidden"));
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
              
        // Load the wish content into the WebView
        loadWebViewContent(htmlContent, cssContent, jsContent);
        
        // Start analytics tracking once content is loaded
        startAnalyticsTracking();
    }
    
    /**
     * Load content into the WebView
     * @param htmlContent The HTML content to load
     * @param cssContent The CSS content to include
     * @param jsContent The JavaScript content to include
     */
    private void loadWebViewContent(String htmlContent, String cssContent, String jsContent) {
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

    /**
     * Load a rewarded ad
     */
    private void loadRewardedAd() {
        if (rewardedAdManager == null || isRewardedAdLoading) {
            Log.e(TAG, "📱🔄 Cannot load rewarded ad: " + 
                  (rewardedAdManager == null ? "manager is null" : "loading already in progress"));
            return;
        }
        
        isRewardedAdLoading = true;
        Log.d(TAG, "📱🔄 Started loading rewarded ad");
        logAdSystemState();
        
        // Ensure the watch button is visible while loading
        if (binding != null && binding.watchAdButton != null && !rewardedAdWatched) {
            binding.watchAdButton.setVisibility(View.VISIBLE);
            binding.watchAdButtonText.setText(R.string.loading_ad);
            Log.d(TAG, "📱🔄 Setting watch ad button visible and showing loading text");
        }
        
        rewardedAdManager.loadAd(new RewardedAdManager.RewardedAdCallback() {
            @Override
            public void onAdLoaded() {
                isRewardedAdLoading = false;
                Log.d(TAG, "📱✅ Rewarded ad loaded successfully");
                
                // Enable watch ad button
                if (binding != null && !rewardedAdWatched) {
                    binding.watchAdButton.setVisibility(View.VISIBLE);
                    binding.watchAdButton.setEnabled(true);
                    binding.watchAdButtonText.setText(R.string.watch_ad_to_share);
                    Log.d(TAG, "📱✅ Watch ad button enabled and made visible");
                }
                
                logAdSystemState();
            }

            @Override
            public void onAdFailedToLoad(String error) {
                isRewardedAdLoading = false;
                Log.e(TAG, "📱❌ Rewarded ad failed to load: " + error);
                Log.e(TAG, "📱❌ DETAILED ERROR: " + error);
                
                // Simply enable sharing without requiring ads
                Log.d(TAG, "📱🔄 Ad failed to load during show attempt - enabling share button directly");
                enableShareWithoutAd();
                
                logAdSystemState();
            }

            @Override
            public void onAdOpened() {
                Log.d(TAG, "📱👁️ Rewarded ad opened and visible to user");
                logAdSystemState();
            }

            @Override
            public void onAdClosed() {
                Log.d(TAG, "📱🚪 Rewarded ad closed by user");
                
                // Preload next ad
                Log.d(TAG, "📱🔄 Preloading next rewarded ad after closure");
                loadRewardedAd();
                
                // Start cooldown timer if applicable
                if (rewardedAdManager.isInCooldownPeriod() && binding != null) {
                    long remainingMs = rewardedAdManager.getRemainingCooldownMs();
                    Log.d(TAG, "📱⏲️ Starting cooldown timer: " + (remainingMs / 1000) + " seconds");
                    startCooldownTimer(rewardedAdManager.getRemainingCooldownMs());
                }
                
                logAdSystemState();
            }

            @Override
            public void onUserEarnedReward(String type, int amount) {
                Log.d(TAG, "📱🎁 User earned reward: " + amount + " " + type);
                Log.d(TAG, "📱🎁 User completed watching the full ad");
                rewardedAdWatched = true;
                
                // Update UI to show share options
                if (binding != null) {
                    binding.watchAdButton.setEnabled(false);
                    binding.watchAdButtonText.setText(R.string.ad_watched);
                    
                    // Unlock share button (now a floating action button)
                    binding.shareButton.setEnabled(true);
                    // Change background color to primary color to indicate it's enabled
                    binding.shareButton.setBackgroundTintList(
                        ColorStateList.valueOf(getResources().getColor(R.color.share_button_enabled)));
                    binding.shareLockIcon.setVisibility(View.GONE);
                    Log.d(TAG, "📱🎁 Share button unlocked after reward earned");
                }
                
                // Show success message
                Snackbar.make(binding.getRoot(), 
                    "Thanks for watching! You can now share.", 
                    Snackbar.LENGTH_SHORT).show();
                
                logAdSystemState();
            }

            @Override
            public void onAdShowFailed(String error) {
                Log.e(TAG, "📱❌ Rewarded ad failed to show: " + error);
                Log.e(TAG, "📱❌ Show failure details: " + error);
                
                // Enable button for retry
                if (binding != null) {
                    binding.watchAdButton.setEnabled(true);
                    binding.watchAdButtonText.setText(R.string.retry_watch_ad);
                    Log.d(TAG, "📱❌ Watch ad button enabled for retry after show failure");
                }
                
                // Show error message
                Snackbar.make(binding.getRoot(), R.string.ad_failed_to_show, Snackbar.LENGTH_SHORT).show();
                
                logAdSystemState();
            }
        });
    }
    
    /**
     * Show the rewarded ad
     */
    private void showRewardedAd() {
        if (rewardedAdManager == null) {
            Log.e(TAG, "Rewarded ad manager is null");
            return;
        }
        
        Log.d(TAG, "📱 User clicked Watch Ad button");
        logAdSystemState();
        
        // Check if in cooldown period
        if (rewardedAdManager.isInCooldownPeriod()) {
            long remainingMs = rewardedAdManager.getRemainingCooldownMs();
            Log.d(TAG, "📱 Ad in cooldown period. Remaining time: " + (remainingMs / 1000) + " seconds");
            startCooldownTimer(remainingMs);
            return;
        }
        
        // Check if ad is loaded
        if (!rewardedAdManager.isAdLoaded()) {
            Log.d(TAG, "📱 Rewarded ad not loaded yet, attempting to load now");
            binding.watchAdButton.setEnabled(false);
            binding.watchAdButtonText.setText(R.string.loading_ad);
            
            // Try to load the ad
            loadRewardedAd();
            return;
        }
        
        Log.d(TAG, "📱 Ad is loaded and ready to show");
        
        // Show the ad
        OnUserEarnedRewardListener rewardListener = reward -> {
            // Mark as watched and unlock sharing
            Log.d(TAG, "📱 User earned reward via listener: " + reward.getAmount() + " " + reward.getType());
            Log.d(TAG, "📱 Ad completed successfully, unlocking share button");
            rewardedAdWatched = true;
            
            // Update UI to unlock share button
            if (binding != null) {
                binding.watchAdButton.setEnabled(false);
                binding.watchAdButtonText.setText(R.string.ad_watched);
                
                // Unlock share button
                binding.shareButton.setEnabled(true);
                binding.shareButton.setAlpha(1.0f);
                binding.shareLockIcon.setVisibility(View.GONE);
                
                // Show success message
                Snackbar.make(binding.getRoot(), 
                    "Thanks for watching! You can now share.", 
                    Snackbar.LENGTH_SHORT).show();
            }
            
            // Log updated state
            logAdSystemState();
        };
        
        // Disable button temporarily
        binding.watchAdButton.setEnabled(false);
        Log.d(TAG, "📱 Disabling Watch Ad button while showing ad");
        
        boolean adShown = rewardedAdManager.showAd(requireActivity(), rewardListener, 
            new RewardedAdManager.RewardedAdCallback() {
                @Override
                public void onAdLoaded() {
                    Log.d(TAG, "📱 Ad loaded callback triggered");
                    logAdSystemState();
                }

                @Override
                public void onAdFailedToLoad(String error) {
                    Log.e(TAG, "📱 Ad failed to load in callback: " + error);
                    Log.e(TAG, "📱 DETAILED ERROR: " + error);
                    
                    // Simply enable sharing without requiring ads
                    Log.d(TAG, "📱🔄 Ad failed to load during show attempt - enabling share button directly");
                    enableShareWithoutAd();
                    
                    logAdSystemState();
                }

                @Override
                public void onAdOpened() {
                    Log.d(TAG, "📱 Ad opened callback triggered - ad is now visible to user");
                }

                @Override
                public void onAdClosed() {
                    Log.d(TAG, "📱 Ad closed callback triggered - user closed the ad");
                    // Will be handled in main callback
                    logAdSystemState();
                }

                @Override
                public void onUserEarnedReward(String type, int amount) {
                    // Also handle reward here as a backup
                    Log.d(TAG, "📱 User earned reward via callback: " + amount + " " + type);
                    Log.d(TAG, "📱 Reward details - Type: " + type + ", Amount: " + amount);
                    rewardedAdWatched = true;
                    
                    // Update UI to unlock share button
                    if (binding != null) {
                        binding.watchAdButton.setEnabled(false);
                        binding.watchAdButtonText.setText(R.string.ad_watched);
                        
                        // Unlock share button (now a floating action button)
                        binding.shareButton.setEnabled(true);
                        // Change background color to primary color to indicate it's enabled
                        binding.shareButton.setBackgroundTintList(
                            ColorStateList.valueOf(getResources().getColor(R.color.share_button_enabled)));
                        binding.shareLockIcon.setVisibility(View.GONE);
                    }
                    
                    logAdSystemState();
                }

                @Override
                public void onAdShowFailed(String error) {
                    Log.e(TAG, "📱 Ad failed to show: " + error);
                    Log.e(TAG, "📱 Error details: " + error);
                    binding.watchAdButton.setEnabled(true);
                    binding.watchAdButtonText.setText(R.string.retry_watch_ad);
                    
                    Snackbar.make(binding.getRoot(), 
                        getString(R.string.ad_failed_to_show_with_reason, error), 
                        Snackbar.LENGTH_SHORT).show();
                    
                    logAdSystemState();
                }
            });
        
        if (!adShown) {
            Log.e(TAG, "📱 Ad failed to show immediately - possibly not loaded or other error");
            binding.watchAdButton.setEnabled(true);
            binding.watchAdButtonText.setText(R.string.retry_watch_ad);
            logAdSystemState();
        } else {
            Log.d(TAG, "📱 Ad show request successful");
        }
    }
    
    /**
     * Start a cooldown timer for rewarded ads
     * @param remainingMs Milliseconds remaining in cooldown
     */
    private void startCooldownTimer(long remainingMs) {
        // Cancel any existing timer
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            Log.d(TAG, "📱⏲️ Cancelled existing cooldown timer");
        }
        
        Log.d(TAG, "📱⏲️ Starting cooldown timer for " + (remainingMs / 1000) + " seconds");
        
        // Create new timer
        cooldownTimer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding != null) {
                    int secondsRemaining = (int) (millisUntilFinished / 1000);
                    binding.watchAdButton.setEnabled(false);
                    binding.watchAdButtonText.setText(getString(R.string.ad_cooldown_remaining, secondsRemaining));
                    
                    // Log every 5 seconds to avoid log spam
                    if (secondsRemaining % 5 == 0 || secondsRemaining <= 3) {
                        Log.d(TAG, "📱⏲️ Cooldown timer: " + secondsRemaining + " seconds remaining");
                    }
                }
            }

            @Override
            public void onFinish() {
                if (binding != null) {
                    binding.watchAdButton.setEnabled(true);
                    binding.watchAdButtonText.setText(R.string.watch_ad_to_share);
                    Log.d(TAG, "📱⏲️ Cooldown timer finished, watch ad button re-enabled");
                    
                    // Log state after cooldown finishes
                    logAdSystemState();
                }
            }
        };
        
        // Start the timer
        cooldownTimer.start();
        Log.d(TAG, "📱⏲️ Cooldown timer started");
        
        // Show cooldown message
        int secondsRemaining = (int) (remainingMs / 1000);
        Snackbar.make(binding.getRoot(), 
            getString(R.string.ad_in_cooldown, secondsRemaining), 
            Snackbar.LENGTH_SHORT).show();
        Log.d(TAG, "📱⏲️ Displayed cooldown message to user: " + secondsRemaining + " seconds");
        
        // Log system state after starting cooldown
        logAdSystemState();
    }

    /**
     * Share wish with validation for rewarded ad watch status
     */
    private void shareWish() {
        // Check if wish is loaded
        if (currentWish == null) {
            Toast.makeText(requireContext(), "Cannot share wish: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Track share button click
        String templateId = null;
        if (currentWish.getTemplateId() != null) {
            templateId = currentWish.getTemplateId();
        } else if (currentWish.getTemplate() != null && currentWish.getTemplate().getId() != null) {
            templateId = currentWish.getTemplate().getId();
        }
        
        AnalyticsUtils.trackShareButtonClick(
            "shareButton", 
            "SharedWishFragment", 
            templateId
        );
        
        // Generate share URL
        String shareUrl = getString(R.string.share_url_format, currentWish.getShortCode());
        
        // Create and show the bottom sheet
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_share, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Add click listeners to the social media buttons
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
        
        bottomSheetView.findViewById(R.id.instagramShare).setOnClickListener(v -> {
            final String shareText = getString(R.string.share_wish_text_instagram, shareUrl);
            handleShareVia(SHARE_VIA_INSTAGRAM);
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

        // Show the bottom sheet dialog
        bottomSheetDialog.show();
        Log.d(TAG, "Share bottom sheet dialog shown");
    }

    /**
     * Track sharing event
     * @param platform The platform used for sharing
     */
    private void trackShare(String platform) {
        try {
            // Track sharing via platform to analytics - now using AnalyticsUtils
            String templateId = null;
            if (currentWish.getTemplateId() != null) {
                templateId = currentWish.getTemplateId();
            } else if (currentWish.getTemplate() != null && currentWish.getTemplate().getId() != null) {
                templateId = currentWish.getTemplate().getId();
            }
            
            // Use our AnalyticsUtils implementation
            AnalyticsUtils.trackSocialShare(platform, templateId, true);
            
            // Send the share event to the server for analytics tracking
            if (shortCode != null && !shortCode.isEmpty()) {
                ApiService apiService = ApiClient.getClient();
                
                // Create a JsonObject for the platform parameter
                JsonObject platformBody = new JsonObject();
                platformBody.addProperty("platform", platform);
                
                apiService.trackWishShare(shortCode, platformBody.toString()).enqueue(new Callback<BaseResponse<Void>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Successfully tracked share event on server");
                        } else {
                            Log.e(TAG, "Failed to track share event on server: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                        Log.e(TAG, "Error tracking share event", t);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error tracking share", e);
        }
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

        // Set templateId if available
        if (currentWish.getTemplateId() != null) {
            sharedWish.setTemplateId(currentWish.getTemplateId());
            Log.d(TAG, "Setting templateId: " + currentWish.getTemplateId());
        } else if (currentWish.getTemplate() != null && currentWish.getTemplate().getId() != null) {
            sharedWish.setTemplateId(currentWish.getTemplate().getId());
            Log.d(TAG, "Setting templateId from template: " + currentWish.getTemplate().getId());
        }

        Log.d(TAG, "Sharing wish with title: " + title +
              ", description: " + description +
              ", deepLink: " + deepLink +
              ", previewUrl: " + sharedWish.getPreviewUrl() +
              ", templateId: " + sharedWish.getTemplateId() +
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
    public void onResume() {
        super.onResume();
        
        // Resume analytics tracking if needed
        if (shortCode != null && !isTracking && currentWish != null) {
            startAnalyticsTracking();
        }
        
        // Check and fix flag consistency issues
        checkAndFixFlagConsistency();
        
        // FORCE the watch ad button to be visible on resume
        if (!rewardedAdWatched && binding != null && binding.watchAdButton != null) {
            binding.watchAdButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "📱🚨 onResume - FORCE: Setting watch ad button visibility to VISIBLE");
        }
        
        // Check if ad is loaded, if not, try to load one
        if (!rewardedAdWatched && rewardedAdManager != null && !rewardedAdManager.isAdLoaded() && !isRewardedAdLoading) {
            Log.d(TAG, "📱🔄 No ad loaded on resume, attempting to load");
            loadRewardedAd();
        }
        
        // Check rewarded ad status and update UI accordingly
        updateRewardedAdButtonState();
        
        // Force dispatch analytics events to ensure they're sent to Firebase
        AnalyticsUtils.forceDispatchEvents();
        
        // Log ad system state
        logAdSystemState();
        
        // Run a delayed check to verify button visibility after layout
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding != null && binding.watchAdButton != null && !rewardedAdWatched) {
                int visibility = binding.watchAdButton.getVisibility();
                Log.d(TAG, "📱🚨 RESUME DELAYED CHECK: Watch ad button visibility = " + 
                    (visibility == View.VISIBLE ? "VISIBLE" : 
                    (visibility == View.GONE ? "GONE" : "INVISIBLE")));
                
                // Force visibility again if not visible
                if (visibility != View.VISIBLE) {
                    binding.watchAdButton.setVisibility(View.VISIBLE);
                    Log.d(TAG, "📱🚨 RESUME DELAYED FORCE: Setting to VISIBLE");
                }
            }
        }, 500); // Check after 500ms
    }
    
    /**
     * Check for inconsistencies between flags and fix them
     */
    private void checkAndFixFlagConsistency() {
        if (rewardedAdManager == null) return;
        
        Log.d(TAG, "📱🚨 Checking flag consistency");
        
        // If rewardedAdWatched is true but an ad is loaded, fix the inconsistency
        if (rewardedAdWatched && rewardedAdManager.isAdLoaded()) {
            Log.d(TAG, "📱🚨 INCONSISTENCY DETECTED: Ad watched flag is true but ad is loaded");
            rewardedAdWatched = false;
            Log.d(TAG, "📱🚨 FIXED: Reset rewardedAdWatched flag to false");
        }
        
        // If button is hidden but ads are available, fix visibility
        if (binding != null && binding.watchAdButton != null) {
            boolean shouldBeVisible = !rewardedAdWatched && 
                (rewardedAdManager.isAdLoaded() || isRewardedAdLoading);
                
            if (shouldBeVisible && binding.watchAdButton.getVisibility() != View.VISIBLE) {
                Log.d(TAG, "📱🚨 INCONSISTENCY DETECTED: Button should be visible but isn't");
                binding.watchAdButton.setVisibility(View.VISIBLE);
                Log.d(TAG, "📱🚨 FIXED: Set button visibility to VISIBLE");
            }
        }
        
        // If we have invalid state, reset to default
        if (rewardedAdWatched && !binding.shareButton.isEnabled()) {
            Log.d(TAG, "📱🚨 INCONSISTENCY DETECTED: Ad watched but share not enabled");
            rewardedAdWatched = false;
            binding.shareButton.setEnabled(true);
            // Change background color to primary color to indicate it's enabled
            binding.shareButton.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.share_button_enabled)));
            binding.shareLockIcon.setVisibility(View.GONE);
            Log.d(TAG, "📱🚨 FIXED: Enabled share button directly and changed color");
        }
        
        // If we're in a bad state overall, just enable sharing
        if (adFailureCount >= MAX_AD_FAILURE_COUNT/2 && !binding.shareButton.isEnabled()) {
            Log.d(TAG, "📱🚨 CRITICAL: Multiple failures detected, enabling share");
            enableShareWithoutAd();
        }
    }

    /**
     * Update the rewarded ad button state based on current conditions
     */
    private void updateRewardedAdButtonState() {
        if (binding == null || rewardedAdManager == null) {
            Log.d(TAG, "📱🔄 Cannot update ad button state: " + 
                (binding == null ? "binding is null" : "ad manager is null"));
            return;
        }
        
        Log.d(TAG, "📱🔄 Updating rewarded ad button state");
        
        // First ensure the button is visible if not watched yet
        if (!rewardedAdWatched && binding.watchAdButton != null) {
            binding.watchAdButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "📱🔄 Ensuring watch ad button is visible during state update");
        }
        
        // IMPORTANT: If the ad is loaded, always show the button, even if rewardedAdWatched is true
        // This fixes issues where the flag gets incorrectly set
        if (rewardedAdManager.isAdLoaded() && binding.watchAdButton != null) {
            binding.watchAdButton.setVisibility(View.VISIBLE);
            binding.watchAdButton.setEnabled(true);
            binding.watchAdButtonText.setText(R.string.watch_ad_to_share);
            Log.d(TAG, "📱🚨 Ad is loaded - FORCING button visible regardless of watched state");
            // Reset the watched flag since we have a loaded ad
            rewardedAdWatched = false;
            return;
        }
        
        // If we've already watched the ad or ad is disabled globally, no need to update
        if (rewardedAdWatched) {
            Log.d(TAG, "📱🔄 Ad already watched, leaving button disabled");
            binding.watchAdButton.setVisibility(View.GONE);
            return;
        }
        
        // Check if we've reached maximum failure count
        if (adFailureCount >= MAX_AD_FAILURE_COUNT) {
            Log.d(TAG, "📱🔄 Maximum ad failure count reached in state update");
            enableShareWithoutAd();
            return;
        }
        
        // Check if ad is loaded
        if (rewardedAdManager.isAdLoaded()) {
            Log.d(TAG, "📱🔄 Ad is loaded, adjusting button state");
            
            if (!rewardedAdWatched && !rewardedAdManager.isInCooldownPeriod()) {
                binding.watchAdButton.setEnabled(true);
                binding.watchAdButtonText.setText(R.string.watch_ad_to_share);
                Log.d(TAG, "📱🔄 Button enabled: Ad loaded, not watched, no cooldown");
            } else if (rewardedAdWatched) {
                binding.watchAdButton.setEnabled(false);
                binding.watchAdButtonText.setText(R.string.ad_watched);
                Log.d(TAG, "📱🔄 Button disabled: Ad already watched");
            }
        } else {
            Log.d(TAG, "📱🔄 Ad is not loaded, checking conditions");
            
            // Check if in cooldown period
            if (rewardedAdManager.isInCooldownPeriod()) {
                long remainingMs = rewardedAdManager.getRemainingCooldownMs();
                Log.d(TAG, "📱🔄 In cooldown period, starting timer with " + (remainingMs / 1000) + " seconds");
                startCooldownTimer(remainingMs);
            } else if (!isRewardedAdLoading && !rewardedAdWatched) {
                binding.watchAdButton.setEnabled(false);
                binding.watchAdButtonText.setText(R.string.loading_ad);
                Log.d(TAG, "📱🔄 Button disabled, starting ad load");
                loadRewardedAd();
            } else if (isRewardedAdLoading) {
                Log.d(TAG, "📱🔄 Ad is currently loading, waiting for callback");
            } else if (rewardedAdWatched) {
                Log.d(TAG, "📱🔄 Ad already watched, button remains disabled");
            }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Stop analytics tracking
        stopAnalyticsTracking();
        
        // Cancel cooldown timer if running
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop analytics tracking if still running
        stopAnalyticsTracking();
        
        binding = null;
        
        // Cancel cooldown timer if running
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop analytics tracking if still running
        stopAnalyticsTracking();
        
        // Remove callback
        if (backPressCallback != null) {
            backPressCallback.remove();
        }
        
        // Clear handlers
        mainHandler.removeCallbacksAndMessages(null);
        
        // Clean up ad manager
        if (rewardedAdManager != null) {
            rewardedAdManager.destroy();
            rewardedAdManager = null;
        }
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
     * Start tracking analytics for real-time viewers
     */
    private void startAnalyticsTracking() {
        if (shortCode == null || isTracking) return;
        
        // Extract recipient and sender names if available
        String recipientName = null;
        String senderName = null;
        if (currentWish != null) {
            recipientName = currentWish.getRecipientName();
            senderName = currentWish.getSenderName();
        }
        
        // Track shared wish view once
        AnalyticsUtils.trackSharedWishView(shortCode, senderName, recipientName);
        
        // Track active viewer
        AnalyticsUtils.trackViewerActive(shortCode);
        
        // Store start time for duration calculation
        viewStartTime = System.currentTimeMillis();
        isTracking = true;
        
        // Setup heartbeat to track active viewers
        analyticsHeartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || getActivity() == null) return;
                
                // Track active viewer again to maintain real-time count
                AnalyticsUtils.trackViewerActive(shortCode);
                
                // Schedule next heartbeat
                mainHandler.postDelayed(this, ANALYTICS_HEARTBEAT_INTERVAL);
            }
        };
        
        // Start heartbeat
        mainHandler.postDelayed(analyticsHeartbeatRunnable, ANALYTICS_HEARTBEAT_INTERVAL);
        
        Log.d(TAG, "Started analytics tracking for shared wish: " + shortCode);
    }
    
    /**
     * Stop tracking analytics for real-time viewers
     */
    private void stopAnalyticsTracking() {
        if (!isTracking) return;
        
        // Remove heartbeat runnable
        if (analyticsHeartbeatRunnable != null) {
            mainHandler.removeCallbacks(analyticsHeartbeatRunnable);
        }
        
        // Calculate view duration
        long endTime = System.currentTimeMillis();
        long durationSeconds = (endTime - viewStartTime) / 1000;
        
        // Track viewer inactive with duration
        if (shortCode != null) {
            AnalyticsUtils.trackViewerInactive(shortCode, durationSeconds);
        }
        
        isTracking = false;
        Log.d(TAG, "Stopped analytics tracking for shared wish: " + shortCode + ", duration: " + durationSeconds + "s");
    }

    /**
     * Navigate to the template detail fragment to reuse the template
     */
    private void reuseTemplate() {
        if (currentWish == null) {
            Toast.makeText(requireContext(), getString(R.string.cannot_reuse_template_missing_data), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get the template ID
        String templateId = null;
        if (currentWish.getTemplateId() != null) {
            templateId = currentWish.getTemplateId();
        } else if (currentWish.getTemplate() != null && currentWish.getTemplate().getId() != null) {
            templateId = currentWish.getTemplate().getId();
        }
        
        if (templateId == null || templateId.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.cannot_reuse_template_missing_id), Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Reusing template with ID: " + templateId);
        
        try {
            // Navigate to TemplateDetailFragment
            NavController navController = Navigation.findNavController(requireView());
            NavDirections action = SharedWishFragmentDirections.actionSharedWishToTemplateDetail(templateId);
            navController.navigate(action);
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to template detail: " + e.getMessage(), e);
            Toast.makeText(requireContext(), getString(R.string.error_opening_template, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Dump the current state of the ad system to logs for debugging
     */
    private void logAdSystemState() {
        StringBuilder state = new StringBuilder();
        state.append("\n📱📊 AD SYSTEM STATE DUMP:");
        state.append("\n----------------------------------------");
        
        if (rewardedAdManager == null) {
            state.append("\n▶️ RewardedAdManager: null");
            state.append("\n▶️ Ad Watched: ").append(rewardedAdWatched);
            state.append("\n▶️ Loading in Progress: ").append(isRewardedAdLoading);
        } else {
            state.append("\n▶️ Ad Loaded: ").append(rewardedAdManager.isAdLoaded());
            state.append("\n▶️ Ad Watched: ").append(rewardedAdWatched);
            state.append("\n▶️ Loading in Progress: ").append(isRewardedAdLoading);
            state.append("\n▶️ In Cooldown: ").append(rewardedAdManager.isInCooldownPeriod());
            
            if (rewardedAdManager.isInCooldownPeriod()) {
                state.append("\n▶️ Cooldown Remaining: ").append(rewardedAdManager.getRemainingCooldownMs() / 1000).append(" seconds");
            }
        }
        
        if (binding != null) {
            state.append("\n▶️ Button Enabled: ").append(binding.watchAdButton != null && binding.watchAdButton.isEnabled());
            state.append("\n▶️ Button Text: ").append(binding.watchAdButtonText != null ? binding.watchAdButtonText.getText() : "null");
            state.append("\n▶️ Share Button Enabled: ").append(binding.shareButton != null && binding.shareButton.isEnabled());
            state.append("\n▶️ Ad Button Visibility: ").append(binding.watchAdButton != null ? 
                (binding.watchAdButton.getVisibility() == View.VISIBLE ? "VISIBLE" : 
                (binding.watchAdButton.getVisibility() == View.GONE ? "GONE" : "INVISIBLE")) : "null");
        } else {
            state.append("\n▶️ Binding: null");
        }
        
        state.append("\n----------------------------------------");
        
        Log.d(TAG, state.toString());
    }

    /**
     * Helper method to enable sharing without ad requirement
     */
    private void enableShareWithoutAd() {
        if (binding == null) {
            Log.e(TAG, "📱❌ Cannot enable sharing - binding is null");
            return;
        }
        
        Log.d(TAG, "📱✅ Enabling sharing without ad requirement");
        
        try {
            // Hide ad button completely
            if (binding.watchAdButton != null) {
                binding.watchAdButton.setVisibility(View.GONE);
                Log.d(TAG, "📱✅ Ad button hidden");
            }
            
            // Unlock share button (now a floating action button)
            if (binding.shareButton != null) {
                binding.shareButton.setEnabled(true);
                // Change background color to primary color to indicate it's enabled
                binding.shareButton.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(R.color.share_button_enabled)));
                Log.d(TAG, "📱✅ Share button enabled and color changed to primary");
            }
            
            // Hide lock icon
            if (binding.shareLockIcon != null) {
                binding.shareLockIcon.setVisibility(View.GONE);
                Log.d(TAG, "📱✅ Share lock icon hidden");
            }
            
            // Mark as watched to prevent other logic from trying to show ads
            rewardedAdWatched = true;
            
            // Show info message to user
            if (getContext() != null) {
                Toast.makeText(getContext(), 
                    "Share enabled! No ads required.", 
                    Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "📱❌ Error enabling share: " + e.getMessage(), e);
        }
        
        // Log state change
        logAdSystemState();
    }

    /**
     * Check if we should skip ads completely based on server configuration or other factors
     */
    private void checkShouldSkipAds() {
        // Check if ads are disabled globally - could be fetched from server or preferences
        boolean areAdsDisabled = false;
        
        try {
            // Check for a system property that might disable ads
            Context context = getContext();
            if (context != null) {
                // Use standard SharedPreferences instead of the custom SharedPrefsManager
                // since the latter doesn't have the getBoolean method with default value
                SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
                areAdsDisabled = prefs.getBoolean("disable_rewarded_ads", false);
                
                // Check if analytics consent was denied (which could also imply no ads)
                if (!areAdsDisabled && prefsManager != null) {
                    areAdsDisabled = !prefsManager.hasAnalyticsConsent();
                }
            }
            
            Log.d(TAG, "📱 Checking if rewarded ads are disabled: " + areAdsDisabled);
            
            // For testing, you can uncomment this line
            // areAdsDisabled = true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking ad disable status", e);
        }
        
        // If ads are disabled, enable sharing without requiring ad viewing
        if (areAdsDisabled) {
            Log.d(TAG, "📱 Rewarded ads are disabled globally - enabling direct sharing");
            enableShareWithoutAd();
        }
    }

    /**
     * Handle case when ads are confirmed unavailable
     */
    private void handleAdsUnavailable(String reason) {
        Log.d(TAG, "📱 Ads confirmed unavailable: " + reason);
        
        // Increment failure counter
        adFailureCount++;
        
        if (adFailureCount >= MAX_AD_FAILURE_COUNT) {
            Log.d(TAG, "📱 Reached maximum ad failure count (" + MAX_AD_FAILURE_COUNT + 
                ") - enabling sharing without ads");
            enableShareWithoutAd();
        } else {
            Log.d(TAG, "📱 Ad failure count: " + adFailureCount + "/" + MAX_AD_FAILURE_COUNT);
            
            // For regular failures, just disable the button but don't remove it yet
            if (binding != null) {
                binding.watchAdButton.setEnabled(false);
                binding.watchAdButtonText.setText(R.string.ad_not_available);
            }
        }
    }
}
