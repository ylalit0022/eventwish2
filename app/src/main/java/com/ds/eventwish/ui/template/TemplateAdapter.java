package com.ds.eventwish.ui.template;

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
import com.bumptech.glide.Glide;

/**
 * Adapter for displaying templates in a RecyclerView
 */
public class TemplateAdapter extends ListAdapter<Template, TemplateAdapter.TemplateViewHolder> {
    
    private final OnTemplateClickListener listener;
    
    /**
     * Interface for handling template clicks
     */
    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }
    
    /**
     * Constructor
     *
     * @param listener Listener for template clicks
     */
    public TemplateAdapter(OnTemplateClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_template, parent, false);
        return new TemplateViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        Template template = getItem(position);
        holder.bind(template, listener);
    }
    
    /**
     * ViewHolder for template items
     */
    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ImageView templateImage;
        private final TextView templateName;
        
        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            templateImage = itemView.findViewById(R.id.template_image);
            templateName = itemView.findViewById(R.id.titleText);
        }
        
        public void bind(Template template, OnTemplateClickListener listener) {
            templateName.setText(template.getName());
            
            // Load image with Glide
            Glide.with(templateImage.getContext())
                .load(template.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .centerCrop()
                .into(templateImage);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTemplateClick(template);
                }
            });
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    private static final DiffUtil.ItemCallback<Template> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<Template>() {
            @Override
            public boolean areItemsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.getId().equals(newItem.getId());
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull Template oldItem, @NonNull Template newItem) {
                return oldItem.getName().equals(newItem.getName()) &&
                    oldItem.getImageUrl().equals(newItem.getImageUrl()) &&
                    oldItem.getCategoryId().equals(newItem.getCategoryId());
            }
        };
} 