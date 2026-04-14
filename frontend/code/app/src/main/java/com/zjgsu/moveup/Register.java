package com.zjgsu.moveup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class Register extends AppCompatActivity {

    private EditText etPhone;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPwd;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // 1. 绑定视图
        etPhone = findViewById(R.id.et_phone1);
        etUsername = findViewById(R.id.et_username1);
        etPassword = findViewById(R.id.et_password1);
        etConfirmPwd = findViewById(R.id.et_confirm_pwd1);
        Button registerBtn = findViewById(R.id.btn_register_confirm1);

        // 2. 设置按钮点击事件
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = etPhone.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPwd = etConfirmPwd.getText().toString().trim();

                // 基础校验
                if (phone.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPwd.isEmpty()) {
                    Toast.makeText(Register.this, "请填写所有注册信息", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPwd)) {
                    Toast.makeText(Register.this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 发起网络请求
                doRegisterRequest(phone, username, password);
            }
        });
    }

    /**
     * 调用后端注册接口
     */
    private void doRegisterRequest(final String phone, final String username, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("http://10.0.2.2:3000/v1/auth/register");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    // 构造发送给后端的 JSON
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("phone", phone);
                    jsonBody.put("username", username);
                    jsonBody.put("password", password);

                    OutputStream os = connection.getOutputStream();
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int httpCode = connection.getResponseCode();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            httpCode >= 200 && httpCode < 300
                                    ? connection.getInputStream()
                                    : connection.getErrorStream()
                    ));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    boolean tempSuccess = false;
                    String message = "注册失败，请稍后重试";

                    if (httpCode == 200) {
                        try {
                            JSONObject resp = new JSONObject(sb.toString());
                            int apiCode = resp.optInt("code", -1);
                            message = resp.optString("message", message);

                            if (apiCode == 200) {
                                tempSuccess = true;
                            }
                        } catch (Exception e) {
                            Log.e("API_TEST", "JSON解析错误: " + e.getMessage());
                        }
                    } else {
                        Log.e("API_TEST", "请求失败，网络状态码: " + httpCode);
                    }

                    final boolean success = tempSuccess;
                    final String toastMsg = message;

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Register.this, toastMsg, Toast.LENGTH_SHORT).show();
                            if (success) {
                                // 注册成功后，关闭当前页面回到登录页
                                finish();
                            }
                        }
                    });

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Register.this, "网络连接异常，请检查网络", Toast.LENGTH_SHORT).show();
                            Log.e("API_TEST", "网络异常", e);
                        }
                    });
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}