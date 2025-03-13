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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new SharedPrefsManager(requireContext());
        viewModel = new ViewModelProvider(this).get(SharedWishViewModel.class);
        
        if (getArguments() != null) {
            shortCode = SharedWishFragmentArgs.fromBundle(getArguments()).getShortCode();
        }

        // Handle back press with proper navigation
        backPressCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    // Check if we can pop back
                    if (navController.getPreviousBackStackEntry() != null) {
                        navController.popBackStack();
                    } else {
                        // If no back stack, go to home
                        navController.navigate(R.id.navigation_home);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling back press", e);
                    // Emergency fallback
                    requireActivity().finish();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentSharedWishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Show loading state immediately
        binding.loadingView.setVisibility(View.VISIBLE);
        binding.contentLayout.setVisibility(View.GONE);
        
        setupWebView();
        setupObservers();
        setupClickListeners();
        
        if (shortCode != null) {
            viewModel.loadSharedWish(shortCode);
        }
    }

    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
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
                    
                    // Set the preview URL from template
                    if (wish.getTemplate() != null && wish.getTemplate().getThumbnailUrl() != null) {
                        historyWish.setPreviewUrl(wish.getTemplate().getThumbnailUrl());
                        Log.d(TAG, "Setting preview URL: " + wish.getTemplate().getThumbnailUrl());
                    }
                    
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

        if (wish == null || wish.getCustomizedHtml() == null){
            Log.e(TAG, "Wish or HTML content is null");
   
            return;
        }

        Template template = wish.getTemplate();
        Log.d(TAG, "Template: " + (template != null ? "found" : "null"));

        if (template == null) {
            Log.e(TAG, "Template is null for wish: " + wish.getShortCode());

            return;
        }

        try{
       // Log.d(TAG,template.getThumbnailUrl());
        String css = template.getCssContent() != null ? template.getCssContent() : "";
        String js = template.getJsContent() != null ? template.getJsContent() : "";
        Log.d(TAG, "CSS length: " + css.length() + ", JS length: " + js.length());

        String fullHtml = "<!DOCTYPE html><html><head>" +
                         "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                         "<style>" + css + "</style>" +
                         "</head><body>" +
                         wish.getCustomizedHtml() +
                         "<script>" + js + "</script>" +
                         "</body></html>";

        binding.webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
        binding.loadingView.setVisibility(View.GONE);
        binding.contentLayout.setVisibility(View.VISIBLE);
        
        // Set recipient and sender names
        binding.recipientNameText.setText("To: " + wish.getRecipientName());
        binding.senderNameText.setText("From: " + wish.getSenderName());
    }
    catch (Exception e) {
        Log.e(TAG, "Error loading wish content", e);
        Log.e(TAG, "Raw wish data: " + new Gson().toJson(wish));    }
    }

    private void shareWish() {
        if (currentWish == null) return;
        
        // Create the share URL
        final String shareUrl = getString(R.string.share_url_format, shortCode);
        
        // Show the share bottom sheet
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_share, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);
        
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
        
        bottomSheetDialog.show();
    }

    /**
     * Handle sharing via a specific platform
     * @param platform The platform to share on
     */
    private void handleShareVia(String platform) {
        if (currentWish == null || currentWish.getTemplate() == null) {
            Toast.makeText(requireContext(), "Cannot share wish: missing data", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update the wish's sharedVia field
        currentWish.setSharedVia(platform);
        
        // Track the share
        trackShare(platform);
        
        // Use the server-side approach for sharing
        String title = currentWish.getTitle();
        String description = currentWish.getDescription();
        String senderName = currentWish.getSenderName();
        String recipientName = currentWish.getRecipientName();
        String templateId = currentWish.getTemplate().getId();
        
        // Create a SharedWish object from the WishResponse
        SharedWish sharedWish = new SharedWish();
        sharedWish.setTitle(title);
        sharedWish.setDescription(description);
        sharedWish.setSenderName(senderName);
        sharedWish.setRecipientName(recipientName);
        sharedWish.setShortCode(currentWish.getShortCode());
        sharedWish.setSharedVia(platform);
        
        // Show loading indicator
        showLoading(true);
        
        // Use the server-side approach
        SocialShareUtil.shareWishViaServer(
            requireContext(),
            sharedWish,
            templateId,
            title,
            description,
            senderName,
            recipientName,
            platform,
            new SocialShareUtil.ShareCallback() {
                @Override
                public void onShareReady(Intent shareIntent, String shareableUrl, String platform) {
                    showLoading(false);
                    
                    // Copy the URL to clipboard
                    copyToClipboard(shareableUrl);
                    
                    // Launch the share intent
                    try {
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching share intent", e);
                        Toast.makeText(requireContext(), "Error launching share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onShareError(String errorMessage) {
                    showLoading(false);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        );
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
        ClipData clip = ClipData.newPlainText("Shareable URL", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), "URL copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    /**
     * Track share analytics
     * @param platform The platform shared on
     */
    private void trackShare(String platform) {
        if (currentWish != null) {
            // Save to history
            ViewModelProvider provider = new ViewModelProvider(requireActivity());
            HistoryViewModel historyViewModel = provider.get(HistoryViewModel.class);
            historyViewModel.updateSharedWish(currentWish.getShortCode(), platform);
            
            // Send analytics to backend
            sendShareAnalyticsToBackend(platform);
        }
    }

    private void copyLinkToClipboard(String shareUrl) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Share URL", shareUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.link_copied, Toast.LENGTH_SHORT).show();
    }
    
    private void saveSharePlatform(String platform) {
        if (currentWish != null && shortCode != null) {
            try {
                // Use HistoryViewModel to update the shared wish with the platform
                ViewModelProvider provider = new ViewModelProvider(requireActivity());
                HistoryViewModel historyViewModel = provider.get(HistoryViewModel.class);
                
                // Update the shared wish with the platform
                boolean updated = historyViewModel.updateSharedWish(shortCode, platform);
                
                if (updated) {
                    Log.d(TAG, "Saved share platform for " + shortCode + ": " + platform);
                    
                    // Update the current wish's sharedVia field
                    if (currentWish != null) {
                        currentWish.setSharedVia(platform);
                        Log.d(TAG, "Updated current wish sharedVia to: " + platform);
                    }
                    
                    // Send analytics to backend
                    sendShareAnalyticsToBackend(platform);
                } else {
                    Log.w(TAG, "Failed to update share platform for " + shortCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving share platform", e);
            }
        }
    }

    /**
     * Send share analytics to the backend
     * @param platform The platform used for sharing
     */
    private void sendShareAnalyticsToBackend(String platform) {
        // TODO: Implement analytics tracking
        Log.d(TAG, "Share via " + platform);
    }

    /**
     * Update the analytics display with data from the backend
     * @param analytics The analytics data from the backend
     */
    private void updateAnalyticsDisplay(JsonObject analytics) {
        try {
            // Extract analytics data
            int views = 0;
            int uniqueViews = 0;
            int shareCount = 0;
            
            if (analytics.has("views")) {
                views = analytics.get("views").getAsInt();
            }
            
            if (analytics.has("uniqueViews")) {
                uniqueViews = analytics.get("uniqueViews").getAsInt();
            }
            
            if (analytics.has("shareCount")) {
                shareCount = analytics.get("shareCount").getAsInt();
            }
            
            // Update the analytics badge with the total views
            if (binding.analyticsButton != null) {
                binding.analyticsButton.setText(String.valueOf(views));
                binding.analyticsButton.setVisibility(View.VISIBLE);
            }
            
            // Save the analytics data for later use
            this.analyticsData = analytics;
        } catch (Exception e) {
            Log.e(TAG, "Error updating analytics display", e);
        }
    }
    
    /**
     * Show a bottom sheet with detailed analytics
     */
    private void showAnalyticsBottomSheet() {
        if (analyticsData == null) return;
        
        try {
            // Create and show the bottom sheet
            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_analytics, null);
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
            bottomSheetDialog.setContentView(bottomSheetView);
            
            // Extract analytics data
            int views = 0;
            int uniqueViews = 0;
            int shareCount = 0;
            
            if (analyticsData.has("views")) {
                views = analyticsData.get("views").getAsInt();
            }
            
            if (analyticsData.has("uniqueViews")) {
                uniqueViews = analyticsData.get("uniqueViews").getAsInt();
            }
            
            if (analyticsData.has("shareCount")) {
                shareCount = analyticsData.get("shareCount").getAsInt();
            }
            
            // Set the analytics values
            TextView viewsText = bottomSheetView.findViewById(R.id.viewsCount);
            TextView uniqueViewsText = bottomSheetView.findViewById(R.id.uniqueViewsCount);
            TextView sharesText = bottomSheetView.findViewById(R.id.sharesCount);
            
            viewsText.setText(String.valueOf(views));
            uniqueViewsText.setText(String.valueOf(uniqueViews));
            sharesText.setText(String.valueOf(shareCount));
            
            // Set up the share breakdown if available
            if (analyticsData.has("shareHistory") && analyticsData.get("shareHistory").isJsonArray()) {
                setupShareBreakdown(bottomSheetView, analyticsData.getAsJsonArray("shareHistory"));
            }
            
            // Show the bottom sheet
            bottomSheetDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing analytics bottom sheet", e);
            Toast.makeText(requireContext(), "Error showing analytics", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Set up the share breakdown chart
     * @param view The bottom sheet view
     * @param shareHistory The share history data
     */
    private void setupShareBreakdown(View view, JsonArray shareHistory) {
        try {
            // Count shares by platform
            Map<String, Integer> platformCounts = new HashMap<>();
            
            for (int i = 0; i < shareHistory.size(); i++) {
                JsonObject shareEntry = shareHistory.get(i).getAsJsonObject();
                if (shareEntry.has("platform")) {
                    String platform = shareEntry.get("platform").getAsString();
                    platformCounts.put(platform, platformCounts.getOrDefault(platform, 0) + 1);
                }
            }
            
            // Create the breakdown view
            LinearLayout breakdownContainer = view.findViewById(R.id.shareBreakdownContainer);
            breakdownContainer.removeAllViews();
            
            // Add a row for each platform
            for (Map.Entry<String, Integer> entry : platformCounts.entrySet()) {
                String platform = entry.getKey();
                int count = entry.getValue();
                
                // Create a row for this platform
                View rowView = getLayoutInflater().inflate(R.layout.item_share_platform, null);
                
                // Set the platform icon and name
                ImageView platformIcon = rowView.findViewById(R.id.platformIcon);
                TextView platformName = rowView.findViewById(R.id.platformName);
                TextView platformCount = rowView.findViewById(R.id.platformCount);
                
                // Set the platform name
                platformName.setText(getPlatformDisplayName(platform));
                platformCount.setText(String.valueOf(count));
                
                // Set the platform icon
                setPlatformIcon(platformIcon, platform);
                
                // Add the row to the container
                breakdownContainer.addView(rowView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up share breakdown", e);
        }
    }
    
    /**
     * Get the display name for a platform
     * @param platform The platform code
     * @return The display name
     */
    private String getPlatformDisplayName(String platform) {
        switch (platform.toUpperCase()) {
            case "WHATSAPP":
                return "WhatsApp";
            case "FACEBOOK":
                return "Facebook";
            case "TWITTER":
                return "Twitter";
            case "INSTAGRAM":
                return "Instagram";
            case "EMAIL":
                return "Email";
            case "SMS":
                return "SMS";
            case "CLIPBOARD":
                return "Copied Link";
            case "LINK":
                return "Direct Link";
            default:
                return "Other";
        }
    }
    
    /**
     * Set the icon for a platform
     * @param imageView The ImageView to set the icon on
     * @param platform The platform code
     */
    private void setPlatformIcon(ImageView imageView, String platform) {
        switch (platform.toUpperCase()) {
            case "WHATSAPP":
                imageView.setImageResource(R.drawable.ic_whatsapp);
                break;
            case "FACEBOOK":
                imageView.setImageResource(R.drawable.ic_facebook);
                break;
            case "TWITTER":
                imageView.setImageResource(R.drawable.ic_twitter);
                break;
            case "INSTAGRAM":
                imageView.setImageResource(R.drawable.ic_instagram);
                break;
            case "EMAIL":
                imageView.setImageResource(R.drawable.ic_email);
                break;
            case "SMS":
                imageView.setImageResource(R.drawable.ic_sms);
                break;
            case "CLIPBOARD":
                imageView.setImageResource(R.drawable.ic_link);
                break;
            case "LINK":
                imageView.setImageResource(R.drawable.ic_link);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_share);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.webView.stopLoading();
            binding.webView.destroy();
            binding = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove back press callback
        if (backPressCallback != null) {
            backPressCallback.remove();
        }
    }
}
