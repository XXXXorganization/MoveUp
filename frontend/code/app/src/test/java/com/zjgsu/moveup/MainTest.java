package com.zjgsu.moveup;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MainTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        Main.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 1. UI 交互与基础页面跳转分支
    // ==========================================

    @Test
    public void testMain_UI_Navigations() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
            }
        });

        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();

        ImageView ivStart = activity.findViewById(R.id.ivStart);
        ivStart.performClick();
        Intent nextIntent1 = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(Runing.class.getName(), nextIntent1.getComponent().getClassName());

        TextView btnSeeAllClubs = activity.findViewById(R.id.btnSeeAllClubs);
        btnSeeAllClubs.performClick();
        Intent nextIntent2 = ShadowApplication.getInstance().getNextStartedActivity();
        assertEquals(Find.class.getName(), nextIntent2.getComponent().getClassName());
    }

    @Test
    public void testMain_Drawer_Menu_Branches() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
            }
        });

        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();

        activity.findViewById(R.id.btnMenu).performClick();

        activity.findViewById(R.id.menu_home).performClick();

        activity.findViewById(R.id.menu_history).performClick();
        assertEquals(History.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_plan).performClick();
        assertEquals(Plan.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_club).performClick();
        assertEquals(clubterm.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());

        activity.findViewById(R.id.menu_profile).performClick();
        assertEquals(Mine.class.getName(), ShadowApplication.getInstance().getNextStartedActivity().getComponent().getClassName());
    }

    // ==========================================
    // 2. 深度覆盖：活动历史卡片渲染逻辑
    // ==========================================

    @Test
    public void testFetchActivities_WithTwoItems_Branch() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    JSONObject run1 = new JSONObject().put("title", "Morning Run").put("distance", "5.0 Km");
                    JSONObject run2 = new JSONObject().put("title", "Night Run").put("distance", "3.0 Km");
                    JSONArray listArray = new JSONArray().put(run1).put(run2);
                    return new MockResponse().setResponseCode(200)
                            .setBody(new JSONObject().put("code", 200).put("data", new JSONObject().put("list", listArray)).toString());
                } catch (Exception e) {
                    return new MockResponse().setResponseCode(500);
                }
            }
        });

        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();

        View card1 = activity.findViewById(R.id.activityCard1);
        View card2 = activity.findViewById(R.id.activityCard2);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (card2.getVisibility() == View.VISIBLE) break;
            Thread.sleep(100);
        }

        assertEquals(View.VISIBLE, card1.getVisibility());
        assertEquals(View.VISIBLE, card2.getVisibility());
    }

    @Test
    public void testFetchActivities_WithOneItem_Branch() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    JSONObject run1 = new JSONObject().put("title", "Solo Run").put("distance", "5.0 Km");
                    JSONArray listArray = new JSONArray().put(run1);
                    return new MockResponse().setResponseCode(200)
                            .setBody(new JSONObject().put("code", 200).put("data", new JSONObject().put("list", listArray)).toString());
                } catch (Exception e) {
                    return new MockResponse().setResponseCode(500);
                }
            }
        });

        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();

        View card1 = activity.findViewById(R.id.activityCard1);
        View card2 = activity.findViewById(R.id.activityCard2);
        for (int i = 0; i < 20; i++) {
            Robolectric.flushForegroundThreadScheduler();
            if (card1.getVisibility() == View.VISIBLE) break;
            Thread.sleep(100);
        }

        assertEquals(View.VISIBLE, card1.getVisibility());
        assertEquals(View.GONE, card2.getVisibility());
    }

    @Test
    public void testFetchActivities_WithZeroItems_Branch() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
            }
        });

        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();

        View card1 = activity.findViewById(R.id.activityCard1);
        View card2 = activity.findViewById(R.id.activityCard2);

        TimeUnit.MILLISECONDS.sleep(300);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals(View.GONE, card1.getVisibility());
        assertEquals(View.GONE, card2.getVisibility());
    }

    @Test
    public void testFetchActivities_NetworkError_CatchBranch() throws Exception {
        mockWebServer.shutdown(); // 断网触发 Exception

        Main activity = Robolectric.buildActivity(Main.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(300);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }
}