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
import com.ds.eventwish.utils.FeatureManager;
import com.google.android.material.snackbar.Snackbar;

public class TemplateCustomizeFragment extends Fragment {
    private static final String TAG = "TemplateCustomizeFragment";
    
    private FragmentTemplateCustomizeBinding binding;
    private TemplateCustomizeViewModel viewModel;
    private FeatureManager featureManager;
    private TextChangeListener textChangeListener;
    
    // Constants
    private static final String ARG_TEMPLATE_ID = "templateId";
    
    public static TemplateCustomizeFragment newInstance(String templateId) {
        TemplateCustomizeFragment fragment = new TemplateCustomizeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEMPLATE_ID, templateId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize view models
        viewModel = new ViewModelProvider(this).get(TemplateCustomizeViewModel.class);
        
        // Initialize feature manager
        featureManager = FeatureManager.getInstance(requireContext());
        
        // Get template ID from arguments
        String templateId = null;
        if (getArguments() != null) {
            templateId = getArguments().getString(ARG_TEMPLATE_ID);
        }
        
        // Load template
        if (templateId != null && !templateId.isEmpty()) {
            viewModel.loadTemplate(templateId);
        }
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTemplateCustomizeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup UI
        setupUI();
        
        // Observe template data
        viewModel.getTemplate().observe(getViewLifecycleOwner(), template -> {
            if (template != null) {
                binding.templateTitle.setText(template.getName());
                binding.previewWebView.loadData(template.getHtml(), "text/html", "UTF-8");
            }
        });
        
        // Observe customized HTML
        viewModel.getPreviewHtml().observe(getViewLifecycleOwner(), html -> {
            if (html != null && !html.isEmpty()) {
                binding.previewWebView.loadData(html, "text/html", "UTF-8");
            }
        });
    }
    
    private void setupUI() {
        // Setup input listeners
        setupInputListeners();
        
        // Setup WebView
        setupWebView();
        
        // Setup HTML editing button
        binding.editHtmlButton.setOnClickListener(v -> {
            if (featureManager.isFeatureUnlocked(FeatureManager.FEATURE_HTML_EDITING)) {
                showHtmlEditor();
            } else {
                Snackbar.make(binding.getRoot(), 
                    getString(R.string.html_editing_locked), 
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.unlock), view -> {
                        // Show premium features dialog
                        featureManager.showPremiumFeaturesDialog(requireActivity());
                    })
                    .show();
            }
        });
        
        // Setup preview button
        binding.previewButton.setOnClickListener(v -> {
            // Set the input values in the ViewModel first
            viewModel.setRecipientName(binding.recipientNameInput.getText().toString());
            viewModel.setSenderName(binding.senderNameInput.getText().toString());
            viewModel.setMessage(binding.messageInput.getText().toString());
            
            // Then update the preview
            viewModel.updatePreview();
        });
        
        // Setup save button
        binding.saveButton.setOnClickListener(v -> {
            // Set the input values in the ViewModel first
            viewModel.setRecipientName(binding.recipientNameInput.getText().toString());
            viewModel.setSenderName(binding.senderNameInput.getText().toString());
            viewModel.setMessage(binding.messageInput.getText().toString());
            
            // Save the current customization
            viewModel.saveCustomization();
            
            // Show confirmation message
            Snackbar.make(binding.getRoot(), 
                getString(R.string.customization_saved), 
                Snackbar.LENGTH_SHORT).show();
        });
        
        // Setup share button
        binding.shareButton.setOnClickListener(v -> {
            // Generate sharing link or image
            String sharingUrl = viewModel.getSharingUrl();
            
            if (sharingUrl != null && !sharingUrl.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, sharingUrl);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)));
            } else {
                Snackbar.make(binding.getRoot(), 
                    getString(R.string.sharing_error), 
                    Snackbar.LENGTH_SHORT).show();
            }
        });
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
    
    private class TextChangeListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not used
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Enable preview button when text changes
            binding.previewButton.setEnabled(true);
        }
        
        @Override
        public void afterTextChanged(Editable s) {
            // Not used
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate back when home/up button is pressed
            Navigation.findNavController(requireView()).navigateUp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        textChangeListener = null;
    }
}
