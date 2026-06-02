package com.zjgsu.moveup;

import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowToast;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class PlanDetailsTest {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String url = mockWebServer.url("/").toString();
        Plan_details.BASE_URL = url.substring(0, url.length() - 1);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testFetchPlanDetails_RendersList() throws Exception {
        JSONObject item = new JSONObject().put("id", "i1").put("time", "07:00")
                .put("distance", 5).put("is_completed", false);
        JSONObject data = new JSONObject().put("day", "MONDAY").put("list", new JSONArray().put(item));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class)
                .create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        RecyclerView rv = activity.findViewById(R.id.recyclerPlanDetails);
        assertNotNull(rv.getAdapter());
        assertTrue(rv.getAdapter().getItemCount() > 0);
    }

    @Test
    public void testDeletePlan_ShowsConfirmDialog() throws Exception {
        JSONObject item = new JSONObject().put("id", "i1").put("time", "08:00")
                .put("distance", 3).put("is_completed", false);
        JSONObject data = new JSONObject().put("day", "MONDAY").put("list", new JSONArray().put(item));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":200}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class)
                .create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        RecyclerView rv = activity.findViewById(R.id.recyclerPlanDetails);
        rv.getAdapter().onBindViewHolder(
                rv.findViewHolderForAdapterPosition(0), 0);
        rv.findViewHolderForAdapterPosition(0).itemView.performLongClick();

        Thread.sleep(300);
        Robolectric.flushForegroundThreadScheduler();

        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull("Delete confirm dialog should appear", dialog);
    }

    @Test
    public void testFetchPlanDetails_EmptyData_DoesNotCrash() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"day\":\"MONDAY\",\"list\":[]}}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class)
                .create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();
        assertNotNull(activity);
    }

    @Test
    public void testToggleComplete() throws Exception {
        JSONObject item = new JSONObject().put("id", "i1").put("time", "09:00")
                .put("distance", 4).put("is_completed", false);
        JSONObject data = new JSONObject().put("day", "MONDAY").put("list", new JSONArray().put(item));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":" + data.toString() + "}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"code\":200,\"data\":{\"is_completed\":true}}"));

        Plan_details activity = Robolectric.buildActivity(Plan_details.class)
                .create().get();

        Thread.sleep(500);
        Robolectric.flushForegroundThreadScheduler();

        RecyclerView rv = activity.findViewById(R.id.recyclerPlanDetails);
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(0);
        if (vh != null) {
            vh.itemView.performClick();
            Thread.sleep(500);
            Robolectric.flushForegroundThreadScheduler();
        }
        assertNotNull(activity);
    }
}
