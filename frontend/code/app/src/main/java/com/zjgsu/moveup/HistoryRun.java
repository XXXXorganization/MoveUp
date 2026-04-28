package com.zjgsu.moveup;

/** 单条跑步历史记录（可对接本地数据库或接口）。 */
public final class HistoryRun {

    public final String id; // 🌟 新增：历史记录的唯一ID
    public final String date;
    public final String title;
    public final String timeValue;
    public final String paceValue;
    public final String distanceValue;

    public HistoryRun(
            String id,
            String date,
            String title,
            String timeValue,
            String paceValue,
            String distanceValue) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.timeValue = timeValue;
        this.paceValue = paceValue;
        this.distanceValue = distanceValue;
    }
}