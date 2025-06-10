package com.ds.eventwish.ui.reminder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
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
import com.ds.eventwish.ui.base.BaseFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.data.model.Reminder.Priority;
import com.ds.eventwish.databinding.DialogReminderBinding;
import com.ds.eventwish.databinding.DialogReminderDetailsBinding;
import com.ds.eventwish.databinding.FragmentReminderBinding;
import com.ds.eventwish.ui.reminder.adapter.ReminderAdapter;
import com.ds.eventwish.ui.reminder.adapter.ReminderFilterAdapter;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import com.ds.eventwish.utils.CountdownNotificationScheduler;
import com.ds.eventwish.utils.ReminderScheduler;
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
import java.util.concurrent.TimeUnit;

public class ReminderFragment extends BaseFragment {
    private static final int MENU_CLEAR_ALL = 1;
    private FragmentReminderBinding binding;
    private ReminderViewModel viewModel;
    private ReminderAdapter adapter;
    private ReminderFilterAdapter filterAdapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // This fragment requires authentication (enforced by BaseFragment)
        setHasOptionsMenu(true);
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d("ReminderFragment", "Notification permission granted");
                    // Permission granted, you can schedule notifications now
                } else {
                    Log.d("ReminderFragment", "Notification permission denied");
                    // Show a message explaining why notifications are important
                    showNotificationPermissionExplanation();
                }
            }
        );
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
        
        // Check notification permission when the fragment is created
        checkNotificationPermission();
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
                onReminderSwiped(reminder, direction);
            }
        }).attachToRecyclerView(binding.remindersRecyclerView);
    }

    private void setupFabs() {
        binding.fabAddReminder.setOnClickListener(v -> showAddReminderDialog());
    }

    private void setupObservers() {
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.submitList(reminders);
            updateEmptyState(reminders);
            updateCounts();
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
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
                
                // Schedule countdown notifications
                try {
                    CountdownNotificationScheduler.scheduleCountdownNotifications(requireContext(), reminder);
                    Log.d("ReminderFragment", "Scheduled countdown notifications for reminder ID: " + reminder.getId());
                    
                    // Show a toast or snackbar to confirm scheduling
                    showInfoSnackbar("Reminder set with countdown notifications");
                } catch (Exception e) {
                    Log.e("ReminderFragment", "Failed to schedule countdown notifications: " + e.getMessage(), e);
                    showErrorSnackbar("Reminder set but countdown notifications failed");
                }
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

        // Add countdown info if applicable
        long daysUntilReminder = TimeUnit.MILLISECONDS.toDays(reminder.getDateTime() - System.currentTimeMillis());
        if (daysUntilReminder > 0 && daysUntilReminder <= 7 && !reminder.isCompleted()) {
            dialogBinding.countdownChip.setVisibility(View.VISIBLE);
            dialogBinding.countdownChip.setText(daysUntilReminder + " days left");
        } else {
            dialogBinding.countdownChip.setVisibility(View.GONE);
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
            
            // Cancel countdown notifications
            try {
                CountdownNotificationScheduler.cancelCountdownNotifications(requireContext(), reminder.getId());
            } catch (Exception e) {
                Log.e("ReminderFragment", "Failed to cancel countdown notifications: " + e.getMessage());
            }
            
            showUndoSnackbar(
                getString(R.string.reminder_deleted),
                () -> {
                    reminder.setId(reminder.getId());
                    viewModel.updateReminder(reminder);
                    
                    // Re-schedule countdown notifications if needed
                    long daysLeft = TimeUnit.MILLISECONDS.toDays(reminder.getDateTime() - System.currentTimeMillis());
                    if (daysLeft > 3 && !reminder.isCompleted()) {
                        try {
                            CountdownNotificationScheduler.scheduleCountdownNotifications(requireContext(), reminder);
                        } catch (Exception e) {
                            Log.e("ReminderFragment", "Failed to re-schedule countdown notifications: " + e.getMessage());
                        }
                    }
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
            case MEDIUM:
                dialogBinding.priorityMedium.setChecked(true);
                break;
            case LOW:
                dialogBinding.priorityLow.setChecked(true);
                break;
        }
        
        // Set repeat
        dialogBinding.repeatSwitch.setChecked(reminder.isRepeating());
        if (reminder.isRepeating()) {
            dialogBinding.repeatIntervalLayout.setVisibility(View.VISIBLE);
            dialogBinding.repeatIntervalInput.setText(String.valueOf(reminder.getRepeatInterval()));
        } else {
            dialogBinding.repeatIntervalLayout.setVisibility(View.GONE);
        }
        
        // Set button listeners
        dialogBinding.dateButton.setOnClickListener(v -> showDatePicker(dialogBinding));
        dialogBinding.timeButton.setOnClickListener(v -> showTimePicker(dialogBinding));
        dialogBinding.repeatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dialogBinding.repeatIntervalLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        // Create dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Reminder")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialogInterface, which) -> {
                // Get priority
                Reminder.Priority priority;
                if (dialogBinding.priorityHigh.isChecked()) {
                    priority = Reminder.Priority.HIGH;
                } else if (dialogBinding.priorityLow.isChecked()) {
                    priority = Reminder.Priority.LOW;
                } else {
                    priority = Reminder.Priority.MEDIUM;
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
                        // Use default value of 1
                    }
                }
                
                // Cancel existing countdown notifications
                try {
                    CountdownNotificationScheduler.cancelCountdownNotifications(requireContext(), reminder.getId());
                } catch (Exception e) {
                    Log.e("ReminderFragment", "Failed to cancel existing countdown notifications: " + e.getMessage());
                }
                
                // Update reminder
                reminder.setTitle(dialogBinding.titleInput.getText().toString().trim());
                reminder.setDescription(dialogBinding.descriptionInput.getText().toString().trim());
                reminder.setDateTime(calendar.getTimeInMillis());
                reminder.setPriority(priority);
                reminder.setRepeating(isRepeating);
                reminder.setRepeatInterval(repeatInterval);
                
                viewModel.updateReminder(reminder);
                
                // Schedule new countdown notifications if needed
                long daysUntilReminder = TimeUnit.MILLISECONDS.toDays(reminder.getDateTime() - System.currentTimeMillis());
                try {
                    // Always schedule countdown notifications regardless of days until reminder
                    // The scheduler will handle the logic for determining if notifications should be scheduled
                    CountdownNotificationScheduler.scheduleCountdownNotifications(requireContext(), reminder);
                    Log.d("ReminderFragment", "Scheduled countdown notifications for reminder: " + reminder.getId());
                    showInfoSnackbar("Countdown reminders updated");
                } catch (Exception e) {
                    Log.e("ReminderFragment", "Failed to schedule countdown notifications: " + e.getMessage(), e);
                    showErrorSnackbar("Failed to schedule countdown notifications");
                }
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
        // Before creating the reminder, check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                showNotificationPermissionRationale();
                return null;
            }
        }

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

    private void showInfoSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void updateCounts() {
        List<Reminder> allReminders = viewModel.getReminderDao().getAllReminders();
        if (allReminders == null) {
            binding.textTodayCount.setText("0 Today");
            binding.textUpcomingCount.setText("0 Upcoming");
            return;
        }

        // Count today's reminders
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.setTimeInMillis(today.getTimeInMillis());
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        
        int todayCount = (int) allReminders.stream()
            .filter(r -> {
                long reminderTime = r.getDateTime();
                return reminderTime >= today.getTimeInMillis() && 
                       reminderTime < tomorrow.getTimeInMillis() &&
                       !r.isCompleted();
            })
            .count();
        
        // Count upcoming reminders
        long now = System.currentTimeMillis();
        int upcomingCount = (int) allReminders.stream()
            .filter(r -> r.getDateTime() > tomorrow.getTimeInMillis() && !r.isCompleted())
            .count();
        
        // Update TextViews
        binding.textTodayCount.setText(todayCount + " Today");
        binding.textUpcomingCount.setText(upcomingCount + " Upcoming");
    }

    private void onReminderSwiped(Reminder reminder, int direction) {
        if (direction == ItemTouchHelper.LEFT) {
            viewModel.deleteReminder(reminder);
            // Store original ID when restoring deleted reminder
            long originalId = reminder.getId();
            
            // Cancel any scheduled countdown notifications
            try {
                CountdownNotificationScheduler.cancelCountdownNotifications(requireContext(), originalId);
            } catch (Exception e) {
                Log.e("ReminderFragment", "Failed to cancel countdown notifications: " + e.getMessage());
            }
            
            showUndoSnackbar("Reminder deleted", () -> {
                reminder.setId(originalId);
                viewModel.updateReminder(reminder);
                
                // Re-schedule countdown notifications if needed
                long daysUntilReminder = TimeUnit.MILLISECONDS.toDays(reminder.getDateTime() - System.currentTimeMillis());
                if (daysUntilReminder > 3) {
                    try {
                        CountdownNotificationScheduler.scheduleCountdownNotifications(requireContext(), reminder);
                    } catch (Exception e) {
                        Log.e("ReminderFragment", "Failed to re-schedule countdown notifications: " + e.getMessage());
                    }
                }
            });
        } else if (direction == ItemTouchHelper.RIGHT) {
            viewModel.toggleReminderCompleted(reminder);
            String message = reminder.isCompleted() ? "Reminder marked as completed" : "Reminder marked as active";
            
            // If marked as completed, cancel countdown notifications
            if (reminder.isCompleted()) {
                try {
                    CountdownNotificationScheduler.cancelCountdownNotifications(requireContext(), reminder.getId());
                } catch (Exception e) {
                    Log.e("ReminderFragment", "Failed to cancel countdown notifications: " + e.getMessage());
                }
            }
            
            showUndoSnackbar(message, () -> {
                reminder.setCompleted(!reminder.isCompleted());
                viewModel.updateReminder(reminder);
                
                // If uncompleted and far enough in the future, reschedule countdown notifications
                if (!reminder.isCompleted()) {
                    long daysUntilReminder = TimeUnit.MILLISECONDS.toDays(reminder.getDateTime() - System.currentTimeMillis());
                    if (daysUntilReminder > 3) {
                        try {
                            CountdownNotificationScheduler.scheduleCountdownNotifications(requireContext(), reminder);
                        } catch (Exception e) {
                            Log.e("ReminderFragment", "Failed to re-schedule countdown notifications: " + e.getMessage());
                        }
                    }
                }
            });
        }
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
            List<Reminder> currentReminders = new ArrayList<>();
            if (viewModel.getReminders().getValue() != null) {
                currentReminders.addAll(viewModel.getReminders().getValue());
            }
            
            if (currentReminders.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.no_reminders_to_clear, Snackbar.LENGTH_SHORT).show();
                return true;
            }

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_all_title)
                .setMessage(R.string.clear_all_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Make a deep copy of the reminders before clearing
                    final List<Reminder> remindersCopy = new ArrayList<>();
                    for (Reminder reminder : currentReminders) {
                        remindersCopy.add(new Reminder(
                            reminder.getId(),
                            reminder.getTitle(),
                            reminder.getDescription(),
                            reminder.getDateTime(),
                            reminder.isCompleted(),
                            reminder.getPriority(),
                            reminder.isRepeating(),
                            reminder.getRepeatInterval()
                        ));
                        
                        // Cancel any scheduled notifications
                        try {
                            ReminderScheduler.cancelReminder(requireContext(), reminder.getId());
                        } catch (Exception e) {
                            Log.e("ReminderFragment", "Failed to cancel reminder: " + e.getMessage());
                        }
                    }
                    
                    // Clear all reminders
                    viewModel.clearAllReminders();
                    
                    // Show undo snackbar
                    showUndoSnackbar(
                        getString(R.string.cleared_all_reminders),
                        () -> {
                            // Restore all reminders
                            for (Reminder reminder : remindersCopy) {
                                viewModel.saveReminder(reminder);
                            }
                        }
                    );
                    
                    // Update UI
                    updateCounts();
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
        viewModel.markAllAsRead();
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

    private void updateEmptyState(List<Reminder> reminders) {
        boolean isEmpty = reminders == null || reminders.isEmpty();
        binding.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.remindersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void checkNotificationPermission() {
        // Only need to check permission on Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                
                // Check if we should show the rationale first
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionRationale();
                } else {
                    // Request the permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
    }

    private void showNotificationPermissionRationale() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            })
            .setNegativeButton(R.string.no, (dialog, which) -> {
                showNotificationPermissionExplanation();
            })
            .show();
    }

    private void showNotificationPermissionExplanation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_disabled_title)
            .setMessage(R.string.notification_disabled_message)
            .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                openAppSettings();
            })
            .setNegativeButton(R.string.not_now, null)
            .show();
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("ReminderFragment", "Error opening app settings", e);
            showErrorSnackbar("Couldn't open app settings");
        }
    }
}