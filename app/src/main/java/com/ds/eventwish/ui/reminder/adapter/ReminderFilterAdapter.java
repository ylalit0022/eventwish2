package com.ds.eventwish.ui.reminder.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ItemReminderFilterBinding;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import java.util.Arrays;
import java.util.List;

public class ReminderFilterAdapter extends RecyclerView.Adapter<ReminderFilterAdapter.FilterViewHolder> {
    private final List<FilterItem> filters = Arrays.asList(
        new FilterItem(R.drawable.ic_all, "All", ReminderViewModel.Filter.ALL),
        new FilterItem(R.drawable.ic_today, "Today", ReminderViewModel.Filter.TODAY),
        new FilterItem(R.drawable.ic_upcoming, "Upcoming", ReminderViewModel.Filter.UPCOMING),
        new FilterItem(R.drawable.ic_completed, "Completed", ReminderViewModel.Filter.COMPLETED)
    );
    
    private int selectedPosition = 0;
    private final OnFilterSelectedListener listener;

    public interface OnFilterSelectedListener {
        void onFilterSelected(ReminderViewModel.Filter filter);
    }

    public ReminderFilterAdapter(OnFilterSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderFilterBinding binding = ItemReminderFilterBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new FilterViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        holder.bind(filters.get(position), position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    class FilterViewHolder extends RecyclerView.ViewHolder {
        private final ItemReminderFilterBinding binding;

        FilterViewHolder(ItemReminderFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position != selectedPosition) {
                    int oldPosition = selectedPosition;
                    selectedPosition = position;
                    notifyItemChanged(oldPosition);
                    notifyItemChanged(selectedPosition);
                    listener.onFilterSelected(filters.get(position).filter);
                }
            });
        }

        void bind(FilterItem item, boolean isSelected) {
            binding.filterIcon.setImageResource(item.iconRes);
            binding.filterName.setText(item.name);
            binding.getRoot().setChecked(isSelected);
            binding.getRoot().setCardBackgroundColor(
                itemView.getContext().getColor(isSelected ? R.color.primary : R.color.card_background)
            );
            binding.filterName.setTextColor(
                itemView.getContext().getColor(isSelected ? R.color.white : R.color.text_primary)
            );
            binding.filterIcon.setColorFilter(
                itemView.getContext().getColor(isSelected ? R.color.white : R.color.text_primary)
            );
        }
    }

    private static class FilterItem {
        final int iconRes;
        final String name;
        final ReminderViewModel.Filter filter;

        FilterItem(int iconRes, String name, ReminderViewModel.Filter filter) {
            this.iconRes = iconRes;
            this.name = name;
            this.filter = filter;
        }
    }
}
