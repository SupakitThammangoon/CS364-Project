package com.example.cs364_project;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

// ตัวกลางในการเก็บ ปริมาณน้ำเป้าหมาย + ปริมาณรวมที่ดื่มจริง + รายการประวัติ -> บันทึกข้อมูลลง SharedPreferences

public class WaterTracker {

    private static final String PREF_NAME = "water_tracker_prefs";
    private static final String KEY_TARGET = "target_ml";
    private static final String KEY_TOTAL = "total_intake_ml";
    private static final String KEY_HISTORY = "history_string";

    private static int targetMl = 0;       // เป้าที่ต้องดื่มทั้งวัน
    private static int totalIntakeMl = 0;  // ปริมาณที่ดื่มจริงรวม
    private static final List<WaterHistoryItem> historyList = new ArrayList<>();

    private static Context appContext = null;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        loadFromPrefs();
    }

    public static void setTargetMl(int ml) {
        targetMl = Math.max(0, ml);
        saveToPrefs();
    }

    public static int getTargetMl() {
        return targetMl;
    }

    public static int getTotalIntakeMl() {
        return totalIntakeMl;
    }

    public static int getProgressValue() {
        return Math.min(totalIntakeMl, targetMl);
    }

    public static void addEntry(int amountMl) {
        if (amountMl <= 0) return;
        totalIntakeMl += amountMl;
        historyList.add(0, new WaterHistoryItem(amountMl, System.currentTimeMillis()));
        saveToPrefs();
    }

    public static void removeEntry(WaterHistoryItem item) {
        if (historyList.remove(item)) {
            totalIntakeMl -= item.getAmountMl();
            if (totalIntakeMl < 0) totalIntakeMl = 0;
            saveToPrefs();
        }
    }

    public static List<WaterHistoryItem> getHistory() {
        return new ArrayList<>(historyList);
    }

    public static void clear() {
        historyList.clear();
        totalIntakeMl = 0;
        targetMl = 0;
        saveToPrefs();
    }

    private static void loadFromPrefs() {
        if (appContext == null) return;

        SharedPreferences prefs =
                appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        targetMl = prefs.getInt(KEY_TARGET, 0);
        totalIntakeMl = prefs.getInt(KEY_TOTAL, 0);

        historyList.clear();
        String historyStr = prefs.getString(KEY_HISTORY, "");
        if (historyStr == null || historyStr.isEmpty()) return;

        String[] parts = historyStr.split(";");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String[] p = part.split("\\|");
            if (p.length != 2) continue;

            try {
                int amount = Integer.parseInt(p[0]);
                long ts = Long.parseLong(p[1]);
                historyList.add(new WaterHistoryItem(amount, ts));
            } catch (NumberFormatException e) {
            }
        }

        // ถ้า total ยัง 0 แต่มี history ให้ sync จาก history เผื่อกรณีเก่า
        if (totalIntakeMl == 0 && !historyList.isEmpty()) {
            int sum = 0;
            for (WaterHistoryItem item : historyList) {
                sum += item.getAmountMl();
            }
            totalIntakeMl = sum;
        }
    }

    private static void saveToPrefs() {
        if (appContext == null) return;

        SharedPreferences prefs =
                appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_TARGET, targetMl);
        editor.putInt(KEY_TOTAL, totalIntakeMl);

        StringBuilder sb = new StringBuilder();
        for (WaterHistoryItem item : historyList) {
            if (sb.length() > 0) sb.append(";");
            sb.append(item.getAmountMl())
                    .append("|")
                    .append(item.getTimestampMillis());
        }
        editor.putString(KEY_HISTORY, sb.toString());

        editor.apply();
    }
}

