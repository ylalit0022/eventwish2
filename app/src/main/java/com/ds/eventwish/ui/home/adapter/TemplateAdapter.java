package com.ds.eventwish.ui.home.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.ItemTemplateBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> {
    private static final String TAG = "TemplateAdapter";
    private final OnTemplateClickListener listener;
    private Set<String> newTemplates = new HashSet<>();
    private List<Template> currentTemplates = new ArrayList<>();

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
            Log.e(TAG, "Error binding view holder at position " + position, e);
        }
    }

    /**
     * Update the list of templates
     * This method ensures proper handling of list changes to prevent RecyclerView inconsistencies
     * @param templates New list of templates
     */
    public void updateTemplates(List<Template> templates) {
        if (templates == null) {
            Log.w(TAG, "Attempted to update templates with null list");
            return;
        }
        
        // Make a copy of the list to avoid external modifications
        currentTemplates = new ArrayList<>(templates);
        
        Log.d(TAG, "Updating templates with " + templates.size() + " items");
        try {
            // Use submitList to leverage DiffUtil for efficient updates
            submitList(new ArrayList<>(templates));
        } catch (Exception e) {
            Log.e(TAG, "Error updating templates", e);
            // Fallback to notifyDataSetChanged in case of errors
            submitList(null);
            submitList(new ArrayList<>(templates));
        }
    }

    /**
     * Set the list of new templates to show the NEW badge
     * @param newTemplateIds Set of template IDs that are new
     */
    public void setNewTemplates(Set<String> newTemplateIds) {
        if (newTemplateIds == null) {
            this.newTemplates = new HashSet<>();
            Log.d(TAG, "Cleared new templates");
        } else {
            this.newTemplates = new HashSet<>(newTemplateIds);
            Log.d(TAG, "Set new templates: " + newTemplates.size() + " items");
        }
        
        // Notify data set changed to update the NEW badges visibility
        notifyDataSetChanged();
    }

    /**
     * Mark a template as viewed (no longer new)
     * @param templateId The ID of the template to mark as viewed
     */
    public void markAsViewed(String templateId) {
        if (newTemplates.contains(templateId)) {
            newTemplates.remove(templateId);
            Log.d(TAG, "Marked template as viewed: " + templateId);
            
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
        }

        void bind(Template template) {
            binding.titleText.setText(template.getTitle());
            binding.categoryText.setText(template.getCategory());
            
            // Show NEW badge if this template is in the newTemplates set
            boolean isNew = newTemplates.contains(template.getId());
            binding.newBadge.setVisibility(isNew ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Template " + template.getId() + " - " + template.getTitle() + " - isNew: " + isNew);
            
            if (template.getThumbnailUrl() != null && !template.getThumbnailUrl().isEmpty()) {
                Log.d(TAG, "Loading image from URL: " + template.getThumbnailUrl());
                Glide.with(binding.getRoot().getContext())
                    .load(template.getThumbnailUrl())
                    .centerCrop()
                    .into(binding.thumbnailImage);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    // Mark as viewed when clicked
                    if (newTemplates.contains(template.getId())) {
                        markAsViewed(template.getId());
                    }
                    listener.onTemplateClick(template);
                }
            });
            
            // Debug log for successful binding
            Log.d(TAG, "Image loaded successfully for: " + template.getTitle());
        }
    }
}
