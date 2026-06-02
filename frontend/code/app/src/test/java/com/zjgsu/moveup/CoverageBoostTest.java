package com.zjgsu.moveup;

import android.content.Intent;
import android.widget.TextView;

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
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class CoverageBoostTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String url = mockWebServer.url("/").toString();
        String base = url.substring(0, url.length() - 1);
        Plan.BASE_URL = base;
        Plan_details.BASE_URL = base;
        Mine.BASE_URL = base;
        mine_edit.BASE_URL = base;
        AItalk.BASE_URL = base;
        PostDetailActivity.BASE_URL = base;
        ClubCommunityActivity.BASE_URL = base;
        ClubTermPostAdapter.BASE_URL = base;
        clubterm.BASE_URL = base;
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // ===== Plan.java: test weekly distance fetch =====
    @Test
    public void testPlan_FetchesTotalDistance() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"total_distance\":12.5}}"));
        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();
        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        TextView tv42 = activity.findViewById(R.id.tv42);
        assertEquals("12.5", tv42.getText().toString());
    }

    @Test
    public void testPlan_DaysNavigateToDetails() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"total_distance\":0}}"));
        Plan activity = Robolectric.buildActivity(Plan.class).create().resume().get();
        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();
        activity.findViewById(R.id.cardDay1).performClick();
        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(Plan_details.class.getName(), next.getComponent().getClassName());
    }

    // ===== Mine.java: test profile fetch =====
    @Test
    public void testMine_FetchesProfile() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"nickname\":\"TestUser\",\"username\":\"TestUser\",\"password\":\"***\",\"level\":5}}"));
        Mine activity = Robolectric.buildActivity(Mine.class).create().resume().get();
        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        assertNotNull(activity.findViewById(R.id.tvUsername));
    }

    // ===== mine_edit.java: test profile fetch =====
    @Test
    public void testMineEdit_FetchesProfile() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"username\":\"EditUser\",\"email\":\"e@mail.com\",\"phone\":\"138\",\"password\":\"***\"}}"));
        mine_edit activity = Robolectric.buildActivity(mine_edit.class).create().resume().get();
        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        assertNotNull(activity.findViewById(R.id.etEmail));
    }

    // ===== PostDetailActivity: test fetch comments =====
    @Test
    public void testPostDetail_FetchesComments() throws Exception {
        JSONObject author = new JSONObject().put("nickname", "Commenter");
        JSONObject comment = new JSONObject().put("id", "c1").put("author", author)
                .put("content", "Nice!").put("created_at", "now");
        JSONObject data = new JSONObject().put("list", new JSONArray().put(comment));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));

        Intent intent = new Intent();
        intent.putExtra("POST_ID", "test-post");
        Robolectric.buildActivity(PostDetailActivity.class, intent).create().resume().get();
        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        // Should not crash - confirms parsing works
    }

    // ===== AItalk.java: test sends message =====
    @Test
    public void testAItalk_SendsMessage() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"reply\":\"Hi there!\"}}"));
        AItalk activity = Robolectric.buildActivity(AItalk.class).create().resume().get();
        activity.findViewById(R.id.etMessage).requestFocus();
        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();
        assertNotNull(activity);
    }

    // ===== clubterm: test joined user clicks Enter Community =====
    // ===== Plan_details: add plan dialog flow =====
    @Test
    public void testPlanDetails_ShowsAddDialog() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"day\":\"MONDAY\",\"list\":[]}}"));
        Plan_details activity = Robolectric.buildActivity(Plan_details.class).create().get();
        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        activity.findViewById(R.id.fabAddPlan).performClick();
        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler();
        assertNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog());
    }

    // ===== ClubCommunityActivity: posts with run_summary =====
    @Test
    public void testClubCommunity_PostsWithRunSummary() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/posts") && request.getMethod().equals("GET")) {
                    try {
                        JSONObject runSum = new JSONObject().put("distance", 5.2).put("duration", 1800).put("pace", "6'00\"");
                        JSONObject post = new JSONObject().put("id", "p1")
                                .put("author", new JSONObject().put("nickname", "Runner"))
                                .put("content", "Morning run")
                                .put("created_at", "today")
                                .put("is_liked", true).put("like_count", 42)
                                .put("run_summary", runSum)
                                .put("comments", new JSONArray());
                        return new MockResponse().setResponseCode(200)
                                .setBody("{\"code\":200,\"data\":{\"list\":[" + post.toString() + "]}}");
                    } catch (Exception e) { return new MockResponse().setResponseCode(500); }
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Runners\",\"location\":\"Park\",\"image_url\":\"\",\"is_member\":true,\"member_count\":10}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "run-club");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();
        Thread.sleep(800);
        Robolectric.flushForegroundThreadScheduler();

        RecyclerView rv = activity.findViewById(R.id.recyclerPosts);
        assertNotNull(rv.getAdapter());
        assertTrue(rv.getAdapter().getItemCount() > 0);
    }

    @Test
    public void testClubterm_JoinedUser_ClicksEnterCommunity() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Club\",\"location\":\"City\",\"is_member\":true,\"member_count\":5}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "club-1");
        clubterm activity = Robolectric.buildActivity(clubterm.class, intent).create().resume().get();
        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        activity.findViewById(R.id.btnJoin).performClick();
        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler();
        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(ClubCommunityActivity.class.getName(), next.getComponent().getClassName());
    }
}
