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
@Config(sdk = 27) // 🌟 继续使用 SDK 27，防止网络拦截崩溃
public class RegisterTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        // 启动本地虚拟服务器
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // 替换 Register 页面里的基础 URL
        String mockUrl = mockWebServer.url("/").toString();
        Register.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
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
        Register activity = Robolectric.buildActivity(Register.class).create().resume().get();
        // 不填写任何内容，直接点击注册
        activity.findViewById(R.id.btn_register_confirm1).performClick();
        assertEquals("请填写所有注册信息", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testPasswordMismatch_ShowsToast() {
        Register activity = Robolectric.buildActivity(Register.class).create().resume().get();
        ((EditText) activity.findViewById(R.id.et_phone1)).setText("13800000000");
        ((EditText) activity.findViewById(R.id.et_username1)).setText("TestUser");
        ((EditText) activity.findViewById(R.id.et_password1)).setText("123456");
        // 确认密码和第一次输入的不一样
        ((EditText) activity.findViewById(R.id.et_confirm_pwd1)).setText("654321");

        activity.findViewById(R.id.btn_register_confirm1).performClick();
        assertEquals("两次输入的密码不一致", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testClickBack_TransitionsToStart() {
        Register activity = Robolectric.buildActivity(Register.class).create().resume().get();
        // 点击左上角返回按钮
        activity.findViewById(R.id.btn_back).performClick();

        Intent expectedIntent = new Intent(activity, Start.class);
        assertEquals(expectedIntent.getComponent(), ShadowApplication.getInstance().getNextStartedActivity().getComponent());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testEyeIcon_TogglesPasswordVisibility() {
        Register activity = Robolectric.buildActivity(Register.class).create().resume().get();
        ImageView ivEyePwd1 = activity.findViewById(R.id.iv_eye_pwd1);
        EditText etPwd1 = activity.findViewById(R.id.et_password1);

        android.text.method.TransformationMethod methodBefore = etPwd1.getTransformationMethod();
        ivEyePwd1.performClick(); // 点击眼睛图标
        android.text.method.TransformationMethod methodAfter = etPwd1.getTransformationMethod();

        // 验证密码的显示状态发生了改变
        assertTrue("点击眼睛图标后，密码可见性应变化", methodBefore != methodAfter);
    }

    // ==========================================
    // 任务2：Mock API 网络测试 (正常、异常)
    // ==========================================

    @Test
    public void testApiSuccess_200_TransitionsToLogin() throws Exception {
        // 1. Mock 虚拟服务器：返回注册成功 200
        JSONObject mockData = new JSONObject();
        mockData.put("code", 200);
        mockData.put("message", "注册成功");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockData.toString()));

        Register activity = Robolectric.buildActivity(Register.class).create().resume().get();
        ((EditText) activity.findViewById(R.id.et_phone1)).setText("13800138000");
        ((EditText) activity.findViewById(R.id.et_username1)).setText("TestName");
        ((EditText) activity.findViewById(R.id.et_password1)).setText("123456");
        ((EditText) activity.findViewById(R.id.et_confirm_pwd1)).setText("123456");

        // 点击注册
        activity.findViewById(R.id.btn_register_confirm1).performClick();

        // 🌟 等待后台线程执行完毕
        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("注册成功", ShadowToast.getTextOfLatestToast());
        // 验证跳转到了 Login 页面
        Intent nextIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(new Intent(activity, Login.class).getComponent(), nextIntent.getComponent());
    }

    @Test
    public void testApiNetworkError_ShowsToast() throws Exception {
        // 让虚拟服务器立即关闭，模拟断网
        mockWebServer.shutdown();

        Register activity = Robolectric.buildActivity(Register.class).create().resume().get();
        ((EditText) activity.findViewById(R.id.et_phone1)).setText("13800138000");
        ((EditText) activity.findViewById(R.id.et_username1)).setText("TestName");
        ((EditText) activity.findViewById(R.id.et_password1)).setText("123456");
        ((EditText) activity.findViewById(R.id.et_confirm_pwd1)).setText("123456");

        activity.findViewById(R.id.btn_register_confirm1).performClick();

        TimeUnit.MILLISECONDS.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("网络连接异常，请检查网络", ShadowToast.getTextOfLatestToast());
    }
}