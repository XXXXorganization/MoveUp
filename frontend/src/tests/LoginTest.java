package com.zjgsu.moveup;

import android.content.Intent;
import android.widget.EditText;
import android.widget.ImageView;

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
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27) // 🌟 修复：使用 SDK 27 运行，完美绕过高版本 Android 对 http:// 明文请求的强制拦截
public class LoginTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        Login.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==========================================
    // 任务1：UI 组件交互测试
    // ==========================================

    @Test
    public void testEmptyInput_ShowsToast() {
        Login activity = Robolectric.buildActivity(Login.class).create().resume().get();
        activity.findViewById(R.id.btn_login).performClick();
        assertEquals("手机号和密码不能为空", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testClickGoRegister_TransitionsToRegister() {
        Login activity = Robolectric.buildActivity(Login.class).create().resume().get();
        activity.findViewById(R.id.btn_go_register).performClick();
        Intent expectedIntent = new Intent(activity, Register.class);
        assertEquals(expectedIntent.getComponent(), ShadowApplication.getInstance().getNextStartedActivity().getComponent());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testEyeIcon_TogglesPasswordVisibility() {
        Login activity = Robolectric.buildActivity(Login.class).create().resume().get();
        ImageView ivEye = activity.findViewById(R.id.iv_eye_login);
        EditText etPwd = activity.findViewById(R.id.et_password);

        // 🌟 修复：获取点击前和点击后的 TransformationMethod 对象对比
        android.text.method.TransformationMethod methodBefore = etPwd.getTransformationMethod();
        ivEye.performClick();
        android.text.method.TransformationMethod methodAfter = etPwd.getTransformationMethod();

        assertTrue("点击眼睛图标后，密码可见性应变化", methodBefore != methodAfter);
    }

    // ==========================================
    // 任务2：Mock API 网络测试
    // ==========================================

    @Test
    public void testApiSuccess_200_TransitionsToMain() throws Exception {
        JSONObject mockData = new JSONObject();
        mockData.put("code", 200);
        mockData.put("data", new JSONObject().put("token", "mock_token"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockData.toString()));

        Login activity = Robolectric.buildActivity(Login.class).create().resume().get();
        ((EditText) activity.findViewById(R.id.et_phone)).setText("13800138000");
        ((EditText) activity.findViewById(R.id.et_password)).setText("123456");

        activity.findViewById(R.id.btn_login).performClick();

        // 🌟 延长等待时间，确保后台网络线程有足够时间返回给主线程
        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("登录成功", ShadowToast.getTextOfLatestToast());
        assertNotNull(ShadowApplication.getInstance().getNextStartedActivity());
    }

    @Test
    public void testApiFail_401_WrongPassword() throws Exception {
        JSONObject mockData = new JSONObject();
        mockData.put("code", 401);
        mockData.put("message", "密码错误");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockData.toString()));

        Login activity = Robolectric.buildActivity(Login.class).create().resume().get();
        ((EditText) activity.findViewById(R.id.et_phone)).setText("13800138000");
        ((EditText) activity.findViewById(R.id.et_password)).setText("wrong_pwd");

        activity.findViewById(R.id.btn_login).performClick();

        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("密码错误", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testApiNetworkError_ShowsToast() throws Exception {
        mockWebServer.shutdown();

        Login activity = Robolectric.buildActivity(Login.class).create().resume().get();
        ((EditText) activity.findViewById(R.id.et_phone)).setText("13800138000");
        ((EditText) activity.findViewById(R.id.et_password)).setText("123456");

        activity.findViewById(R.id.btn_login).performClick();

        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("网络连接异常，请检查网络", ShadowToast.getTextOfLatestToast());
    }
}