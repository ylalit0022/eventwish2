package com.ds.eventwish.ui.history;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.databinding.ItemHistoryBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<SharedWish, HistoryAdapter.ViewHolder> {
    private static final String TAG = "HistoryAdapter";
    private final OnHistoryItemClickListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public interface OnHistoryItemClickListener {
        void onViewClick(SharedWish wish);
        void onShareClick(SharedWish wish);
    }

    public HistoryAdapter(OnHistoryItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<SharedWish> DIFF_CALLBACK = new DiffUtil.ItemCallback<SharedWish>() {
        @Override
        public boolean areItemsTheSame(@NonNull SharedWish oldItem, @NonNull SharedWish newItem) {
            return oldItem.getShortCode().equals(newItem.getShortCode());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SharedWish oldItem, @NonNull SharedWish newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SharedWish wish = getItem(position);
        if (wish != null) {
            holder.bind(wish);
        }
    }

    @Override
    public void submitList(@Nullable List<SharedWish> list) {
        Log.d(TAG, "submitList: Submitting new list with size: " + (list != null ? list.size() : 0));
        super.submitList(list != null ? new ArrayList<>(list) : null);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding binding;

        ViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    SharedWish wish = getItem(position);
                    if (wish != null) {
                        listener.onViewClick(wish);
                    }
                }
            });

            binding.shareButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    SharedWish wish = getItem(position);
                    if (wish != null) {
                        listener.onShareClick(wish);
                    }
                }
            });
        }

        void bind(SharedWish wish) {
            if (wish == null) return;

            binding.recipientName.setText(itemView.getContext().getString(R.string.to_format, wish.getRecipientName()));
            binding.senderName.setText(itemView.getContext().getString(R.string.from_format, wish.getSenderName()));
            binding.shortCode.setText(itemView.getContext().getString(R.string.code_format, wish.getShortCode()));

            if (wish.getCreatedAt() != null) {
                String formattedDate = dateFormat.format(wish.getCreatedAt());
                binding.dateCreated.setText(itemView.getContext().getString(R.string.created_format, formattedDate));
            }

            String previewUrl = wish.getPreviewUrl();
            Log.d(TAG, "Binding wish with shortCode: " + wish.getShortCode() + 
          ", previewUrl: " + previewUrl);

               // Handle preview image
    if (wish.getPreviewUrl() != null && !wish.getPreviewUrl().isEmpty()) {
        binding.templateImage.setVisibility(View.VISIBLE);
        Log.d(TAG, "Loading preview image from: " + wish.getPreviewUrl());
        
        Glide.with(itemView.getContext())
            .load(wish.getPreviewUrl())
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .centerCrop()
            .listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                            Target<Drawable> target, boolean isFirstResource) {
                    Log.e(TAG, "Failed to load preview image: " + e.getMessage());
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model,
                                               Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    Log.d(TAG, "Preview image loaded successfully");
                    return false;
                }
            })
            .into(binding.templateImage);
    } else {
        binding.templateImage.setVisibility(View.GONE);
        Log.d(TAG, "No preview URL available for wish");
    }

//            if (wish.getTemplate() != null && wish.getTemplate().getHtmlContent() != null) {
//                setupWebView(wish);
//            }

            binding.statusChip.setText(wish.getSharedVia());
        }

//        private void setupWebView(SharedWish wish) {
//            WebView webView = binding.templateImage;
//            webView.setWebViewClient(new WebViewClient());
//            webView.getSettings().setJavaScriptEnabled(true);
//
//            String html = wish.getTemplate().getHtmlContent();
//            String css = wish.getTemplate().getCssContent();
//            String fullHtml = String.format("<html><head><style>%s</style></head><body>%s</body></html>",
//                css, html);
//
//            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
//        }
    }
}