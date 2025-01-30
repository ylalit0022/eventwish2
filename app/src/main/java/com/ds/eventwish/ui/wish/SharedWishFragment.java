package com.ds.eventwish.ui.wish;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.databinding.FragmentSharedWishBinding;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

public class SharedWishFragment extends Fragment {
    private FragmentSharedWishBinding binding;
    private SharedWishViewModel viewModel;
    private String shortCode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SharedWishViewModel.class);
        
        // Get shortCode from arguments or deep link
        if (getArguments() != null) {
            shortCode = getArguments().getString("shortCode");
        }
        if (shortCode == null) {
            Uri data = requireActivity().getIntent().getData();
            if (data != null) {
                shortCode = DeepLinkUtil.extractShortCode(data);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSharedWishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupWebView();
        setupObservers();
        setupShareButton();
        
        if (shortCode != null) {
            viewModel.loadSharedWish(shortCode);
        } else {
            showError(getString(R.string.error_loading_wish));
        }
    }

    private void setupWebView() {
        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
    }

    private void setupObservers() {
        viewModel.getSharedWish().observe(getViewLifecycleOwner(), this::displayWish);
        viewModel.getError().observe(getViewLifecycleOwner(), this::showError);
    }

    private void setupShareButton() {
        binding.shareButton.setOnClickListener(v -> showShareOptions());
    }

    private void displayWish(SharedWish wish) {
        if (wish != null && wish.getCustomizedHtml() != null) {
            binding.loadingView.setVisibility(View.GONE);
            binding.webView.setVisibility(View.VISIBLE);
            binding.webView.loadDataWithBaseURL(null, wish.getCustomizedHtml(), "text/html", "UTF-8", null);
            binding.shareButton.setVisibility(View.VISIBLE);
        } else {
            showError(getString(R.string.error_loading_wish));
        }
    }

    private void showShareOptions() {
        SharedWish wish = viewModel.getSharedWish().getValue();
        if (wish == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_share, null);
        dialog.setContentView(sheetView);

        String shareUrl = DeepLinkUtil.createShareUrl(wish.getShortCode());

        // WhatsApp share
        sheetView.findViewById(R.id.whatsappButton).setOnClickListener(v -> {
            String text = getString(R.string.share_wish_text, shareUrl);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            try {
                startActivity(intent);
            } catch (Exception e) {
                showError(getString(R.string.error_generic));
            }
            dialog.dismiss();
        });

        // Copy link
        sheetView.findViewById(R.id.copyLinkButton).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Share URL", shareUrl);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(binding.getRoot(), R.string.link_copied, Snackbar.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Other share options
        sheetView.findViewById(R.id.otherShareButton).setOnClickListener(v -> {
            String text = getString(R.string.share_wish_text, shareUrl);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(intent, getString(R.string.share_via_other)));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showError(String message) {
        binding.loadingView.setVisibility(View.GONE);
        binding.webView.setVisibility(View.GONE);
        binding.shareButton.setVisibility(View.GONE);
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
