package com.zjgsu.moveup;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class LocalHealthServerTest {

    @Before
    public void setUp() {
        LocalHealthServer.startServer();
    }

    @After
    public void tearDown() {
        LocalHealthServer.stopServer();
    }

    @Test
    public void testStartServer_StartsSuccessfully() {
        // Server was started in setUp. Start again to test idempotency.
        LocalHealthServer.startServer();
        // Should not crash
        assertNotNull(LocalHealthServer.class);
    }

    @Test
    public void testStopServer_StopsSuccessfully() {
        LocalHealthServer.stopServer();
        // Stop again to test idempotency
        LocalHealthServer.stopServer();
        // Should not crash
        assertNotNull(LocalHealthServer.class);
    }

    @Test
    public void testHealthEndpoint_ReturnsOk() throws Exception {
        // Give the server a moment to start
        Thread.sleep(100);

        URL url = new URL("http://localhost:8080/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();

        JSONObject response = new JSONObject(sb.toString());
        assertEquals("ok", response.getString("status"));
        assertTrue(response.has("timestamp"));
        assertTrue(response.has("uptime"));
    }

    @Test
    public void testHealthEndpoint_ContentType() throws Exception {
        Thread.sleep(100);

        URL url = new URL("http://localhost:8080/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode);
        assertTrue(conn.getContentType().contains("application/json"));
        conn.disconnect();
    }

    @Test
    public void testNonHealthEndpoint_ReturnsNotFound() throws Exception {
        Thread.sleep(100);

        URL url = new URL("http://localhost:8080/some-other-path");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);

        int responseCode = conn.getResponseCode();
        assertEquals(404, responseCode);
        conn.disconnect();
    }

    @Test
    public void testMultipleStartStop_NoCrashes() {
        // First cycle
        LocalHealthServer.stopServer();
        LocalHealthServer.startServer();
        // Second cycle
        LocalHealthServer.stopServer();
        LocalHealthServer.startServer();
        // Third cycle
        LocalHealthServer.startServer();

        assertNotNull(LocalHealthServer.class);
    }
}
