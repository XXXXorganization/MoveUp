package com.zjgsu.moveup;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ClubCommunityActivityTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        ClubCommunityActivity.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
        ClubTermPostAdapter.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testActivity_LaunchesSuccessfully() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/posts")) {
                    try {
                        JSONObject post = new JSONObject()
                                .put("id", "p1")
                                .put("author", new JSONObject().put("nickname", "Tester"))
                                .put("content", "Test post")
                                .put("created_at", "2026-01-01")
                                .put("is_liked", false)
                                .put("like_count", 0)
                                .put("comments", new org.json.JSONArray());
                        return new MockResponse().setResponseCode(200)
                                .setBody("{\"code\":200,\"data\":{\"list\":[" + post.toString() + "]}}");
                    } catch (Exception e) {
                        return new MockResponse().setResponseCode(500);
                    }
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Test Club\",\"location\":\"City\",\"image_url\":\"\",\"is_member\":true,\"member_count\":5}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "test-club-id");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // Verify basic UI elements exist
        assertNotNull(activity.findViewById(com.zjgsu.moveup.R.id.tvClubName));
        assertNotNull(activity.findViewById(com.zjgsu.moveup.R.id.etNewPost));
        assertNotNull(activity.findViewById(com.zjgsu.moveup.R.id.recyclerPosts));
        assertNotNull(activity.findViewById(com.zjgsu.moveup.R.id.btnExit));
    }

    @Test
    public void testSendPost_UpdatesUI() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/posts") && request.getMethod().equals("POST")) {
                    try {
                        JSONObject data = new JSONObject()
                                .put("id", "new-post")
                                .put("author", new JSONObject().put("nickname", "Me"))
                                .put("content", "My post")
                                .put("created_at", "now")
                                .put("is_liked", false)
                                .put("like_count", 0)
                                .put("comments", new org.json.JSONArray());
                        return new MockResponse().setResponseCode(200)
                                .setBody("{\"code\":200,\"data\":" + data.toString() + "}");
                    } catch (Exception e) {
                        return new MockResponse().setResponseCode(500);
                    }
                } else if (path != null && path.contains("/posts") && request.getMethod().equals("GET")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Club\",\"location\":\"City\",\"image_url\":\"\",\"is_member\":true,\"member_count\":3}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "club-1");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        EditText et = activity.findViewById(R.id.etNewPost);
        et.setText("Hello world!");
        activity.findViewById(R.id.btnSendPost).performClick();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        // Post input should be cleared after successful send
        assertEquals("", et.getText().toString());
    }

    @Test
    public void testExitButton_ShowsDialog() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/toggle")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"joined\":false}}");
                } else if (path != null && path.contains("/posts")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Club\",\"location\":\"City\",\"image_url\":\"\",\"is_member\":true,\"member_count\":3}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "club-1");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        activity.findViewById(R.id.btnExit).performClick();

        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler();

        // Should show exit confirmation dialog
        assertNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog());
    }
}
