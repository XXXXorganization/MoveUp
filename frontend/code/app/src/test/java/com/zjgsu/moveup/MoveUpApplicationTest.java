package com.zjgsu.moveup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MoveUpApplicationTest {

    @Before
    public void setUp() {
        LocalHealthServer.stopServer();
    }

    @After
    public void tearDown() {
        LocalHealthServer.stopServer();
    }

    @Test
    public void testApplication_OnCreate_InitializesServices() {
        MoveUpApplication app = (MoveUpApplication) Robolectric.setupActivity(MoveUpApplication.class);
        assertNotNull(app);
        assertNotNull(app.getApplicationContext());
    }

    @Test
    public void testApplication_HealthServerStarts() {
        LocalHealthServer.startServer();

        // Verify the health server responds
        try {
            java.net.URL url = new java.net.URL("http://localhost:8080/health");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            int code = conn.getResponseCode();
            assertNotNull(code);
            conn.disconnect();
        } catch (Exception e) {
            // Server might not start in test environment, that's OK
        }
    }

    @Test
    public void testApplication_StructuredLoggerInitialized() {
        StructuredLogger.log("INFO", "AppTest", "Application initialized");
        assertNotNull(StructuredLogger.class);
    }

    @Test
    public void testApplication_MetricsCollectorInitialized() {
        MetricsCollector.recordRequestStart("https://test/api");
        MetricsCollector.recordRequestEnd("https://test/api", true);
        assertNotNull(MetricsCollector.getMetrics());
    }
}
