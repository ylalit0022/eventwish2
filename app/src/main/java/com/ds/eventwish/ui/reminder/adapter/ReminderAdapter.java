package com.ds.eventwish.ui.reminder.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.databinding.ItemReminderBinding;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReminderAdapter extends ListAdapter<Reminder, ReminderAdapter.ReminderViewHolder> {
    private final ReminderClickListener clickListener;
    private final ReminderDeleteListener deleteListener;

    public interface ReminderClickListener {
        void onReminderClick(Reminder reminder);
    }

    public interface ReminderDeleteListener {
        void onReminderDelete(Reminder reminder);
    }

    public ReminderAdapter(ReminderClickListener clickListener, ReminderDeleteListener deleteListener) {
        super(new ReminderDiffCallback());
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderBinding binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ReminderViewHolder(binding, clickListener, deleteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final ItemReminderBinding binding;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        private final ReminderClickListener clickListener;
        private final ReminderDeleteListener deleteListener;

        ReminderViewHolder(ItemReminderBinding binding, 
                          ReminderClickListener clickListener,
                          ReminderDeleteListener deleteListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.clickListener = clickListener;
            this.deleteListener = deleteListener;

            binding.getRoot().setOnClickListener(v -> clickListener.onReminderClick(null));
            binding.buttonDelete.setOnClickListener(v -> deleteListener.onReminderDelete(null));
        }

        void bind(Reminder reminder) {
            binding.textTitle.setText(reminder.getTitle());
            binding.textDateTime.setText(dateFormat.format(reminder.getDateTime()));

            // Set priority indicator color
            int priorityColor;
            switch (reminder.getPriority()) {
                case HIGH:
                    priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_high);
                    break;
                case LOW:
                    priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_low);
                    break;
                default:
                    priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_medium);
            }
            binding.priorityIndicator.setColorFilter(priorityColor);

            // Show repeat indicator and info if reminder is repeating
            if (reminder.isRepeating()) {
                binding.repeatIndicator.setVisibility(View.VISIBLE);
                binding.textRepeatInfo.setVisibility(View.VISIBLE);
                String repeatText = "Repeats every " + reminder.getRepeatInterval() +
                    (reminder.getRepeatInterval() == 1 ? " day" : " days");
                binding.textRepeatInfo.setText(repeatText);
            } else {
                binding.repeatIndicator.setVisibility(View.GONE);
                binding.textRepeatInfo.setVisibility(View.GONE);
            }

            // Visual distinction for expired reminders
            boolean isExpired = reminder.getDateTime() < System.currentTimeMillis();
            binding.cardView.setAlpha(isExpired ? 0.6f : 1.0f);

            // Apply strikethrough for completed reminders
            int paintFlags = reminder.isCompleted() ? Paint.STRIKE_THRU_TEXT_FLAG : 0;
            binding.textTitle.setPaintFlags(paintFlags);
            binding.textDateTime.setPaintFlags(paintFlags);
            binding.textRepeatInfo.setPaintFlags(paintFlags);

            binding.getRoot().setOnClickListener(v -> clickListener.onReminderClick(reminder));
            binding.buttonDelete.setOnClickListener(v -> deleteListener.onReminderDelete(reminder));
        }
    }

    static class ReminderDiffCallback extends DiffUtil.ItemCallback<Reminder> {
        @Override
        public boolean areItemsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Reminder oldItem, @NonNull Reminder newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getDescription().equals(newItem.getDescription()) &&
                   oldItem.getDateTime() == newItem.getDateTime() &&
                   oldItem.isCompleted() == newItem.isCompleted() &&
                   oldItem.getPriority() == newItem.getPriority() &&
                   oldItem.isRepeating() == newItem.isRepeating() &&
                   oldItem.getRepeatInterval() == newItem.getRepeatInterval();
        }
    }
}