package com.zjgsu.moveup;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class mine_edit extends AppCompatActivity {

    public static String BASE_URL = "https://moveup-v7mf.onrender.com";

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPassword;
    private Handler mainHandler;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mine_edit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        etUsername = findViewById(R.id.et_edit_username);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        etPassword = findViewById(R.id.et_edit_password);
        Button btnUpdate = findViewById(R.id.btnUpdateProfile);
        ImageButton btnBack = findViewById(R.id.btnBack);

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        btnBack.setOnClickListener(v -> finish());

        fetchProfileData();

        btnUpdate.setOnClickListener(v -> updateProfileData());
    }

    // ------------------- fetchProfileData -------------------
    private void fetchProfileData() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/user/profile";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = connection.getResponseCode();
                if (httpCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        success = true;
                        JSONObject data = resp.getJSONObject("data");
                        final String username = data.optString("username", "");
                        final String email = data.optString("email", "");
                        final String phone = data.optString("phone", "");
                        final String password = data.optString("password", "");

                        mainHandler.post(() -> {
                            etUsername.setText(username);
                            etEmail.setText(email);
                            etPhone.setText(phone);
                            etPassword.setText(password);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取个人资料异常", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(mine_edit.this, "无法拉取个人资料", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // ------------------- updateProfileData -------------------
    private void updateProfileData() {
        final String newUsername = etUsername.getText().toString().trim();
        final String newEmail = etEmail.getText().toString().trim();
        final String newPassword = etPassword.getText().toString().trim();

        if (newUsername.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/user/profile";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("username", newUsername);
                jsonBody.put("email", newEmail);
                jsonBody.put("password", newPassword);

                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int httpCode = connection.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    mainHandler.post(() -> {
                        String toastMsg = String.format("资料更新成功！\n新昵称: %s\n新邮箱: %s\n新密码: %s",
                                newUsername, newEmail, newPassword);
                        Toast.makeText(mine_edit.this, toastMsg, Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(mine_edit.this, "更新失败，请重试", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("API_TEST", "更新资料异常", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(mine_edit.this, "网络连接异常", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}