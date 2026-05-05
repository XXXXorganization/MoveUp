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

    // 🌟 新增这一行，设为 public static 方便测试代码动态修改
    public static String BASE_URL = "http://10.0.2.2:3000";

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

        // 🌟 1. 设置主密码可视/隐藏逻辑
        ivEyePwd1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPwd1Visible = !isPwd1Visible;
                if (isPwd1Visible) {
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivEyePwd1.setColorFilter(Color.parseColor("#C7FB58"));
                } else {
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivEyePwd1.setColorFilter(Color.parseColor("#888888"));
                }
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        // 🌟 2. 设置确认密码可视/隐藏逻辑
        ivEyeConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isConfirmPwdVisible = !isConfirmPwdVisible;
                if (isConfirmPwdVisible) {
                    etConfirmPwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivEyeConfirm.setColorFilter(Color.parseColor("#C7FB58"));
                } else {
                    etConfirmPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivEyeConfirm.setColorFilter(Color.parseColor("#888888"));
                }
                etConfirmPwd.setSelection(etConfirmPwd.getText().length());
            }
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Register.this, Start.class);
                    startActivity(intent);
                    finish();
                }
            });
        }

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }

    private void doRegisterRequest(final String phone, final String username, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    // 🌟 修改点：将写死的地址改为使用 BASE_URL 拼接
                    URL url = new URL(BASE_URL + "/v1/auth/register");
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
                    }

                    final boolean success = tempSuccess;
                    final String toastMsg = message;

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Register.this, toastMsg, Toast.LENGTH_SHORT).show();
                            if (success) {
                                // 🌟 3. 注册成功时，直接把刚才填的账号密码写进本地配置中
                                // 这样跳转回 Login 页面时就自动填好了
                                SharedPreferences localPrefs = getSharedPreferences("Local_History", MODE_PRIVATE);
                                localPrefs.edit()
                                        .putString("saved_phone", phone)
                                        .putString("saved_password", password)
                                        .apply();

                                Intent intent = new Intent(Register.this, Login.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });

                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Register.this, "网络连接异常，请检查网络", Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        }).start();
    }
}