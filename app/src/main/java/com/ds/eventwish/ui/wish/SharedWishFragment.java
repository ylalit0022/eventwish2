package com.ds.eventwish.ui.wish;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
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

import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.databinding.FragmentSharedWishBinding;
import com.ds.eventwish.utils.DeepLinkUtil;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.google.gson.Gson;

import android.content.Intent;

public class SharedWishFragment extends Fragment {
    private FragmentSharedWishBinding binding;
    private SharedWishViewModel viewModel;
    private String shortCode;
    private String TAG = "SharedWishFragment";
    private WishResponse currentWish;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SharedWishViewModel.class);
        
        if (getArguments() != null) {
            shortCode = SharedWishFragmentArgs.fromBundle(getArguments()).getShortCode();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        binding = FragmentSharedWishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupWebView();
        setupObservers();
        setupClickListeners();
        
        if (shortCode != null) {
            viewModel.loadSharedWish(shortCode);
        }
    }

    private void setupWebView() {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
    }

    private void setupObservers() {
        viewModel.getSharedWish().observe(getViewLifecycleOwner(), wish -> {
            if (wish != null) {
                Log.d(TAG, "Received Wish JSON: " + new Gson().toJson(wish));
                currentWish = wish;
                loadWishContent(wish);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.d("SharedWishFragment", error);
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.shareButton.setOnClickListener(v -> shareWish());
    }

    private void loadWishContent(WishResponse wish) {
        Log.d(TAG, "Loading wish content, wish: " + (wish != null ? "not null" : "null"));

        if (wish == null || wish.getCustomizedHtml() == null){
            Log.e(TAG, "Wish or HTML content is null");
   
            return;
        }

        Template template = wish.getTemplate();
        Log.d(TAG, "Template: " + (template != null ? "found" : "null"));

        if (template == null) {
            Log.e(TAG, "Template is null for wish: " + wish.getShortCode());

            return;
        }

        try{

        String css = template.getCssContent() != null ? template.getCssContent() : "";
        String js = template.getJsContent() != null ? template.getJsContent() : "";
        Log.d(TAG, "CSS length: " + css.length() + ", JS length: " + js.length());

        String fullHtml = "<!DOCTYPE html><html><head>" +
                         "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                         "<style>" + css + "</style>" +
                         "</head><body>" +
                         wish.getCustomizedHtml() +
                         "<script>" + js + "</script>" +
                         "</body></html>";

        binding.webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
        binding.loadingView.setVisibility(View.GONE);
        binding.contentLayout.setVisibility(View.VISIBLE);
        
        // Set recipient and sender names
        binding.recipientNameText.setText("To: " + wish.getRecipientName());
        binding.senderNameText.setText("From: " + wish.getSenderName());
    }
    catch (Exception e) {
        Log.e(TAG, "Error loading wish content", e);
        Log.e(TAG, "Raw wish data: " + new Gson().toJson(wish));    }
    }

    private void shareWish() {
        if (currentWish == null) return;

        String shareUrl = DeepLinkUtil.generateWebUrl(currentWish.getShortCode());
        String message = String.format("Check out this greeting from %s to %s!\n%s",
            currentWish.getSenderName(), currentWish.getRecipientName(), shareUrl);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        
        // Create a chooser with a custom title
        Intent chooser = Intent.createChooser(shareIntent, "Share Greeting");
        
        // Verify the intent will resolve to at least one activity
        if (shareIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(requireContext(), "No apps available to share", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.webView.stopLoading();
            binding.webView.destroy();
            binding = null;
        }
    }
}
