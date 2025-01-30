package com.ds.eventwish.ui.customize;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentTemplateCustomizeBinding;
import com.google.android.material.snackbar.Snackbar;

public class TemplateCustomizeFragment extends Fragment {
    private FragmentTemplateCustomizeBinding binding;
    private TemplateCustomizeViewModel viewModel;
    private TextChangeListener textChangeListener;

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
            }
            return false;
        });
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
