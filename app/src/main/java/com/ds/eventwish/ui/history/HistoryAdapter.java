package com.ds.eventwish.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.databinding.ItemHistoryBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<SharedWish, HistoryAdapter.HistoryViewHolder> {
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onViewClick(SharedWish wish);
        void onShareClick(SharedWish wish);
    }

    public HistoryAdapter(OnHistoryItemClickListener listener) {
        super(new DiffUtil.ItemCallback<SharedWish>() {
            @Override
            public boolean areItemsTheSame(@NonNull SharedWish oldItem, @NonNull SharedWish newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull SharedWish oldItem, @NonNull SharedWish newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding binding;
        private final SimpleDateFormat dateFormat;
        private final Context context;

        HistoryViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = binding.getRoot().getContext();
            this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            setupAccessibility();
        }

        private void setupAccessibility() {
            binding.viewButton.setContentDescription(context.getString(R.string.view_button));
            binding.shareButton.setContentDescription(context.getString(R.string.share_button));
            binding.templateImage.setContentDescription(context.getString(R.string.template_image));
        }

        void bind(SharedWish wish) {
            String recipientName = wish.getRecipientName();
            binding.recipientText.setText(context.getString(R.string.sent_to, recipientName));
            
            // Format date safely
            String formattedDate = formatDate(wish.getCreatedAt());
            binding.dateText.setText(context.getString(R.string.sent_on, formattedDate));
            
            // Set message preview with fallback
            String message = wish.getMessage();
            if (message != null && !message.isEmpty()) {
                binding.messagePreview.setText(message);
                binding.messagePreview.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.messagePreview.setVisibility(android.view.View.GONE);
            }

            // Load template thumbnail with proper error handling
            if (wish.getTemplate() != null && wish.getTemplate().getThumbnailUrl() != null) {
                Glide.with(binding.templateImage)
                    .load(wish.getTemplate().getThumbnailUrl())
                    .placeholder(R.drawable.placeholder_template)
                    .error(R.drawable.error_template)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.templateImage);
            } else {
                binding.templateImage.setImageResource(R.drawable.error_template);
            }

            // Update accessibility labels
            binding.templateImage.setContentDescription(
                context.getString(R.string.template_image) + " " + recipientName);
            
            // Set click listeners
            binding.viewButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewClick(wish);
                }
            });

            binding.shareButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(wish);
                }
            });

            // Make the whole item clickable
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewClick(wish);
                }
            });
        }

        private String formatDate(Date date) {
            if (date == null) {
                return context.getString(R.string.date_unknown);
            }
            try {
                return dateFormat.format(date);
            } catch (Exception e) {
                return context.getString(R.string.date_unknown);
            }
        }
    }
}
