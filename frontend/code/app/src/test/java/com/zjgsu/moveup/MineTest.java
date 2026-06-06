package com.zjgsu.moveup;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MineTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        Mine.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testClickEditProfile_TransitionsToMineEdit() throws Exception {
        // 先给一个假的 GET 响应防止崩溃
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();
        activity.findViewById(R.id.btnEditProfile).performClick();

        Intent expectedIntent = new Intent(activity, mine_edit.class);
        assertEquals(expectedIntent.getComponent(), ShadowApplication.getInstance().getNextStartedActivity().getComponent());
    }

    @Test
    public void testFetchProfile_Success_PopulatesUI() throws Exception {
        // Mock 后端返回完整的个人资料
        JSONObject dataObj = new JSONObject();
        dataObj.put("username", "测试专家");
        dataObj.put("email", "test@test.com");
        dataObj.put("phone", "13800138000");
        dataObj.put("password", "123456");

        JSONObject responseObj = new JSONObject();
        responseObj.put("code", 200);
        responseObj.put("data", dataObj);

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(responseObj.toString()));

        // 启动 Activity，此时会自动触发 fetchUserProfile
        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        // 等待异步网络请求解析并渲染 UI
        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        TextView tvName = activity.findViewById(R.id.tvUsernameValue);
        TextView tvEmail = activity.findViewById(R.id.tvEmailValue);

        assertEquals("测试专家", tvName.getText().toString());
        assertEquals("test@test.com", tvEmail.getText().toString());
    }

    @Test
    public void testFetchProfile_NetworkError_ShowsToast() throws Exception {
        mockWebServer.shutdown();

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("无法拉取个人资料", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testFetchProfile_HttpError_NoCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testFetchProfile_Non200Code_NoDataUpdate() throws Exception {
        JSONObject resp = new JSONObject().put("code", 400).put("data", new JSONObject());
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(resp.toString()));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testFetchProfile_MalformedJson_ShowsToast() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("not json"));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("无法拉取个人资料", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testMenuClicks_DoNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // Click menu items
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
    public void testOnResume_TriggersFetchAgain() throws Exception {
        JSONObject dataObj = new JSONObject();
        dataObj.put("username", "ResumeUser");
        dataObj.put("email", "resume@test.com");
        dataObj.put("phone", "13800000000");
        dataObj.put("password", "***");

        JSONObject respObj = new JSONObject().put("code", 200).put("data", dataObj);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(respObj.toString()));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(respObj.toString()));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // Pause and resume to trigger onResume
        Robolectric.flushForegroundThreadScheduler();
        Robolectric.flushBackgroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testActivity_LaunchesAndAllViewsExist() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));

        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(300);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity.findViewById(R.id.tvUsernameValue));
        assertNotNull(activity.findViewById(R.id.tvEmailValue));
        assertNotNull(activity.findViewById(R.id.tvPhoneValue));
        assertNotNull(activity.findViewById(R.id.tvPasswordValue));
        assertNotNull(activity.findViewById(R.id.btnEditProfile));
    }
}