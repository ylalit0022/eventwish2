package com.ds.eventwish.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HorizontalTemplateAdapter extends RecyclerView.Adapter<HorizontalTemplateAdapter.ViewHolder> {

    private static final String TAG = "HorizontalTemplateAdapter";
    private static final long CLICK_DEBOUNCE_TIME = 800; // ms
    
    private final Context context;
    private final List<Template> templates;
    private final OnTemplateInteractionListener listener;
    private final Map<String, Long> lastClickTimes = new HashMap<>();

    public interface OnTemplateInteractionListener {
        void onTemplateClick(Template template);
        void onTemplateLike(Template template);
        void onTemplateFavorite(Template template);
    }

    public HorizontalTemplateAdapter(Context context, OnTemplateInteractionListener listener) {
        this.context = context;
        this.templates = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_template_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Template template = templates.get(position);
        
        // Set title
        holder.titleText.setText(template.getTitle());
        
        // Load image
        if (template.getImageUrl() != null && !template.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(template.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.templateImage);
        } else {
            holder.templateImage.setImageResource(R.drawable.placeholder_image);
        }
        
        // Set like icon state
        if (template.isLiked()) {
            holder.likeIcon.setImageResource(R.drawable.ic_like_filled);
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_like);
        }
        
        // Set favorite icon state
        if (template.isFavorited()) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_favorite);
        }
        
        // Set click listeners
        holder.cardView.setOnClickListener(v -> {
            if (listener != null && canHandleClick("click_" + template.getId())) {
                listener.onTemplateClick(template);
            }
        });
        
        holder.likeIcon.setOnClickListener(v -> {
            if (listener != null && canHandleClick("like_" + template.getId())) {
                listener.onTemplateLike(template);
                // Update UI immediately for better user experience
                if (template.isLiked()) {
                    holder.likeIcon.setImageResource(R.drawable.ic_like);
                } else {
                    holder.likeIcon.setImageResource(R.drawable.ic_like_filled);
                }
                template.setLiked(!template.isLiked());
            }
        });
        
        holder.favoriteIcon.setOnClickListener(v -> {
            if (listener != null && canHandleClick("favorite_" + template.getId())) {
                listener.onTemplateFavorite(template);
                // Update UI immediately for better user experience
                if (template.isFavorited()) {
                    holder.favoriteIcon.setImageResource(R.drawable.ic_favorite);
                } else {
                    holder.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);
                }
                template.setFavorited(!template.isFavorited());
            }
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }
    
    public void setTemplates(List<Template> templates) {
        this.templates.clear();
        if (templates != null) {
            this.templates.addAll(templates);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Prevent rapid clicks by implementing debounce
     */
    private boolean canHandleClick(String key) {
        long currentTime = System.currentTimeMillis();
        Long lastClickTime = lastClickTimes.get(key);
        
        if (lastClickTime == null || (currentTime - lastClickTime) > CLICK_DEBOUNCE_TIME) {
            lastClickTimes.put(key, currentTime);
            return true;
        }
        
        return false;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView templateImage;
        final TextView titleText;
        final ImageView likeIcon;
        final ImageView favoriteIcon;
        
        public ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            templateImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
} 