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

    // 🌟 新增这一行：设为 public static 方便测试代码动态修改
    public static String BASE_URL = "http://10.234.4.72:3500";

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

        // 绑定视图
        etUsername = findViewById(R.id.et_edit_username);
        etEmail = findViewById(R.id.et_edit_email);
        etPhone = findViewById(R.id.et_edit_phone);
        etPassword = findViewById(R.id.et_edit_password);
        Button btnUpdate = findViewById(R.id.btnUpdateProfile);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // 获取当前账号ID (手机号)
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        // 返回按钮逻辑
        btnBack.setOnClickListener(v -> finish());

        // 获取并填充数据
        fetchProfileData();

        // 点击更新按钮
        btnUpdate.setOnClickListener(v -> updateProfileData());
    }

    /**
     * 发送 GET 请求获取当前用户的资料
     */
    private void fetchProfileData() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 修改点 1：使用 BASE_URL 拼接请求地址
                URL url = new URL(BASE_URL + "/v1/user/profile?user_id=" + currentUserId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        JSONObject data = resp.getJSONObject("data");
                        final String username = data.optString("username", "");
                        final String email = data.optString("email", "");
                        final String phone = data.optString("phone", "");
                        final String password = data.optString("password", "");

                        // 切回主线程更新 UI
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
                mainHandler.post(() -> Toast.makeText(mine_edit.this, "无法拉取个人资料", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * 发送 PUT 请求更新用户资料
     */
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
            try {
                // 🌟 修改点 2：使用 BASE_URL 拼接请求地址
                URL url = new URL(BASE_URL + "/v1/user/profile");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                // 构造更新数据
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("user_id", currentUserId); // 告诉后端更新哪个用户
                jsonBody.put("username", newUsername);
                jsonBody.put("email", newEmail);
                jsonBody.put("password", newPassword);

                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                if (connection.getResponseCode() == 200) {
                    mainHandler.post(() -> {
                        // Toast 打印出上传的具体数据
                        String toastMsg = String.format("资料更新成功！\n新昵称: %s\n新邮箱: %s\n新密码: %s",
                                newUsername, newEmail, newPassword);
                        Toast.makeText(mine_edit.this, toastMsg, Toast.LENGTH_LONG).show();

                        finish(); // 更新成功后自动关闭页面，返回上一页
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(mine_edit.this, "更新失败，请重试", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e("API_TEST", "更新资料异常", e);
                mainHandler.post(() -> Toast.makeText(mine_edit.this, "网络连接异常", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}