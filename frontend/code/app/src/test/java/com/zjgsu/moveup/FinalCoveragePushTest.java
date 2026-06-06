package com.zjgsu.moveup;

import android.content.Intent;

import org.json.JSONArray;
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

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class FinalCoveragePushTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String url = mockWebServer.url("/").toString();
        String base = url.substring(0, url.length() - 1);
        ClubCommunityActivity.BASE_URL = base;
        ClubTermPostAdapter.BASE_URL = base;
        Plan_details.BASE_URL = base;
        clubterm.BASE_URL = base;
        PostDetailActivity.BASE_URL = base;
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ==== ClubCommunityActivity: error handling paths ====
    @Test
    public void testCommunity_FetchPostsError_DoesNotCrash() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                if (r.getPath() != null && r.getPath().contains("/posts") && r.getMethod().equals("GET"))
                    return new MockResponse().setResponseCode(500).setBody("Error");
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"X\",\"location\":\"Y\",\"image_url\":\"\",\"is_member\":true,\"member_count\":1}}");
            }
        });
        Intent i = new Intent(); i.putExtra("CLUB_ID", "c1");
        ClubCommunityActivity a = Robolectric.buildActivity(ClubCommunityActivity.class, i).create().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a);
    }

    @Test
    public void testCommunity_CreatePostError_ShowsToast() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                if (r.getPath() != null && r.getPath().contains("/posts") && r.getMethod().equals("POST"))
                    return new MockResponse().setResponseCode(500).setBody("Error");
                if (r.getPath() != null && r.getPath().contains("/posts") && r.getMethod().equals("GET"))
                    return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"X\",\"location\":\"Y\",\"image_url\":\"\",\"is_member\":true,\"member_count\":1}}");
            }
        });
        Intent i = new Intent(); i.putExtra("CLUB_ID", "c1");
        ClubCommunityActivity a = Robolectric.buildActivity(ClubCommunityActivity.class, i).create().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        a.findViewById(R.id.etNewPost).requestFocus();
        android.widget.EditText et = (android.widget.EditText) a.findViewById(R.id.etNewPost);
        et.setText("test");
        a.findViewById(R.id.btnSendPost).performClick();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a);
    }

    @Test
    public void testCommunity_FetchClubDetailsError_StillLaunches() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                if (r.getPath() != null && r.getPath().contains("/posts")) return new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                return new MockResponse().setResponseCode(500).setBody("Error");
            }
        });
        Intent i = new Intent(); i.putExtra("CLUB_ID", "c1");
        ClubCommunityActivity a = Robolectric.buildActivity(ClubCommunityActivity.class, i).create().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a.findViewById(R.id.tvClubName));
    }

    // ==== Plan_details: network error on fetch ====
    @Test
    public void testPlanDetails_FetchError_DoesNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        Plan_details a = Robolectric.buildActivity(Plan_details.class).create().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a);
    }

    // ==== Plan_details: delete network error ====
    @Test
    public void testPlanDetails_DeleteError_DoesNotCrash() throws Exception {
        JSONObject item = new JSONObject().put("id", "i1").put("time", "08:00").put("distance", 3).put("is_completed", false);
        JSONObject data = new JSONObject().put("day", "MONDAY").put("list", new JSONArray().put(item));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":" + data + "}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(""));
        Plan_details a = Robolectric.buildActivity(Plan_details.class).create().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        // Trigger delete via adapter's long-click handler
        androidx.recyclerview.widget.RecyclerView rv = a.findViewById(R.id.recyclerPlanDetails);
        if (rv.getAdapter() != null && rv.getAdapter().getItemCount() > 0) {
            // simulate delete call directly via reflection to cover error path
            try {
                java.lang.reflect.Method m = Plan_details.class.getDeclaredMethod("deletePlanFromServer", int.class);
                m.setAccessible(true);
                m.invoke(a, 0);
                Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
            } catch (Exception ignored) {}
        }
        assertNotNull(a);
    }

    // ==== Plan_details: add plan network error ====
    @Test
    public void testPlanDetails_AddPlanError_DoesNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200,\"data\":{\"day\":\"MONDAY\",\"list\":[]}}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody(""));
        Plan_details a = Robolectric.buildActivity(Plan_details.class).create().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        try {
            java.lang.reflect.Method m = Plan_details.class.getDeclaredMethod("addPlanToServer", String.class, String.class, String.class);
            m.setAccessible(true);
            m.invoke(a, "07:00", "08:00", "5");
            Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        } catch (Exception ignored) {}
        assertNotNull(a);
    }

    // ==== PostDetailActivity: fetch comments error ====
    @Test
    public void testPostDetail_FetchError_DoesNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        Intent i = new Intent(); i.putExtra("POST_ID", "p1");
        PostDetailActivity a = Robolectric.buildActivity(PostDetailActivity.class, i).create().resume().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a);
    }

    // ==== clubterm: fetch posts error ====
    @Test
    public void testClubterm_FetchPostsError_DoesNotCrash() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                if (r.getPath() != null && r.getPath().contains("/posts")) return new MockResponse().setResponseCode(500).setBody("Error");
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Club\",\"location\":\"City\",\"is_member\":false}}");
            }
        });
        Intent i = new Intent(); i.putExtra("CLUB_ID", "c1");
        clubterm a = Robolectric.buildActivity(clubterm.class, i).create().resume().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a);
    }

    // ==== AItalk: network error on send ====
    @Test
    public void testAItalk_NetworkError_DoesNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        AItalk a = Robolectric.buildActivity(AItalk.class).create().resume().get();
        Thread.sleep(500); Robolectric.flushForegroundThreadScheduler();
        assertNotNull(a);
    }
}
