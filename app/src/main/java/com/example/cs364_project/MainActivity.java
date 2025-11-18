package com.example.cs364_project;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressWater;
    private TextView tvWaterStatus, tvWaterRemaining;
    private TextView tvGenderValue, tvActivityValue;
    private EditText edtWeight;

    private String gender = null;         // "male" หรือ "female"
    private String activityLevel = null;  // "none", "light", "medium", "high"
    private int totalMl = 0;
    private int currentMl = 0;            // ตอนนี้ยังไม่เก็บดื่มจริง = 0 ไปก่อน

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) ผูก View กับ id จาก XML
        progressWater      = findViewById(R.id.progressWater);
        tvWaterStatus      = findViewById(R.id.tvWaterStatus);
        tvWaterRemaining   = findViewById(R.id.tvWaterRemaining);
        tvGenderValue      = findViewById(R.id.tvGenderValue);
        tvActivityValue    = findViewById(R.id.tvActivityValue);
        edtWeight          = findViewById(R.id.edtWeight);

        LinearLayout layoutGender   = findViewById(R.id.layoutGender);
        LinearLayout layoutActivity = findViewById(R.id.layoutActivity);
        Button btnCalculate         = findViewById(R.id.btnCalculate);

        // 2) คลิกเลือกเพศ
        layoutGender.setOnClickListener(v -> showGenderDialog());

        // 3) คลิกเลือกกิจกรรม
        layoutActivity.setOnClickListener(v -> showActivityDialog());

        // 4) น้ำหนักเปลี่ยนเมื่อพิมพ์ → คำนวณใหม่ทุกครั้ง
        edtWeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculate();   // เรียกคำนวณทุกครั้งที่น้ำหนักเปลี่ยน
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // 5) ปุ่มคำนวณปริมาณน้ำ → ไว้ไปหน้าถัดไป (ตอนนี้ใส่ TODO ไว้ก่อน)
        btnCalculate.setOnClickListener(v -> {
            // TODO: เริ่ม Activity ใหม่และส่ง totalMl ไปด้วย
            // ตัวอย่าง: startActivity(new Intent(this, ResultActivity.class));
        });

        // ค่าตั้งต้น 0 / 0
        recalculate();
    }

    private void showGenderDialog() {
        // ใช้ string-array จาก strings.xml
        final String[] genderArray = getResources().getStringArray(R.array.gender_array);

        new AlertDialog.Builder(this)
                .setTitle(R.string.label_gender)
                .setItems(genderArray, (dialog, which) -> {
                    tvGenderValue.setText(genderArray[which]);
                    gender = (which == 0) ? "male" : "female";
                    recalculate();
                })
                .show();
    }

    private void showActivityDialog() {
        final String[] activityArray = getResources().getStringArray(R.array.activity_array);

        new AlertDialog.Builder(this)
                .setTitle(R.string.label_activity)
                .setItems(activityArray, (dialog, which) -> {
                    tvActivityValue.setText(activityArray[which]);
                    switch (which) {
                        case 0: activityLevel = "none";   break;
                        case 1: activityLevel = "light";  break;
                        case 2: activityLevel = "medium"; break;
                        case 3: activityLevel = "high";   break;
                    }
                    recalculate();
                })
                .show();
    }

    // ฟังก์ชันคำนวณปริมาณน้ำรวม (ml) และอัปเดตหลอด + ข้อความ
    private void recalculate() {
        String weightStr = edtWeight.getText().toString().trim();

        if (weightStr.isEmpty() || gender == null || activityLevel == null) {
            totalMl = 0;
        } else {
            int weight = Integer.parseInt(weightStr);

            // สูตรตัวอย่าง: 35 ml / kg + โบนัสตามกิจกรรม
            double base = weight * 35.0;
            double actBonus = 0;

            switch (activityLevel) {
                case "light":
                    actBonus = 300;
                    break;
                case "medium":
                    actBonus = 600;
                    break;
                case "high":
                    actBonus = 900;
                    break;
                case "none":
                default:
                    actBonus = 0;
                    break;
            }

            // ผู้ชายเพิ่มอีกนิด (ตัวอย่าง)
            if ("male".equals(gender)) {
                base += 250;
            }

            totalMl = (int) Math.round(base + actBonus);
        }

        // ตั้งค่า ProgressBar
        progressWater.setMax(Math.max(totalMl, 1));   // กัน division by zero
        progressWater.setProgress(currentMl);

        // อัปเดตข้อความ
        tvWaterStatus.setText(currentMl + " ml / " + totalMl + " ml");
        int remaining = Math.max(totalMl - currentMl, 0);
        tvWaterRemaining.setText("เหลือต้องดื่มอีก " + remaining + " ml");
    }
}