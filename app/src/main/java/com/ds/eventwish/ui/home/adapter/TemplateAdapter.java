package com.ds.eventwish.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.ItemTemplateBinding;
import com.bumptech.glide.Glide;

public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> {
    private final OnTemplateClickListener listener;

    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }

    public TemplateAdapter(OnTemplateClickListener listener) {
        super(new DiffUtil.ItemCallback<Template>() {
            @Override
            public boolean areItemsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTemplateBinding binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new TemplateViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        try {
            Template template = getItem(position);
            if (template != null) {
                holder.bind(template);
            }
        } catch (IndexOutOfBoundsException e) {
            // Prevent crashes due to RecyclerView inconsistency
            android.util.Log.e("TemplateAdapter", "Error binding view holder at position " + position, e);
        }
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ItemTemplateBinding binding;

        TemplateViewHolder(ItemTemplateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Template template) {
            binding.titleText.setText(template.getTitle());
            binding.categoryText.setText(template.getCategory());
            
            if (template.getThumbnailUrl() != null && !template.getThumbnailUrl().isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                    .load(template.getThumbnailUrl())
                    .centerCrop()
                    .into(binding.thumbnailImage);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });
        }
    }
}
