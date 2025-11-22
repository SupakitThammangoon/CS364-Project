package com.example.cs364_project;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

// หน้าในการเพิ่มปริมาณน้ำ + เปิดการเเจ้งเตือน + progress bar

public class GoalFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView tvWaterTotal;

    private LinearLayout btnAdd250, btnAdd500, btnAdd750, btnAddCustom;
    private EditText edtCustomAmount;

    private Switch switchReminder;
    private EditText edtInterval;
    private LinearLayout btnSaveInterval;
    private boolean isReminderOn = false;
    private long intervalMillis = 0L;

    public GoalFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestNotificationPermissionIfNeeded();

        LinearLayout homeContent = view.findViewById(R.id.homeContent);
        int padLeft   = homeContent.getPaddingLeft();
        int padTop    = homeContent.getPaddingTop();
        int padRight  = homeContent.getPaddingRight();
        int padBottom = homeContent.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(homeContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    padLeft,
                    padTop + systemBars.top,
                    padRight,
                    padBottom
            );
            return insets;
        });

        progressBar     = view.findViewById(R.id.progressBar);
        tvWaterTotal    = view.findViewById(R.id.tvWaterTotal);

        btnAdd250       = view.findViewById(R.id.btnAdd250);
        btnAdd500       = view.findViewById(R.id.btnAdd500);
        btnAdd750       = view.findViewById(R.id.btnAdd750);
        btnAddCustom    = view.findViewById(R.id.btnAddCustom);
        edtCustomAmount = view.findViewById(R.id.edtCustomAmount);

        switchReminder  = view.findViewById(R.id.switchReminder);
        edtInterval     = view.findViewById(R.id.edtInterval);
        btnSaveInterval = view.findViewById(R.id.btnSaveInterval);

        if (edtInterval != null) {
            edtInterval.setFocusable(false);
            edtInterval.setClickable(true);
        }

        Bundle args = getArguments();
        if (args != null && args.containsKey("TOTAL_ML")) {
            int target = args.getInt("TOTAL_ML", 0);
            WaterTracker.setTargetMl(target);
        }

        loadReminderPrefs();
        refreshProgressUi();

        btnAdd250.setOnClickListener(v -> addWater(250));
        btnAdd500.setOnClickListener(v -> addWater(500));
        btnAdd750.setOnClickListener(v -> addWater(750));

        btnAddCustom.setOnClickListener(v -> {
            String text = edtCustomAmount.getText().toString().trim();

            if (text.isEmpty()) {
                String warn1 = getString(R.string.warn1);
                Toast.makeText(requireContext(), warn1, Toast.LENGTH_SHORT).show();
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(text);
            } catch (Exception e) {
                String warn2 = getString(R.string.warn2);
                Toast.makeText(requireContext(), warn2, Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                String warn3 = getString(R.string.warn3);
                Toast.makeText(requireContext(), warn3, Toast.LENGTH_SHORT).show();
                return;
            }

            addWater(amount);
            edtCustomAmount.setText("");
        });

        if (edtInterval != null) {
            edtInterval.setOnClickListener(v -> showIntervalPickerDialog());
        }

        if (btnSaveInterval != null) {
            btnSaveInterval.setOnClickListener(v -> {
                String text = edtInterval != null ? edtInterval.getText().toString().trim() : "";

                if (text.isEmpty()) {
                    String select_time_range = getString(R.string.time_range);
                    Toast.makeText(requireContext(), select_time_range, Toast.LENGTH_SHORT).show();
                    return;
                }

                Long millis = parseIntervalToMillis(text);
                if (millis == null) {
                    String time_error1 = getString(R.string.time_range_error);
                    Toast.makeText(requireContext(), time_error1, Toast.LENGTH_SHORT).show();
                } else {
                    intervalMillis = millis;
                    String msg1 = getString(R.string.toast_1);
                    String msg2 = getString(R.string.hint_notify);
                    Toast.makeText(
                            requireContext(),
                            msg1 + text + msg2,
                            Toast.LENGTH_SHORT
                    ).show();

                    if (isReminderOn && intervalMillis > 0) {
                        ReminderScheduler.scheduleRepeatingAlarm(requireContext(), intervalMillis);
                    }
                    saveReminderPrefs();
                }
            });
        }

        if (switchReminder != null) {
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isReminderOn = isChecked;
                if (isChecked) {
                    String open_notify = getString(R.string.open_notify);
                    Toast.makeText(requireContext(), open_notify, Toast.LENGTH_SHORT).show();

                    if (intervalMillis > 0) {
                        ReminderScheduler.scheduleRepeatingAlarm(requireContext(), intervalMillis);
                    } else {
                        String select_time_range = getString(R.string.time_range);
                        Toast.makeText(requireContext(), select_time_range, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String close_notify = getString(R.string.close_notify);
                    Toast.makeText(requireContext(), close_notify, Toast.LENGTH_SHORT).show();
                    ReminderScheduler.cancelAlarm(requireContext());
                }

                saveReminderPrefs();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // กลับมาหน้านี้จาก History ให้ sync อีกครั้ง
        refreshProgressUi();
    }

    private void addWater(int amount) {
        int target = WaterTracker.getTargetMl();
        if (target <= 0) {
            String warn4 = getString(R.string.warn4);
            Toast.makeText(
                    requireContext(),
                    warn4,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        WaterTracker.addEntry(amount);
        refreshProgressUi();
    }

    private void refreshProgressUi() {
        int target = WaterTracker.getTargetMl();
        int intake = WaterTracker.getTotalIntakeMl();
        int progress = WaterTracker.getProgressValue();

        if (target <= 0) {
            progressBar.setMax(1);
            progressBar.setProgress(0);
            String compare = getString(R.string.compare);
            tvWaterTotal.setText(compare);
        } else {
            progressBar.setMax(target);
            progressBar.setProgress(progress);
            String ml = getString(R.string.ml);
            tvWaterTotal.setText(intake + " " + ml + " / " + target + " " + ml);
        }
    }

    private void showIntervalPickerDialog() {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_interval_picker, null);
        NumberPicker pickerMinute = dialogView.findViewById(R.id.pickerMinute);

        pickerMinute.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        pickerMinute.setMinValue(0);
        pickerMinute.setMaxValue(59);

        if (edtInterval != null) {
            String current = edtInterval.getText().toString().trim();
            if (current.matches("\\d{1,2}")) {
                try {
                    int m = Integer.parseInt(current);
                    if (m >= 0 && m <= 59) {
                        pickerMinute.setValue(m);
                    } else {
                        pickerMinute.setValue(0);
                    }
                } catch (NumberFormatException e) {
                    pickerMinute.setValue(0);
                }
            } else {
                pickerMinute.setValue(0);
            }
        }

        String select_time = getString(R.string.time_range);
        String ok = getString(R.string.ok);
        String cancel = getString(R.string.cancel);

        new AlertDialog.Builder(requireContext())
                .setTitle(select_time)
                .setView(dialogView)
                .setPositiveButton(ok, (dialog, which) -> {
                    int m = pickerMinute.getValue();
                    String text = String.valueOf(m);

                    if (edtInterval != null) {
                        edtInterval.setText(text);
                    }

                    intervalMillis = m * 60L * 1000L;
                    saveReminderPrefs();
                })
                .setNegativeButton(cancel, null)
                .show();
    }

    private Long parseIntervalToMillis(String minutesText) {
        try {
            int m = Integer.parseInt(minutesText);
            if (m < 0 || m > 59) {
                return null;
            }
            long totalSec = m * 60L;
            return totalSec * 1000L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void saveReminderPrefs() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("reminder_on", isReminderOn)
                .putLong("interval_millis", intervalMillis)
                .apply();
    }

    private void loadReminderPrefs() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE);

        isReminderOn = prefs.getBoolean("reminder_on", false);
        intervalMillis = prefs.getLong("interval_millis", 0L);

        if (switchReminder != null) {
            switchReminder.setChecked(isReminderOn);
        }

        if (edtInterval != null && intervalMillis > 0) {
            int minutes = (int) (intervalMillis / (60L * 1000L));
            edtInterval.setText(String.valueOf(minutes));
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }
}
