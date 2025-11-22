package com.example.cs364_project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;

// หน้าในการเก็บประวัติการดื่มน้ำทั้งวัน + ลบรายการดื่มน้ำ

public class HistoryFragment extends Fragment {

    private LinearLayout layoutHistoryList;
    private final SimpleDateFormat timeFormatter =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public HistoryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutHistoryList = view.findViewById(R.id.layoutHistoryList);
        renderHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        renderHistory();
    }

    private void renderHistory() {
        if (layoutHistoryList == null || getContext() == null) return;

        layoutHistoryList.removeAllViews();

        List<WaterHistoryItem> history = WaterTracker.getHistory();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (WaterHistoryItem item : history) {
            View row = inflater.inflate(R.layout.item_history_water, layoutHistoryList, false);

            TextView tvAmount = row.findViewById(R.id.tvAmount);
            TextView tvTime   = row.findViewById(R.id.tvTime);
            ImageView btnDelete = row.findViewById(R.id.btnDelete);

            String ml = getString(R.string.ml);
            tvAmount.setText(item.getAmountMl() + " " + ml);

            Date date = new Date(item.getTimestampMillis());
            String timeText = timeFormatter.format(date);
            tvTime.setText(timeText);

            btnDelete.setOnClickListener(v -> {
                WaterTracker.removeEntry(item);
                renderHistory();
            });

            layoutHistoryList.addView(row);
        }

    }
}
