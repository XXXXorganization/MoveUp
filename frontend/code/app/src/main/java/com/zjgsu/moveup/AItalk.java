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

    private static final String BACKEND_URL = "http://10.0.2.2:3000/v1/ai/chat";

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
            try {
                // 纯文本格式直接传
                JSONObject userMsgObj = new JSONObject();
                userMsgObj.put("role", "user");
                userMsgObj.put("content", userText);
                chatHistory.put(userMsgObj);

                JSONObject requestBody = new JSONObject();
                requestBody.put("user_id", currentUserId);
                requestBody.put("chat_history", chatHistory);

                URL url = new URL(BACKEND_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000); // 纯文字秒连
                connection.setReadTimeout(15000);

                byte[] bytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(bytes);
                }

                int code = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                String responseStr = sb.toString();

                mainHandler.post(() -> {
                    chatContainer.removeView(loadingTv);

                    if (code == 200) {
                        try {
                            JSONObject respJson = new JSONObject(responseStr);
                            if (respJson.optInt("code") == 200) {
                                String aiReply = respJson.getJSONObject("data").getString("reply");

                                JSONObject modelMsgObj = new JSONObject();
                                modelMsgObj.put("role", "assistant");
                                modelMsgObj.put("content", aiReply);
                                chatHistory.put(modelMsgObj);

                                addMessageBubble("AI", aiReply);
                            } else {
                                addMessageBubble("系统", "后端报错: " + respJson.optString("message"));
                                chatHistory.remove(chatHistory.length() - 1);
                            }
                        } catch (Exception e) {
                            addMessageBubble("系统", "解析后端数据失败: " + e.getMessage());
                        }
                    } else {
                        Log.e("AItalk", "Backend failed: " + responseStr);
                        addMessageBubble("系统", "连接后端失败 (HTTP " + code + ")");
                        chatHistory.remove(chatHistory.length() - 1);
                    }
                });

            } catch (Exception e) {
                Log.e("AItalk", "Network Error", e);
                mainHandler.post(() -> {
                    chatContainer.removeView(loadingTv);
                    addMessageBubble("系统", "网络连接异常: \n" + e.getMessage());
                });
                chatHistory.remove(chatHistory.length() - 1);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}