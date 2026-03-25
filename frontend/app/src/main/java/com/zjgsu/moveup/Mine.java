package com.zjgsu.moveup;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Mine extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mine);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBack)
                .setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        findViewById(R.id.btnEditProfile)
                .setOnClickListener(v ->
                        Toast.makeText(this, "Edit Profile", Toast.LENGTH_SHORT).show());

        findViewById(R.id.rowLanguage)
                .setOnClickListener(v ->
                        Toast.makeText(this, "Language", Toast.LENGTH_SHORT).show());

        findViewById(R.id.rowDarkMode)
                .setOnClickListener(v ->
                        Toast.makeText(this, "Darkmode", Toast.LENGTH_SHORT).show());
    }
}