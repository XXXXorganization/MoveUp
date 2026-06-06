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

public class Register extends AppCompatActivity {

    public static String BASE_URL = "https://moveup-v7mf.onrender.com";

    private EditText etPhone;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPwd;
    private Handler mainHandler;

    private boolean isPwd1Visible = false;
    private boolean isConfirmPwdVisible = false;

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

        etPhone = findViewById(R.id.et_phone1);
        etUsername = findViewById(R.id.et_username1);
        etPassword = findViewById(R.id.et_password1);
        etConfirmPwd = findViewById(R.id.et_confirm_pwd1);
        Button registerBtn = findViewById(R.id.btn_register_confirm1);
        View btnBack = findViewById(R.id.btn_back);

        ImageView ivEyePwd1 = findViewById(R.id.iv_eye_pwd1);
        ImageView ivEyeConfirm = findViewById(R.id.iv_eye_confirm_pwd1);

        ivEyePwd1.setOnClickListener(v -> {
            isPwd1Visible = !isPwd1Visible;
            if (isPwd1Visible) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivEyePwd1.setColorFilter(Color.parseColor("#C7FB58"));
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivEyePwd1.setColorFilter(Color.parseColor("#888888"));
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        ivEyeConfirm.setOnClickListener(v -> {
            isConfirmPwdVisible = !isConfirmPwdVisible;
            if (isConfirmPwdVisible) {
                etConfirmPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivEyeConfirm.setColorFilter(Color.parseColor("#C7FB58"));
            } else {
                etConfirmPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivEyeConfirm.setColorFilter(Color.parseColor("#888888"));
            }
            etConfirmPwd.setSelection(etConfirmPwd.getText().length());
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(Register.this, Start.class);
                startActivity(intent);
                finish();
            });
        }

        registerBtn.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPwd = etConfirmPwd.getText().toString().trim();

            if (phone.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPwd.isEmpty()) {
                Toast.makeText(Register.this, "请填写所有注册信息", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPwd)) {
                Toast.makeText(Register.this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            doRegisterRequest(phone, username, password);
        });
    }

    private void doRegisterRequest(final String phone, final String username, final String password) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/auth/register";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

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

                String message = "注册失败，请稍后重试";

                if (httpCode == 200) {
                    try {
                        JSONObject resp = new JSONObject(sb.toString());
                        int apiCode = resp.optInt("code", -1);
                        message = resp.optString("message", message);

                        if (apiCode == 200) {
                            success = true;
                        }
                    } catch (Exception e) {
                        Log.e("API_TEST", "JSON解析错误: " + e.getMessage());
                    }
                }

                final boolean finalSuccess = success;
                final String toastMsg = message;

                mainHandler.post(() -> {
                    Toast.makeText(Register.this, toastMsg, Toast.LENGTH_SHORT).show();
                    if (finalSuccess) {
                        SharedPreferences localPrefs = getSharedPreferences("Local_History", MODE_PRIVATE);
                        localPrefs.edit()
                                .putString("saved_phone", phone)
                                .putString("saved_password", password)
                                .apply();

                        Intent intent = new Intent(Register.this, Login.class);
                        startActivity(intent);
                        finish();
                    }
                });

            } catch (Exception e) {
                success = false;
                mainHandler.post(() -> Toast.makeText(Register.this, "网络连接异常，请检查网络", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}