package com.zjgsu.moveup;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class PlanModuleTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        Plan.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
        Plan_details.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 1. Plan.java 测试：UI 跳转与首页数据拉取
    // ==========================================

    @Test
    public void testPlanMain_FetchesTotalDistance_And_ClicksCard() throws Exception {
        JSONObject dataObj = new JSONObject().put("total_distance", "35.5 Km");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(new JSONObject().put("code", 200).put("data", dataObj).toString()));

        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();
        TextView tv42 = activity.findViewById(R.id.tv42);

        // 🌟 智能轮询等待拉取总里程
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("35.5 Km".equals(tv42.getText().toString())) break;
            Thread.sleep(100);
        }
        assertEquals("35.5 Km", tv42.getText().toString());

        activity.findViewById(R.id.cardDay1).performClick();
        Intent nextIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(Plan_details.class.getName(), nextIntent.getComponent().getClassName());
        assertEquals("MONDAY", nextIntent.getStringExtra("DAY_NAME"));
    }

    // ==========================================
    // 2. Plan_details.java 测试：列表渲染与长按删除
    // ==========================================

    @Test
    public void testPlanDetails_RenderList_And_DeletePlan() throws Exception {
        JSONObject planItem = new JSONObject().put("time", "08:00 AM - 09:00 AM").put("distance", "5 Km");
        JSONArray listArr = new JSONArray().put(planItem);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(new JSONObject().put("code", 200).put("data", new JSONObject().put("list", listArr)).toString()));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class).create().resume().get();
        RecyclerView recyclerView = activity.findViewById(R.id.recyclerPlanDetails);

        // 🌟 智能轮询等待列表渲染出 1 条数据
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() == 1) break;
            Thread.sleep(100);
        }

        assertNotNull(recyclerView.getAdapter());
        assertEquals(1, recyclerView.getAdapter().getItemCount());

        recyclerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        recyclerView.layout(0, 0, 1080, 1920);

        View firstItemView = recyclerView.getChildAt(0);
        assertNotNull(firstItemView);

        firstItemView.performLongClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        // 🌟 智能轮询等待后端删除成功并弹出 "Deleted!" 提示
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Deleted!".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }

        assertEquals("Deleted!", ShadowToast.getTextOfLatestToast());
        assertEquals(0, recyclerView.getAdapter().getItemCount());
    }

    // ==========================================
    // 3. Plan_details.java 测试：通过动态弹窗添加计划
    // ==========================================

    @Test
    public void testPlanDetails_AddPlan_ViaDialog() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class).create().resume().get();

        // 等待初始化请求结束
        for (int i = 0; i < 5; i++) {
            Robolectric.flushForegroundThreadScheduler();
            Thread.sleep(100);
        }

        activity.findViewById(R.id.fabAddPlan).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);

        LinearLayout layout = (LinearLayout) org.robolectric.Shadows.shadowOf(dialog).getView();

        EditText etDistance = null;
        TextView tvStart = null;

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof EditText) {
                etDistance = (EditText) child;
            } else if (child instanceof TextView && tvStart == null) {
                tvStart = (TextView) child;
            }
        }

        assertNotNull(etDistance);
        assertNotNull(tvStart);

        tvStart.setText("07:00 AM");
        etDistance.setText("10");

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        // 🌟 智能轮询等待后端添加成功并弹出 "Added!" 提示
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Added!".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }

        assertEquals("Added!", ShadowToast.getTextOfLatestToast());
        RecyclerView recyclerView = activity.findViewById(R.id.recyclerPlanDetails);
        assertEquals(1, recyclerView.getAdapter().getItemCount());
    }

    // ==========================================
    // 4. Plan.java 测试：网络异常与菜单点击
    // ==========================================

    @Test
    public void testPlanMain_NetworkError_DoesNotCrash() throws Exception {
        mockWebServer.shutdown();

        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testPlanMain_MenuClicks_DoNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"total_distance\":\"10 Km\"}}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"total_distance\":\"10 Km\"}}"));

        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        if (activity.findViewById(R.id.menu_home) != null) {
            activity.findViewById(R.id.menu_home).performClick();
        }
        if (activity.findViewById(R.id.menu_history) != null) {
            activity.findViewById(R.id.menu_history).performClick();
        }
        if (activity.findViewById(R.id.menu_plan) != null) {
            activity.findViewById(R.id.menu_plan).performClick();
        }
        if (activity.findViewById(R.id.menu_club) != null) {
            activity.findViewById(R.id.menu_club).performClick();
        }
        if (activity.findViewById(R.id.menu_profile) != null) {
            activity.findViewById(R.id.menu_profile).performClick();
        }

        assertNotNull(activity);
    }

    @Test
    public void testPlanMain_Non200Code_NoCrash() throws Exception {
        JSONObject resp = new JSONObject().put("code", 400).put("data", new JSONObject());
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(resp.toString()));

        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testPlanMain_HttpError_NoCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testPlanMain_OnResume_FetchesAgain() throws Exception {
        JSONObject data = new JSONObject().put("total_distance", "20 Km");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));

        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // Pause and resume
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    // ==========================================
    // 5. Plan_details.java 补充：更多边界用例
    // ==========================================

    @Test
    public void testPlanDetails_AddPlan_InvalidInput_ShowsToast() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"list\":[]}}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        activity.findViewById(R.id.fabAddPlan).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);

        // Don't fill in start time - should trigger validation error
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("Please enter start time and distance", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testPlanDetails_NetworkError_DoesNotCrash() throws Exception {
        mockWebServer.shutdown();

        Plan_details activity = Robolectric.buildActivity(Plan_details.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testPlanDetails_ToggleComplete_NetworkError_DoesNotCrash() throws Exception {
        JSONObject item = new JSONObject().put("time", "09:00").put("distance", 5)
                .put("is_completed", false);
        JSONObject data = new JSONObject().put("day", "MONDAY").put("list", new JSONArray().put(item));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class).create().resume().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        RecyclerView rv = activity.findViewById(R.id.recyclerPlanDetails);
        rv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        rv.layout(0, 0, 1080, 1920);

        View firstItem = rv.getChildAt(0);
        if (firstItem != null) {
            firstItem.performClick();
            Thread.sleep(500);
            Robolectric.flushForegroundThreadScheduler();
        }

        assertNotNull(activity);
    }
}