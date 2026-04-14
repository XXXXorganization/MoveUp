package com.zjgsu.moveup;

import androidx.annotation.DrawableRes;

/** 跑步俱乐部列表项数据 */
public final class Club {

    public final String id; // 新增：跑团唯一标识
    public final String name;
    public final String location;
    @DrawableRes
    public final int imageResId;
    public final String flag;

    // 兼容 Main.java 原有的构造函数（如果不传 ID，默认给个空字符串）
    public Club(String name, String location, @DrawableRes int imageResId) {
        this("", name, location, imageResId, "🇨🇳");
    }

    // 完整构造函数
    public Club(String id, String name, String location, @DrawableRes int imageResId, String flag) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.imageResId = imageResId;
        this.flag = flag;
    }
}