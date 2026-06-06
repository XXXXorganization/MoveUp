package com.zjgsu.moveup;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StructuredLogger {
    private static Context appContext;
    private static final String LOG_FILE = "app_log.json";
    private static final Object lock = new Object();

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void log(String level, String module, String message) {
        if (appContext == null) return;
        JSONObject logEntry = new JSONObject();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
            logEntry.put("time", sdf.format(new Date()));
            logEntry.put("level", level);
            logEntry.put("module", module);
            logEntry.put("message", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String json = logEntry.toString();
        Log.d("StructuredLog", json);
        File logFile = new File(appContext.getFilesDir(), LOG_FILE);
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(logFile, true)) {
                fw.write(json + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}