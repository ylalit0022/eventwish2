package com.ds.eventwish.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.view.WindowManager;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ds.eventwish.ui.base.BaseFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentTemplateDetailBinding;
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.ui.render.TemplateRenderer;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.EdgeToEdgeManager;
import com.ds.eventwish.utils.PictureInPictureManager;
import com.ds.eventwish.utils.AppExecutors;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TemplateDetailFragment extends BaseFragment implements TemplateRenderer.TemplateRenderListener, PictureInPictureManager.PipModeCallback {
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
    
    // Edge-to-edge and PiP support
    private EdgeToEdgeManager edgeToEdgeManager;
    private PictureInPictureManager pipManager;
    private FloatingActionButton pipFab;
    private boolean isInPipMode = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // This fragment requires authentication (enforced by BaseFragment)
        viewModel = new ViewModelProvider(this).get(TemplateDetailViewModel.class);
        
        // Initialize display managers
        edgeToEdgeManager = EdgeToEdgeManager.getInstance();
        pipManager = PictureInPictureManager.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTemplateDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated called");

        // Safely hide the action bar
        if (getActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }

        // Get bottom navigation from activity
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            setupWindowInsets();
            setupPictureInPicture();
        }
        
        // Set current user's name as sender name
        UserRepository userRepository = UserRepository.getInstance(requireContext());
        String currentUserId = userRepository.getCurrentUserId();
        if (currentUserId != null) {
            userRepository.getUserProfile(currentUserId).observe(getViewLifecycleOwner(), user -> {
                if (user != null && user.getDisplayName() != null) {
                    binding.senderNameInput.setText(user.getDisplayName());
                } else {
                    // Fallback to Firebase user
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null && firebaseUser.getDisplayName() != null) {
                        binding.senderNameInput.setText(firebaseUser.getDisplayName());
                    }
                }
            });
        } else {
            // Fallback to Firebase user
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null && firebaseUser.getDisplayName() != null) {
                binding.senderNameInput.setText(firebaseUser.getDisplayName());
            }
        }
        
        setupWebView();
        setupInputListeners();
        setupObservers();
        setupClickListeners();
        adjustWebViewLayout();
        
        // Load template data
        Bundle args = getArguments();
        Log.d(TAG, "Arguments bundle: " + (args != null ? "present" : "null"));
        if (args != null) {
            templateId = args.getString("templateId");
            Log.d(TAG, "Template ID from arguments: " + templateId);
            if (templateId != null) {
                Log.d(TAG, "Loading template with ID: " + templateId);
                viewModel.loadTemplate(templateId);
            } else {
                Log.e(TAG, "Invalid template ID: null");
                showError("Invalid template ID");
            }
        } else {
            Log.e(TAG, "No arguments bundle provided");
            showError("No arguments provided");
        }
        isViewCreated = true;
    }

    private void setupWindowInsets() {
        if (binding == null || !isAdded()) return;

        View rootView = binding.getRoot();
        WindowCompat.setDecorFitsSystemWindows(requireActivity().getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            boolean isKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            int imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navigationHeight = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            
            mainHandler.post(() -> {
                if (isKeyboardVisible) {
                    // When keyboard is visible
                    if (bottomNav != null) {
                        bottomNav.setVisibility(View.GONE);
                    }
                    if (binding.shareButton != null) {
                        binding.shareButton.setVisibility(View.GONE);
                    }
                    if (binding.backButton != null) {
                        binding.backButton.setVisibility(View.GONE);
                    }
                    // Hide PiP FAB when keyboard is visible
                    if (pipFab != null) {
                        pipFab.setVisibility(View.GONE);
                    }
                    
                    // Adjust content padding
                    binding.contentLayout.setPadding(
                        binding.contentLayout.getPaddingLeft(),
                        binding.contentLayout.getPaddingTop(),
                        binding.contentLayout.getPaddingRight(),
                        imeHeight
                    );
                } else {
                    // When keyboard is hidden
                    if (bottomNav != null && !isInPipMode) {
                        bottomNav.setVisibility(View.VISIBLE);
                    }
                    if (binding.shareButton != null && !isInPipMode) {
                        binding.shareButton.setVisibility(View.VISIBLE);
                    }
                    if (binding.backButton != null && !isInPipMode) {
                        binding.backButton.setVisibility(View.VISIBLE);
                    }
                    // Show PiP FAB when keyboard is hidden and not in PiP mode
                    if (pipFab != null && !isInPipMode && 
                        pipManager != null && pipManager.isPictureInPictureSupported(requireContext())) {
                        pipFab.setVisibility(View.VISIBLE);
                    }
                    
                    // Reset content padding
                    binding.contentLayout.setPadding(
                        binding.contentLayout.getPaddingLeft(),
                        binding.contentLayout.getPaddingTop(),
                        binding.contentLayout.getPaddingRight(),
                        navigationHeight
                    );
                }
            });

            return windowInsets;
        });
    }

    private void hideKeyboard() {
        if (getActivity() == null || !isAdded()) return;

        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            
            // Ensure UI elements are visible after keyboard is hidden (if not in PiP mode)
            if (!isInPipMode) {
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.VISIBLE);
                }
                if (binding != null) {
                    if (binding.shareButton != null) {
                        binding.shareButton.setVisibility(View.VISIBLE);
                    }
                    if (binding.backButton != null) {
                        binding.backButton.setVisibility(View.VISIBLE);
                    }
                }
                if (pipFab != null && pipManager != null && 
                    pipManager.isPictureInPictureSupported(requireContext())) {
                    pipFab.setVisibility(View.VISIBLE);
                }
            }
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
                    mainHandler.post(() -> {
                        if (binding != null && isAdded()) {
                            // Show loading indicators
                            binding.webViewProgressContainer.setVisibility(View.VISIBLE);
                            binding.webView.setVisibility(View.INVISIBLE); // Hide WebView while loading
                            Log.d(TAG, "WebView loading started: " + url);
                        }
                    });
                }
                
                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    super.onPageFinished(view, url);
                    mainHandler.post(() -> {
                        if (binding != null && isAdded()) {
                            // Hide loading indicators
                            binding.webViewProgressContainer.setVisibility(View.GONE);
                            binding.webView.setVisibility(View.VISIBLE); // Show WebView after loading
                            
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
                            
                            Log.d(TAG, "WebView loading completed: " + url);
                        }
                    });
                }
                
                @Override
                public void onReceivedError(android.webkit.WebView view, android.webkit.WebResourceRequest request, 
                                           android.webkit.WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    mainHandler.post(() -> {
                        if (binding != null && isAdded()) {
                            // Hide loading indicators
                            binding.webViewProgressContainer.setVisibility(View.GONE);
                            binding.webView.setVisibility(View.VISIBLE);
                            
                            // Show error message
                            showError("Error loading template: " + error.getDescription());
                            Log.e(TAG, "WebView loading error: " + error.getDescription());
                        }
                    });
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
                
                // Track share button click
                String templateId = viewModel.getTemplateId();
                AnalyticsUtils.trackShareButtonClick(
                    "shareButton", 
                    "TemplateDetailFragment", 
                    templateId
                );
                
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

        // Add back button click listener and handle visibility
        binding.backButton.setOnClickListener(v -> {
            // Navigate back
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        // Set back button visibility based on navigation bar status
        updateBackButtonVisibility();

        binding.customizeButton.setVisibility(View.GONE);

        // Setup touch listeners for keyboard dismissal
        View rootView = binding.getRoot();
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Check if touch is outside of input fields
                if (!isTouchInsideView(event, binding.recipientNameInput) && 
                    !isTouchInsideView(event, binding.senderNameInput)) {
                    hideKeyboard();
                    return true; // Consume the touch event
                }
            }
            return false;
        });

        // Setup input field touch listeners with proper focus handling
        if (binding.recipientNameInput != null) {
            binding.recipientNameInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    return true;
                }
                return false;
            });
        }

        if (binding.senderNameInput != null) {
            binding.senderNameInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                    binding.recipientNameInput.requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Check if touch event is inside a view
     */
    private boolean isTouchInsideView(MotionEvent event, View view) {
        if (view == null) return false;
        
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        
        float x = event.getRawX();
        float y = event.getRawY();
        
        return (x >= viewLocation[0] && x <= (viewLocation[0] + view.getWidth()) &&
                y >= viewLocation[1] && y <= (viewLocation[1] + view.getHeight()));
    }

    /**
     * Check if device has navigation bar and if it's enabled
     */
    private boolean hasVisibleNavigationBar() {
        if (getActivity() == null) return true;
        
        android.content.res.Resources resources = getResources();
        int resourceId = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId > 0) {
            boolean hasNav = resources.getBoolean(resourceId);
            
            // Get the real device config
            android.view.WindowManager windowManager = getActivity().getWindowManager();
            android.view.Display display = windowManager.getDefaultDisplay();
            android.graphics.Point realSize = new android.graphics.Point();
            android.graphics.Point screenSize = new android.graphics.Point();
            
            try {
                // Get real size
                display.getRealSize(realSize);
                // Get screen size
                display.getSize(screenSize);
                
                // If real size is larger than screen size, navigation bar is present
                boolean hasNavBar = (realSize.y != screenSize.y) || (realSize.x != screenSize.x);
                
                // Check if navigation gestures are enabled
                int navBarInteractionMode = android.provider.Settings.Secure.getInt(
                    requireContext().getContentResolver(),
                    "navigation_mode",
                    0
                );
                
                // Return true if we have a nav bar and gestures are not enabled
                return hasNav && hasNavBar && navBarInteractionMode == 0;
            } catch (Exception e) {
                Log.e(TAG, "Error checking navigation bar status", e);
                return true; // Default to true to hide our back button
            }
        }
        return true; // Default to true to hide our back button
    }

    /**
     * Update back button visibility based on navigation bar status
     */
    private void updateBackButtonVisibility() {
        if (binding != null && binding.backButton != null) {
            boolean showBackButton = !hasVisibleNavigationBar();
            binding.backButton.setVisibility(showBackButton ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Back button visibility updated: " + (showBackButton ? "visible" : "gone"));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update back button visibility when configuration changes (e.g., rotation)
        updateBackButtonVisibility();
    }

    private void setupObservers() {
        if (!isAdded()) return;

        // Observe template data
        viewModel.getTemplate().observe(getViewLifecycleOwner(), template -> {
            Log.d(TAG, "Template data received: " + (template != null ? template.getId() : "null"));
            if (template != null && isViewCreated) {
                Log.d(TAG, "🎯 TEMPLATE TYPE DETECTED: " + template.getTemplateType());
                Log.d(TAG, "🎯 TEMPLATE TYPE ENUM: " + template.getTemplateTypeEnum());
                Log.d(TAG, "🎯 SUPPORTS EDITING: " + template.supportsEditing());
                Log.d(TAG, "🎯 REQUIRES MEDIA PLAYER: " + template.requiresMediaPlayer());
                Log.d(TAG, "🎯 IS IMAGE BASED: " + template.isImageBased());
                Log.d(TAG, "🎯 CAN RENDER: " + template.canRender());
                
                // Setup type-based rendering and interactions
                setupTemplateTypeInteractions(template);
                
                // Initialize template renderer with type awareness
                if (templateRenderer == null) {
                    templateRenderer = new TemplateRenderer(
                        binding.webView, 
                        this
                    );
                }
                
                // Load template content based on type
                loadTemplateByType(template);
                
                // Update UI based on template capabilities
                updateUIForTemplateType(template);
                
                startAnalyticsTracking();
            }
        });

        // Observe loading state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });

        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error observed: " + error);
                showError(error);
            }
        });

        viewModel.getWishSaved().observe(getViewLifecycleOwner(), shortCode -> {
            if (shortCode != null && binding != null && isAdded()) {
                hideLoading();
                // Navigate to shared wish view
                TemplateDetailFragmentDirections.ActionTemplateDetailToSharedWish action =
                    TemplateDetailFragmentDirections.actionTemplateDetailToSharedWish(shortCode);
                Navigation.findNavController(requireView()).navigate(action);
            }
        });
    }
    
    /**
     * Setup interactions based on template type
     * @param template Template object with type information
     */
    private void setupTemplateTypeInteractions(com.ds.eventwish.data.model.Template template) {
        if (template == null) return;
        
        com.ds.eventwish.data.model.Template.TemplateType type = template.getTemplateTypeEnum();
        Log.d(TAG, "🔧 Setting up interactions for template type: " + type);
        
        // Configure interaction capabilities
        switch (type) {
            case HTML:
                setupHtmlInteractions(template);
                break;
            case VIDEO:
                setupVideoInteractions(template);
                break;
            case IMAGE:
                setupImageInteractions(template);
                break;
            default:
                Log.w(TAG, "Unknown template type, defaulting to HTML interactions");
                setupHtmlInteractions(template);
                break;
        }
    }
    
    /**
     * Setup interactions for HTML templates
     * @param template HTML template
     */
    private void setupHtmlInteractions(com.ds.eventwish.data.model.Template template) {
        Log.d(TAG, "🌐 Setting up HTML template interactions");
        
        // Enable editing capabilities
        if (binding.senderNameInput != null) {
            binding.senderNameInput.setEnabled(true);
            binding.senderNameInput.setVisibility(View.VISIBLE);
        }
        if (binding.recipientNameInput != null) {
            binding.recipientNameInput.setEnabled(true);
            binding.recipientNameInput.setVisibility(View.VISIBLE);
        }
        
        // Configure WebView for interactive HTML
        if (binding.webView != null) {
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            Log.d(TAG, "✅ HTML template WebView configured for interactive editing");
        }
        
        // Show edit-related UI elements
        showEditingUI(true);
    }
    
    /**
     * Setup interactions for VIDEO templates
     * @param template Video template
     */
    private void setupVideoInteractions(com.ds.eventwish.data.model.Template template) {
        Log.d(TAG, "🎥 Setting up VIDEO template interactions");
        
        // Limited editing for video templates
        if (binding.senderNameInput != null) {
            binding.senderNameInput.setEnabled(true);
            binding.senderNameInput.setVisibility(View.VISIBLE);
        }
        if (binding.recipientNameInput != null) {
            binding.recipientNameInput.setEnabled(true);
            binding.recipientNameInput.setVisibility(View.VISIBLE);
        }
        
        // Configure WebView for video playback
        if (binding.webView != null) {
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setMediaPlaybackRequiresUserGesture(false);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            Log.d(TAG, "✅ VIDEO template WebView configured for media playback");
        }
        
        // Show limited editing UI
        showEditingUI(false);
        
        // Add video-specific controls if needed
        // TODO: Add video player controls, replace video option, etc.
    }
    
    /**
     * Setup interactions for IMAGE templates
     * @param template Image template
     */
    private void setupImageInteractions(com.ds.eventwish.data.model.Template template) {
        Log.d(TAG, "🖼️ Setting up IMAGE template interactions");
        
        // Basic editing for image templates
        if (binding.senderNameInput != null) {
            binding.senderNameInput.setEnabled(true);
            binding.senderNameInput.setVisibility(View.VISIBLE);
        }
        if (binding.recipientNameInput != null) {
            binding.recipientNameInput.setEnabled(true);
            binding.recipientNameInput.setVisibility(View.VISIBLE);
        }
        
        // Configure WebView for image display
        if (binding.webView != null) {
            WebSettings webSettings = binding.webView.getSettings();
            webSettings.setJavaScriptEnabled(false); // Images don't need JS
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            
            Log.d(TAG, "✅ IMAGE template WebView configured for image display");
        }
        
        // Show basic editing UI
        showEditingUI(false);
        
        // Add image-specific controls if needed
        // TODO: Add image editor, replace image option, filters, etc.
    }
    
    /**
     * Load template content based on its type
     * @param template Template to load
     */
    private void loadTemplateByType(com.ds.eventwish.data.model.Template template) {
        if (template == null || !template.canRender()) {
            Log.e(TAG, "❌ Template cannot be rendered: " + (template != null ? template.getId() : "null"));
            showError("Template cannot be displayed");
            return;
        }
        
        com.ds.eventwish.data.model.Template.TemplateType type = template.getTemplateTypeEnum();
        Log.d(TAG, "📥 Loading template content for type: " + type);
        
        switch (type) {
            case HTML:
                loadHtmlTemplate(template);
                break;
            case VIDEO:
                loadVideoTemplate(template);
                break;
            case IMAGE:
                loadImageTemplate(template);
                break;
            default:
                Log.w(TAG, "Unknown template type, attempting HTML load");
                loadHtmlTemplate(template);
                break;
        }
    }
    
    /**
     * Load HTML template content
     * @param template HTML template
     */
    private void loadHtmlTemplate(com.ds.eventwish.data.model.Template template) {
        Log.d(TAG, "🌐 Loading HTML template: " + template.getId());
        
        if (templateRenderer != null) {
            // Use existing renderer logic for HTML templates
            templateRenderer.renderTemplate(template);
            
            // Update names from input fields
            String senderName = binding.senderNameInput.getText().toString().trim();
            String recipientName = binding.recipientNameInput.getText().toString().trim();
            
            if (!senderName.isEmpty()) {
                templateRenderer.setSenderName(senderName);
            }
            if (!recipientName.isEmpty()) {
                templateRenderer.setRecipientName(recipientName);
            }
        } else {
            Log.e(TAG, "TemplateRenderer is null, cannot load HTML template");
        }
    }
    
    /**
     * Load VIDEO template content
     * @param template Video template
     */
    private void loadVideoTemplate(com.ds.eventwish.data.model.Template template) {
        try {
            Log.d(TAG, "🎥 Loading VIDEO template: " + template.getId());
            Log.d(TAG, "🎥 Template title: " + template.getTitle());
            Log.d(TAG, "🎥 Template category: " + template.getCategory());
            
            String videoUrl = template.getVideoUrl();
            Log.d(TAG, "🎥 Primary video URL: " + (videoUrl != null ? videoUrl : "NULL"));
            
            if (videoUrl == null || videoUrl.isEmpty()) {
                Log.w(TAG, "🎥 No video URL found, falling back to preview URL");
                videoUrl = template.getPreviewUrl();
                Log.d(TAG, "🎥 Preview URL fallback: " + (videoUrl != null ? videoUrl : "NULL"));
            }
            
            if (videoUrl != null && !videoUrl.isEmpty()) {
                Log.d(TAG, "🎥 Creating HTML wrapper for video playback");
                
                // Show loading state
                showLoading();
                
                // Create HTML wrapper for video
                String videoHtml = createVideoHtml(videoUrl, template);
                
                Log.d(TAG, "🎥 Loading video HTML into WebView");
                Log.d(TAG, "🎥 Video HTML preview: " + videoHtml.substring(0, Math.min(200, videoHtml.length())) + "...");
                
                binding.webView.loadDataWithBaseURL(null, videoHtml, "text/html", "UTF-8", null);
                
                Log.d(TAG, "✅ Video template loaded successfully with URL: " + videoUrl);
                
                // Update UI for video template
                updateUIForTemplateType(template);
                
            } else {
                Log.e(TAG, "❌ No valid video URL found for template: " + template.getId());
                Log.e(TAG, "❌ Checked videoUrl: " + template.getVideoUrl());
                Log.e(TAG, "❌ Checked previewUrl: " + template.getPreviewUrl());
                Log.e(TAG, "❌ Checked imageUrl: " + template.getImageUrl());
                
                // Try to show error gracefully
                try {
                    hideLoading();
                    showError("Video content not available for this template");
                    
                    // Fallback to showing template info without video
                    String fallbackHtml = createFallbackVideoHtml(template);
                    binding.webView.loadDataWithBaseURL(null, fallbackHtml, "text/html", "UTF-8", null);
                    
                } catch (Exception fallbackError) {
                    Log.e(TAG, "🎥 Error showing fallback content", fallbackError);
                    showError("Unable to load video template");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "🎥 CRITICAL ERROR loading video template: " + template.getId(), e);
            
            try {
                hideLoading();
                showError("Error loading video: " + e.getMessage());
            } catch (Exception errorHandlingError) {
                Log.e(TAG, "🎥 Error handling video load error", errorHandlingError);
            }
        }
    }
    
    /**
     * Create fallback HTML when video URL is not available
     * @param template Video template
     * @return HTML string for fallback display
     */
    private String createFallbackVideoHtml(com.ds.eventwish.data.model.Template template) {
        String senderName = binding.senderNameInput.getText().toString().trim();
        String recipientName = binding.recipientNameInput.getText().toString().trim();
        
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { margin: 0; padding: 20px; background: #f5f5f5; font-family: Arial, sans-serif; text-align: center; }" +
                ".error-container { background: white; border-radius: 12px; padding: 40px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                ".error-icon { font-size: 48px; color: #ff6b6b; margin-bottom: 16px; }" +
                ".error-title { font-size: 20px; font-weight: bold; color: #333; margin-bottom: 8px; }" +
                ".error-message { font-size: 14px; color: #666; margin-bottom: 20px; }" +
                ".template-info { margin-top: 20px; }" +
                ".sender { font-size: 16px; font-weight: bold; color: #333; }" +
                ".recipient { font-size: 14px; color: #666; margin-top: 8px; }" +
                "</style>" +
                "</head><body>" +
                "<div class='error-container'>" +
                "<div class='error-icon'>🎥</div>" +
                "<div class='error-title'>Video Not Available</div>" +
                "<div class='error-message'>The video content for this template is currently unavailable.</div>" +
                "<div class='template-info'>" +
                "<div class='sender'>From: " + (senderName.isEmpty() ? "Your Name" : senderName) + "</div>" +
                "<div class='recipient'>To: " + (recipientName.isEmpty() ? "Recipient Name" : recipientName) + "</div>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }
    
    /**
     * Load IMAGE template content
     * @param template Image template
     */
    private void loadImageTemplate(com.ds.eventwish.data.model.Template template) {
        Log.d(TAG, "🖼️ Loading IMAGE template: " + template.getId());
        
        String imageUrl = template.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.w(TAG, "No image URL found, falling back to preview URL");
            imageUrl = template.getPreviewUrl();
        }
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Create HTML wrapper for image
            String imageHtml = createImageHtml(imageUrl, template);
            binding.webView.loadDataWithBaseURL(null, imageHtml, "text/html", "UTF-8", null);
            Log.d(TAG, "✅ Image template loaded with URL: " + imageUrl);
        } else {
            Log.e(TAG, "❌ No valid image URL found for template: " + template.getId());
            showError("Image content not available");
        }
    }
    
    /**
     * Create HTML wrapper for video content
     * @param videoUrl URL of the video
     * @param template Template object
     * @return HTML string for video display
     */
    private String createVideoHtml(String videoUrl, com.ds.eventwish.data.model.Template template) {
        String senderName = binding.senderNameInput.getText().toString().trim();
        String recipientName = binding.recipientNameInput.getText().toString().trim();
        
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { margin: 0; padding: 20px; background: #f5f5f5; font-family: Arial, sans-serif; }" +
                "video { width: 100%; height: auto; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                ".template-info { text-align: center; margin: 20px 0; }" +
                ".sender { font-size: 18px; font-weight: bold; color: #333; }" +
                ".recipient { font-size: 16px; color: #666; margin-top: 8px; }" +
                "</style>" +
                "</head><body>" +
                "<div class='template-info'>" +
                "<div class='sender'>From: " + (senderName.isEmpty() ? "Your Name" : senderName) + "</div>" +
                "<div class='recipient'>To: " + (recipientName.isEmpty() ? "Recipient Name" : recipientName) + "</div>" +
                "</div>" +
                "<video controls autoplay muted loop>" +
                "<source src='" + videoUrl + "' type='video/mp4'>" +
                "Your browser does not support the video tag." +
                "</video>" +
                "</body></html>";
    }
    
    /**
     * Create HTML wrapper for image content
     * @param imageUrl URL of the image
     * @param template Template object
     * @return HTML string for image display
     */
    private String createImageHtml(String imageUrl, com.ds.eventwish.data.model.Template template) {
        String senderName = binding.senderNameInput.getText().toString().trim();
        String recipientName = binding.recipientNameInput.getText().toString().trim();
        
        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { margin: 0; padding: 20px; background: #f5f5f5; font-family: Arial, sans-serif; }" +
                "img { width: 100%; height: auto; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                ".template-info { text-align: center; margin: 20px 0; }" +
                ".sender { font-size: 18px; font-weight: bold; color: #333; }" +
                ".recipient { font-size: 16px; color: #666; margin-top: 8px; }" +
                "</style>" +
                "</head><body>" +
                "<div class='template-info'>" +
                "<div class='sender'>From: " + (senderName.isEmpty() ? "Your Name" : senderName) + "</div>" +
                "<div class='recipient'>To: " + (recipientName.isEmpty() ? "Recipient Name" : recipientName) + "</div>" +
                "</div>" +
                "<img src='" + imageUrl + "' alt='Template Image' />" +
                "</body></html>";
    }
    
    /**
     * Update UI elements based on template type capabilities
     * @param template Template object
     */
    private void updateUIForTemplateType(com.ds.eventwish.data.model.Template template) {
        com.ds.eventwish.data.model.Template.TemplateType type = template.getTemplateTypeEnum();
        Log.d(TAG, "🎨 Updating UI for template type: " + type);
        
        // Update share button text based on template type
        if (binding.shareButton != null) {
            String shareText = "Share";
            switch (type) {
                case VIDEO:
                    shareText = "Share Video";
                    break;
                case IMAGE:
                    shareText = "Share Image";
                    break;
                case HTML:
                default:
                    shareText = "Share Greeting";
                    break;
            }
            binding.shareButton.setText(shareText);
        }
        
        // Show/hide editing capabilities based on template type
        boolean supportsEditing = template.supportsEditing();
        showEditingUI(supportsEditing);
        
        Log.d(TAG, "✅ UI updated for template type: " + type + ", editing: " + supportsEditing);
    }
    
    /**
     * Show or hide editing UI elements
     * @param showEditing Whether to show editing capabilities
     */
    private void showEditingUI(boolean showEditing) {
        // For now, all templates support basic name editing
        // Future enhancement: different editing UIs for different types
        if (binding.senderNameInput != null) {
            binding.senderNameInput.setVisibility(View.VISIBLE);
        }
        if (binding.recipientNameInput != null) {
            binding.recipientNameInput.setVisibility(View.VISIBLE);
        }
        
        Log.d(TAG, "📝 Editing UI visibility: " + (showEditing ? "shown" : "limited"));
    }

    private void showLoading() {
        if (binding != null && isAdded()) {
            mainHandler.post(() -> {
                binding.webViewProgressContainer.setVisibility(View.VISIBLE);
                binding.webView.setVisibility(View.INVISIBLE);
            });
        }
    }

    private void hideLoading() {
        if (binding != null && isAdded()) {
            mainHandler.post(() -> {
                binding.webViewProgressContainer.setVisibility(View.GONE);
                binding.webView.setVisibility(View.VISIBLE);
            });
        }
    }

    @Override
    protected void showError(String message) {
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
                    hideLoading();
                    
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
        hideLoading();
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
        
        // Cleanup WebView
        if (binding != null && binding.webView != null) {
            binding.webView.stopLoading();
            binding.webView.clearCache(true);
            binding.webView.clearHistory();
            binding.webView.clearFormData();
            binding.webView.clearSslPreferences();
            binding.webView.removeJavascriptInterface("Android");
            binding.webView.destroy();
        }
        
        if (pendingNameUpdate != null) {
            backgroundHandler.removeCallbacks(pendingNameUpdate);
            pendingNameUpdate = null;
        }
        
        // Ensure bottom navigation is visible when leaving
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
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

    /**
     * Sets up picture-in-picture functionality for template viewing
     */
    private void setupPictureInPicture() {
        try {
            if (pipManager != null && pipManager.isPictureInPictureSupported(requireContext())) {
                // Create PiP FAB if not already in layout
                if (pipFab == null) {
                    pipFab = new FloatingActionButton(requireContext());
                    pipFab.setImageResource(R.drawable.ic_picture_in_picture_alt);
                    pipFab.setContentDescription("Enter Picture-in-Picture mode");
                    
                    // Style the FAB with festive theming
                    pipFab.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(
                        requireContext(), R.color.md_theme_light_primary));
                    pipFab.setImageTintList(androidx.core.content.ContextCompat.getColorStateList(
                        requireContext(), R.color.md_theme_light_onPrimary));
                    
                    // Position the FAB
                    if (binding.getRoot() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
                        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                            new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.WRAP_CONTENT);
                        params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
                        params.setMargins(0, 0, 
                            getResources().getDimensionPixelSize(R.dimen.fab_margin),
                            getResources().getDimensionPixelSize(R.dimen.fab_margin) + 
                            getResources().getDimensionPixelSize(R.dimen.bottom_nav_height));
                        pipFab.setLayoutParams(params);
                        
                        ((androidx.coordinatorlayout.widget.CoordinatorLayout) binding.getRoot()).addView(pipFab);
                    }
                    
                    // Set click listener
                    pipFab.setOnClickListener(v -> enterPictureInPictureMode());
                }
                
                Log.d(TAG, "Picture-in-picture setup completed");
            } else {
                Log.d(TAG, "Picture-in-picture not supported on this device");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up picture-in-picture", e);
        }
    }
    
    /**
     * Enters picture-in-picture mode for template viewing
     */
    private void enterPictureInPictureMode() {
        try {
            if (pipManager != null && getActivity() != null && binding.webView != null) {
                boolean success = pipManager.enterPictureInPictureMode(
                    requireActivity(), binding.webView, this);
                
                if (!success) {
                    // Fallback to activity-level PiP
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).enterPictureInPictureMode();
                    }
                }
                
                Log.d(TAG, "Attempted to enter PiP mode: " + success);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error entering picture-in-picture mode", e);
        }
    }
    
    /**
     * Enables enhanced edge-to-edge experience with dynamic theming
     */
    public void enableEnhancedEdgeToEdge() {
        try {
            if (edgeToEdgeManager != null && getActivity() != null) {
                // Enable edge-to-edge if not already enabled
                if (!edgeToEdgeManager.isEdgeToEdgeEnabled()) {
                    edgeToEdgeManager.enableEdgeToEdge(requireActivity(), true);
                }
                
                // Apply dynamic theming based on template content
                if (binding.webView != null) {
                    // Extract dominant color from WebView content (simplified approach)
                    int primaryColor = androidx.core.content.ContextCompat.getColor(
                        requireContext(), R.color.md_theme_light_primary);
                    edgeToEdgeManager.applyDynamicSystemBarTheming(requireActivity(), primaryColor);
                }
                
                Log.d(TAG, "Enhanced edge-to-edge enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling enhanced edge-to-edge", e);
        }
    }

    // PictureInPictureManager.PipModeCallback implementation
    @Override
    public void onEnterPictureInPicture() {
        try {
            isInPipMode = true;
            Log.d(TAG, "Entered picture-in-picture mode");
            
            // Hide UI elements that shouldn't be visible in PiP mode
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            if (binding.shareButton != null) {
                binding.shareButton.setVisibility(View.GONE);
            }
            if (binding.backButton != null) {
                binding.backButton.setVisibility(View.GONE);
            }
            if (pipFab != null) {
                pipFab.setVisibility(View.GONE);
            }
            if (binding.recipientNameInput != null) {
                binding.recipientNameInput.setVisibility(View.GONE);
            }
            if (binding.senderNameInput != null) {
                binding.senderNameInput.setVisibility(View.GONE);
            }
            
            // Optimize WebView for PiP
            if (pipManager != null && binding.webView != null) {
                pipManager.optimizeWebViewForPip(binding.webView);
            }
            
            // Enable enhanced edge-to-edge for PiP
            enableEnhancedEdgeToEdge();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onEnterPictureInPicture", e);
        }
    }
    
    @Override
    public void onExitPictureInPicture() {
        try {
            isInPipMode = false;
            Log.d(TAG, "Exited picture-in-picture mode");
            
            // Restore UI elements
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
            if (binding.shareButton != null) {
                binding.shareButton.setVisibility(View.VISIBLE);
            }
            if (binding.backButton != null) {
                binding.backButton.setVisibility(View.VISIBLE);
            }
            if (pipFab != null && pipManager != null && 
                pipManager.isPictureInPictureSupported(requireContext())) {
                pipFab.setVisibility(View.VISIBLE);
            }
            if (binding.recipientNameInput != null) {
                binding.recipientNameInput.setVisibility(View.VISIBLE);
            }
            if (binding.senderNameInput != null) {
                binding.senderNameInput.setVisibility(View.VISIBLE);
            }
            
            // Restore WebView settings
            if (pipManager != null && binding.webView != null) {
                pipManager.restoreWebViewFromPip(binding.webView);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onExitPictureInPicture", e);
        }
    }
    
    @Override
    public void onPipAction(String action) {
        try {
            Log.d(TAG, "PiP action received: " + action);
            
            switch (action) {
                case PictureInPictureManager.ACTION_SHARE:
                    // Trigger share functionality
                    if (binding.shareButton != null) {
                        binding.shareButton.performClick();
                    }
                    break;
                case PictureInPictureManager.ACTION_SAVE:
                    // Trigger save functionality (if available)
                    // You can implement save logic here
                    break;
                default:
                    Log.w(TAG, "Unknown PiP action: " + action);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling PiP action", e);
        }
    }
    
    @Override
    public void onPipError(String error) {
        try {
            Log.e(TAG, "PiP error: " + error);
            // Show user-friendly error message
            if (isAdded() && getContext() != null) {
                Snackbar.make(binding.getRoot(), "Picture-in-picture not available", Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPipError", e);
        }
    }
}
