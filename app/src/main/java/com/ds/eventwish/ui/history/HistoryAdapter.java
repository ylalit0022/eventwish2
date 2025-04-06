package com.ds.eventwish.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<SharedWish, HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryItemClickListener {
        void onViewClick(SharedWish wish);
        void onShareClick(SharedWish wish);
    }

    private final OnHistoryItemClickListener listener;

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

    public HistoryAdapter(OnHistoryItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SharedWish wish = getItem(position);
        if (wish != null) {
            // Set recipient name
            holder.recipientNameView.setText(wish.getRecipientName());
            
            // Format and set date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String formattedDate = wish.getCreatedAt() != null ? 
                    sdf.format(wish.getCreatedAt()) : "Unknown date";
            holder.dateView.setText(formattedDate);
            
            // Set click listeners
            holder.viewButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewClick(wish);
                }
            });
            
            holder.shareButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(wish);
                }
            });
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView recipientNameView;
        final TextView dateView;
        final View viewButton;
        final View shareButton;
        final ImageView templateImageView;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            recipientNameView = itemView.findViewById(R.id.recipientName);
            dateView = itemView.findViewById(R.id.sentDate);
            viewButton = itemView.findViewById(R.id.viewButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            templateImageView = itemView.findViewById(R.id.templateImage);
        }
    }
}