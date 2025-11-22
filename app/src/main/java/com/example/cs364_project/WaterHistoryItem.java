package com.example.cs364_project;

// รูปเเบบการเก็บประวัติการดื่มน้ำ

public class WaterHistoryItem {

    private final int amountMl;
    private final long timestampMillis;

    public WaterHistoryItem(int amountMl, long timestampMillis) {
        this.amountMl = amountMl;
        this.timestampMillis = timestampMillis;
    }

    public int getAmountMl() {
        return amountMl;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
