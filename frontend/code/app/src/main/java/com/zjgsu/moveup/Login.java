package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

    // 🌟 新增这一行，设为 public static 方便测试代码修改 [cite: 22]
    public static String BASE_URL = "http://10.234.4.72:3500";

    private EditText etPhone;
    private EditText etPassword;
    private ImageView ivEyeLogin;
    private Handler mainHandler;

    private boolean isPasswordVisible = false;

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

        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        ivEyeLogin = findViewById(R.id.iv_eye_login);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnGoRegister = findViewById(R.id.btn_go_register);
        View btnBack = findViewById(R.id.btn_back);

        // 🌟 1. 读取本地历史保存的账户和密码 (记住密码) [cite: 22]
        SharedPreferences localPrefs = getSharedPreferences("Local_History", MODE_PRIVATE);
        String savedPhone = localPrefs.getString("saved_phone", "");
        String savedPwd = localPrefs.getString("saved_password", "");
        if (!savedPhone.isEmpty()) {
            etPhone.setText(savedPhone);
            etPassword.setText(savedPwd);
        }

        // 🌟 2. 密码可视/隐藏 切换逻辑 [cite: 22]
        ivEyeLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    // 密码可见，眼睛图标变亮变绿
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivEyeLogin.setColorFilter(Color.parseColor("#C7FB58"));
                } else {
                    // 密码隐藏，眼睛图标恢复灰色
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivEyeLogin.setColorFilter(Color.parseColor("#888888"));
                }
                // 将光标移至末尾
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Login.this, Start.class);
                    startActivity(intent);
                    finish();
                }
            });
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                finish();
            }
        });
    }

    private void doLoginRequest(final String phone, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    // 🌟 修改点：将写死的地址改为使用 BASE_URL 拼接
                    URL url = new URL(BASE_URL + "/v1/auth/login");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("phone", phone);
                    jsonBody.put("code", password);

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
                            JSONObject resp = new JSONObject(responseString);
                            int apiCode = resp.optInt("code", -1);
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
                            }
                        } catch (Exception e) {
                            Log.e("API_TEST", "JSON解析错误: " + e.getMessage());
                        }
                    }

                    final boolean success = tempSuccess;
                    final String toastMsg = message;
                    final String finalData = parsedData;

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // 🌟 3. 登录成功时，将账号和密码写入本地历史记录 [cite: 22]
                                SharedPreferences localPrefs = getSharedPreferences("Local_History", MODE_PRIVATE);
                                localPrefs.edit()
                                        .putString("saved_phone", phone)
                                        .putString("saved_password", password)
                                        .apply();

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
                        }
                    });
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        }).start();
    }
}