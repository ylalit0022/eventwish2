package com.ds.eventwish.ui.reminder.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.databinding.ItemReminderBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {
    private List<Reminder> reminders = new ArrayList<>();
    private OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
    }

    public void setOnReminderClickListener(OnReminderClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderBinding binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ReminderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        holder.bind(reminders.get(position));
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    public void submitList(List<Reminder> newReminders) {
        this.reminders = new ArrayList<>(newReminders);
        notifyDataSetChanged();
    }

    class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final ItemReminderBinding binding;

        ReminderViewHolder(@NonNull ItemReminderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onReminderClick(reminders.get(position));
                }
            });
        }

        void bind(Reminder reminder) {
            binding.titleText.setText(reminder.getTitle());
            binding.descriptionText.setText(reminder.getDescription());
            binding.dateTimeText.setText(formatDateTime(reminder.getDateTime()));
        }

        private String formatDateTime(long dateTime) {
            return new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new Date(dateTime));
        }
    }
} 