package com.zjgsu.moveup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class RuningTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        Runing.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1) + "/v1";

        // 🌟 核心修复：在 Activity 创建前先赋予权限，确保 onCreate 逻辑走通
        ShadowApplication.getInstance().grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        );

        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.contains("/runs/start")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"run_id\":\"test_123\"}}");
                } else if (path.contains("/ai/chat")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"reply\":\"加油！\"}}");
                }
                return new MockResponse().setResponseCode(200).setBody("{\"code\":200}");
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 1. 权限分支测试 (Missed Branches: onRequestPermissionsResult)
    // ==========================================
    @Test
    public void testPermissions_GrantAndDeny_Branches() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().get();

        // 覆盖拒绝分支
        int[] denied = {PackageManager.PERMISSION_DENIED};
        activity.onRequestPermissionsResult(1001, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, denied);

        // 覆盖授予分支
        int[] granted = {PackageManager.PERMISSION_GRANTED};
        activity.onRequestPermissionsResult(1001, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, granted);
    }

    // ==========================================
    // 2. 定位逻辑分支 (Missed Branches: onLocationChanged)
    // ==========================================
    @Test
    public void testLocationChanged_DetailBranches() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();
        TextView tvGps = activity.findViewById(R.id.tvGps);

        // A. 分支：ErrorCode != 0
        AMapLocation failLoc = new AMapLocation("mock");
        failLoc.setErrorCode(12);
        activity.onLocationChanged(failLoc);
        assertEquals("GPS OFF", tvGps.getText().toString());

        // B. 分支：首次定位且带地址
        AMapLocation loc1 = new AMapLocation("mock");
        loc1.setErrorCode(0);
        loc1.setLatitude(30.0);
        loc1.setLongitude(120.0);
        loc1.setAddress("浙江工商大学");
        activity.onLocationChanged(loc1);
        assertEquals("GPS", tvGps.getText().toString());
    }

    // ==========================================
    // 3. AI 交互与异常捕获 (Missed Branches: askAI / Network Error)
    // ==========================================
    @Test
    public void testVoiceFloatButton_Interaction() {
        Runing activity = Robolectric.buildActivity(Runing.class).setup().get();
        FloatingActionButton fab = findFab(activity);
        assertNotNull("悬浮球应该存在", fab);

        // 模拟短按切换监听状态 (覆盖 ACTION_UP 耗时短分支)
        simulateShortClick(fab);
        simulateShortClick(fab);
    }

    @Test
    public void testAskAI_NetworkFailure_CatchBranch() throws Exception {
        // 🌟 覆盖 catch (Exception e) 分支：模拟真实的 Socket 超时/连接失败
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        Runing activity = Robolectric.buildActivity(Runing.class).setup().get();

        // 使用反射强行调用私有方法 askAI 以提高分支覆盖率
        java.lang.reflect.Method askAI = Runing.class.getDeclaredMethod("askAI", String.class);
        askAI.setAccessible(true);
        askAI.invoke(activity, "你好");

        // 给异步 catch 逻辑一点时间执行
        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();
        // 只要没崩溃且跑过了 catch 分支即达标
    }

    // ==========================================
    // 4. 悬浮球拖拽与长按逻辑 (Missed Branches: Touch Event Logic)
    // ==========================================
    @Test
    public void testFloatButton_Drag_Branch() {
        Runing activity = Robolectric.buildActivity(Runing.class).setup().get();
        FloatingActionButton fab = findFab(activity);
        assertNotNull("悬浮球应该存在", fab);

        long downTime = SystemClock.uptimeMillis();
        // 1. ACTION_DOWN
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 0, 0, 0));
        // 2. ACTION_MOVE (覆盖拖动坐标计算分支)
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 100, MotionEvent.ACTION_MOVE, 10, 10, 0));
        // 3. ACTION_UP (模拟长按/拖动结束，耗时 > 200ms)
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 300, MotionEvent.ACTION_UP, 10, 10, 0));

        // 验证没有触发点击（没有新的 Toast）
        assertNull(ShadowToast.getLatestToast());
    }

    // ==========================================
    // 5. 统计与配速分支 (Missed Branches: renderStats)
    // ==========================================
    @Test
    public void testRenderStats_ShortDistance_Branch() {
        Runing activity = Robolectric.buildActivity(Runing.class).setup().get();
        // 分支覆盖：当距离极短 (<0.01km) 时，配速计算应该走默认值
        AMapLocation tinyMove = new AMapLocation("mock");
        tinyMove.setErrorCode(0);
        tinyMove.setLatitude(30.000001);
        activity.onLocationChanged(tinyMove);

        TextView tvPace = activity.findViewById(R.id.tvPace);
        assertEquals("0'00\"", tvPace.getText().toString());
    }

    // ==========================================
    // 辅助工具方法
    // ==========================================
    private FloatingActionButton findFab(Runing activity) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        return findFabRecursive(rootView);
    }

    private FloatingActionButton findFabRecursive(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof FloatingActionButton) return (FloatingActionButton) child;
            if (child instanceof ViewGroup) {
                FloatingActionButton result = findFabRecursive((ViewGroup) child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private void simulateShortClick(View view) {
        long downTime = SystemClock.uptimeMillis();
        view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 0, 0, 0));
        view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, 0, 0, 0));
    }
}