package com.zjgsu.moveup;

import android.util.Log;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;

public class LocalHealthServer extends NanoHTTPD {
    private static final int PORT = 8080;
    private static LocalHealthServer instance;

    private LocalHealthServer() {
        super(PORT);
    }

    public static synchronized void startServer() {
        if (instance == null) {
            instance = new LocalHealthServer();
            try {
                instance.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                Log.i("HealthServer", "Health server started on port " + PORT);
            } catch (Exception e) {
                Log.e("HealthServer", "Failed to start health server", e);
            }
        }
    }

    public static synchronized void stopServer() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/health".equals(uri)) {
            JSONObject response = new JSONObject();
            try {
                response.put("status", "ok");
                response.put("timestamp", System.currentTimeMillis());
                // 可扩展更多健康信息，如内存、网络状态
                response.put("uptime", android.os.SystemClock.elapsedRealtime());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", response.toString());
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }
}