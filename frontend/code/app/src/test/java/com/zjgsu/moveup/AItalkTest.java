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

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class AItalkTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // 将 BASE_URL 指向 MockWebServer
        AItalk.BASE_URL = mockWebServer.url("/").toString();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testSendMessage_UpdatesUI_AndMocksSuccess() throws Exception {
        // 1. Mock 返回数据
        JSONObject mockData = new JSONObject();
        mockData.put("code", 200);
        JSONObject dataObj = new JSONObject().put("reply", "好的，为你推荐慢跑5公里。");
        mockData.put("data", dataObj);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockData.toString()));

        // 2. 启动 Activity
        AItalk activity = Robolectric.buildActivity(AItalk.class).create().resume().get();
        EditText etInput = activity.findViewById(R.id.etInput);

        // 3. 模拟输入并发送
        etInput.setText("我想跑步");
        activity.findViewById(R.id.btnSend).performClick();

        // 4. 断言：发送后输入框应清空
        assertEquals("", etInput.getText().toString());

        // 5. 等待 Mock 返回并刷新 UI
        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
    }
}