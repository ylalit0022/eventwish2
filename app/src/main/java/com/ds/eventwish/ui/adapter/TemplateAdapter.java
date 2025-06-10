package com.ds.eventwish.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.remote.TemplateInteractionManager;
import java.util.ArrayList;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Template> templates;
    private final TemplateInteractionManager interactionManager;
    private OnItemClickListener onItemClickListener;

    public TemplateAdapter(Context context) {
        this.context = context;
        this.templates = new ArrayList<>();
        this.interactionManager = TemplateInteractionManager.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Template template = templates.get(position);
        
        // Set basic template info
        holder.titleText.setText(template.getTitle());
        holder.categoryText.setText(template.getCategory());
        
        // Load image using Glide
        Glide.with(context)
            .load(template.getPreviewUrl())
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.templateImage);
        
        // Check and set initial states
        interactionManager.checkLikeStatus(template.getId())
            .addOnSuccessListener(isLiked -> {
                template.setLiked(isLiked);
                holder.likeIcon.setSelected(isLiked);
            });
            
        interactionManager.checkFavoriteStatus(template.getId())
            .addOnSuccessListener(isFavorited -> {
                template.setFavorited(isFavorited);
                holder.favoriteIcon.setSelected(isFavorited);
            });
        
        // Handle like click
        holder.likeIcon.setOnClickListener(v -> {
            // Disable the button temporarily
            holder.likeIcon.setEnabled(false);
            
            // Optimistic UI update
            boolean newLikeState = !template.isLiked();
            template.setLiked(newLikeState);
            holder.likeIcon.setSelected(newLikeState);
            
            // Use the fixed version of toggleLike
            interactionManager.toggleLike(template.getId())
                .addOnSuccessListener(aVoid -> {
                    // Re-enable the button on success
                    holder.likeIcon.setEnabled(true);
                    Log.d("TemplateAdapter", "Successfully toggled like for template: " + template.getId());
                })
                .addOnFailureListener(e -> {
                    // Revert on failure
                    template.setLiked(!newLikeState);
                    holder.likeIcon.setSelected(!newLikeState);
                    holder.likeIcon.setEnabled(true);
                    Log.e("TemplateAdapter", "Failed to toggle like for template: " + template.getId(), e);
                    Toast.makeText(context, "Failed to update like status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
        
        // Handle favorite click
        holder.favoriteIcon.setOnClickListener(v -> {
            // Disable the button temporarily
            holder.favoriteIcon.setEnabled(false);
            
            // Optimistic UI update
            boolean newFavoriteState = !template.isFavorited();
            template.setFavorited(newFavoriteState);
            holder.favoriteIcon.setSelected(newFavoriteState);
            
            interactionManager.toggleFavorite(template.getId())
                .addOnSuccessListener(aVoid -> {
                    // Re-enable the button on success
                    holder.favoriteIcon.setEnabled(true);
                    Log.d("TemplateAdapter", "Successfully toggled favorite for template: " + template.getId());
                })
                .addOnFailureListener(e -> {
                    // Revert on failure
                    template.setFavorited(!newFavoriteState);
                    holder.favoriteIcon.setSelected(!newFavoriteState);
                    holder.favoriteIcon.setEnabled(true);
                    Log.e("TemplateAdapter", "Failed to toggle favorite for template: " + template.getId(), e);
                    Toast.makeText(context, "Failed to update favorite status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
        
        // Handle card click
        holder.cardView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(template);
            }
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public void setTemplates(ArrayList<Template> templates) {
        this.templates.clear();
        this.templates.addAll(templates);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Template template);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView templateImage;
        final TextView titleText;
        final TextView categoryText;
        final ImageView likeIcon;
        final ImageView favoriteIcon;
        final TextView newBadge;
        final View recommendedBadge;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            templateImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            newBadge = itemView.findViewById(R.id.newBadge);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
        }
    }
} 