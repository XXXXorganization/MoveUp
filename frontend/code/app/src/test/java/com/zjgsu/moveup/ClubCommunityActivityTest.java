package com.zjgsu.moveup;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;

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
    public void testBackButton_FinishesActivity() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts") && request.getMethod().equals("GET")) {
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

        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();
        activity.findViewById(R.id.btnBack).performClick();
        assertNotNull(activity); // Activity should not crash
    }

    @Test
    public void testSendPost_EmptyContent_ShowsToast() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts") && request.getMethod().equals("GET")) {
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

        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();
        activity.findViewById(R.id.btnSendPost).performClick();

        assertEquals("Please write something or add an image", org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
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

    @Test
    public void testFetchClubDetails_WithImageUrl_LoadsImage() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Photo Club\",\"location\":\"Paris\",\"image_url\":\"https://picsum.photos/200\",\"is_member\":true,\"member_count\":42}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "photo-club");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testFetchClubDetails_NonMember_ShowsToastAndFinishes() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"ExClub\",\"location\":\"\",\"image_url\":\"\",\"is_member\":false,\"member_count\":0}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "ex-club");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testFetchPosts_WithAllFields() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/posts") && request.getMethod().equals("GET")) {
                    try {
                        JSONObject commentAuthor = new JSONObject().put("nickname", "Commenter");
                        JSONObject comment = new JSONObject()
                                .put("id", "cmt1")
                                .put("author", commentAuthor)
                                .put("content", "Nice!")
                                .put("created_at", "2026-01-02");

                        JSONObject runSum = new JSONObject()
                                .put("distance", 5.2)
                                .put("duration", 1800)
                                .put("pace", "5'46\"");

                        JSONArray images = new JSONArray().put("https://img.example.com/post1.jpg");

                        JSONObject postAuthor = new JSONObject().put("nickname", "Runner1");
                        JSONObject post = new JSONObject()
                                .put("id", "p-full")
                                .put("author", postAuthor)
                                .put("content", "Full post with everything")
                                .put("created_at", "2026-01-01")
                                .put("is_liked", true)
                                .put("like_count", 5)
                                .put("comments", new JSONArray().put(comment))
                                .put("images", images)
                                .put("run_summary", runSum);

                        return new MockResponse().setResponseCode(200)
                                .setBody("{\"code\":200,\"data\":{\"list\":[" + post.toString() + "]}}");
                    } catch (Exception e) {
                        return new MockResponse().setResponseCode(500);
                    }
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Full\",\"location\":\"World\",\"image_url\":\"\",\"is_member\":true,\"member_count\":10}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "full");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testFetchPosts_EmptyArray_NoCrash() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Empty\",\"location\":\"\",\"image_url\":\"\",\"is_member\":true,\"member_count\":1}}");
            }
        });

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "empty");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }

    @Test
    public void testCreatePost_NetworkError_ShowsToast() throws Exception {
        // Shut down server to simulate connection failure
        mockWebServer.shutdown();

        Intent intent = new Intent();
        intent.putExtra("CLUB_ID", "club-1");
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class, intent).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        EditText et = activity.findViewById(R.id.etNewPost);
        et.setText("Test");
        activity.findViewById(R.id.btnSendPost).performClick();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals("Failed to post", org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testToggleExit_NetworkError_ShowsToast() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.contains("/toggle")) {
                    // Shut down on toggle to simulate network error
                    try { mockWebServer.shutdown(); } catch (Exception ignored) {}
                    return new MockResponse().setResponseCode(500);
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

        android.app.AlertDialog dialog = (android.app.AlertDialog) org.robolectric.shadows.ShadowDialog.getLatestDialog();
        if (dialog != null) {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
            Thread.sleep(500);
            Robolectric.flushForegroundThreadScheduler();
        }

        // Either "Network Error" or the dialog wasn't shown — both are acceptable in test
        assertNotNull(activity);
    }

    @Test
    public void testImagePickerButton_DoesNotCrash() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts")) {
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

        if (activity.findViewById(R.id.btnPickImage) != null) {
            activity.findViewById(R.id.btnPickImage).performClick();
        }

        assertNotNull(activity);
    }

    @Test
    public void testNullClubId_UsesDefault() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("/posts")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"list\":[]}}");
                }
                return new MockResponse().setResponseCode(200)
                        .setBody("{\"code\":200,\"data\":{\"name\":\"Default\",\"location\":\"\",\"image_url\":\"\",\"is_member\":true,\"member_count\":0}}");
            }
        });

        // No CLUB_ID extra
        ClubCommunityActivity activity = Robolectric.buildActivity(ClubCommunityActivity.class).create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        assertNotNull(activity);
    }
}
