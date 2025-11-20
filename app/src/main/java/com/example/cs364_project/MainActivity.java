package com.example.cs364_project;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // แสดง "ปริมาณน้ำทั้งหมดที่ควรดื่มต่อวัน"
    private TextView tvWaterTotal;
    // แสดงระดับกิจกรรมที่เลือก
    private TextView tvActivityValue;
    // ช่องกรอกน้ำหนัก
    private EditText edtWeight;

    // "none", "light", "medium", "high"
    private String activityLevel = null;
    private int totalMl = 0;

    private int lastTotalMl = 0;          // ค่าที่แสดงอยู่ก่อนหน้า
    private ValueAnimator waterAnimator;  // เอาไว้เก็บ animator ปัจจุบัน


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) ผูก View กับ id จาก XML
        tvWaterTotal     = findViewById(R.id.tvWaterTotal);
        tvActivityValue  = findViewById(R.id.tvActivityValue);
        edtWeight        = findViewById(R.id.edtWeight);

        LinearLayout layoutActivity = findViewById(R.id.layoutActivity);
        Button btnCalculate         = findViewById(R.id.btnCalculate);

        // 2) คลิกเลือกกิจกรรม
        layoutActivity.setOnClickListener(v -> showActivityDialog());

        // 3) น้ำหนักเปลี่ยนเมื่อพิมพ์ → คำนวณใหม่ทุกครั้ง
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

        // 4) ปุ่มคำนวณ
        btnCalculate.setOnClickListener(v -> {
            // TODO: เริ่ม Activity ใหม่และส่ง totalMl ไปยังหน้า HomeActivity ถ้าจะมีหน้าผลลัพธ์
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("TOTAL_ML", totalMl);
            startActivity(intent);
        });

        // แสดงข้อความเริ่มต้น
        recalculate();

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

    // ฟังก์ชันคำนวณปริมาณน้ำรวม (ml) และอัปเดตข้อความในกล่อง layoutWaterBox
    private void recalculate() {
        String weightStr = edtWeight.getText().toString().trim();

        // ถ้ายังไม่กรอกน้ำหนัก หรือยังไม่เลือกกิจกรรม → ให้บอกผู้ใช้ก่อน
        if (weightStr.isEmpty() || activityLevel == null) {
            totalMl = 0;

            if (waterAnimator != null && waterAnimator.isRunning()) {
                waterAnimator.cancel();
            }

            lastTotalMl = 0;

            return;
        }

        int weight = Integer.parseInt(weightStr);

        // สูตร: 35 ml / kg + ระดับกิจกรรม
        // ปริมาณน้ำควรอยู่ในช่วย 30-35 kg / ml
        double base = weight * 35.0;
        double actBonus;

        switch (activityLevel) {
            // กิจกรรมเบาเพิ่ม 300 - 500 ml เช่น ทำงานบ้าน, โยคะเบาๆ
            case "light":
                actBonus = 300;
                break;
            // กิจกรรมปานกลางเพิ่ม 525 - 800 ml เช่น การเดินเร็ว, เต้นเเอโรบิก
            case "medium":
                actBonus = 600;
                break;
            // กิจกรรมสูงเพิ่ม 600 - 1000 ml เช่น HIIT, เตะบอล
            case "high":
                actBonus = 900;
                break;
            case "none":
            default:
                actBonus = 0;
                break;
        }

        totalMl = (int) Math.round(base + actBonus);

        // แสดง น้ำที่ควรดื่มทั้งหมดหน่วย ml ต่อวัน"
        // ถ้าค่าใหม่เท่ากับค่าเดิม ไม่ต้อง animate
        if (totalMl == lastTotalMl) {
            String ml = getString(R.string.ml);
            tvWaterTotal.setText(totalMl + " " + ml);
            return;
        }

        // เริ่มทำ animation จาก lastTotalMl ไป totalMl
        animateWaterTotal(lastTotalMl, totalMl);

        // เก็บค่าใหม่ไว้ใช้รอบหน้า
        lastTotalMl = totalMl;
    }

    // อนิเมชั่น เปลี่ยนค่าปริมาณน้ำทั้งหมด
    private void animateWaterTotal(int from, int to) {
        // ถ้ามี animator เดิมวิ่งอยู่ให้ยกเลิก
        if (waterAnimator != null && waterAnimator.isRunning()) {
            waterAnimator.cancel();
        }

        waterAnimator = ValueAnimator.ofInt(from, to);
        waterAnimator.setDuration(600); // 0.6 วินาที
        waterAnimator.setInterpolator(new DecelerateInterpolator());

        waterAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            String ml = getString(R.string.ml);
            tvWaterTotal.setText(value + " " + ml);
        });

        waterAnimator.start();
    }

}
