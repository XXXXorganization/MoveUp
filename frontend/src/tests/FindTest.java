package com.zjgsu.moveup;

import android.widget.Toast;

import org.json.JSONArray;
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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27) // 使用 SDK 27 避免高版本默认拦截 Http 请求
public class FindTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        // 启动本地虚拟服务器
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 🌟 将 Find.java 中的全局 BASE_URL 指向我们本地的虚拟测试服务器
        String mockUrl = mockWebServer.url("/").toString();
        Find.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testFetchClubs_EmptyResult_ShowsToast() throws Exception {
        // 1. Mock 构建后端返回的数据：返回一个空的跑团列表
        JSONObject dataObj = new JSONObject();
        dataObj.put("list", new JSONArray()); // 空数组
        JSONObject respObj = new JSONObject().put("code", 200).put("data", dataObj);

        // 让虚拟服务器准备好返回这个 JSON 响应
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(respObj.toString()));

        // 2. 启动 Find Activity (一启动就会自动触发 fetchClubs(""))
        Robolectric.buildActivity(Find.class).create().resume().get();

        // 3. 等待子线程网络请求回来并刷新主线程 UI
        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // 4. 断言验证：检查是否正确弹出了提示未找到跑团的 Toast
        assertEquals("未找到相关的跑团", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testFetchClubs_NetworkError_ShowsToast() throws Exception {
        // 1. 模拟网络异常：直接把虚拟服务器关掉
        mockWebServer.shutdown();

        // 2. 启动 Find Activity
        Robolectric.buildActivity(Find.class).create().resume().get();

        // 3. 等待子线程 catch 块执行并抛出 Toast
        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // 4. 断言验证：捕捉网络异常的提示
        assertEquals("网络异常", ShadowToast.getTextOfLatestToast());
    }
}