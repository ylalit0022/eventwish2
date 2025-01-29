package com.ds.eventwish.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ItemGreetingBinding;
import com.ds.eventwish.ui.home.GreetingItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends ListAdapter<GreetingItem, HistoryAdapter.HistoryViewHolder> {
    

    protected HistoryAdapter() {
        super(new DiffUtil.ItemCallback<GreetingItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull GreetingItem oldItem, @NonNull GreetingItem newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull GreetingItem oldItem, @NonNull GreetingItem newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGreetingBinding binding = ItemGreetingBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemGreetingBinding binding;

        HistoryViewHolder(@NonNull ItemGreetingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GreetingItem item) {
            binding.greetingTitle.setText(item.getTitle());
            binding.greetingCategory.setText(item.getCategory());

            // Add timestamp
            if (item.getShareTime() != null) {
                binding.shareTimeTextView.setText(item.getShareTime());
            }

          // Load image
          if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(itemView.getContext())
                 .load(item.getImageUrl())
                 .placeholder(R.drawable.placeholder_image)
                 .error(R.drawable.error_image)
                 .into(binding.greetingImageView);
        }

            binding.getRoot().setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("greetingId", item.getId());
                Navigation.findNavController(v).navigate(
                    R.id.action_history_to_detail,
                    args
                );
            });
        }
    }

    public void addHistoryItem(GreetingItem item) {
        List<GreetingItem> currentList = new ArrayList<>(getCurrentList());
        if (!currentList.contains(item)) {
            item.setShareTime(new SimpleDateFormat("dd MMM yyyy, HH:mm",
                            Locale.getDefault()).format(new Date()));
            currentList.add(0, item);
            submitList(currentList);
        }
    }
}
