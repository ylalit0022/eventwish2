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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
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
        // Set programmatic width and height for template items
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        params.width = 700;
        holder.itemView.setLayoutParams(params);
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
        private final CardView cardView;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(FestivalTemplate template, OnTemplateClickListener listener) {
            titleText.setText(template.getTitle());
            categoryText.setText(template.getCategory());

            // Load image with Glide
            String imageUrl = template.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d(TAG, "Loading image for template: " + template.getTitle() + ", URL: " + imageUrl);
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(R.drawable.error_image))
                        .into(thumbnailImage);
            } else {
                Log.w(TAG, "No image URL available for template: " + template.getTitle());
                thumbnailImage.setImageResource(R.drawable.error_image);
            }

            // Set click listener on the CardView
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    Log.d(TAG, "Template clicked: " + template.getId());
                    listener.onTemplateClick(template);
                }
            });
        }
    }
}
