package com.zjgsu.moveup;

import android.content.Intent;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AIFloatManagerTest {

    @Test
    public void testAddFloat_AndClickToJump() {
        // 1. 在一个测试 Activity 中添加悬浮球
        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();
        AIFloatManager.addFloat(activity);

        // 2. 查找是否添加了 FloatingActionButton
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        FloatingActionButton fab = null;
        for (int i = 0; i < rootView.getChildCount(); i++) {
            if (rootView.getChildAt(i) instanceof FloatingActionButton) {
                fab = (FloatingActionButton) rootView.getChildAt(i);
                break;
            }
        }

        // 3. 断言悬浮球存在
        assertNotNull("悬浮球应该被成功添加到页面中", fab);

        // 4. 🌟 修复点：模拟真实的 Touch 事件流以触发 AIFloatManager 内部逻辑
        long downTime = System.currentTimeMillis();

        // 发送 ACTION_DOWN (按下)
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 0, 0, 0));

        // 发送 ACTION_UP (立即抬起，保证 duration < 200ms)
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_UP, 0, 0, 0));

        // 5. 验证是否成功跳转到了 AItalk
        Intent nextIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull("点击悬浮球后应该有页面跳转记录", nextIntent);
        assertEquals(AItalk.class.getName(), nextIntent.getComponent().getClassName());
    }
}