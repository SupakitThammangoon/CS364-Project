package com.example.cs364_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomeActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvWaterTotal;

    private LinearLayout btnAdd250, btnAdd500, btnAdd750, btnAddCustom;
    private EditText edtCustomAmount;

    // ====== เพิ่มสำหรับส่วนแจ้งเตือน ======
    private Switch switchReminder;
    private EditText edtInterval;
    private LinearLayout btnSaveInterval;
    private boolean isReminderOn = false;
    private long intervalMillis = 0L;

    private int totalMl;       // ปริมาณน้ำที่ต้องดื่มทั้งหมด (รับจาก MainActivity)
    private int currentMl = 0; // ปริมาณน้ำที่ดื่มแล้ววันนี้

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // ขอสิทธิแจ้งเตือน (สำหรับ Android 13+ / targetSdk 36)
        requestNotificationPermissionIfNeeded();

        // จัดการ Edge-to-edge โดยไม่ทำให้ padding XML หาย
        LinearLayout homeContent = findViewById(R.id.homeContent);
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


        // ผูก View
        progressBar = findViewById(R.id.progressBar);
        tvWaterTotal = findViewById(R.id.tvWaterTotal);

        btnAdd250 = findViewById(R.id.btnAdd250);
        btnAdd500 = findViewById(R.id.btnAdd500);
        btnAdd750 = findViewById(R.id.btnAdd750);
        btnAddCustom = findViewById(R.id.btnAddCustom);
        edtCustomAmount = findViewById(R.id.edtCustomAmount);

        // ====== ผูก View ของส่วนแจ้งเตือน (เพิ่มใหม่) ======
        switchReminder = findViewById(R.id.switchReminder);
        edtInterval    = findViewById(R.id.edtInterval);
        btnSaveInterval = findViewById(R.id.btnSaveInterval);

        // ทำให้ช่อง interval ไม่เด้งคีย์บอร์ด แต่ใช้กดเปิดตัวเลือกแทน
        if (edtInterval != null) {
            edtInterval.setFocusable(false);
            edtInterval.setClickable(true);
        }

        // โหลดค่าการแจ้งเตือนที่เคยบันทึกไว้ (ก่อนตั้ง listener)
        loadReminderPrefs();

        // รับค่า totalMl จาก MainActivity
        totalMl = getIntent().getIntExtra("TOTAL_ML", 0);

        // ตั้งค่าเริ่มต้น
        progressBar.setMax(totalMl);
        progressBar.setProgress(currentMl);
        updateWaterText();

        // --- ปุ่มเพิ่มน้ำแต่ละขนาด ---
        btnAdd250.setOnClickListener(v -> addWater(250));
        btnAdd500.setOnClickListener(v -> addWater(500));
        btnAdd750.setOnClickListener(v -> addWater(750));

        // --- ปุ่มเพิ่มน้ำกำหนดเอง ---
        btnAddCustom.setOnClickListener(v -> {
            String text = edtCustomAmount.getText().toString().trim();

            if (text.isEmpty()) {
                String warn1 = getString(R.string.warn1);
                Toast.makeText(this, warn1, Toast.LENGTH_SHORT).show();
                return;
            }

            int amount;

            try {
                amount = Integer.parseInt(text);
            } catch (Exception e) {
                String warn2 = getString(R.string.warn2);
                Toast.makeText(this, warn2, Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                String warn3 = getString(R.string.warn3);
                Toast.makeText(this, warn3, Toast.LENGTH_SHORT).show();
                return;
            }

            addWater(amount);
            edtCustomAmount.setText(""); // เคลียร์ช่องหลังเพิ่มเสร็จ
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(0,0);
                return true;
            }

            return false;
        });

        // TODO : สำหรับส่วนแจ้งเตือน

        // เมื่อกดที่ช่อง Minute ให้เปิด dialog เลือกเวลาแบบเลื่อน
        if (edtInterval != null) {
            edtInterval.setOnClickListener(v -> showIntervalPickerDialog());
        }

        // ปุ่มบันทึกช่วงเวลาแจ้งเตือน
        if (btnSaveInterval != null) {
            btnSaveInterval.setOnClickListener(v -> {
                String text = edtInterval != null ? edtInterval.getText().toString().trim() : "";

                if (text.isEmpty()) {
                    String select_time_range = getString(R.string.time_range);
                    Toast.makeText(this, select_time_range, Toast.LENGTH_SHORT).show();
                    return;
                }

                Long millis = parseIntervalToMillis(text);
                if (millis == null) {
                    String time_error1 = getString(R.string.time_range_error);
                    Toast.makeText(this, time_error1, Toast.LENGTH_SHORT).show();
                } else {
                    intervalMillis = millis;
                    String msg1 = getString(R.string.toast_1);
                    String msg2 = getString(R.string.hint_notify);
                    Toast.makeText(this,
                            msg1 + text + msg2,
                            Toast.LENGTH_SHORT).show();

                    // ถ้าเปิดสวิตช์อยู่ → ตั้ง Alarm ทันที
                    if (isReminderOn && intervalMillis > 0) {
                        ReminderScheduler.scheduleRepeatingAlarm(this, intervalMillis);
                    }

                    // บันทึกค่าลง SharedPreferences
                    saveReminderPrefs();
                }
            });
        }

        // สวิตช์เปิด/ปิดแจ้งเตือน
        if (switchReminder != null) {
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isReminderOn = isChecked;
                if (isChecked) {
                    String open_notify = getString(R.string.open_notify);
                    Toast.makeText(this, open_notify, Toast.LENGTH_SHORT).show();

                    if (intervalMillis > 0) {
                        ReminderScheduler.scheduleRepeatingAlarm(this, intervalMillis);
                    } else {
                        // ยังไม่ได้เลือกเวลา
                        String select_time_range = getString(R.string.time_range);
                        Toast.makeText(this, select_time_range, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String close_notify = getString(R.string.close_notify);
                    Toast.makeText(this, close_notify, Toast.LENGTH_SHORT).show();
                    ReminderScheduler.cancelAlarm(this);
                }

                // เซฟสถานะสวิตช์ + interval ปัจจุบัน
                saveReminderPrefs();
            });
        }

    }

    // ฟังก์ชันเพิ่มปริมาณน้ำ
    private void addWater(int amount) {
        currentMl += amount;

        if (currentMl > totalMl) {
            currentMl = totalMl; // ห้ามเกินเป้า
        }

        progressBar.setProgress(currentMl);
        updateWaterText();
    }

    // อัปเดตข้อความ "xxx ml / yyy ml"
    private void updateWaterText() {
        String ml = getString(R.string.ml);
        tvWaterTotal.setText(currentMl + " " + ml + " / " + totalMl + " " + ml);
    }

    // ====== เพิ่ม: เปิด dialog เลือกช่วงเวลาแบบนาที
    private void showIntervalPickerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_interval_picker, null);

        NumberPicker pickerMinute = dialogView.findViewById(R.id.pickerMinute);

        // ป้องกันไม่ให้ NumberPicker เด้งคีย์บอร์ด
        pickerMinute.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        // กำหนดช่วงนาที (0–59 หรือจะเปลี่ยนเองก็ได้)
        pickerMinute.setMinValue(0);
        pickerMinute.setMaxValue(59);

        // ถ้ามีค่าใน edtInterval อยู่แล้ว เอามาเป็นค่าเริ่มต้น (รูปแบบ MM)
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
        new AlertDialog.Builder(this)
                .setTitle(select_time)
                .setView(dialogView)
                .setPositiveButton(ok, (dialog, which) -> {
                    int m = pickerMinute.getValue();

                    String text = String.valueOf(m);   // เช่น "5"
                    if (edtInterval != null) {
                        edtInterval.setText(text);
                    }
                    // แปลงเป็น millis (เฉพาะนาที)
                    intervalMillis = m * 60L * 1000L;

                    // อัพเดต prefs (แต่ยังไม่ตั้ง alarm จนกด save หรือเปิด switch)
                    saveReminderPrefs();
                })
                .setNegativeButton(cancel, null)
                .show();
    }

    // แปลงสตริง Minute -> milliseconds (ใช้ตอนกดปุ่ม Save)
    private Long parseIntervalToMillis(String minutesText) {
        try {
            int m = Integer.parseInt(minutesText);

            if (m < 0 || m > 59) {   // หรือจะเปลี่ยนช่วงเองก็ได้
                return null;
            }

            long totalSec = m * 60L;
            return totalSec * 1000L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ====== SharedPreferences: เซฟค่าการแจ้งเตือน ======
    private void saveReminderPrefs() {
        SharedPreferences prefs = getSharedPreferences("reminder_prefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("reminder_on", isReminderOn)
                .putLong("interval_millis", intervalMillis)
                .apply();
    }

    // โหลดค่าทุกครั้งที่เข้า HomeActivity
    private void loadReminderPrefs() {
        SharedPreferences prefs = getSharedPreferences("reminder_prefs", MODE_PRIVATE);
        isReminderOn = prefs.getBoolean("reminder_on", false);
        intervalMillis = prefs.getLong("interval_millis", 0L);

        // ตั้งค่าตามที่โหลดได้ (ยังไม่มี listener ตอนนี้เลยไม่ลูปซ้ำ)
        if (switchReminder != null) {
            switchReminder.setChecked(isReminderOn);
        }

        if (edtInterval != null && intervalMillis > 0) {
            int minutes = (int) (intervalMillis / (60L * 1000L));
            edtInterval.setText(String.valueOf(minutes));
        }
    }

    // ร้องขอสิทธิ์ในการเเจ้งเตือน
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }







}
