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
import com.google.gson.Gson;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class SharedWishFragment extends Fragment {
    private SharedPrefsManager prefsManager;
    private FragmentSharedWishBinding binding;
    private SharedWishViewModel viewModel;
    private String shortCode;
    private String TAG = "SharedWishFragment";
    private WishResponse currentWish;
    private OnBackPressedCallback backPressCallback;

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
    }

    private void setupClickListeners() {
        binding.shareButton.setOnClickListener(v -> shareWish());
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
        String shareUrl = getString(R.string.share_url_format, shortCode);
        
        // Create the share text
        String shareText = getString(R.string.share_wish_text, shareUrl);
        
        // Show the share bottom sheet
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_share, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Set up click listeners for share options
        bottomSheetView.findViewById(R.id.whatsappShare).setOnClickListener(v -> {
            shareViaWhatsApp(shareText);
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetView.findViewById(R.id.moreOptions).setOnClickListener(v -> {
            shareViaOther(shareText);
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetView.findViewById(R.id.copyLink).setOnClickListener(v -> {
            copyLinkToClipboard(shareUrl);
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.show();
    }

    private void shareViaWhatsApp(String shareText) {
        try {
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            // Save the share platform before starting the activity
            saveSharePlatform(SHARE_VIA_WHATSAPP);
            
            startActivity(whatsappIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing via WhatsApp", e);
            Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            // Fallback to other share
            shareViaOther(shareText);
        }
    }

    private void shareViaOther(String shareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        // Create a chooser with a custom title
        Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share_via));
        
        // Create a listener to detect which app was chosen
        startActivityForResult(chooser, 100);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100) {
            // This is called when returning from the share chooser
            // We need to detect which app was used
            if (data != null && data.getComponent() != null) {
                String packageName = data.getComponent().getPackageName().toLowerCase();
                String sharePlatform = SHARE_VIA_OTHER;
                
                // Determine which platform was used
                if (packageName.contains("whatsapp")) {
                    sharePlatform = SHARE_VIA_WHATSAPP;
                } else if (packageName.contains("facebook") || packageName.contains("fb")) {
                    sharePlatform = SHARE_VIA_FACEBOOK;
                } else if (packageName.contains("twitter") || packageName.contains("tweet")) {
                    sharePlatform = SHARE_VIA_TWITTER;
                } else if (packageName.contains("instagram")) {
                    sharePlatform = SHARE_VIA_INSTAGRAM;
                } else if (packageName.contains("mail") || packageName.contains("gmail")) {
                    sharePlatform = SHARE_VIA_EMAIL;
                } else if (packageName.contains("sms") || packageName.contains("mms") || packageName.contains("message")) {
                    sharePlatform = SHARE_VIA_SMS;
                }
                
                // Save the share platform
                saveSharePlatform(sharePlatform);
                Log.d(TAG, "Shared via: " + sharePlatform + " (package: " + packageName + ")");
            } else {
                // If we can't determine the app, save as "other"
                saveSharePlatform(SHARE_VIA_OTHER);
                Log.d(TAG, "Shared via: unknown app");
            }
        }
    }

    private void copyLinkToClipboard(String link) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("EventWish Link", link);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.link_copied, Toast.LENGTH_SHORT).show();
        
        // Save the share platform
        saveSharePlatform(SHARE_VIA_CLIPBOARD);
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
                } else {
                    Log.w(TAG, "Failed to update share platform for " + shortCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving share platform", e);
            }
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
