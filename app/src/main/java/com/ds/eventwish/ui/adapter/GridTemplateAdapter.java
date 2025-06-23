package com.ds.eventwish.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class GridTemplateAdapter extends RecyclerView.Adapter<GridTemplateAdapter.ViewHolder> {

    private final Context context;
    private List<Template> templates;
    private OnItemClickListener onItemClickListener;

    public GridTemplateAdapter(Context context) {
        this.context = context;
        this.templates = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_template_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Template template = templates.get(position);
        
        // Load image using Glide
        Glide.with(context)
                .load(template.getPreviewUrl())
                .placeholder(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.imageView);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(template);
            }
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Template template);
    }
}
