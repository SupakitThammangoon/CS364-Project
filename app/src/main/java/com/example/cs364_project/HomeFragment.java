package com.example.cs364_project;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

// หน้าหลักในการใส่ข้อมูล น้ำหนัก + ระดับกิจกรรม -> คำนวณเป็นปริมาณน้ำที่ต้องดื่มต่อวัน

public class HomeFragment extends Fragment {

    private TextView tvWaterTotal;
    private TextView tvActivityValue;
    private EditText edtWeight;

    private String activityLevel = null;
    private int totalMl = 0;

    private int lastTotalMl = 0;
    private ValueAnimator waterAnimator;

    // SharedPreferences สำหรับ state ของ Home
    private static final String HOME_PREF_NAME = "home_prefs";
    private static final String KEY_HOME_WEIGHT = "home_weight";
    private static final String KEY_HOME_ACTIVITY = "home_activity_level";

    // callback ไปยัง Activity
    public interface HomeFragmentListener {
        void onTotalCalculated(int totalMl);
    }

    private HomeFragmentListener listener;

    public HomeFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeFragmentListener) {
            listener = (HomeFragmentListener) context;
        } else {
            throw new IllegalStateException("Activity must implement HomeFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWaterTotal    = view.findViewById(R.id.tvWaterTotal);
        tvActivityValue = view.findViewById(R.id.tvActivityValue);
        edtWeight       = view.findViewById(R.id.edtWeight);

        LinearLayout layoutActivity = view.findViewById(R.id.layoutActivity);
        Button btnCalculate         = view.findViewById(R.id.btnCalculate);

        layoutActivity.setOnClickListener(v -> showActivityDialog());

        edtWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculate();
                saveHomeState(); // เซฟน้ำหนักทุกครั้งที่เปลี่ยน
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCalculate.setOnClickListener(v -> {
            recalculate();

            if (listener != null) {
                listener.onTotalCalculated(totalMl);
            }
        });

        // โหลดค่าที่เคยกรอกไว้ (น้ำหนัก + ระดับกิจกรรม)
        loadHomeState();
    }

    private void showActivityDialog() {
        if (getContext() == null) return;

        final String[] activityArray = getResources().getStringArray(R.array.activity_array);

        new AlertDialog.Builder(requireContext())
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
                    saveHomeState(); // เซฟ activity ที่เลือก
                })
                .show();
    }

    private void recalculate() {
        if (edtWeight == null) return;

        String weightStr = edtWeight.getText().toString().trim();

        if (weightStr.isEmpty() || activityLevel == null) {
            totalMl = 0;
            if (waterAnimator != null && waterAnimator.isRunning()) {
                waterAnimator.cancel();
            }
            lastTotalMl = 0;
            if (tvWaterTotal != null) {
                tvWaterTotal.setText(getString(R.string.total_water));
            }
            return;
        }

        int weight = Integer.parseInt(weightStr);

        double base = weight * 35.0;
        double actBonus;

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

        totalMl = (int) Math.round(base + actBonus);

        if (totalMl == lastTotalMl) {
            if (tvWaterTotal != null) {
                String ml = getString(R.string.ml);
                tvWaterTotal.setText(totalMl + " " + ml);
            }
            return;
        }

        animateWaterTotal(lastTotalMl, totalMl);
        lastTotalMl = totalMl;
    }

    private void animateWaterTotal(int from, int to) {
        if (tvWaterTotal == null) return;

        if (waterAnimator != null && waterAnimator.isRunning()) {
            waterAnimator.cancel();
        }

        waterAnimator = ValueAnimator.ofInt(from, to);
        waterAnimator.setDuration(600);
        waterAnimator.setInterpolator(new DecelerateInterpolator());

        waterAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            String ml = getString(R.string.ml);
            tvWaterTotal.setText(value + " " + ml);
        });

        waterAnimator.start();
    }

    private void saveHomeState() {
        if (getContext() == null) return;

        SharedPreferences prefs =
                requireContext().getSharedPreferences(HOME_PREF_NAME, Context.MODE_PRIVATE);

        String weightStr = edtWeight != null ? edtWeight.getText().toString() : "";
        prefs.edit()
                .putString(KEY_HOME_WEIGHT, weightStr)
                .putString(KEY_HOME_ACTIVITY, activityLevel)
                .apply();
    }

    private void loadHomeState() {
        if (getContext() == null) return;

        SharedPreferences prefs =
                requireContext().getSharedPreferences(HOME_PREF_NAME, Context.MODE_PRIVATE);

        String weightStr = prefs.getString(KEY_HOME_WEIGHT, "");
        String savedActivity = prefs.getString(KEY_HOME_ACTIVITY, null);

        if (edtWeight != null) {
            edtWeight.setText(weightStr);
        }

        if (savedActivity != null) {
            activityLevel = savedActivity;
            if (tvActivityValue != null) {
                String[] activityArray = getResources().getStringArray(R.array.activity_array);
                switch (activityLevel) {
                    case "none":
                        tvActivityValue.setText(activityArray[0]);
                        break;
                    case "light":
                        tvActivityValue.setText(activityArray[1]);
                        break;
                    case "medium":
                        tvActivityValue.setText(activityArray[2]);
                        break;
                    case "high":
                        tvActivityValue.setText(activityArray[3]);
                        break;
                }
            }
        }

        recalculate();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveHomeState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (waterAnimator != null && waterAnimator.isRunning()) {
            waterAnimator.cancel();
        }
        waterAnimator = null;
    }
}
