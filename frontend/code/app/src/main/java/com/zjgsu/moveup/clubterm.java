package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class clubterm extends AppCompatActivity {

    // 🌟 新增：暴露 BASE_URL 供测试修改
    public static String BASE_URL = "http://10.234.4.72:3500";

    private String clubId;
    private String currentUserId;
    private boolean isJoined = false;

    private ImageView clubHeroImage; // 🌟 绑定顶部大图
    private TextView tvClubName;
    private TextView tvClubLocation;
    private MaterialButton btnJoin;

    // DrawerLayout 引用
    private DrawerLayout drawerLayout;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clubterm);

        // 初始化 DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // 🌟 绑定视图
        clubHeroImage = findViewById(R.id.clubHeroImage);
        tvClubName = findViewById(R.id.tvClubName);
        tvClubLocation = findViewById(R.id.tvClubLocation);
        btnJoin = findViewById(R.id.btnJoin);

        clubId = getIntent().getStringExtra("CLUB_ID");
        if (clubId == null) clubId = "c1";

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        fetchClubDetails();
        btnJoin.setOnClickListener(v -> showConfirmDialog());

        // 为按钮添加展开侧滑菜单的点击事件
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (drawerLayout != null) {
                // 使用 GravityCompat.START 从屏幕左侧划出菜单
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // ================= 绑定侧滑菜单的点击跳转逻辑 =================
        setupMenuClicks();
        // =============================================================

        RecyclerView recycler = findViewById(R.id.recyclerPosts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        fetchClubPosts();
    }

    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        // Home → 跳转回主页
        if (menuHome != null) menuHome.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Main.class));
            finish();
        });

        // History
        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, History.class));
            finish();
        });

        // Plan
        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Plan.class));
            finish();
        });

        // Club → 当前已在 Club 页面，直接关闭侧滑菜单即可
        if (menuClub != null) menuClub.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.closeDrawers();
            }
        });

        // Profile → Mine
        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Mine.class));
            finish();
        });
    }

    private void fetchClubPosts() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/v1/clubs/" + clubId + "/posts?user_id=" + currentUserId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONArray list = res.getJSONObject("data").getJSONArray("list");

                    List<ClubTermPost> postList = new ArrayList<>();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject p = list.getJSONObject(i);

                        // 解析 comments 里的 reply_to_name
                        JSONArray cmts = p.optJSONArray("comments");
                        List<ClubComment> cList = new ArrayList<>();
                        if(cmts != null) {
                            for(int j=0; j<cmts.length(); j++){
                                JSONObject c = cmts.getJSONObject(j);
                                cList.add(new ClubComment(
                                        c.getString("id"),
                                        c.getString("author"),
                                        c.getString("content"),
                                        c.getString("time"),
                                        c.optString("reply_to_id", null),
                                        c.optString("reply_to_name", null) // 提取被回复人名字
                                ));
                            }
                        }

                        postList.add(new ClubTermPost(
                                p.getString("id"),
                                p.getString("author"),
                                p.getString("timeText"),
                                p.getString("lateTitle"),
                                R.drawable.term1,
                                p.getString("postBadgeText"),
                                p.getString("subLine"),
                                p.getString("subDetail"),
                                R.drawable.ic_avatar_placeholder,
                                p.getBoolean("is_liked"),
                                p.getInt("like_count"),
                                p.getInt("total_comments"),
                                cList
                        ));
                    }

                    mainHandler.post(() -> {
                        RecyclerView recycler = findViewById(R.id.recyclerPosts);
                        recycler.setAdapter(new ClubTermPostAdapter(postList, currentUserId));
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Fetch posts error", e);
            }
        }).start();
    }

    private void showConfirmDialog() {
        String title = isJoined ? "Exit Club" : "Join Club";
        String message = isJoined ? "Are you sure you want to exit this club?" : "Are you sure you want to join this club?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> toggleJoinStatus())
                .setNegativeButton("No", null)
                .show();
    }

    private void fetchClubDetails() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/v1/clubs/" + clubId + "?user_id=" + currentUserId);
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
                        final String name = data.optString("name");
                        final String location = data.optString("location");
                        final boolean joined = data.optBoolean("is_joined", false);
                        // 🌟 解析获取后端返回的图片 URL
                        final String imageUrl = data.optString("image_url", "");

                        mainHandler.post(() -> {
                            tvClubName.setText(name);
                            tvClubLocation.setText(location);
                            updateButtonUI(joined);

                            // 🌟 使用 Glide 加载图片到顶部的 ImageView
                            if (!imageUrl.isEmpty() && clubHeroImage != null) {
                                Glide.with(clubterm.this)
                                        .load(imageUrl)
                                        .centerCrop()
                                        .placeholder(R.drawable.term1) // XML中默认的占位图
                                        .error(R.drawable.term1)       // 加载失败时使用默认图
                                        .into(clubHeroImage);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取社团详情失败", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void toggleJoinStatus() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/v1/clubs/" + clubId + "/toggle");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject body = new JSONObject();
                body.put("user_id", currentUserId);
                body.put("timestamp", System.currentTimeMillis());

                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        JSONObject data = resp.getJSONObject("data");
                        final boolean currentStatus = data.optBoolean("is_joined");

                        mainHandler.post(() -> {
                            updateButtonUI(currentStatus);
                            Toast.makeText(this, currentStatus ? "Successfully Joined!" : "Successfully Exited!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void updateButtonUI(boolean status) {
        isJoined = status;
        if (isJoined) {
            btnJoin.setText("Exit");
            btnJoin.setBackgroundColor(Color.parseColor("#FF6B6B"));
            btnJoin.setTextColor(Color.WHITE);
        } else {
            btnJoin.setText("Join");
            btnJoin.setBackgroundColor(Color.parseColor("#C7FB58"));
            btnJoin.setTextColor(Color.parseColor("#1E1F22"));
        }
    }
}