package com.zjgsu.moveup;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Edit extends AppCompatActivity {

    // 声明输入框控件（可选，用于读取内容）
    private EditText etUsername, etEmail, etPhone, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化输入框（可选）
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 更换头像按钮（ID已对齐）
        findViewById(R.id.btnChangeAvatar).setOnClickListener(v -> {
            Toast.makeText(this, "Change Avatar", Toast.LENGTH_SHORT).show();
        });

        // 保存按钮（ID已对齐）
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            // 可选：读取输入框内容
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // 简单校验（可选）
            if (username.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Username/Email cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}