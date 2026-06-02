package com.zjgsu.moveup;

public class PlanDetailItem {
    public String time;
    public String distance;
    public boolean isCompleted;

    public PlanDetailItem(String time, String distance) {
        this(time, distance, false);
    }

    public PlanDetailItem(String time, String distance, boolean isCompleted) {
        this.time = time;
        this.distance = distance;
        this.isCompleted = isCompleted;
    }
}