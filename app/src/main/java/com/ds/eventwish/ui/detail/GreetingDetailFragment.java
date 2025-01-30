//package com.ds.eventwish.ui.detail;
//
//import static com.google.android.material.internal.ViewUtils.hideKeyboard;
//
//import android.app.Activity;
//import android.content.Context;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.inputmethod.InputMethodManager;
//import android.webkit.WebResourceError;
//import android.webkit.WebResourceRequest;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.bumptech.glide.Glide;
//import com.ds.eventwish.R;
//import com.ds.eventwish.databinding.FragmentGreetingDetailBinding;
//import com.ds.eventwish.databinding.ItemGreetingBinding;
//import com.ds.eventwish.ui.base.BaseFragment;
//import com.ds.eventwish.ui.history.HistoryFragment;
//import com.ds.eventwish.ui.home.GreetingItem;
//
//public class GreetingDetailFragment extends BaseFragment {
//    // Add field to store current greeting
//    private static final String TAG = "GreetingDetailFragment";
//    private GreetingItem currentGreeting;
//    private FragmentGreetingDetailBinding binding;
//    private int greetingId = -1;
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate called");
//        if (getArguments() != null) {
//            try {
//                GreetingDetailFragmentArgs args = GreetingDetailFragmentArgs.fromBundle(getArguments());
//                greetingId = args.getGreetingId();
//                Log.d(TAG, "Received greetingId: " + greetingId);
//            } catch (Exception e) {
//                Log.e(TAG, "Error getting arguments: " + e.getMessage());
//            }
//
//        }
//    }
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        binding = FragmentGreetingDetailBinding.inflate(inflater, container, false);
//        return binding.getRoot();
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        setupTouchListener();
//        setupWebView();
//        setupShareButton();
//        setupTextChangeListeners();
//
//        // if (currentGreeting != null) {
//        //     loadGreetingData();
//        // }
//
//        WebView.setWebContentsDebuggingEnabled(true);
//
//        // Load greeting data based on greetingId
//        if (greetingId != -1) {
//            try {
//                loadGreetingData();
//                Log.d(TAG, "Loading greeting data for ID: " + greetingId);
//            } catch (Exception e) {
//                Log.e(TAG, "Error loading greeting: " + e.getMessage());
//            }
//        }
//
//    }
//
//
//    private void setupTouchListener() {
//        View rootView = binding.rootLayout; // Use rootLayout from XML
//
//        rootView.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                hideKeyboard();
//            }
//            return false; // Let other touch events propagate
//        });
//
//        rootView.setOnClickListener(v -> {
//            Log.d(TAG, "Root layout clicked"); // Debugging log
//            hideKeyboard();
//        });
//
//        binding.greetingPreview.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                Log.d(TAG, "WebView clicked");
//                hideKeyboard();
//            }
//            return false;
//        });
//
//    }
//
//    private void hideKeyboard() {
//        Activity activity = getActivity();
//    if (activity != null) {
//        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
//        View view = activity.getCurrentFocus();
//        if (view == null) {
//            view = new View(activity);
//        }
//        Log.d(TAG, "Hiding keyboard"); // Debug log
//        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//        view.clearFocus();
//    }
//}
//
//    private void loadGreetingData() {
//        if (binding == null) {
//            Log.e(TAG, "Binding is null");
//            return;
//        }
//
//        try {
//            // Update WebView content
//            String htmlContent = getGreetingHtml();
//            binding.greetingPreview.loadData(htmlContent, "text/html", "UTF-8");
//            Log.d(TAG, "Greeting data loaded successfully");
//        } catch (Exception e) {
//            Log.e(TAG, "Error in loadGreetingData: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//
//    // private void loadGreetingData() {
//    //     if (currentGreeting != null) {
//    //         binding2.greetingTitle.setText(currentGreeting.getTitle());
//    //         binding2.greetingCategory.setText(currentGreeting.getCategory());
//
//    //         if (currentGreeting.getImageUrl() != null) {
//    //             Glide.with(requireContext())
//    //                 .load(currentGreeting.getImageUrl())
//    //                 .into(binding2.greetingImageView);
//    //         }
//    //     }
//    // }
//
//    private void setupWebView() {
//        WebView webView = binding.greetingPreview;
//        webView.setVisibility(View.VISIBLE);
//        WebSettings settings = webView.getSettings();
//        settings.setJavaScriptEnabled(true); // Already set
//        settings.setDomStorageEnabled(true); // Enable DOM storage for advanced content
//        settings.setLoadWithOverviewMode(true); // Fit content to the WebView
//        settings.setUseWideViewPort(true); // Adjust viewport to screen size
//
//
//        // binding.greetingPreview.loadData(getGreetingHtml(), "text/html", "UTF-8");
//        // Log.d("HTMLContent", getGreetingHtml());
//
//         // Add WebView client for debugging
//    webView.setWebViewClient(new WebViewClient() {
//        @Override
//        public void onPageFinished(WebView view, String url) {
//            super.onPageFinished(view, url);
//            Log.d(TAG, "Page loaded successfully");
//            view.setVisibility(View.VISIBLE);
//        }
//
//        @Override
//        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//            super.onReceivedError(view, request, error);
//            Log.e(TAG, "WebView error: " + error.toString());
//        }
//    });
//
//    // Load initial HTML content
//    String htmlContent = getGreetingHtml();
//    Log.d(TAG, "Loading HTML: " + htmlContent);
//    webView.loadDataWithBaseURL(
//        null,
//        htmlContent,
//        "text/html",
//        "UTF-8",
//        null
//    );
//        // Load initial preview
//        updatePreview();
//    }
//
//    private void setupShareButton() {
//        binding.shareButton.setOnClickListener(v -> {
//            if (validateInput() && currentGreeting != null) {
//                shareGreeting(
//                        binding.senderNameEdit.getText().toString(),
//                        binding.recipientNameEdit.getText().toString()
//                );
//            }
//        });
//    }
//
//    private void setupTextChangeListeners() {
//        binding.senderNameEdit.addTextChangedListener(new SimpleTextWatcher() {
//            @Override
//            public void afterTextChanged(Editable s) {
//                updatePreview();
//            }
//        });
//
//        binding.recipientNameEdit.addTextChangedListener(new SimpleTextWatcher() {
//            @Override
//            public void afterTextChanged(Editable s) {
//                updatePreview();
//            }
//        });
//    }
//
//    private void updatePreview() {
//        String htmlContent = getGreetingHtml();
//        binding.greetingPreview.loadData(htmlContent, "text/html", "UTF-8");
//    }
//
//
//
//    private String getGreetingHtml() {
//        String senderName = binding.senderNameEdit.getText().toString().trim();
//        String recipientName = binding.recipientNameEdit.getText().toString().trim();
//
//        String htmlContent = "<!DOCTYPE html>" +
//        "<html>" +
//        "<head>" +
//        "<meta charset='UTF-8'>" +
//        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
//        "<style>" +
//        "body { margin: 0; padding: 20px; font-family: Arial; background-color: #fff; }" +
//        ".greeting { text-align: center; padding: 20px; }" +
//        ".title { color: #FF6B6B; font-size: 24px; margin-bottom: 20px; }" +
//        "p { color: #333; font-size: 18px; line-height: 1.5; }" +
//        "</style>" +
//        "</head>" +
//        "<body>" +
//        "<div class='greeting'>" +
//        "<h1 class='title'>Happy Birthday!</h1>" +
//        "<p>Dear " + (recipientName.isEmpty() ? "Friend" : recipientName) + ",</p>" +
//        "<p>Wishing you a wonderful day!</p>" +
//        "<p>From, " + (senderName.isEmpty() ? "Me" : senderName) + "</p>" +
//        "</div>" +
//        "</body>" +
//        "</html>";
//
//    Log.d(TAG, "Generated HTML: " + htmlContent);
//    return htmlContent;
//}
//
//    private boolean validateInput() {
//        boolean isValid = true;
//
//        if (binding.senderNameEdit.getText().toString().trim().isEmpty()) {
//            binding.senderNameLayout.setError("Please enter your name");
//            isValid = false;
//        } else {
//            binding.senderNameLayout.setError(null);
//        }
//
//        if (binding.recipientNameEdit.getText().toString().trim().isEmpty()) {
//            binding.recipientNameLayout.setError("Please enter recipient's name");
//            isValid = false;
//        } else {
//            binding.recipientNameLayout.setError(null);
//        }
//
//        return isValid;
//    }
//
//    // private void shareGreeting(String senderName, String recipientName) {
//    //     String shareText = String.format("Happy Birthday %s!\n\nWishing you a wonderful day filled with joy and happiness!\n\nBest wishes,\n%s",
//    //             recipientName, senderName);
//
//    //     Intent shareIntent = new Intent(Intent.ACTION_SEND);
//    //     shareIntent.setType("text/plain");
//    //     shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
//    //     startActivity(Intent.createChooser(shareIntent, "Share Greeting"));
//    // }
//
//    private void shareGreeting(String senderName, String recipientName) {
//        GreetingItem greetingToShare = new GreetingItem(
//            currentGreeting.getId(),
//            String.format("To: %s\n%s\nFrom: %s",
//                recipientName,
//                currentGreeting.getTitle(),
//                senderName),
//            currentGreeting.getCategory(),
//            currentGreeting.getImageUrl()
//        );
//
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, greetingToShare.getTitle());
//        startActivity(Intent.createChooser(shareIntent, "Share via"));
//
//        // Save to history
//        HistoryFragment historyFragment = (HistoryFragment) requireActivity()
//                .getSupportFragmentManager()
//                .findFragmentByTag("history_fragment");
//
//        if (historyFragment != null) {
//            historyFragment.addToHistory(greetingToShare);
//        }
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
//}
