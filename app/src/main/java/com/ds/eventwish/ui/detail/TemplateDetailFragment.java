package com.ds.eventwish.ui.detail;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTemplateDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((AppCompatActivity) requireActivity()).getSupportActionBar().hide();


        // Get bottom navigation from activity
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            Button shareButton = binding.shareButton;
            view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
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
        
        setupViewModel();
        setupWebView();
        setupInputListeners();
        setupObservers();
        setupClickListeners();
        
        // Load template data
        templateId = TemplateDetailFragmentArgs.fromBundle(getArguments()).getTemplateId();
        viewModel.loadTemplate(templateId);
        isViewCreated = true;
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TemplateDetailViewModel.class);
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
            binding.shareButton.setEnabled(hasRecipient && hasSender);
        } catch (Exception e) {
            binding.shareButton.setEnabled(false);
        }
    }

    private void setupClickListeners() {
        if (binding == null) return;

        binding.shareButton.setOnClickListener(v -> {
            if (binding != null && binding.shareButton.isEnabled()) {
                viewModel.saveWish();
            }
        });

        binding.customizeButton.setVisibility(View.GONE);
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
    public void onDestroyView() {

        super.onDestroyView();
        isViewCreated = false;
        if (templateRenderer != null) {
            binding.webView.removeJavascriptInterface("Android");
            templateRenderer = null;
        }
        // Show bottom navigation when leaving
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
        // Remove any pending callbacks
        if (pendingNameUpdate != null) {
            backgroundHandler.removeCallbacks(pendingNameUpdate);
            pendingNameUpdate = null;
        }
        binding = null;
    }
}
