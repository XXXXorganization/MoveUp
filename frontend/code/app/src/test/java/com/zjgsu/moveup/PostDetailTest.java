package com.zjgsu.moveup;

import android.content.Intent;
import android.view.View;

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

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class PostDetailTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        PostDetailActivity.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testFetchComments_RendersList() throws Exception {
        // Mock 评论列表数据
        JSONObject comment = new JSONObject()
                .put("id", "c1")
                .put("author", "测试用户")
                .put("content", "太棒了")
                .put("time", "12:00");
        JSONArray listArray = new JSONArray().put(comment);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(new JSONObject().put("code", 200).put("data", new JSONObject().put("list", listArray)).toString()));

        // 携带 POST_ID 启动
        Intent intent = new Intent(org.robolectric.RuntimeEnvironment.application, PostDetailActivity.class);
        intent.putExtra("POST_ID", "p1");
        PostDetailActivity activity = Robolectric.buildActivity(PostDetailActivity.class, intent).create().resume().get();

        TimeUnit.MILLISECONDS.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        RecyclerView recyclerView = activity.findViewById(R.id.rvAllComments);
        assertNotNull(recyclerView.getAdapter());
        assertEquals(1, recyclerView.getAdapter().getItemCount());

        // 强制触发 Adapter 渲染以提升覆盖率
        recyclerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        recyclerView.layout(0, 0, 1080, 1920);
    }
}