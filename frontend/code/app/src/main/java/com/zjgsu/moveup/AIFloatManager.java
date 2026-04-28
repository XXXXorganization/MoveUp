package com.zjgsu.moveup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AIFloatManager {

    /**
     * 在任意 Activity 中调用此方法，即可在屏幕上添加一个可拖拽的 AI 悬浮球
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void addFloat(final Activity activity) {
        // 获取当前界面的根布局
        ViewGroup rootView = activity.findViewById(android.R.id.content);

        // 动态创建一个悬浮按钮 (FloatingActionButton)
        FloatingActionButton fab = new FloatingActionButton(activity);
        // 设置图标，这里暂时使用系统自带的聊天气泡图标，你可以换成自己的 AI 机器人图标
        fab.setImageResource(android.R.drawable.ic_dialog_email);
        // 设置按钮颜色，匹配你 APP 的荧光绿主题
        fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C7FB58")));

        // 设置它默认出现在屏幕右下角
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 60, 250); // 距离右边60，底边250
        fab.setLayoutParams(params);

        // 核心逻辑：实现悬浮球的拖拽与点击事件
        fab.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private long startTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录手指按下时的偏移量
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // 手指滑动时，让按钮跟着手指移动
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - startTime;
                        // 如果按下到抬起的时间极短（小于200毫秒），判定为“点击事件”
                        if (duration < 200) {
                            // 点击后打开 AI 对话悬浮框
                            Intent intent = new Intent(activity, AItalk.class);
                            activity.startActivity(intent);
                        }
                        return true;
                }
                return false;
            }
        });

        // 将按钮添加到当前界面中
        rootView.addView(fab);
    }
}