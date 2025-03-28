package com.ds.eventwish.ui.customize;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentTemplateCustomizeBinding;
import com.ds.eventwish.ui.dialog.UnifiedAdRewardDialog;
import com.ds.eventwish.ui.viewmodel.CoinsViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.ds.eventwish.utils.FeatureManager;

public class TemplateCustomizeFragment extends Fragment {
    private FragmentTemplateCustomizeBinding binding;
    private TemplateCustomizeViewModel viewModel;
    private CoinsViewModel coinsViewModel;
    private TextChangeListener textChangeListener;
    private boolean isHtmlTemplate = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTemplateCustomizeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        setupToolbar();
        setupInputListeners();
        setupWebView();
        setupObservers();
        setupClickListeners();
        
        // Load template data
        String templateId = TemplateCustomizeFragmentArgs.fromBundle(getArguments()).getTemplateId();
        viewModel.loadTemplate(templateId);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TemplateCustomizeViewModel.class);
        coinsViewModel = new ViewModelProvider(this).get(CoinsViewModel.class);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> 
            Navigation.findNavController(requireView()).navigateUp());
            
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_preview) {
                // Preview is now auto-generated when text changes
                return true;
            } else if (item.getItemId() == R.id.action_save) {
                viewModel.saveWish();
                return true;
            } else if (item.getItemId() == R.id.action_edit_html) {
                // Check if HTML editing is unlocked
                if (checkHtmlEditingUnlocked()) {
                    // Show HTML editor dialog or navigate to HTML editor fragment
                    showHtmlEditor();
                } else {
                    // Show dialog to unlock HTML editing
                    showAdRewardDialog();
                }
                return true;
            }
            return false;
        });
    }

    private boolean checkHtmlEditingUnlocked() {
        if (!isHtmlTemplate) {
            return true; // Not an HTML template, no unlock needed
        }
        
        // Use FeatureManager to check unlock status
        return FeatureManager.getInstance(requireContext())
                .isFeatureUnlocked(FeatureManager.HTML_EDITING);
    }
    
    private void showAdRewardDialog() {
        // Create and show the UnifiedAdRewardDialog
        UnifiedAdRewardDialog dialog = UnifiedAdRewardDialog.newInstance("Unlock HTML Editing")
            .setCallback(new UnifiedAdRewardDialog.AdRewardCallback() {
                @Override
                public void onCoinsEarned(int amount) {
                    Toast.makeText(requireContext(), "You earned " + amount + " coins!", Toast.LENGTH_SHORT).show();
                    // Check if they now have enough coins to unlock
                    updateUiForTemplateType();
                }
                
                @Override
                public void onFeatureUnlocked(int durationDays) {
                    Toast.makeText(requireContext(), "HTML Editing unlocked for " + durationDays + " days!", Toast.LENGTH_SHORT).show();
                    showHtmlEditor();
                }
                
                @Override
                public void onDismissed() {
                    // Nothing to do here
                }
            });
        
        dialog.show(requireActivity().getSupportFragmentManager());
    }
    
    private void showHtmlEditor() {
        // Show HTML editor dialog or functionality
        // This implementation depends on how the HTML editor is implemented
        Toast.makeText(requireContext(), "HTML editor opened", Toast.LENGTH_SHORT).show();
        // Implement HTML editing functionality here
    }

    private void setupInputListeners() {
        textChangeListener = new TextChangeListener();
        
        binding.recipientNameInput.addTextChangedListener(textChangeListener);
        binding.senderNameInput.addTextChangedListener(textChangeListener);
        binding.messageInput.addTextChangedListener(textChangeListener);
    }

    private void setupWebView() {
        binding.previewWebView.getSettings().setJavaScriptEnabled(true);
        binding.previewWebView.getSettings().setLoadWithOverviewMode(true);
        binding.previewWebView.getSettings().setUseWideViewPort(true);
    }

    private void setupObservers() {
        viewModel.getTemplate().observe(getViewLifecycleOwner(), template -> {
            if (template != null) {
                // Check if this is an HTML template
                isHtmlTemplate = "html".equals(template.getType());
                
                // Update UI based on template type
                updateUiForTemplateType();
            }
        });
        
        viewModel.getPreviewHtml().observe(getViewLifecycleOwner(), html -> {
            if (html != null) {
                binding.previewWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        });

        viewModel.getSharedWish().observe(getViewLifecycleOwner(), sharedWish -> {
            if (sharedWish != null) {
                Bundle args = new Bundle();
                args.putString("shortCode", sharedWish.getShortCode());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_template_customize_to_shared_wish, args);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });
        
        // Observe feature unlock status to update UI
        coinsViewModel.getIsUnlockedLiveData().observe(getViewLifecycleOwner(), isUnlocked -> {
            if (isHtmlTemplate) {
                updateUiForTemplateType();
            }
        });
    }
    
    private void updateUiForTemplateType() {
        if (isHtmlTemplate) {
            // Show or hide HTML edit button based on unlock status
            boolean isUnlocked = FeatureManager.getInstance(requireContext())
                                               .isFeatureUnlocked(FeatureManager.HTML_EDITING);
            
            // Assuming you have a menu item for HTML editing
            MenuItem htmlEditItem = binding.toolbar.getMenu().findItem(R.id.action_edit_html);
            if (htmlEditItem != null) {
                htmlEditItem.setVisible(true);
                
                // Optionally change the icon or title based on lock status
                if (!isUnlocked) {
                    htmlEditItem.setTitle(R.string.unlock_html_editing);
                    htmlEditItem.setIcon(R.drawable.ic_lock);
                } else {
                    htmlEditItem.setTitle(R.string.edit_html);
                    htmlEditItem.setIcon(R.drawable.ic_edit);
                }
            }
        } else {
            // Not an HTML template, hide HTML editing options
            MenuItem htmlEditItem = binding.toolbar.getMenu().findItem(R.id.action_edit_html);
            if (htmlEditItem != null) {
                htmlEditItem.setVisible(false);
            }
        }
    }

    private void setupClickListeners() {
        binding.shareButton.setOnClickListener(v -> viewModel.saveWish());
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private class TextChangeListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            // Update the ViewModel with new text values
            viewModel.setRecipientName(binding.recipientNameInput.getText().toString());
            viewModel.setSenderName(binding.senderNameInput.getText().toString());
            viewModel.setMessage(binding.messageInput.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.previewWebView.loadUrl("about:blank");
        binding = null;
    }
}
