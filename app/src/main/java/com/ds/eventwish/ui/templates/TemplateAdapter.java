package com.ds.eventwish.ui.templates;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {
    private List<Template> templates = new ArrayList<>();
    private final OnTemplateClickListener listener;

    public interface OnTemplateClickListener {
        void onTemplateClick(Template template);
    }

    public TemplateAdapter(OnTemplateClickListener listener) {
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
        Template template = templates.get(position);
        holder.bind(template);
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
        notifyDataSetChanged();
    }

    public void addTemplates(List<Template> newTemplates) {
        int startPos = templates.size();
        templates.addAll(newTemplates);
        notifyItemRangeInserted(startPos, newTemplates.size());
    }

    class TemplateViewHolder extends RecyclerView.ViewHolder {
        private final ImageView previewImage;
        private final TextView titleText;
        private final TextView categoryText;

        TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            previewImage = itemView.findViewById(R.id.previewImage);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTemplateClick(templates.get(position));
                }
            });
        }

        void bind(Template template) {
            titleText.setText(template.getTitle());
            categoryText.setText(template.getCategory());

            if (template.getPreviewUrl() != null && !template.getPreviewUrl().isEmpty()) {
                Glide.with(previewImage.getContext())
                    .load(template.getPreviewUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(previewImage);
            } else {
                previewImage.setImageResource(R.drawable.placeholder_image);
            }
        }
    }
}
