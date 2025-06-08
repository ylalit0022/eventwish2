package com.ds.eventwish.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.ItemTemplateBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> {
    private static final String TAG = "TemplateAdapter";
    private final OnTemplateClickListener listener;
    private Set<String> newTemplates = new HashSet<>();

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
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }

    /**
     * Update the list of templates in the adapter
     * @param templates New list of templates
     */
    public void updateTemplates(List<Template> templates) {
        submitList(templates);
    }

    /**
     * Set the list of new templates to show the NEW badge
     * @param newTemplateIds Set of template IDs that are new
     */
    public void setNewTemplates(Set<String> newTemplateIds) {
        this.newTemplates = newTemplateIds != null ? new HashSet<>(newTemplateIds) : new HashSet<>();
        notifyDataSetChanged();
    }

    /**
     * Mark a template as viewed (no longer new)
     * @param templateId The ID of the template to mark as viewed
     */
    public void markAsViewed(String templateId) {
        if (newTemplates.contains(templateId)) {
            newTemplates.remove(templateId);
            // Find the position of this template and update it
            for (int i = 0; i < getItemCount(); i++) {
                Template template = getItem(i);
                if (template != null && template.getId().equals(templateId)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ItemTemplateBinding binding;

        TemplateViewHolder(ItemTemplateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener in constructor to avoid creating new instances
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Template template = getItem(position);
                    if (template != null && listener != null) {
                        Log.d(TAG, "Template clicked: " + template.getId());
                        if (newTemplates.contains(template.getId())) {
                            markAsViewed(template.getId());
                        }
                        listener.onTemplateClick(template);
                    }
                }
            });
        }

        void bind(Template template) {
            try {
                // Set title
                binding.titleText.setText(template.getTitle());
                
                // Set category if available
                String category = template.getCategory();
                if (category != null && !category.isEmpty()) {
                    binding.categoryText.setVisibility(View.VISIBLE);
                    binding.categoryText.setText(category);
                } else {
                    binding.categoryText.setVisibility(View.GONE);
                }
                
                // Show NEW badge if this template is in the newTemplates set
                binding.newBadge.setVisibility(newTemplates.contains(template.getId()) ? 
                    View.VISIBLE : View.GONE);
                
                // Load image if URL is available
                String thumbnailUrl = template.getThumbnailUrl();
                if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    Glide.with(binding.getRoot().getContext())
                        .load(thumbnailUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(binding.templateImage);
                } else {
                    binding.templateImage.setImageResource(android.R.color.transparent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error binding template: " + e.getMessage());
            }
        }
    }
}
