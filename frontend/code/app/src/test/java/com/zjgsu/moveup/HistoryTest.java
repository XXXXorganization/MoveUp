package com.zjgsu.moveup;

import android.app.Dialog;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class HistoryTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        History.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 1. 菜单跳转分支 (全面覆盖)
    // ==========================================
    @Test
    public void testMenu_AllBranches_Coverage() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"stats\":{},\"list\":[]}}"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();
        TimeUnit.MILLISECONDS.sleep(300);
        Robolectric.flushForegroundThreadScheduler();

        activity.findViewById(R.id.btnMenu).performClick();

        // 点击 History (当前页，只关闭菜单不跳转)
        activity.findViewById(R.id.menu_history).performClick();
        assertNull(ShadowApplication.getInstance().getNextStartedActivity());

        // 依次点击其余四个菜单，覆盖所有 Intent 跳转分支
        activity.findViewById(R.id.menu_home).performClick();
        assertEquals(Main.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_plan).performClick();
        assertEquals(Plan.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_club).performClick();
        assertEquals(clubterm.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_profile).performClick();
        assertEquals(Mine.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());
    }

    // ==========================================
    // 2. 数据为空/字段缺失的 Json 解析分支
    // ==========================================
    @Test
    public void testFetchHistory_NullDataBranches() throws Exception {
        // 🌟 覆盖 stats 为空、list 为空时的防御性代码分支
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();

        TextView tvTotalKm = activity.findViewById(R.id.totalKmValue);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("0".equals(tvTotalKm.getText().toString())) break;
            Thread.sleep(100);
        }

        // 验证所有的 null 防御机制生效，赋予了正确的默认值
        assertEquals("0", tvTotalKm.getText().toString());
        assertEquals("0", ((TextView) activity.findViewById(R.id.summaryRunValue)).getText().toString());
        assertEquals("0'00\"", ((TextView) activity.findViewById(R.id.summaryPaceValue)).getText().toString());
        assertEquals("0h", ((TextView) activity.findViewById(R.id.summaryTimeValue)).getText().toString());
    }

    // ==========================================
    // 3. HTTP 非 200 的读取分支
    // ==========================================
    @Test
    public void testFetchHistory_HttpError_Branch() throws Exception {
        // 🌟 覆盖 `if (httpCode >= 200 && httpCode < 300)` 为 false 时去读取 ErrorStream 的分支
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad Request"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(400);
        Robolectric.flushForegroundThreadScheduler();

        // 确保不会崩溃，组件依然存活
        assertNotNull(activity.findViewById(R.id.totalKmValue));
    }

    // ==========================================
    // 4. 分享贴文流程 - 全链路成功分支
    // ==========================================
    @Test
    public void testShareFlow_Success_FullSequence() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"stats\":{},\"list\":[{\"id\":\"run1\"}]}}"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();

        RecyclerView recyclerView = activity.findViewById(R.id.recyclerHistory);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) break;
            Thread.sleep(100);
        }
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 1080, 1920);

        triggerShareDialog(recyclerView.getChildAt(0));
        AlertDialog confirmDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(confirmDialog);

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[{\"id\":\"c1\",\"name\":\"Test Club\",\"location\":\"Loc\"}]}}"));
        confirmDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (ShadowDialog.getLatestDialog() != confirmDialog) break;
            Thread.sleep(100);
        }

        AlertDialog clubSelectDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(clubSelectDialog);
        clubSelectDialog.getListView().performItemClick(
                clubSelectDialog.getListView().getAdapter().getView(0, null, null), 0, 0);

        AlertDialog inputDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(inputDialog);

        EditText editText = findEditText(inputDialog.getWindow().getDecorView());
        if (editText != null) editText.setText("Great run!");

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200}"));
        inputDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Shared successfully to Test Club".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }

        assertEquals("Shared successfully to Test Club", ShadowToast.getTextOfLatestToast());
    }

    // ==========================================
    // 5. 分享贴文流程 - 异常分支 (未加入跑团)
    // ==========================================
    @Test
    public void testShareFlow_EmptyClubs() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"stats\":{},\"list\":[{\"id\":\"run1\"}]}}"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();

        RecyclerView recyclerView = activity.findViewById(R.id.recyclerHistory);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) break;
            Thread.sleep(100);
        }
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 1080, 1920);

        triggerShareDialog(recyclerView.getChildAt(0));
        AlertDialog confirmDialogA = (AlertDialog) ShadowDialog.getLatestDialog();

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}"));
        confirmDialogA.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("You haven't joined any clubs yet.".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }
        assertEquals("You haven't joined any clubs yet.", ShadowToast.getTextOfLatestToast());
    }

    // ==========================================
    // 6. 分享贴文流程 - 异常分支 (拉取跑团断网)
    // ==========================================
    @Test
    public void testShareFlow_NetworkError_OnFetchClubs() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"stats\":{},\"list\":[{\"id\":\"run1\"}]}}"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();

        RecyclerView recyclerView = activity.findViewById(R.id.recyclerHistory);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) break;
            Thread.sleep(100);
        }
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 1080, 1920);

        triggerShareDialog(recyclerView.getChildAt(0));
        AlertDialog confirmDialogB = (AlertDialog) ShadowDialog.getLatestDialog();

        // 🌟 拔网线！强制引发真实的 Java ConnectException 异常进入 catch 块
        mockWebServer.shutdown();

        confirmDialogB.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Network Error".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }
        assertEquals("Network Error", ShadowToast.getTextOfLatestToast());
    }

    // ==========================================
    // 7. 分享贴文流程 - 异常分支 (最终分享提交失败)
    // ==========================================
    @Test
    public void testShareFlow_NetworkError_OnSubmitShare() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"stats\":{},\"list\":[{\"id\":\"run1\"}]}}"));
        History activity = Robolectric.buildActivity(History.class).create().resume().get();

        RecyclerView recyclerView = activity.findViewById(R.id.recyclerHistory);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) break;
            Thread.sleep(100);
        }
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 1080, 1920);

        triggerShareDialog(recyclerView.getChildAt(0));
        AlertDialog confirmDialog = (AlertDialog) ShadowDialog.getLatestDialog();

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[{\"id\":\"c1\",\"name\":\"Test Club\",\"location\":\"Loc\"}]}}"));
        confirmDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (ShadowDialog.getLatestDialog() != confirmDialog) break;
            Thread.sleep(100);
        }

        AlertDialog clubSelectDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        clubSelectDialog.getListView().performItemClick(
                clubSelectDialog.getListView().getAdapter().getView(0, null, null), 0, 0);

        AlertDialog inputDialog = (AlertDialog) ShadowDialog.getLatestDialog();

        // 🌟 拔网线！强制引发真实的 Java ConnectException 异常进入 catch 块
        mockWebServer.shutdown();

        inputDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Share Failed".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }
        assertEquals("Share Failed", ShadowToast.getTextOfLatestToast());
    }

    // ==========================================
    // 辅助工具方法：递归点击寻找弹窗触发器与输入框
    // ==========================================
    private void triggerShareDialog(View view) {
        if (view == null) return;
        Dialog initialDialog = ShadowDialog.getLatestDialog();
        if (view.hasOnClickListeners()) {
            view.performClick();
            if (ShadowDialog.getLatestDialog() != initialDialog) return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                triggerShareDialog(vg.getChildAt(i));
                if (ShadowDialog.getLatestDialog() != initialDialog) return;
            }
        }
    }

    private EditText findEditText(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                EditText et = findEditText(vg.getChildAt(i));
                if (et != null) return et;
            }
        }
        return null;
    }
}