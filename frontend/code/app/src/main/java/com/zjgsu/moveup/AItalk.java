package com.zjgsu.moveup;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AItalk extends AppCompatActivity {

    public static String BASE_URL = "http://192.168.25.47:3000/v1/ai/chat";

    private LinearLayout chatContainer;
    private ScrollView chatScrollView;
    private EditText etInput;
    private Button btnSend;

    private JSONArray chatHistory = new JSONArray();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aitalk);

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        if (getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.75);
            getWindow().setLayout(width, height);
        }

        chatContainer = findViewById(R.id.chatContainer);
        chatScrollView = findViewById(R.id.chatScrollView);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);

        addMessageBubble("AI", "你好！我是 MoveUp 智能跑步助理。直接发文字告诉我你想怎么练，我不仅能给你建议，还能自动帮你把计划安排进日历！");

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (text.isEmpty()) return;

            addMessageBubble("User", text);
            etInput.setText("");

            callMyBackendAPI(text);
        });
    }

    private void addMessageBubble(String sender, String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(15f);
        tv.setTextColor(Color.parseColor("#1A1A1A"));
        tv.setPadding(40, 25, 40, 25);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 12, 0, 12);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(30f);

        if (sender.equals("User")) {
            bg.setColor(Color.parseColor("#C7FB58"));
            params.gravity = Gravity.END;
            params.setMarginStart(100);
        } else if (sender.equals("系统")) {
            bg.setColor(Color.parseColor("#FFCDD2"));
            params.gravity = Gravity.CENTER;
        } else {
            bg.setColor(Color.parseColor("#FFFFFF"));
            params.gravity = Gravity.START;
            params.setMarginEnd(100);
        }

        tv.setBackground(bg);
        tv.setLayoutParams(params);
        chatContainer.addView(tv);
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void callMyBackendAPI(String userText) {
        TextView loadingTv = new TextView(this);
        loadingTv.setText("AI 教练正在排查数据和定制计划...");
        loadingTv.setPadding(20, 10, 20, 10);
        loadingTv.setTextColor(Color.GRAY);
        chatContainer.addView(loadingTv);
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL;   // 完整 URL
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                JSONObject userMsgObj = new JSONObject();
                userMsgObj.put("role", "user");
                userMsgObj.put("content", userText);
                chatHistory.put(userMsgObj);

                JSONObject requestBody = new JSONObject();
                requestBody.put("chat_history", chatHistory);

                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                byte[] bytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(bytes);
                }

                int httpCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpCode >= 200 && httpCode < 300 ? connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                String responseStr = sb.toString();

                if (httpCode == 200) {
                    try {
                        JSONObject respJson = new JSONObject(responseStr);
                        if (respJson.optInt("code") == 200) {
                            success = true;
                            String aiReply = respJson.getJSONObject("data").getString("reply");

                            JSONObject modelMsgObj = new JSONObject();
                            modelMsgObj.put("role", "assistant");
                            modelMsgObj.put("content", aiReply);
                            chatHistory.put(modelMsgObj);

                            final String finalReply = aiReply;
                            mainHandler.post(() -> addMessageBubble("AI", finalReply));
                        } else {
                            mainHandler.post(() -> addMessageBubble("系统", "后端报错: " + respJson.optString("message")));
                            chatHistory.remove(chatHistory.length() - 1);
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> addMessageBubble("系统", "解析后端数据失败: " + e.getMessage()));
                    }
                } else {
                    Log.e("AItalk", "Backend failed: " + responseStr);
                    mainHandler.post(() -> addMessageBubble("系统", "连接后端失败 (HTTP " + httpCode + ")"));
                    chatHistory.remove(chatHistory.length() - 1);
                }

            } catch (Exception e) {
                Log.e("AItalk", "Network Error", e);
                success = false;
                mainHandler.post(() -> {
                    chatContainer.removeView(loadingTv);
                    addMessageBubble("系统", "网络连接异常: \n" + e.getMessage());
                });
                chatHistory.remove(chatHistory.length() - 1);
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) {
                    connection.disconnect();
                }
                // 无论成功与否，移除加载提示（仅当尚未被移除时）
                mainHandler.post(() -> {
                    if (loadingTv.getParent() != null) {
                        chatContainer.removeView(loadingTv);
                    }
                });
            }
        }).start();
    }
}