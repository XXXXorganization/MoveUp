package com.zjgsu.moveup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {
    private static final Map<String, AtomicLong> requestCount = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> errorCount = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final Map<String, Long> totalResponseTime = new ConcurrentHashMap<>();

    public static void init(Context context) {
        startPeriodicLogging();
    }

    public static void recordRequestStart(String url) {
        lastRequestTime.put(url, System.currentTimeMillis());
    }

    public static void recordRequestEnd(String url, boolean success) {
        Long start = lastRequestTime.get(url);
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            totalResponseTime.merge(url, duration, Long::sum);
            requestCount.computeIfAbsent(url, k -> new AtomicLong()).incrementAndGet();
            if (!success) {
                errorCount.computeIfAbsent(url, k -> new AtomicLong()).incrementAndGet();
            }
        }
    }

    public static Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        for (String url : requestCount.keySet()) {
            long count = requestCount.get(url).get();
            long errors = errorCount.getOrDefault(url, new AtomicLong()).get();
            long totalTime = totalResponseTime.getOrDefault(url, 0L);
            double avgTime = count > 0 ? totalTime / (double) count : 0;
            double errorRate = count > 0 ? (errors / (double) count) * 100 : 0;
            metrics.put(url + ".count", count);
            metrics.put(url + ".error_rate", errorRate);
            metrics.put(url + ".avg_response_time_ms", avgTime);
        }
        return metrics;
    }

    private static void startPeriodicLogging() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> metrics = getMetrics();
                if (!metrics.isEmpty()) {
                    StructuredLogger.log("INFO", "Metrics", metrics.toString());
                }
                handler.postDelayed(this, 60000); // 每分钟输出一次
            }
        }, 60000);
    }
}