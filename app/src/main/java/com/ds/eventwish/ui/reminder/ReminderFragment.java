package com.ds.eventwish.ui.reminder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentReminderBinding;
import com.ds.eventwish.databinding.DialogAddReminderBinding;
import com.ds.eventwish.data.model.Reminder;
import com.ds.eventwish.ui.reminder.adapter.ReminderAdapter;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModel;
import com.ds.eventwish.workers.ReminderNotificationWorker;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ds.eventwish.MainActivity;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.ds.eventwish.data.local.ReminderDao;
import com.ds.eventwish.ui.reminder.viewmodel.ReminderViewModelFactory;

public class ReminderFragment extends Fragment {
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    
    private FragmentReminderBinding binding;
    private ReminderViewModel viewModel;
    private ReminderAdapter adapter;
    private ReminderDao reminderDao;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReminderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        reminderDao = new ReminderDao(requireContext());
        ReminderViewModelFactory factory = new ReminderViewModelFactory(reminderDao);
        viewModel = new ViewModelProvider(this, factory).get(ReminderViewModel.class);
        
        setupBottomNavigation();
        checkNotificationPermission();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
        setupObservers();
        
        // Handle notification click
        handleNotificationClick();
    }

    private void setupBottomNavigation() {
        if (getActivity() instanceof MainActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setVisibility(View.VISIBLE);
            // Set the selected item to reminder
            bottomNav.setSelectedItemId(R.id.navigation_reminder);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), 
                    "Notification permission is required for reminders", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ReminderAdapter();
        adapter.setOnReminderClickListener(this::showReminderDetails);
        binding.remindersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.remindersRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.loadReminders();
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void setupObservers() {
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> {
            adapter.submitList(reminders);
        });
    }

    private void setupFab() {
        binding.fabAddReminder.setOnClickListener(v -> showAddReminderDialog());
    }

    private void showAddReminderDialog() {
        DialogAddReminderBinding dialogBinding = DialogAddReminderBinding.inflate(getLayoutInflater());
        Calendar calendar = Calendar.getInstance();

        dialogBinding.dateInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

            datePicker.addOnPositiveButtonClickListener(date -> {
                calendar.setTimeInMillis(date);
                dialogBinding.dateInput.setText(formatDate(calendar.getTime()));
            });

            datePicker.show(getChildFragmentManager(), "date_picker");
        });

        dialogBinding.timeInput.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build();

            timePicker.addOnPositiveButtonClickListener(view -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                dialogBinding.timeInput.setText(formatTime(calendar.getTime()));
            });

            timePicker.show(getChildFragmentManager(), "time_picker");
        });

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Reminder")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialog, which) -> {
                String title = dialogBinding.titleInput.getText().toString();
                String description = dialogBinding.descriptionInput.getText().toString();
                
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!dialogBinding.dateInput.getText().toString().isEmpty() &&
                    !isValidDateTime(calendar)) {
                    return;
                }

                long dateTime = calendar.getTimeInMillis();
                Reminder reminder = new Reminder(title, description, dateTime);
                viewModel.addReminder(reminder);

                if (!dialogBinding.timeInput.getText().toString().isEmpty()) {
                    scheduleNotification(reminder);
                }

                Toast.makeText(requireContext(), "Reminder added", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void handleNotificationClick() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("reminderId")) {
            long reminderId = args.getLong("reminderId");
            viewModel.getReminder(reminderId).observe(getViewLifecycleOwner(), reminder -> {
                if (reminder != null) {
                    showReminderDetails(reminder);
                }
            });
        }
    }

    private void showReminderDetails(Reminder reminder) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(reminder.getTitle())
            .setMessage(reminder.getDescription())
            .setPositiveButton("Edit", (dialog, which) -> showEditReminderDialog(reminder))
            .setNegativeButton("Delete", (dialog, which) -> {
                viewModel.deleteReminder(reminder.getId());
                Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Close", null)
            .show();
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    private void scheduleNotification(Reminder reminder) {
        WorkManager workManager = WorkManager.getInstance(requireContext());
        
        Data inputData = new Data.Builder()
            .putString("title", reminder.getTitle())
            .putString("description", reminder.getDescription())
            .putLong("reminderId", reminder.getId())
            .build();

        long delayInMillis = reminder.getDateTime() - System.currentTimeMillis();
        
        OneTimeWorkRequest notificationWork = 
            new OneTimeWorkRequest.Builder(ReminderNotificationWorker.class)
                .setInputData(inputData)
                .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueue(notificationWork);
    }

    private void showEditReminderDialog(Reminder reminder) {
        DialogAddReminderBinding dialogBinding = DialogAddReminderBinding.inflate(getLayoutInflater());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(reminder.getDateTime());

        // Pre-fill the existing data
        dialogBinding.titleInput.setText(reminder.getTitle());
        dialogBinding.descriptionInput.setText(reminder.getDescription());
        dialogBinding.dateInput.setText(formatDate(calendar.getTime()));
        if (reminder.getDateTime() > 0) {
            dialogBinding.timeInput.setText(formatTime(calendar.getTime()));
        }

        // Setup date picker
        dialogBinding.dateInput.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(calendar.getTimeInMillis())
                .build();

            datePicker.addOnPositiveButtonClickListener(date -> {
                calendar.setTimeInMillis(date);
                dialogBinding.dateInput.setText(formatDate(calendar.getTime()));
            });

            datePicker.show(getChildFragmentManager(), "date_picker");
        });

        // Setup time picker
        dialogBinding.timeInput.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build();

            timePicker.addOnPositiveButtonClickListener(view -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                dialogBinding.timeInput.setText(formatTime(calendar.getTime()));
            });

            timePicker.show(getChildFragmentManager(), "time_picker");
        });

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Reminder")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialog, which) -> {
                String title = dialogBinding.titleInput.getText().toString();
                String description = dialogBinding.descriptionInput.getText().toString();
                
                if (title.isEmpty()) {
                    Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update reminder with new values
                reminder.setTitle(title);
                reminder.setDescription(description);
                reminder.setDateTime(calendar.getTimeInMillis());
                
                viewModel.updateReminder(reminder);

                // Schedule notification if time is set
                if (!dialogBinding.timeInput.getText().toString().isEmpty()) {
                    scheduleNotification(reminder);
                }

                Toast.makeText(requireContext(), "Reminder updated", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Add helper method to validate date and time
    private boolean isValidDateTime(Calendar calendar) {
        Calendar now = Calendar.getInstance();
        if (calendar.before(now)) {
            Toast.makeText(requireContext(), 
                "Please select a future date and time", 
                Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 