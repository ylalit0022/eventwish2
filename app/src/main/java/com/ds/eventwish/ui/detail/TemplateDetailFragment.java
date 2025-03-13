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
import android.widget.Button;

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
import com.ds.eventwish.ui.render.TemplateRenderer;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class TemplateDetailFragment extends Fragment implements TemplateRenderer.TemplateRenderListener {
    private static final String TAG = "TemplateDetailFragment";
    private static final long TEXT_CHANGE_DELAY = 100; // Debounce delay in milliseconds
    
    private FragmentTemplateDetailBinding binding;
    private TemplateDetailViewModel viewModel;
    private TemplateRenderer templateRenderer;
    private String templateId;
    private boolean isViewCreated = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler backgroundHandler = new Handler(Looper.getMainLooper()); // Can be changed to background if needed
    private BottomNavigationView bottomNav;
    private Runnable pendingNameUpdate;

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
        
        setupWebView();
        setupInputListeners();
        setupObservers();
        setupClickListeners();
        
        // Load template data
        templateId = TemplateDetailFragmentArgs.fromBundle(getArguments()).getTemplateId();
        if (templateId != null) {
            viewModel.loadTemplate(templateId);
        } else {
            showError("Invalid template ID");
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
            } else { // Keyboard is hidden
                if (bottomNav != null) {
                    bottomNav.setVisibility(View.VISIBLE);
                }
                if (shareButton != null) {
                    shareButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupViewModel() {
        // viewModel is already initialized in onCreate
    }

    private void setupWebView() {
        
        if (binding != null) {
            binding.webView.getSettings().setJavaScriptEnabled(true);
            binding.webView.getSettings().setLoadWithOverviewMode(true);
            binding.webView.getSettings().setUseWideViewPort(true);
            binding.webView.getSettings().setDomStorageEnabled(true);
            
            templateRenderer = new TemplateRenderer(binding.webView, this);
        }
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
        
        binding.recipientNameInput.addTextChangedListener(textWatcher);
        binding.senderNameInput.addTextChangedListener(textWatcher);
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
                
                Log.d(TAG, "Sharing template with recipient: " + recipientName + ", sender: " + senderName);
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

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private void setupObservers() {
        viewModel.getTemplate().observe(getViewLifecycleOwner(), template -> {
            if (template != null && binding != null && isAdded()) {
                binding.loadingView.setVisibility(View.GONE);
                binding.contentLayout.setVisibility(View.VISIBLE);
                templateRenderer.renderTemplate(template);
                
                // Restore any previously entered names
                String savedRecipient = viewModel.getRecipientName();
                String savedSender = viewModel.getSenderName();
                if (savedRecipient != null && !savedRecipient.isEmpty()) {
                    binding.recipientNameInput.setText(savedRecipient);
                }
                if (savedSender != null && !savedSender.isEmpty()) {
                    binding.senderNameInput.setText(savedSender);
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && binding != null && isAdded()) {
                binding.loadingView.setVisibility(View.GONE);
                showError(error);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding != null && isAdded()) {
                binding.loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
                scheduleNameUpdate();
                
                // Update the customized HTML in the ViewModel
                if (templateRenderer != null) {
                    String customizedHtml = templateRenderer.getCustomizedHtml();
                    viewModel.setCustomizedHtml(customizedHtml);
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

    @Override
    public void onPause() {
        super.onPause();
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
        isViewCreated = false;
        
        // Cleanup WebView
        if (binding != null && binding.webView != null) {
            binding.webView.stopLoading();
            binding.webView.clearCache(true);
            binding.webView.clearHistory();
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
}
