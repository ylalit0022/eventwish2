package com.ds.eventwish.ui.reminder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.model.Reminder.Priority;
import com.ds.eventwish.databinding.DialogReminderBinding;
import com.ds.eventwish.databinding.DialogReminderDetailsBinding;
import com.ds.eventwish.databinding.FragmentReminderBinding;
import com.ds.eventwish.ui.reminder.adapter.ReminderAdapter;
import com.ds.eventwish.ui.reminder.adapter.ReminderFilterAdapter;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.flexbox.AlignItems;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReminderFragment extends Fragment {
    private static final int MENU_CLEAR_ALL = 1;
    private FragmentReminderBinding binding;
    private ReminderViewModel viewModel;
    private ReminderAdapter adapter;
    private ReminderFilterAdapter filterAdapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReminderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel with ReminderDao
        ReminderViewModel.Factory factory = new ReminderViewModel.Factory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(ReminderViewModel.class);
        
        setupRecyclerView();
        setupSwipeActions();
        setupFabs();
        setupObservers();
        setupSwipeRefresh();
    }

    private void setupRecyclerView() {
        adapter = new ReminderAdapter(
            this::showReminderDetails,
            reminder -> viewModel.deleteReminder(reminder)
        );
        binding.remindersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.remindersRecyclerView.setAdapter(adapter);

        // Setup filter adapter
        filterAdapter = new ReminderFilterAdapter(filter -> viewModel.setFilter(filter));
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(requireContext());
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);
        flexboxLayoutManager.setAlignItems(AlignItems.FLEX_START);
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        binding.filterContainer.setLayoutManager(flexboxLayoutManager);
        binding.filterContainer.setAdapter(filterAdapter);
    }

    private void setupSwipeActions() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                @NonNull RecyclerView.ViewHolder viewHolder, 
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Reminder reminder = adapter.getCurrentList().get(position);
                
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.deleteReminder(reminder);
                    // Store original ID when restoring deleted reminder
                    long originalId = reminder.getId();
                    showUndoSnackbar("Reminder deleted", () -> {
                        reminder.setId(originalId);
                        viewModel.updateReminder(reminder);
                    });
                } else if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.toggleReminderCompleted(reminder);
                    String message = reminder.isCompleted() ? "Reminder marked as completed" : "Reminder marked as active";
                    showUndoSnackbar(message, () -> {
                        reminder.setCompleted(!reminder.isCompleted());
                        viewModel.updateReminder(reminder);
                    });
                }
            }
        }).attachToRecyclerView(binding.remindersRecyclerView);
    }

    private void setupFabs() {
        binding.fabAddReminder.setOnClickListener(v -> showAddReminderDialog());
    }

    private void setupObservers() {
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.submitList(reminders);
            
            // Update reminder count text
            String countText = reminders.size() == 1 ? 
                getString(R.string.reminder_count_single) :
                getString(R.string.reminder_count, reminders.size());
            binding.textTodayCount.setText(countText);
            
            // Show/hide empty state
            if (reminders.isEmpty()) {
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.remindersRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.remindersRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getBadgeCount().observe(getViewLifecycleOwner(), count -> {
            // Update badge count in bottom navigation if needed
            // This will be handled by the activity
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadReminders());
    }

    private void showAddReminderDialog() {
        DialogReminderBinding dialogBinding = DialogReminderBinding.inflate(getLayoutInflater());
        calendar.setTimeInMillis(System.currentTimeMillis());
        
        // Set up date and time buttons
        updateDateButton(dialogBinding);
        updateTimeButton(dialogBinding);
        
        dialogBinding.dateButton.setOnClickListener(v -> showDatePicker(dialogBinding));
        dialogBinding.timeButton.setOnClickListener(v -> showTimePicker(dialogBinding));
        
        // Set up repeat switch
        dialogBinding.repeatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dialogBinding.repeatIntervalLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Reminder")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialogInterface, which) -> {
                Reminder reminder = createReminderFromDialog(dialogBinding);
                viewModel.saveReminder(reminder);
            })
            .setNegativeButton("Cancel", null)
            .create();
            
        dialog.show();
    }

    private void showReminderDetails(Reminder reminder) {
        DialogReminderDetailsBinding dialogBinding = DialogReminderDetailsBinding.inflate(getLayoutInflater());
        dialogBinding.titleText.setText(reminder.getTitle());
        dialogBinding.descriptionText.setText(reminder.getDescription());
        dialogBinding.dateChip.setText(dateFormat.format(reminder.getDateTime()));
        
        // Set priority chip with user-friendly text
        int priorityColor = getResources().getColor(
            reminder.getPriority() == Reminder.Priority.HIGH ? R.color.priority_high :
            reminder.getPriority() == Reminder.Priority.MEDIUM ? R.color.priority_medium :
            R.color.priority_low
        );
        dialogBinding.priorityChip.setChipBackgroundColor(ColorStateList.valueOf(priorityColor));
        String priorityText = getString(
            reminder.getPriority() == Reminder.Priority.HIGH ? R.string.priority_high :
            reminder.getPriority() == Reminder.Priority.MEDIUM ? R.string.priority_medium :
            R.string.priority_low
        );
        dialogBinding.priorityChip.setText(priorityText);
        
        if (reminder.isRepeating()) {
            dialogBinding.repeatChip.setVisibility(View.VISIBLE);
            dialogBinding.repeatChip.setText(getString(R.string.repeats_every_n_days, reminder.getRepeatInterval()));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.getRoot())
            .create();
        
        // Set up edit button
        dialogBinding.editButton.setOnClickListener(v -> {
            dialog.dismiss();
            showEditReminderDialog(reminder);
        });

        // Set up delete button with undo functionality
        dialogBinding.deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.deleteReminder(reminder);
            showUndoSnackbar(
                getString(R.string.reminder_deleted),
                () -> {
                    reminder.setId(reminder.getId());
                    viewModel.updateReminder(reminder);
                }
            );
        });

        dialog.show();
    }

    private void showEditReminderDialog(Reminder reminder) {
        DialogReminderBinding dialogBinding = DialogReminderBinding.inflate(getLayoutInflater());
        
        // Pre-fill existing values
        dialogBinding.titleInput.setText(reminder.getTitle());
        dialogBinding.descriptionInput.setText(reminder.getDescription());
        
        // Set date
        calendar.setTimeInMillis(reminder.getDateTime());
        updateDateButton(dialogBinding);
        updateTimeButton(dialogBinding);
        
        // Set priority
        switch (reminder.getPriority()) {
            case HIGH:
                dialogBinding.priorityHigh.setChecked(true);
                break;
            case LOW:
                dialogBinding.priorityLow.setChecked(true);
                break;
            default:
                dialogBinding.priorityMedium.setChecked(true);
        }
        
        // Set repeat options
        dialogBinding.repeatSwitch.setChecked(reminder.isRepeating());
        if (reminder.isRepeating()) {
            dialogBinding.repeatIntervalInput.setText(String.valueOf(reminder.getRepeatInterval()));
            dialogBinding.repeatIntervalLayout.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.repeatIntervalLayout.setVisibility(View.GONE);
        }
        
        dialogBinding.repeatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dialogBinding.repeatIntervalLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        // Set date/time pickers
        dialogBinding.dateButton.setOnClickListener(v -> showDatePicker(dialogBinding));
        dialogBinding.timeButton.setOnClickListener(v -> showTimePicker(dialogBinding));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_reminder)
            .setView(dialogBinding.getRoot())
            .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                // Update existing reminder instead of creating new one
                reminder.setTitle(dialogBinding.titleInput.getText().toString().trim());
                reminder.setDescription(dialogBinding.descriptionInput.getText().toString().trim());
                
                // Get priority
                Reminder.Priority priority = Reminder.Priority.MEDIUM;
                int checkedId = dialogBinding.priorityGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.priorityHigh) {
                    priority = Reminder.Priority.HIGH;
                } else if (checkedId == R.id.priorityLow) {
                    priority = Reminder.Priority.LOW;
                }
                
                // Get repeat settings
                boolean isRepeating = dialogBinding.repeatSwitch.isChecked();
                int repeatInterval = 0;
                if (isRepeating) {
                    try {
                        repeatInterval = Integer.parseInt(dialogBinding.repeatIntervalInput.getText().toString());
                        if (repeatInterval <= 0) {
                            dialogBinding.repeatIntervalInput.setError(getString(R.string.error_invalid_interval));
                            return;
                        }
                    } catch (NumberFormatException e) {
                        dialogBinding.repeatIntervalInput.setError(getString(R.string.error_invalid_interval));
                        return;
                    }
                }
                
                // Update reminder
                reminder.setDateTime(calendar.getTimeInMillis());
                reminder.setPriority(priority);
                reminder.setRepeating(isRepeating);
                reminder.setRepeatInterval(repeatInterval);
                
                viewModel.updateReminder(reminder);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
            
        dialog.show();
    }

    private void showDatePicker(DialogReminderBinding binding) {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateButton(binding);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(DialogReminderBinding binding) {
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateTimeButton(binding);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    private void updateDateButton(DialogReminderBinding binding) {
        binding.dateButton.setText(dateFormat.format(calendar.getTime()));
    }

    private void updateTimeButton(DialogReminderBinding binding) {
        binding.timeButton.setText(timeFormat.format(calendar.getTime()));
    }

    private Reminder createReminderFromDialog(DialogReminderBinding binding) {
        String title = binding.titleInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();
        
        Reminder.Priority priority;
        if (binding.priorityHigh.isChecked()) {
            priority = Reminder.Priority.HIGH;
        } else if (binding.priorityLow.isChecked()) {
            priority = Reminder.Priority.LOW;
        } else {
            priority = Reminder.Priority.MEDIUM;
        }
        
        boolean isRepeating = binding.repeatSwitch.isChecked();
        int repeatInterval = 1;
        if (isRepeating) {
            try {
                repeatInterval = Integer.parseInt(binding.repeatIntervalInput.getText().toString());
            } catch (NumberFormatException e) {
                // Use default value of 1
            }
        }
        
        return new Reminder(
            title,
            description,
            calendar.getTimeInMillis(),
            priority,
            isRepeating,
            repeatInterval
        );
    }

    private void showUndoSnackbar(String message, Runnable undoAction) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
            .setAction("Undo", v -> {
                if (undoAction != null) {
                    undoAction.run();
                }
            })
            .show();
    }

    private void showErrorSnackbar(String error) {
        Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem clearItem = menu.add(Menu.NONE, MENU_CLEAR_ALL, Menu.NONE, R.string.clear_all);
        clearItem.setIcon(R.drawable.ic_delete);
        clearItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == MENU_CLEAR_ALL) {
            List<Reminder> currentReminders = new ArrayList<>(viewModel.getReminders().getValue());
            if (currentReminders == null || currentReminders.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.no_reminders_to_clear, Snackbar.LENGTH_SHORT).show();
                return true;
            }

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_all_title)
                .setMessage(R.string.clear_all_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    viewModel.clearAllReminders();
                    showUndoSnackbar(
                        getString(R.string.cleared_all_reminders),
                        () -> {
                            for (Reminder reminder : currentReminders) {
                                // Preserve original IDs when restoring cleared reminders
                                viewModel.updateReminder(reminder);
                            }
                        }
                    );
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}