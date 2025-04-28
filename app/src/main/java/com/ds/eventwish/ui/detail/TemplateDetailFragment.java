package com.ds.eventwish.ui.detail;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentTemplateDetailBinding;
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.ui.render.TemplateRenderer;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class TemplateDetailFragment extends Fragment implements TemplateRenderer.TemplateRenderListener {
    private static final String TAG = "TemplateDetailFragment";
    private static final long TEXT_CHANGE_DELAY = 100; // Debounce delay in milliseconds
    private static final long ANALYTICS_HEARTBEAT_INTERVAL = 30000; // 30 seconds
    
    private FragmentTemplateDetailBinding binding;
    private TemplateDetailViewModel viewModel;
    private TemplateRenderer templateRenderer;
    private String templateId;
    private boolean isViewCreated = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler backgroundHandler = new Handler(Looper.getMainLooper()); // Can be changed to background if needed
    private BottomNavigationView bottomNav;
    private Runnable pendingNameUpdate;
    private Runnable analyticsHeartbeatRunnable;
    private long viewStartTime;
    private boolean isTracking = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TemplateDetailViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTemplateDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Safely hide the action bar
        if (getActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }

        // Get bottom navigation from activity and setup
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            // Keep bottom navigation visible but disable it temporarily
            bottomNav.setEnabled(false);
            setupKeyboardVisibilityListener(view);
        }
        
        // Setup touch interceptor for the entire layout
        binding.getRoot().setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false; // Allow the event to be processed further
        });
        
        setupWebView();
        setupInputListeners();
        setupObservers();
        setupClickListeners();
        
        // Adjust WebView container layout parameters for better display
        adjustWebViewLayout();
        
        // Load template data
        if (getArguments() != null) {
            templateId = getArguments().getString("templateId");
            if (templateId != null) {
                viewModel.loadTemplate(templateId);
            } else {
                showError("Invalid template ID");
            }
        } else {
            showError("No arguments provided");
        }
        isViewCreated = true;
    }

    private void setupKeyboardVisibilityListener(View view) {
        Button shareButton = binding.shareButton;
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isAdded() || binding == null) return;
            
            Rect r = new Rect();
            view.getWindowVisibleDisplayFrame(r);
            int screenHeight = view.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) { // Keyboard is shown
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.GONE);
                }
                if (shareButton != null) {
                    shareButton.setVisibility(View.GONE);
                }
                // Reduce WebView width when keyboard is shown
                if (binding.templatePreview != null) {
                    ViewGroup.LayoutParams params = binding.templatePreview.getLayoutParams();
                    params.width = (int) (view.getWidth() * 0.8f); // 80% of screen width
                    binding.templatePreview.setLayoutParams(params);
                }
            } else { // Keyboard is hidden
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.VISIBLE);
                }
                if (shareButton != null) {
                    shareButton.setVisibility(View.VISIBLE);
                }
                // Restore WebView width when keyboard is hidden
                if (binding.templatePreview != null) {
                    ViewGroup.LayoutParams params = binding.templatePreview.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    binding.templatePreview.setLayoutParams(params);
                }
            }
        });

        // Add touch listener to the root view
        binding.getRoot().setOnTouchListener((v, event) -> {
            // Check if the touch is on an input field
            View focusedView = requireActivity().getCurrentFocus();
            if (focusedView instanceof EditText) {
                // If touch is on an input field, don't hide keyboard
                return false;
            }
            
            // If touch is not on an input field, hide keyboard
            hideKeyboard();
            return false;
        });
        
        // Add touch listener to the WebView container
        binding.templatePreview.setOnTouchListener((v, event) -> {
            // Check if the touch is on an input field
            View focusedView = requireActivity().getCurrentFocus();
            if (focusedView instanceof EditText) {
                // If touch is on an input field, don't hide keyboard
                return false;
            }
            
            // If touch is not on an input field, hide keyboard
            hideKeyboard();
            return false;
        });
        
        // Add touch listener to the WebView
        binding.webView.setOnTouchListener((v, event) -> {
            // Check if the touch is on an input field
            View focusedView = requireActivity().getCurrentFocus();
            if (focusedView instanceof EditText) {
                // If touch is on an input field, don't hide keyboard
                return false;
            }
            
            // If touch is not on an input field, hide keyboard
            hideKeyboard();
            return false;
        });
        
        // Add focus change listeners to input fields to prevent keyboard hiding
        binding.recipientNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // When input field gets focus, don't hide keyboard
                return;
            }
        });
        
        binding.senderNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // When input field gets focus, don't hide keyboard
                return;
            }
        });
    }

    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            View currentFocus = requireActivity().getCurrentFocus();
            
            // Clear focus from any EditText
            if (binding.recipientNameInput != null) {
                binding.recipientNameInput.clearFocus();
            }
            if (binding.senderNameInput != null) {
                binding.senderNameInput.clearFocus();
            }
            
            // Hide keyboard
            if (currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            } else {
                // If no focus, try hiding using the root view
                View rootView = binding.getRoot();
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
            }
            
            // Ensure WebView width is restored to full width
            if (binding.templatePreview != null) {
                binding.templatePreview.post(() -> {
                    ViewGroup.LayoutParams params = binding.templatePreview.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    binding.templatePreview.setLayoutParams(params);
                });
            }
            
            Log.d(TAG, "Keyboard hidden and WebView width adjusted");
        } catch (Exception e) {
            Log.e(TAG, "Error hiding keyboard: " + e.getMessage());
        }
    }

    private void setupViewModel() {
        // viewModel is already initialized in onCreate
    }

    private void setupWebView() {
        
        if (binding != null) {
            // Configure WebView for optimal rendering
            android.webkit.WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            webSettings.setDefaultTextEncodingName("UTF-8");
            webSettings.setLoadsImagesAutomatically(true);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            webSettings.setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
            
            // Clear any existing cache
            binding.webView.clearCache(true);
            binding.webView.clearHistory();
            
            // Add improved styling
            binding.webView.setBackgroundColor(android.graphics.Color.WHITE);
            binding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            binding.webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            
            // Fix rendering issues by setting content width
            binding.webView.evaluateJavascript(
                "document.getElementsByTagName('meta')['viewport'].content = " +
                "'width=device-width, initial-scale=1.0, maximum-scale=1.0';", null);
            
            // Add a WebViewClient to handle page loading and fix rendering
            binding.webView.setWebViewClient(new android.webkit.WebViewClient() {
                @Override
                public void onPageStarted(android.webkit.WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (binding != null) {
                        binding.webViewProgress.setVisibility(View.VISIBLE);
                    }
                }
                
                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (binding != null) {
                        binding.webViewProgress.setVisibility(View.GONE);
                        
                        // Force proper rendering by injecting viewport scale fix
                        String javascript = 
                            "var meta = document.createElement('meta');" +
                            "meta.name = 'viewport';" +
                            "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0';" +
                            "document.getElementsByTagName('head')[0].appendChild(meta);" +
                            "document.body.style.width = '100%';" +
                            "document.body.style.margin = '0';" +
                            "document.body.style.padding = '0';";
                        view.evaluateJavascript(javascript, null);
                    }
                }
                
                @Override
                public void onReceivedError(android.webkit.WebView view, android.webkit.WebResourceRequest request, 
                                           android.webkit.WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    if (binding != null) {
                        binding.webViewProgress.setVisibility(View.GONE);
                        showError("Error loading template: " + error.getDescription());
                    }
                }
            });
            
            // Add border styling to WebView container
            if (binding.templatePreview != null) {
                binding.templatePreview.setElevation(8f);
                binding.templatePreview.setBackgroundColor(android.graphics.Color.WHITE);
                
                // Add rounded corners to WebView container
                binding.templatePreview.setClipToOutline(true);
                try {
                    binding.templatePreview.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, android.graphics.Outline outline) {
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 
                                    getResources().getDimensionPixelSize(R.dimen.corner_radius));
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error setting outline provider", e);
                }
                
                // Add subtle border for better definition
                binding.templatePreview.setForeground(createBorderDrawable());
            }
            
            // Disable local storage
            binding.webView.getSettings().setDatabaseEnabled(false);
            
            templateRenderer = new TemplateRenderer(binding.webView, this);
        }
    }
    
    /**
     * Create a border drawable for WebView container
     */
    private android.graphics.drawable.Drawable createBorderDrawable() {
        float density = getResources().getDisplayMetrics().density;
        float cornerRadius = getResources().getDimensionPixelSize(R.dimen.corner_radius);
        
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(cornerRadius);
        drawable.setStroke((int)(1 * density), android.graphics.Color.parseColor("#E0E0E0"));
        
        return drawable;
    }

    private void setupInputListeners() {
        if (binding == null) return;

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                scheduleNameUpdate();
            }
        };
        
        // Add visual styling for input fields
        styleInputField(binding.recipientNameInput, "Recipient Name");
        styleInputField(binding.senderNameInput, "Sender Name");
        
        binding.recipientNameInput.addTextChangedListener(textWatcher);
        binding.senderNameInput.addTextChangedListener(textWatcher);
    }
    
    /**
     * Apply consistent styling to input fields
     */
    private void styleInputField(android.widget.EditText editText, String hintText) {
        if (editText == null) return;
        
        // Set hint text
        editText.setHint(hintText);
        
        // Apply text color and size
        editText.setTextColor(android.graphics.Color.parseColor("#333333"));
        editText.setHintTextColor(android.graphics.Color.parseColor("#9E9E9E"));
        editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        
        // Add padding
        int paddingDp = 16;
        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int)(paddingDp * density);
        editText.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        
        // Add background with rounded corners programmatically
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(8 * density);
        shape.setColor(android.graphics.Color.parseColor("#F5F5F5"));
        shape.setStroke(2, android.graphics.Color.parseColor("#E0E0E0"));
        editText.setBackground(shape);
        
        // Add animation effect for focus
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // When focused
                android.graphics.drawable.GradientDrawable focusedShape = new android.graphics.drawable.GradientDrawable();
                focusedShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                focusedShape.setCornerRadius(8 * density);
                focusedShape.setColor(android.graphics.Color.parseColor("#F5F5F5"));
                focusedShape.setStroke(2, android.graphics.Color.parseColor("#5C6BC0"));
                editText.setBackground(focusedShape);
                
                // Add subtle animation
                editText.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
            } else {
                // When not focused
                android.graphics.drawable.GradientDrawable unfocusedShape = new android.graphics.drawable.GradientDrawable();
                unfocusedShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                unfocusedShape.setCornerRadius(8 * density);
                unfocusedShape.setColor(android.graphics.Color.parseColor("#F5F5F5"));
                unfocusedShape.setStroke(2, android.graphics.Color.parseColor("#E0E0E0"));
                editText.setBackground(unfocusedShape);
                
                // Return to normal size
                editText.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        });
    }

    private void scheduleNameUpdate() {
        if (pendingNameUpdate != null) {
            backgroundHandler.removeCallbacks(pendingNameUpdate);
        }

        pendingNameUpdate = () -> {
            if (!isViewCreated || !isAdded() || binding == null) return;

            String recipientName = binding.recipientNameInput.getText().toString();
            String senderName = binding.senderNameInput.getText().toString();
            
            mainHandler.post(() -> {
                try {
                    viewModel.setRecipientName(recipientName);
                    viewModel.setSenderName(senderName);
                    
                    if (templateRenderer != null) {
                        templateRenderer.setRecipientName(recipientName);
                        templateRenderer.setSenderName(senderName);
                        
                        // Update the customized HTML in the ViewModel
                        String customizedHtml = templateRenderer.getCustomizedHtml();
                        viewModel.setCustomizedHtml(customizedHtml);
                    }
                    
                    updateShareButton(recipientName, senderName);
                } catch (Exception e) {
                    showError("Error updating names: " + e.getMessage());
                }
            });
        };

        backgroundHandler.postDelayed(pendingNameUpdate, TEXT_CHANGE_DELAY);
    }

    private void updateShareButton(String recipientName, String senderName) {
        if (binding == null) return;

        try {
            boolean hasRecipient = recipientName != null && !recipientName.trim().isEmpty();
            boolean hasSender = senderName != null && !senderName.trim().isEmpty();
            boolean enabled = hasRecipient && hasSender;
            
            Log.d(TAG, "Updating share button - recipient: " + hasRecipient + 
                      ", sender: " + hasSender + 
                      ", enabled: " + enabled);

            binding.shareButton.setEnabled(hasRecipient && hasSender);
        } catch (Exception e) {
            Log.e(TAG, "Error updating share button", e);
            binding.shareButton.setEnabled(false);
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        // Style the share button
        styleShareButton(binding.shareButton);

        binding.shareButton.setOnClickListener(v -> {
            Log.d(TAG, "Share button clicked");
            if (binding != null && binding.shareButton.isEnabled()) {
                String recipientName = binding.recipientNameInput.getText().toString();
                String senderName = binding.senderNameInput.getText().toString();
                
                // Update the customized HTML before saving
                if (templateRenderer != null) {
                    String customizedHtml = templateRenderer.getCustomizedHtml();
                    viewModel.setCustomizedHtml(customizedHtml);
                    Log.d(TAG, "Updated customizedHtml before saving: " + 
                          (customizedHtml != null ? customizedHtml.substring(0, Math.min(50, customizedHtml.length())) + "..." : "null"));
                }
                
                Log.d(TAG, "Preparing to share template with recipient: " + recipientName + ", sender: " + senderName);
                
                // Save wish directly since ads are disabled
                viewModel.saveWish();
            }
        });

        binding.customizeButton.setVisibility(View.GONE);

        View rootView = binding.getRoot();
        rootView.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        binding.webView.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        binding.templatePreview.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });
    }

    private void setupObservers() {
        viewModel.getTemplate().observe(getViewLifecycleOwner(), template -> {
            if (template != null) {
                // Initialize template renderer with the correct constructor
                templateRenderer = new TemplateRenderer(binding.webView, this);
                
                // Track template category view
                if (template.getCategory() != null) {
                    UserRepository.getInstance(requireContext()).trackCategoryClick(template.getCategory());
                }
                
                // Render template using the correct method
                templateRenderer.renderTemplate(template);
            }
        });
        
        // Observe loading state - use the existing binding fields
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && isAdded()) {
                binding.loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
        
        // Observe error messages - use the existing error handling
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && binding != null && isAdded()) {
                showError(error);
            }
        });

        viewModel.getWishSaved().observe(getViewLifecycleOwner(), shortCode -> {
            if (shortCode != null && binding != null && isAdded()) {
                // Navigate to shared wish view
                TemplateDetailFragmentDirections.ActionTemplateDetailToSharedWish action =
                    TemplateDetailFragmentDirections.actionTemplateDetailToSharedWish(shortCode);
                Navigation.findNavController(requireView()).navigate(action);
            }
        });
    }

    private void showError(String message) {
        if (binding != null && isAdded()) {
            mainHandler.post(() -> {
                try {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                } catch (Exception ignored) {
                    // View might be detached
                }
            });
        }
    }

    @Override
    public void onRenderComplete() {
        mainHandler.post(() -> {
            if (isViewCreated && isAdded() && binding != null) {
                try {
                    // Inject additional CSS to fix potential display issues
                    String fixCss = 
                        "body{width:100% !important;margin:0 !important;padding:0 !important;}" +
                        "img{max-width:100% !important;height:auto !important;}" +
                        "table{width:100% !important;}" +
                        "div{max-width:100% !important;}";
                    
                    // Apply the CSS fix
                    String injectScript = 
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.appendChild(document.createTextNode('" + fixCss + "'));" +
                        "if (document.head) document.head.appendChild(style);";
                    
                    if (binding.webView != null) {
                        binding.webView.evaluateJavascript(injectScript, null);
                    }
                    
                    // Add subtle animation without flashing
                    if (binding.templatePreview != null && binding.templatePreview.getAlpha() == 0f) {
                        binding.templatePreview.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                    }
                    
                    scheduleNameUpdate();
                    
                    // Update the customized HTML in the ViewModel
                    if (templateRenderer != null) {
                        String customizedHtml = templateRenderer.getCustomizedHtml();
                        viewModel.setCustomizedHtml(customizedHtml);
                    }
                    
                    // Add the template to recently viewed for analytics
                    if (templateId != null) {
                        try {
                            android.content.SharedPreferences prefs = requireActivity()
                                .getSharedPreferences("template_prefs", android.content.Context.MODE_PRIVATE);
                            android.content.SharedPreferences.Editor editor = prefs.edit();
                            
                            // Store the current time as last viewed time
                            editor.putLong("template_" + templateId + "_last_viewed", System.currentTimeMillis());
                            editor.apply();
                        } catch (Exception e) {
                            Log.e(TAG, "Error storing template view time", e);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in onRenderComplete: " + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void onRenderError(String error) {
        showError(error);
    }

    @Override
    public void onLoadingStateChanged(boolean isLoading) {
        mainHandler.post(() -> {
            if (binding != null && isAdded()) {
                binding.webViewProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Start tracking analytics for real-time viewers
     */
    private void startAnalyticsTracking() {
        if (templateId == null || isTracking) return;
        
        // Track template view once
        AnalyticsUtils.trackTemplateView(templateId);
        
        // Track active viewer
        AnalyticsUtils.trackViewerActive(templateId);
        
        // Store start time for duration calculation
        viewStartTime = System.currentTimeMillis();
        isTracking = true;
        
        // Setup heartbeat to track active viewers
        analyticsHeartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !isViewCreated) return;
                
                // Track active viewer again to maintain real-time count
                AnalyticsUtils.trackViewerActive(templateId);
                
                // Schedule next heartbeat
                mainHandler.postDelayed(this, ANALYTICS_HEARTBEAT_INTERVAL);
            }
        };
        
        // Start heartbeat
        mainHandler.postDelayed(analyticsHeartbeatRunnable, ANALYTICS_HEARTBEAT_INTERVAL);
        
        Log.d(TAG, "Started analytics tracking for template: " + templateId);
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
        if (templateId != null) {
            AnalyticsUtils.trackViewerInactive(templateId, durationSeconds);
        }
        
        isTracking = false;
        Log.d(TAG, "Stopped analytics tracking for template: " + templateId + ", duration: " + durationSeconds + "s");
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Resume analytics tracking if needed
        if (templateId != null && !isTracking) {
            startAnalyticsTracking();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Stop analytics tracking
        stopAnalyticsTracking();
        
        // Save current input state
        if (binding != null) {
            viewModel.setRecipientName(binding.recipientNameInput.getText().toString());
            viewModel.setSenderName(binding.senderNameInput.getText().toString());
            
            // Update the customized HTML in the ViewModel
            if (templateRenderer != null) {
                String customizedHtml = templateRenderer.getCustomizedHtml();
                viewModel.setCustomizedHtml(customizedHtml);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clear any pending updates
        if (pendingNameUpdate != null) {
            backgroundHandler.removeCallbacks(pendingNameUpdate);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop analytics tracking if still running
        stopAnalyticsTracking();
        
        isViewCreated = false;
        
        // Cleanup WebView more thoroughly
        if (binding != null && binding.webView != null) {
            binding.webView.stopLoading();
            binding.webView.clearCache(true);
            binding.webView.clearHistory();
            binding.webView.clearFormData();
            binding.webView.clearSslPreferences();
            binding.webView.removeJavascriptInterface("Android");
            binding.webView.destroy();
        }
        
        // Re-enable bottom navigation when leaving
        if (bottomNav != null) {
            bottomNav.setEnabled(true);
            bottomNav.setVisibility(View.VISIBLE);
        }
        
        // Remove any pending callbacks
        if (pendingNameUpdate != null) {
            backgroundHandler.removeCallbacks(pendingNameUpdate);
            pendingNameUpdate = null;
        }
        
        binding = null;
        templateRenderer = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop analytics tracking if still running
        stopAnalyticsTracking();
        
        // Remove observers
        if (isAdded()) {
            viewModel.getTemplate().removeObservers(this);
            viewModel.getError().removeObservers(this);
            viewModel.isLoading().removeObservers(this);
            viewModel.getWishSaved().removeObservers(this);
        }
        
        // Clear handlers
        mainHandler.removeCallbacksAndMessages(null);
        backgroundHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Apply styling to the share button
     */
    private void styleShareButton(Button button) {
        if (button == null) return;
        
        // Set text
        button.setText("SHARE");
        button.setAllCaps(true);
        
        // Set text appearance
        button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        // Create gradient background
        float density = getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable enabledShape = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[] {
                        android.graphics.Color.parseColor("#3949AB"), 
                        android.graphics.Color.parseColor("#5C6BC0")
                });
        enabledShape.setCornerRadius(8 * density);
        
        // Create disabled state
        android.graphics.drawable.GradientDrawable disabledShape = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[] {
                        android.graphics.Color.parseColor("#BDBDBD"),
                        android.graphics.Color.parseColor("#E0E0E0")
                });
        disabledShape.setCornerRadius(8 * density);
        
        // Create state list drawable
        android.graphics.drawable.StateListDrawable stateListDrawable = new android.graphics.drawable.StateListDrawable();
        stateListDrawable.addState(new int[] {-android.R.attr.state_enabled}, disabledShape);
        stateListDrawable.addState(new int[] {}, enabledShape);
        
        // Apply background
        button.setBackground(stateListDrawable);
        
        // Set text color
        button.setTextColor(android.graphics.Color.WHITE);
        
        // Add padding
        int paddingDp = 16;
        int paddingPx = (int)(paddingDp * density);
        button.setPadding(paddingPx, paddingPx/2, paddingPx, paddingPx/2);
        
        // Add elevation effect
        button.setElevation(4 * density);
        
        // Add ripple effect
        android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#42A5F5"));
        android.graphics.drawable.RippleDrawable rippleDrawable = new android.graphics.drawable.RippleDrawable(
                rippleColor, stateListDrawable, null);
        button.setBackground(rippleDrawable);
        
        // Add click animation
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    button.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            // Return false to allow normal click processing
            return false;
        });
    }

    /**
     * Adjust WebView layout to fix common display issues
     */
    private void adjustWebViewLayout() {
        if (binding == null || binding.templatePreview == null) return;
        
        try {
            // Get screen dimensions
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;
            
            binding.templatePreview.post(() -> {
                try {
                    int width = binding.templatePreview.getWidth();
                    if (width > 0) {
                        // Calculate desired height (70% of screen height)
                        int desiredHeight = (int) (screenHeight * 0.7);
                        
                        // Apply padding inside the template preview container
                        int padding = getResources().getDimensionPixelSize(R.dimen.small_padding);
                        binding.templatePreview.setPadding(padding, padding, padding, padding);
                        
                        // Set the CardView height
                        ViewGroup.LayoutParams cardParams = binding.templatePreview.getLayoutParams();
                        cardParams.height = desiredHeight;
                        binding.templatePreview.setLayoutParams(cardParams);
                        
                        // Ensure the WebView fills its container while maintaining padding
                        ViewGroup.LayoutParams webViewParams = binding.webView.getLayoutParams();
                        webViewParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        webViewParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        binding.webView.setLayoutParams(webViewParams);
                        
                        // Add margin to the CardView for better spacing
                        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) binding.templatePreview.getLayoutParams();
                        int margin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                        marginParams.setMargins(margin, margin, margin, margin);
                        binding.templatePreview.setLayoutParams(marginParams);
                        
                        Log.d(TAG, "Adjusted template preview height to: " + desiredHeight);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error adjusting layout parameters", e);
                }
            });
            
            // Improve content scaling in WebView with better viewport settings
            binding.webView.getSettings().setLoadWithOverviewMode(true);
            binding.webView.getSettings().setUseWideViewPort(true);
            binding.webView.getSettings().setBuiltInZoomControls(false);
            binding.webView.getSettings().setSupportZoom(false);
            
            // Set background color to prevent white flash
            binding.webView.setBackgroundColor(android.graphics.Color.parseColor("#FAFAFA"));
            
            // Make sure the WebView does not interfere with scroll behavior
            binding.webView.setOnTouchListener((v, event) -> {
                // Forward touch events to parent if we're at the edge
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // Disallow parent intercepting touch events
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        // Allow parent to intercept touch events again
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                
                // Pass to WebView for handling
                return false;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting WebView layout", e);
        }
    }
    
    /**
     * Fix and refresh WebView display
     */
    private void refreshWebViewDisplay() {
        if (binding == null || binding.webView == null) return;
        
        try {
            Log.d(TAG, "Refreshing WebView display");
            mainHandler.post(() -> {
                // Force re-layout without blinking
                binding.webView.invalidate();
                
                // Apply comprehensive JavaScript fixes for content sizing and styling
                String fixScript = 
                    "javascript:(function() {" +
                    "  var body = document.body;" +
                    "  if (!body) return;" + // Check if body exists
                    "  body.style.width = '100%';" +
                    "  body.style.margin = '0';" +
                    "  body.style.padding = '8px';" +
                    "  body.style.boxSizing = 'border-box';" +
                    "  body.style.overflowX = 'hidden';" +
                    
                    // Ensure images scale properly and maintain aspect ratio
                    "  var imgs = document.getElementsByTagName('img');" +
                    "  for(var i = 0; i < imgs.length; i++) {" +
                    "    imgs[i].style.maxWidth = '100%';" +
                    "    imgs[i].style.height = 'auto';" +
                    "    imgs[i].style.display = 'block';" +
                    "    if (imgs[i].width > document.body.clientWidth) {" +
                    "      imgs[i].style.width = '100%';" +
                    "    }" +
                    "  }" +
                    
                    // Make tables responsive
                    "  var tables = document.getElementsByTagName('table');" +
                    "  for(var i = 0; i < tables.length; i++) {" +
                    "    tables[i].style.width = '100%';" +
                    "    tables[i].style.maxWidth = '100%';" +
                    "    tables[i].style.tableLayout = 'fixed';" +
                    "  }" +
                    
                    // Fix any div elements that might have fixed widths
                    "  var divs = document.getElementsByTagName('div');" +
                    "  for(var i = 0; i < divs.length; i++) {" +
                    "    if (parseInt(divs[i].style.width) > document.body.clientWidth) {" +
                    "      divs[i].style.width = '100%';" +
                    "      divs[i].style.maxWidth = '100%';" +
                    "    }" +
                    "  }" +
                    
                    // Add meta viewport if missing - with safer check
                    "  var head = document.head;" +
                    "  if (head) {" +
                    "    var found = false;" +
                    "    var metas = head.getElementsByTagName('meta');" +
                    "    for(var i = 0; i < metas.length; i++) {" +
                    "      if (metas[i].name === 'viewport') {" +
                    "        found = true;" +
                    "        metas[i].content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0';" +
                    "      }" +
                    "    }" +
                    "    if (!found) {" +
                    "      var meta = document.createElement('meta');" +
                    "      meta.name = 'viewport';" +
                    "      meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0';" +
                    "      head.appendChild(meta);" +
                    "    }" +
                    "  }" +
                    
                    // Force reflow/repaint after changes without blinking
                    "  setTimeout(function() {" +
                    "    document.documentElement.style.visibility = 'visible';" +
                    "  }, 50);" +
                    
                    "  console.log('WebView display refresh complete');" +
                    "})()";
                
                // First set visibility to hidden to prevent flash
                binding.webView.evaluateJavascript(
                    "document.documentElement.style.visibility = 'hidden';", null);
                
                // Then apply the fixes
                binding.webView.evaluateJavascript(fixScript.replace("javascript:", ""), null);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing WebView display: " + e.getMessage(), e);
        }
    }

    private void navigateToHome() {
        if (isAdded()) {
            Navigation.findNavController(requireView()).navigate(R.id.action_template_detail_to_home);
        }
    }
}
