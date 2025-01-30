package com.ds.eventwish.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextWatcher;
import android.text.Editable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentTemplateDetailBinding;
import com.ds.eventwish.ui.render.TemplateRenderer;
import com.google.android.material.snackbar.Snackbar;

public class TemplateDetailFragment extends Fragment implements TemplateRenderer.TemplateRenderListener {
    private FragmentTemplateDetailBinding binding;
    private TemplateDetailViewModel viewModel;
    private TemplateRenderer templateRenderer;
    private String templateId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTemplateDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupViewModel();
        setupWebView();
        setupInputListeners();
        setupObservers();
        setupClickListeners();
        
        // Load template data
        templateId = TemplateDetailFragmentArgs.fromBundle(getArguments()).getTemplateId();
        viewModel.loadTemplate(templateId);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TemplateDetailViewModel.class);
    }

    private void setupWebView() {
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setLoadWithOverviewMode(true);
        binding.webView.getSettings().setUseWideViewPort(true);
        
        templateRenderer = new TemplateRenderer(binding.webView, this);
    }

    private void setupInputListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateViewModel();
            }
        };
        
        binding.recipientNameInput.addTextChangedListener(textWatcher);
        binding.senderNameInput.addTextChangedListener(textWatcher);
    }

    private void updateViewModel() {
        viewModel.setRecipientName(binding.recipientNameInput.getText().toString());
        viewModel.setSenderName(binding.senderNameInput.getText().toString());
    }

    private void setupObservers() {
        viewModel.getTemplate().observe(getViewLifecycleOwner(), template -> {
            if (template != null) {
                templateRenderer.renderTemplate(template);
                binding.shareButton.setEnabled(true);
            }
        });

        viewModel.getSharedWish().observe(getViewLifecycleOwner(), sharedWish -> {
            if (sharedWish != null) {
                navigateToSharedWish(sharedWish.getShortCode());
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.shareButton.setEnabled(!isLoading);
            binding.customizeButton.setEnabled(!isLoading);
        });
    }

    private void setupClickListeners() {
        binding.customizeButton.setOnClickListener(v -> navigateToCustomize());
        binding.shareButton.setOnClickListener(v -> viewModel.saveWish());
    }

    private void navigateToCustomize() {
        TemplateDetailFragmentDirections.ActionTemplateDetailToCustomize action =
            TemplateDetailFragmentDirections.actionTemplateDetailToCustomize(templateId);
        Navigation.findNavController(requireView()).navigate(action);
    }

    private void navigateToSharedWish(String shortCode) {
        TemplateDetailFragmentDirections.ActionTemplateDetailToSharedWish action =
            TemplateDetailFragmentDirections.actionTemplateDetailToSharedWish(shortCode);
        Navigation.findNavController(requireView()).navigate(action);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRenderComplete() {
        binding.shareButton.setEnabled(true);
    }

    @Override
    public void onRenderError(String error) {
        showError(error);
        binding.shareButton.setEnabled(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.webView.loadUrl("about:blank");
        binding = null;
    }
}
