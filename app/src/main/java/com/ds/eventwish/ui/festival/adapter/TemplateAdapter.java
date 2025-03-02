package com.ds.eventwish.ui.festival.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.FestivalTemplate;

import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {
    private static final String TAG = "TemplateAdapter";

    private final List<FestivalTemplate> templates;
    private final OnTemplateClickListener listener;

    public interface OnTemplateClickListener {
        void onTemplateClick(FestivalTemplate template);
    }

    public TemplateAdapter(List<FestivalTemplate> templates, OnTemplateClickListener listener) {
        this.templates = templates;
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
        FestivalTemplate template = templates.get(position);
        holder.bind(template, listener);
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnailImage;
        private final TextView titleText;
        private final TextView categoryText;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
        }

        public void bind(FestivalTemplate template, OnTemplateClickListener listener) {
            titleText.setText(template.getTitle());
            categoryText.setText(template.getCategory());

            // Log the image URL for debugging
            String imageUrl = template.getImageUrl();
            Log.d(TAG, "Template: " + template.getTitle() + ", Image URL: " + imageUrl);

            // Load image with Glide
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d(TAG, "Attempting to load image from URL: " + imageUrl);
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.error_image)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                      Target<Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "Image load failed for URL: " + imageUrl + 
                                         ", Template: " + template.getTitle(), e);
                                return false; // let Glide handle the error image
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                        Target<Drawable> target, DataSource dataSource,
                                                        boolean isFirstResource) {
                                Log.d(TAG, "Image loaded successfully for: " + template.getTitle());
                                return false; // let Glide handle setting the resource
                            }
                        })
                        .centerCrop()
                        .into(thumbnailImage);
            } else {
                Log.w(TAG, "No image URL available for template: " + template.getTitle());
                thumbnailImage.setImageResource(R.drawable.error_image);
            }

            // Set click listener
            itemView.setOnClickListener(v -> listener.onTemplateClick(template));
        }
    }
}
