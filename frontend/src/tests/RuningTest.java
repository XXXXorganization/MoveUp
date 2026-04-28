package com.zjgsu.moveup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27) // 🌟 继续使用 27 以保证兼容性并允许 Http 请求
public class RuningTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // 将 Runing 中的全局 BASE_URL 指向我们本地的虚拟测试服务器
        String mockUrl = mockWebServer.url("/").toString();
        Runing.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);

        // 🌟 智能路由：防止后台自动上传数据或请求AI导致的多线程测试崩溃
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/runs/start")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"run_id\":\"test_id\"}}");
                } else if (path != null && path.contains("/runs/finish")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200}");
                } else if (path != null && path.contains("/ai/chat")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"reply\":\"继续加油！\"}}");
                }
                return new MockResponse().setResponseCode(200);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 任务 1：基础 UI 交互与状态栏切换分支
    // ==========================================

    @Test
    public void testTogglePause_ChangesUI() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        Button btnCenter = activity.findViewById(R.id.btnCenter);
        View btnFinish = activity.findViewById(R.id.btnFinish);

        // 第一次点击：暂停跑步 (进入 if 分支)
        btnCenter.performClick();
        assertEquals("CONTINUE", btnCenter.getText().toString());
        assertEquals(View.VISIBLE, btnFinish.getVisibility());

        // 第二次点击：继续跑步 (进入 else 分支)
        btnCenter.performClick();
        assertEquals("PAUSE", btnCenter.getText().toString());
        assertEquals(View.GONE, btnFinish.getVisibility());
    }

    @Test
    public void testClickLeft_TogglesMapView() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        View btnLeft = activity.findViewById(R.id.btnLeft);
        ScrollView statsScroll = activity.findViewById(R.id.statsScroll);
        View mapArea = activity.findViewById(R.id.mapArea);

        // 点击切换视图按钮 (覆盖三元运算符分支)
        btnLeft.performClick();
        assertEquals(View.GONE, statsScroll.getVisibility());
        assertEquals(View.VISIBLE, mapArea.getVisibility());
    }

    // ==========================================
    // 任务 2：核心业务流与结算面板
    // ==========================================

    @Test
    public void testFinishRun_ShowsSummaryPanel() {
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        // 模拟用户操作流程：先暂停，后点击完成
        activity.findViewById(R.id.btnCenter).performClick();
        activity.findViewById(R.id.btnFinish).performClick();

        View layoutSummary = activity.findViewById(R.id.layoutSummary);
        View headerRunning = activity.findViewById(R.id.headerRunning);
        View panelControls = activity.findViewById(R.id.panelControls);

        assertEquals(View.VISIBLE, layoutSummary.getVisibility());
        assertEquals(View.GONE, headerRunning.getVisibility());
        assertEquals(View.GONE, panelControls.getVisibility());
    }

    // ==========================================
    // 任务 3：深度分支覆盖 (Missed Branches)
    // ==========================================

    @Test
    public void testPermissions_Branches() {
        // 1. 分支：未授予权限时，进入 else 分支发起请求
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        // 2. 分支：模拟用户同意权限请求 (onRequestPermissionsResult -> 命中成功分支)
        int[] granted = {PackageManager.PERMISSION_GRANTED};
        activity.onRequestPermissionsResult(1001, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, granted);

        // 3. 分支：模拟用户拒绝权限请求 (命中失败/忽略分支)
        int[] denied = {PackageManager.PERMISSION_DENIED};
        activity.onRequestPermissionsResult(1001, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, denied);
    }

    @Test
    public void testLocationChanged_BranchCoverage() {
        // 赋予权限以初始化高德对象
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO);
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        TextView tvGps = activity.findViewById(R.id.tvGps);

        // 1. 分支：定位错误 (errorCode != 0) -> 进入 else 分支，显示 "GPS OFF"
        AMapLocation errorLoc = new AMapLocation("mock");
        errorLoc.setErrorCode(12); // 缺少权限或其他错误
        activity.onLocationChanged(errorLoc);
        assertEquals("GPS OFF", tvGps.getText().toString());

        // 2. 分支：定位成功且首次加入坐标 (latLngPoints.isEmpty() 为 true)
        AMapLocation validLoc1 = new AMapLocation("mock");
        validLoc1.setErrorCode(0);
        validLoc1.setLatitude(30.0);
        validLoc1.setLongitude(120.0);
        validLoc1.setAddress("浙江省杭州市下沙高教园区"); // 命中 address 不为空分支
        validLoc1.setSpeed(5.0f);
        activity.onLocationChanged(validLoc1);
        assertEquals("GPS", tvGps.getText().toString());

        // 3. 分支：位置发生显著移动，计算距离 (dist > 1.0f 分支)
        AMapLocation validLoc2 = new AMapLocation("mock");
        validLoc2.setErrorCode(0);
        validLoc2.setLatitude(30.001); // 移动了一点点
        validLoc2.setLongitude(120.001);
        activity.onLocationChanged(validLoc2);

        // 4. 分支：暂停追踪状态下收到定位 (isTracking == false 分支)
        activity.findViewById(R.id.btnCenter).performClick(); // PAUSE
        AMapLocation validLoc3 = new AMapLocation("mock");
        validLoc3.setErrorCode(0);
        validLoc3.setLatitude(30.002);
        validLoc3.setLongitude(120.002);
        activity.onLocationChanged(validLoc3);
    }

    @Test
    public void testFloatButton_DragAndDrop_BranchCoverage() {
        // 赋予权限使得悬浮球被添加到视图中
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO);
        Runing activity = Robolectric.buildActivity(Runing.class).create().resume().get();

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        FloatingActionButton fab = null;
        for (int i = 0; i < rootView.getChildCount(); i++) {
            if (rootView.getChildAt(i) instanceof FloatingActionButton) {
                fab = (FloatingActionButton) rootView.getChildAt(i);
                break;
            }
        }
        assertNotNull("悬浮球应该存在", fab);

        // 模拟长按拖拽事件 (duration >= 200) -> 覆盖 ACTION_MOVE 以及 ACTION_UP 时 duration > 200 的 false 分支
        long downTime = SystemClock.uptimeMillis();

        // 1. 按下 ACTION_DOWN
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100, 100, 0));

        // 2. 移动 ACTION_MOVE (触发 setX, setY 的拖动分支)
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_MOVE, 150, 150, 0));

        // 3. 延迟 250 毫秒后抬起 ACTION_UP (不应触发点击事件)
        fab.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 250, MotionEvent.ACTION_UP, 150, 150, 0));

        // 验证没有跳转发生 (因为被判定为拖拽而非点击)
        Intent nextIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals("不应该发生跳转", null, nextIntent);
    }
}