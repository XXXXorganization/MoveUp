package com.zjgsu.moveup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class Main extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 抽屉菜单
        drawerLayout = findViewById(R.id.drawerLayout);
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            drawerLayout.openDrawer(findViewById(R.id.drawerMenu));
        });

        // 菜单点击
        setupMenuClicks();

        // 跑步跳转
        ImageView ivStart = findViewById(R.id.ivStart);
        ivStart.setOnClickListener(v -> {
            Intent intent = new Intent(Main.this, Runing.class);
            startActivity(intent);
        });

        // 俱乐部列表
        RecyclerView clubList = findViewById(R.id.recyclerClubs);
        clubList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        clubList.setAdapter(new ClubAdapter(buildSampleClubs()));
    }

    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        // Home → 主页（关闭菜单即可）
        menuHome.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
        });

        // History
        menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, History.class));
            finish();
        });

        // Plan
        menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, Plan.class));
            finish();
        });

        // Club
        menuClub.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, clubterm.class));
            finish();
        });

        // Profile → Mine
        menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, Mine.class));
            finish();
        });
    }

    @NonNull
    private List<Club> buildSampleClubs() {
        return Arrays.asList(
                new Club(
                        getString(R.string.club_tangerang),
                        getString(R.string.club_tangerang_loc),
                        R.drawable.bg_club_card_1),
                new Club(
                        getString(R.string.club_jakarta),
                        getString(R.string.club_jakarta_loc),
                        R.drawable.bg_club_card_2),
                new Club(
                        getString(R.string.club_bandung),
                        getString(R.string.club_bandung_loc),
                        R.drawable.bg_club_card_3));
    }
}