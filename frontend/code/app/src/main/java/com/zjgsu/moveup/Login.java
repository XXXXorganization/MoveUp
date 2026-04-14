package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class Login extends AppCompatActivity {

    private EditText etPhone; // 改为电话号码输入框
    private EditText etPassword;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // 绑定新的 UI ID
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnGoRegister = findViewById(R.id.btn_go_register);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取手机号和密码
                String phone = etPhone.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (phone.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Login.this, "手机号和密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                doLoginRequest(phone, password);
            }
        });

        btnGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 调用后端登录接口
     */
    private void doLoginRequest(final String phone, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("http://10.0.2.2:3000/v1/auth/login");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    // 构造发送给后端的 JSON
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("phone", phone);
                    jsonBody.put("code", password);

                    Log.d("API_TEST", "=== 前端准备发送数据 ===");
                    Log.d("API_TEST", "请求Body: " + jsonBody.toString());

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
                    String message = "登录失败，请稍后重试";
                    String parsedData = "";

                    if (httpCode == 200) {
                        try {
                            String responseString = sb.toString();
                            Log.d("API_TEST", "完整返回: " + responseString);

                            JSONObject resp = new JSONObject(responseString);
                            // 提取后端的业务码 (200=成功, 404=无账号, 401=密码错等)
                            int apiCode = resp.optInt("code", -1);
                            // 提取后端自定义的报错信息用于 Toast
                            message = resp.optString("message", message);

                            if (apiCode == 200) {
                                tempSuccess = true;
                                if (resp.has("data")) {
                                    JSONObject dataObj = resp.getJSONObject("data");
                                    parsedData = dataObj.toString();
                                    String token = dataObj.optString("token", "");
                                    if (!token.isEmpty()) {
                                        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                                        prefs.edit().putString("jwt", token).putString("user_phone", phone).apply();
                                    }
                                }
                            } else {
                                tempSuccess = false;
                                Log.w("API_TEST", "业务拦截: " + message);
                            }
                        } catch (Exception e) {
                            tempSuccess = false;
                            message = "服务器数据解析异常";
                            Log.e("API_TEST", "JSON解析错误: " + e.getMessage());
                        }
                    } else {
                        message = "请求失败，网络状态码: " + httpCode;
                        Log.e("API_TEST", message);
                    }

                    final boolean success = tempSuccess;
                    final String toastMsg = message;
                    final String finalData = parsedData;

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(Login.this, "登录成功", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Login.this, Main.class);
                                intent.putExtra("USER_DATA_JSON", finalData);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(Login.this, toastMsg, Toast.LENGTH_SHORT).show();

                                if (toastMsg.contains("密码错误")) {
                                    etPassword.setText("");
                                    etPassword.requestFocus();
                                }
                            }
                        }
                    });

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Login.this, "网络连接异常，请检查网络", Toast.LENGTH_SHORT).show();
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