package com.zjgsu.moveup;

import android.widget.EditText;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MineEditTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        mine_edit.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testEmptyInput_ShowsToast() throws Exception {
        // Mock 初始化的 GET 请求
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));

        mine_edit activity = Robolectric.buildActivity(mine_edit.class).create().resume().get();

        // 🌟 修复：先等初始化请求跑完
        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // 清空输入框
        ((EditText) activity.findViewById(R.id.et_edit_username)).setText("");
        ((EditText) activity.findViewById(R.id.et_edit_password)).setText("");

        // 点击更新
        activity.findViewById(R.id.btnUpdateProfile).performClick();

        assertEquals("用户名和密码不能为空", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testClickBack_FinishesActivity() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));
        mine_edit activity = Robolectric.buildActivity(mine_edit.class).create().resume().get();

        // 🌟 修复：先等初始化请求跑完
        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        activity.findViewById(R.id.btnBack).performClick();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testUpdateProfile_Success_ShowsDetailedToast() throws Exception {
        // 1. 第一个请求：页面创建时拉取数据的 GET 请求
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{}}"));

        // 2. 第二个请求：点击保存时上传数据的 PUT 请求
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"message\":\"success\"}"));

        mine_edit activity = Robolectric.buildActivity(mine_edit.class).create().resume().get();

        // 🌟 核心修复：在这里等待初始化的 GET 请求完成！
        // 否则刚才用 setText 填写的"新昵称123"会被 GET 请求返回的空数据覆盖掉，导致走入"用户名不能为空"的报错分支
        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // 填写新资料
        ((EditText) activity.findViewById(R.id.et_edit_username)).setText("新昵称123");
        ((EditText) activity.findViewById(R.id.et_edit_email)).setText("new@mail.com");
        ((EditText) activity.findViewById(R.id.et_edit_password)).setText("888888");

        activity.findViewById(R.id.btnUpdateProfile).performClick();

        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        // 断言是否弹出了包含你刚刚修改数据的详细 Toast
        String expectedToast = "资料更新成功！\n新昵称: 新昵称123\n新邮箱: new@mail.com\n新密码: 888888";
        assertEquals(expectedToast, ShadowToast.getTextOfLatestToast());
        assertTrue(activity.isFinishing()); // 保存成功后应该销毁当前页
    }
}