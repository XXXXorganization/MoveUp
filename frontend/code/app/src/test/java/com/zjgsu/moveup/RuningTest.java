package com.zjgsu.moveup;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class RuningTest {

    private MockWebServer mockWebServer;
    private String originalUrl;
    // 用于控制 Mock 服务器动态返回的状态码
    private int mockResponseCode = 200;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        originalUrl = Runing.BASE_URL;
        Runing.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1) + "/v1";

        ShadowApplication.getInstance().grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        );

        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                // 覆盖返回非 200 的失败分支
                if (mockResponseCode != 200) {
                    return new MockResponse().setResponseCode(mockResponseCode).setBody("{\"code\":400,\"data\":{}}");
                }
                if (path.contains("/runs/start")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"run_id\":\"test_123\"}}");
                } else if (path.contains("/runs/finish") || path.contains("/points")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200}");
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
        Runing.BASE_URL = originalUrl;
    }

    // ==========================================
    // 1. 生命周期覆盖
    // ==========================================
    @Test
    public void testLifecycle_Methods() {
        ActivityController<Runing> controller = Robolectric.buildActivity(Runing.class).create().start().resume();
        controller.pause();
        Bundle outState = new Bundle();
        controller.saveInstanceState(outState);
        controller.stop().destroy();
    }

    // ==========================================
    // 2. 界面按钮点击与正常结算流
    // ==========================================
    @Test
    public void testUI_AllButtonClicks_And_FinishRun() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        activity.findViewById(R.id.btnLeft).performClick();
        activity.findViewById(R.id.btnLeft).performClick();

        MaterialButton btnCenter = activity.findViewById(R.id.btnCenter);
        btnCenter.performClick();
        btnCenter.performClick();
        btnCenter.performClick(); // 再次暂停露出 Finish

        activity.findViewById(R.id.btnFinish).performClick();
        activity.findViewById(R.id.btnBackHome).performClick();
    }

    // ==========================================
    // 3. 高德定位核心分支：所有边缘条件与异常拦截
    // ==========================================
    @Test
    public void testLocation_AllEdgeCases() throws Exception {
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();
        TextView tvGps = activity.findViewById(R.id.tvGps);

        // A. amapLocation 为 null 分支
        activity.onLocationChanged(null);
        assertEquals("GPS OFF", tvGps.getText().toString());

        // B. errorCode != 0 分支
        AMapLocation errLoc = new AMapLocation("mock");
        errLoc.setErrorCode(12);
        activity.onLocationChanged(errLoc);
        assertEquals("GPS OFF", tvGps.getText().toString());

        // C. 地址为空或 Null 分支 (覆盖 address != null && !isEmpty)
        AMapLocation noAddressLoc = new AMapLocation("mock");
        noAddressLoc.setErrorCode(0);
        noAddressLoc.setAddress(""); // 空地址
        activity.onLocationChanged(noAddressLoc);
        noAddressLoc.setAddress(null); // Null 地址
        activity.onLocationChanged(noAddressLoc);

        // D. 距离极短（<1米）不连线分支 & 缺少高度/速度分支 (覆盖 hasAltitude / hasSpeed)
        AMapLocation loc1 = new AMapLocation("mock");
        loc1.setErrorCode(0);
        loc1.setLatitude(30.0); loc1.setLongitude(120.0);
        activity.onLocationChanged(loc1);

        AMapLocation loc2 = new AMapLocation("mock");
        loc2.setErrorCode(0);
        loc2.setLatitude(30.000001); loc2.setLongitude(120.000001); // 移动极小
        activity.onLocationChanged(loc2);

        // E. 暂停追踪时的定位分支 (覆盖 if (isTracking) 的 else 逻辑)
        activity.findViewById(R.id.btnCenter).performClick(); // 暂停
        activity.onLocationChanged(loc1);
    }

    // ==========================================
    // 4. 结算面板地图缩放极端分支 (0个点和1个点)
    // ==========================================
    @Test
    public void testFinishRun_ZoomEdgeCases() throws Exception {
        // 场景 1：完全没有定位数据就结束
        Runing activity0 = Robolectric.buildActivity(Runing.class).create().resume().get();
        activity0.findViewById(R.id.btnCenter).performClick();
        activity0.findViewById(R.id.btnFinish).performClick();

        // 场景 2：只有 1 个定位数据就结束 (覆盖 latLngPoints.size() == 1 分支)
        Runing activity1 = Robolectric.buildActivity(Runing.class).create().resume().get();
        AMapLocation loc1 = new AMapLocation("mock");
        loc1.setErrorCode(0); loc1.setLatitude(30.0); loc1.setLongitude(120.0);
        activity1.onLocationChanged(loc1);
        activity1.findViewById(R.id.btnCenter).performClick();
        activity1.findViewById(R.id.btnFinish).performClick();
    }

    // ==========================================
    // 5. 权限回调分支
    // ==========================================
    @Test
    public void testPermissions_Callbacks() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().get();

        // 覆盖空数组
        activity.onRequestPermissionsResult(1001, new String[]{}, new int[]{});
        // 覆盖拒绝
        activity.onRequestPermissionsResult(1001, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, new int[]{PackageManager.PERMISSION_DENIED});
        // 覆盖通过
        activity.onRequestPermissionsResult(1001, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, new int[]{PackageManager.PERMISSION_GRANTED});
    }

    // ==========================================
    // 6. 悬浮球触控逻辑 (短按切换监听状态 vs 长按拖拽)
    // ==========================================
    @Test
    public void testVoiceFloatButton_TouchBranches() {
        Runing activity = Robolectric.buildActivity(Runing.class).setup().get();
        FloatingActionButton fab = findFab(activity);

        // A. 模拟拖拽 (ACTION_UP 耗时 > 200ms)
        long dragTime = SystemClock.uptimeMillis();
        fab.dispatchTouchEvent(MotionEvent.obtain(dragTime, dragTime, MotionEvent.ACTION_DOWN, 0, 0, 0));
        fab.dispatchTouchEvent(MotionEvent.obtain(dragTime, dragTime + 100, MotionEvent.ACTION_MOVE, 10, 10, 0));
        fab.dispatchTouchEvent(MotionEvent.obtain(dragTime, dragTime + 300, MotionEvent.ACTION_UP, 10, 10, 0));

        // B. 模拟短按 (< 200ms)，触发 if(!isListening) startListening()
        long clickTime1 = SystemClock.uptimeMillis();
        fab.dispatchTouchEvent(MotionEvent.obtain(clickTime1, clickTime1, MotionEvent.ACTION_DOWN, 0, 0, 0));
        fab.dispatchTouchEvent(MotionEvent.obtain(clickTime1, clickTime1 + 50, MotionEvent.ACTION_UP, 0, 0, 0));

        // C. 再次短按，触发 else -> stopListening()
        long clickTime2 = SystemClock.uptimeMillis();
        fab.dispatchTouchEvent(MotionEvent.obtain(clickTime2, clickTime2, MotionEvent.ACTION_DOWN, 0, 0, 0));
        fab.dispatchTouchEvent(MotionEvent.obtain(clickTime2, clickTime2 + 50, MotionEvent.ACTION_UP, 0, 0, 0));
    }

    // ==========================================
    // 7. 网络请求的业务/拦截分支 (带有/无Token, 非200状态码)
    // ==========================================
    @Test
    public void testNetwork_Token_And_Non200() throws Exception {
        Runing activity = Robolectric.buildActivity(Runing.class).create().get();

        // 注入 Token，覆盖 if (token != null && !token.isEmpty()) 的 TRUE 分支
        SharedPreferences prefs = activity.getSharedPreferences("moveup_auth", Context.MODE_PRIVATE);
        prefs.edit().putString("jwt", "mock_token_123").apply();

        // 让服务器返回 404 (覆盖 if (code == 200) 的 FALSE 分支)
        mockResponseCode = 404;

        // 强行触发所有网络请求
        Method postRunsStart = Runing.class.getDeclaredMethod("postRunsStart");
        postRunsStart.setAccessible(true);
        postRunsStart.invoke(activity);

        Method postRunFinish = Runing.class.getDeclaredMethod("postRunFinish");
        postRunFinish.setAccessible(true);
        postRunFinish.invoke(activity);

        Method askAI = Runing.class.getDeclaredMethod("askAI", String.class);
        askAI.setAccessible(true);
        askAI.invoke(activity, "你好");

        // 触发 flushPointsUpload() 里的 runId 为 null 直接 return 的分支
        Method flushPointsUpload = Runing.class.getDeclaredMethod("flushPointsUpload");
        flushPointsUpload.setAccessible(true);
        flushPointsUpload.invoke(activity);

        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();
    }

    // ==========================================
    // 8. 强行制造断网异常 (覆盖 catch 块)
    // ==========================================
    @Test
    public void testNetworkExceptions_AllCatchBranches() throws Exception {
        Runing.BASE_URL = "http://invalid-domain.test";
        Runing activity = Robolectric.buildActivity(Runing.class).create().get();

        Method postRunsStart = Runing.class.getDeclaredMethod("postRunsStart");
        postRunsStart.setAccessible(true);
        postRunsStart.invoke(activity);

        Method postRunFinish = Runing.class.getDeclaredMethod("postRunFinish");
        postRunFinish.setAccessible(true);
        postRunFinish.invoke(activity);

        // 强行注入假点数据以越过 isEmpty 拦截
        Field runIdField = Runing.class.getDeclaredField("runId");
        runIdField.setAccessible(true); runIdField.set(activity, "id");
        Field pendingPointsField = Runing.class.getDeclaredField("pendingPoints");
        pendingPointsField.setAccessible(true);
        ((ArrayList<JSONObject>) pendingPointsField.get(activity)).add(new JSONObject());

        Method flushPointsUpload = Runing.class.getDeclaredMethod("flushPointsUpload");
        flushPointsUpload.setAccessible(true);
        flushPointsUpload.invoke(activity);

        Method askAI = Runing.class.getDeclaredMethod("askAI", String.class);
        askAI.setAccessible(true);
        askAI.invoke(activity, "test");

        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler();
    }

    // ==========================================
    // 辅助工具方法
    // ==========================================
    private FloatingActionButton findFab(Runing activity) {
        return findFabRecursive(activity.findViewById(android.R.id.content));
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
}