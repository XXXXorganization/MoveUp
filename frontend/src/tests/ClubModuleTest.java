package com.zjgsu.moveup;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
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
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ClubModuleTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        clubterm.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
        ClubTermPostAdapter.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 1. 深度覆盖：侧边栏所有导航路径测试
    // ==========================================

    @Test
    public void testSideMenu_FullNavigationCoverage() {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
            }
        });

        clubterm activity = Robolectric.buildActivity(clubterm.class).create().resume().get();

        activity.findViewById(R.id.menu_history).performClick();
        assertEquals(History.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_plan).performClick();
        assertEquals(Plan.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_profile).performClick();
        assertEquals(Mine.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());
    }

    // ==========================================
    // 2. 深度覆盖：API 成功态与 UI 状态同步 (解决超时玄学报错)
    // ==========================================

    @Test
    public void testJoinClub_Success_UpdatesUIAndToast() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/toggle")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"is_joined\":true}}");
                } else if (path != null && path.contains("/posts")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                } else {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"name\":\"跑团A\",\"location\":\"校区\",\"is_joined\":false}}");
                }
            }
        });

        clubterm activity = Robolectric.buildActivity(clubterm.class).create().resume().get();
        Button btnJoin = activity.findViewById(R.id.btnJoin);

        // 🌟 智能轮询等待：最多等 2 秒，只要拉取详情成功并显示 "Join" 就立刻往下走
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Join".equals(btnJoin.getText().toString())) break;
            Thread.sleep(100);
        }
        assertEquals("Join", btnJoin.getText().toString());

        // 模拟点击并确认弹窗
        btnJoin.performClick();
        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("弹窗应该被成功显示", dialog);
        Button positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positiveButton);
        positiveButton.performClick();

        // 🌟 智能轮询等待：监控后端 toggle 接口是否成功把文字改成了 "Exit"
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Exit".equals(btnJoin.getText().toString())) break;
            Thread.sleep(100);
        }

        // 最终断言判定
        assertEquals("Exit", btnJoin.getText().toString());
        assertEquals("Successfully Joined!", ShadowToast.getTextOfLatestToast());
    }

    // ==========================================
    // 3. 深度覆盖：JSON 数据解析分支 (提升适配器覆盖率)
    // ==========================================

    @Test
    public void testPostList_WithCommentsAndLikes_RendersCorrectly() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/posts")) {
                    try {
                        JSONObject comment = new JSONObject()
                                .put("id", "c1").put("author", "小明").put("content", "给力")
                                .put("time", "12:00").put("reply_to_id", "u1").put("reply_to_name", "小红");

                        JSONObject post = new JSONObject()
                                .put("id", "p1").put("author", "队长").put("timeText", "1小时前")
                                .put("lateTitle", "打卡").put("postBadgeText", "优秀").put("subLine", "晨跑")
                                .put("subDetail", "5km").put("is_liked", true).put("like_count", 99)
                                .put("total_comments", 1).put("comments", new JSONArray().put(comment));

                        return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[" + post.toString() + "]}}");
                    } catch (Exception e) {
                        return new MockResponse().setResponseCode(500);
                    }
                } else {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"name\":\"A\"}}");
                }
            }
        });

        clubterm activity = Robolectric.buildActivity(clubterm.class).create().resume().get();
        RecyclerView recyclerView = activity.findViewById(R.id.recyclerPosts);

        // 🌟 智能轮询等待：直到 RecyclerView 成功渲染出数据
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) break;
            Thread.sleep(100);
        }

        assertNotNull(recyclerView.getAdapter());
        assertTrue(recyclerView.getAdapter().getItemCount() > 0);

        recyclerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        recyclerView.layout(0, 0, 1080, 1920);

        ClubTermPostAdapter.PostViewHolder holder = (ClubTermPostAdapter.PostViewHolder) recyclerView.findViewHolderForAdapterPosition(0);
        assertNotNull(holder);
        assertEquals("99 likes", holder.tvLikeCount.getText().toString());
    }

    // ==========================================
    // 4. 深度覆盖：异常与边界测试
    // ==========================================

    @Test
    public void testToggleJoinStatus_NetworkError_ShowsToast() throws Exception {
        // 保留 enqueue 因为要按顺序：先吃完两个成功，立刻断网
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}"));

        clubterm activity = Robolectric.buildActivity(clubterm.class).create().resume().get();

        // 智能轮询等初始化完成
        Button btnJoin = activity.findViewById(R.id.btnJoin);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Join".equals(btnJoin.getText().toString())) break;
            Thread.sleep(100);
        }

        // 强行关闭服务器模拟断网
        mockWebServer.shutdown();

        activity.findViewById(R.id.btnJoin).performClick();
        androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();

        // 🌟 智能轮询等待 Network Error 的 Toast 弹出
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if ("Network Error".equals(ShadowToast.getTextOfLatestToast())) break;
            Thread.sleep(100);
        }

        assertEquals("Network Error", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testFetchDetails_EmptyJson_HandlesGracefully() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/posts")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                } else {
                    return new MockResponse().setResponseCode(500).setBody("Internal Server Error");
                }
            }
        });

        clubterm activity = Robolectric.buildActivity(clubterm.class).create().resume().get();

        // 给一点点时间确保网络错误被捕获
        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();

        // 就算服务器回传 500 错误，UI 元件依旧存在，不会让 APP 闪退
        assertNotNull(activity.findViewById(R.id.tvClubName));
    }
}