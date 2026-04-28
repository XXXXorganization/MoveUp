package com.zjgsu.moveup;

import androidx.annotation.DrawableRes;

/** 跑步俱乐部列表项数据（可后续改为接口返回的模型）。 */
public final class Club {

    public final String name;
    public final String location;
    @DrawableRes
    public final int imageResId;

    public Club(String name, String location, @DrawableRes int imageResId) {
        this.name = name;
        this.location = location;
        this.imageResId = imageResId;
    }
}
