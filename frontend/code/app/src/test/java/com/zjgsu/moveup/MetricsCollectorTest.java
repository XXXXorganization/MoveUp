package com.zjgsu.moveup;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MetricsCollectorTest {

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        MetricsCollector.init(context);
    }

    @Test
    public void testRecordRequestStart() {
        MetricsCollector.recordRequestStart("https://api.example.com/test");
        // Should not crash
        assertNotNull(MetricsCollector.getMetrics());
    }

    @Test
    public void testRecordRequestEnd_Success() {
        String url = "https://api.example.com/v1/success-test";
        MetricsCollector.recordRequestStart(url);
        MetricsCollector.recordRequestEnd(url, true);

        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics.get(url + ".count"));
        assertEquals(0.0, metrics.get(url + ".error_rate"));
        assertNotNull(metrics.get(url + ".avg_response_time_ms"));
    }

    @Test
    public void testRecordRequestEnd_Failure() {
        String url = "https://api.example.com/v1/fail-test";
        MetricsCollector.recordRequestStart(url);
        MetricsCollector.recordRequestEnd(url, false);

        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics.get(url + ".count"));
        assertTrue((Double) metrics.get(url + ".error_rate") > 0);
    }

    @Test
    public void testRecordRequestEnd_NoStart() {
        // Calling end without start should not crash
        MetricsCollector.recordRequestEnd("https://unstarted.example.com/unique", true);
        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics);
    }

    @Test
    public void testMultipleRequests() {
        String url1 = "https://api.example.com/v1/multi-1";
        String url2 = "https://api.example.com/v1/multi-2";

        MetricsCollector.recordRequestStart(url1);
        MetricsCollector.recordRequestEnd(url1, true);
        MetricsCollector.recordRequestStart(url1);
        MetricsCollector.recordRequestEnd(url1, true);
        MetricsCollector.recordRequestStart(url2);
        MetricsCollector.recordRequestEnd(url2, false);

        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics.get(url1 + ".count"));
        assertNotNull(metrics.get(url2 + ".count"));
    }

    @Test
    public void testGetMetrics_ReturnsValidMap() {
        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics);
    }

    @Test
    public void testMixSuccessAndFailure() {
        String url = "https://api.example.com/v1/mix-test";
        for (int i = 0; i < 5; i++) {
            MetricsCollector.recordRequestStart(url);
            MetricsCollector.recordRequestEnd(url, i % 2 == 0);
        }

        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics.get(url + ".count"));
        double errorRate = (Double) metrics.get(url + ".error_rate");
        assertTrue(errorRate > 0);
    }

    @Test
    public void testResponseTime_AvgIsCalculated() {
        String url = "https://api.example.com/v1/timing-test";
        MetricsCollector.recordRequestStart(url);
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        MetricsCollector.recordRequestEnd(url, true);

        Map<String, Object> metrics = MetricsCollector.getMetrics();
        assertNotNull(metrics.get(url + ".avg_response_time_ms"));
    }
}
