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

    public static String BASE_URL = "http://192.168.25.47:3000";

    private String clubId;
    private String currentUserId;
    private boolean isJoined = false;

    private ImageView clubHeroImage;
    private TextView tvClubName;
    private TextView tvClubLocation;
    private MaterialButton btnJoin;

    private DrawerLayout drawerLayout;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clubterm);

        drawerLayout = findViewById(R.id.drawerLayout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        clubHeroImage = findViewById(R.id.clubHeroImage);
        tvClubName = findViewById(R.id.tvClubName);
        tvClubLocation = findViewById(R.id.tvClubLocation);
        btnJoin = findViewById(R.id.btnJoin);

        clubId = getIntent().getStringExtra("CLUB_ID");
        if (clubId == null) clubId = "c1";

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        fetchClubDetails();
        btnJoin.setOnClickListener(v -> {
            if (isJoined) {
                Intent intent = new Intent(clubterm.this, ClubCommunityActivity.class);
                intent.putExtra("CLUB_ID", clubId);
                startActivity(intent);
            } else {
                showConfirmDialog();
            }
        });

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        setupMenuClicks();

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

        if (menuHome != null) menuHome.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Main.class));
            finish();
        });
        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, History.class));
            finish();
        });
        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Plan.class));
            finish();
        });
        if (menuClub != null) menuClub.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Find.class));
            finish();
        });
        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(clubterm.this, Mine.class));
            finish();
        });
    }

    // ------------------- fetchClubPosts -------------------
    private void fetchClubPosts() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/clubs/" + clubId + "/posts";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONArray list = res.getJSONObject("data").getJSONArray("list");

                    List<ClubTermPost> postList = new ArrayList<>();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject p = list.getJSONObject(i);
                        JSONArray cmts = p.optJSONArray("comments");
                        List<ClubComment> cList = new ArrayList<>();
                        if (cmts != null) {
                            for (int j = 0; j < cmts.length(); j++) {
                                JSONObject c = cmts.getJSONObject(j);
                                String commentAuthor = "Unknown";
                                JSONObject commentAuthorObj = c.optJSONObject("author");
                                if (commentAuthorObj != null) {
                                    commentAuthor = commentAuthorObj.optString("nickname", "Unknown");
                                }
                                cList.add(new ClubComment(
                                        c.optString("id", ""),
                                        commentAuthor,
                                        c.optString("content", ""),
                                        c.optString("created_at", ""),
                                        c.optString("reply_to_id", null),
                                        ""
                                ));
                            }
                        }

                        String postAuthor = "Unknown";
                        JSONObject authorObj = p.optJSONObject("author");
                        if (authorObj != null) {
                            postAuthor = authorObj.optString("nickname", "Unknown");
                        }
                        int commentsCount = cmts != null ? cmts.length() : 0;

                        List<String> imageList = null;
                        JSONArray imgArr = p.optJSONArray("images");
                        if (imgArr != null && imgArr.length() > 0) {
                            imageList = new ArrayList<>();
                            for (int k = 0; k < imgArr.length(); k++) {
                                imageList.add(imgArr.optString(k, ""));
                            }
                        }

                        String runDist = "", runDur = "", runPace = "";
                        boolean hasRun = false;
                        JSONObject runSum = p.optJSONObject("run_summary");
                        if (runSum != null) {
                            double dist = runSum.optDouble("distance", 0);
                            int dur = runSum.optInt("duration", 0);
                            String pace = runSum.optString("pace", "0'00\"");
                            if (dist > 0 || dur > 0) {
                                hasRun = true;
                                runDist = String.format("%.2f Km", dist);
                                int mins = dur / 60;
                                int secs = dur % 60;
                                runDur = String.format("%d'%02d\"", mins, secs);
                                runPace = pace;
                            }
                        }

                        ClubTermPost ctp = new ClubTermPost(
                                p.optString("id", ""),
                                postAuthor,
                                p.optString("created_at", ""),
                                p.optString("content", "No title"),
                                R.drawable.term1,
                                "",
                                hasRun ? runDist : "",
                                hasRun ? (runDur + " • " + runPace) : "",
                                R.drawable.ic_avatar_placeholder,
                                p.optBoolean("is_liked", false),
                                p.optInt("like_count", 0),
                                commentsCount,
                                cList,
                                imageList
                        );
                        ctp.hasRunData = hasRun;
                        ctp.runDistance = runDist;
                        ctp.runDuration = runDur;
                        ctp.runPace = runPace;
                        postList.add(ctp);
                    }

                    final List<ClubTermPost> finalPostList = postList;
                    mainHandler.post(() -> {
                        RecyclerView recycler = findViewById(R.id.recyclerPosts);
                        recycler.setAdapter(new ClubTermPostAdapter(finalPostList, currentUserId));
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Fetch posts error", e);
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
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

    // ------------------- fetchClubDetails -------------------
    private void fetchClubDetails() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/clubs/" + clubId;
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = connection.getResponseCode();
                if (httpCode == 200) {
                    success = true;
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
                        final boolean joined = data.optBoolean("is_member", false);
                        final String imageUrl = data.optString("image_url", "");

                        mainHandler.post(() -> {
                            tvClubName.setText(name);
                            tvClubLocation.setText(location);
                            updateButtonUI(joined);
                            if (!imageUrl.isEmpty() && clubHeroImage != null) {
                                Glide.with(clubterm.this)
                                        .load(imageUrl)
                                        .centerCrop()
                                        .placeholder(R.drawable.term1)
                                        .error(R.drawable.term1)
                                        .into(clubHeroImage);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取社团详情失败", e);
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // ------------------- toggleJoinStatus -------------------
    private void toggleJoinStatus() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/clubs/" + clubId + "/toggle";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                JSONObject body = new JSONObject();
                body.put("timestamp", System.currentTimeMillis());

                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int httpCode = connection.getResponseCode();
                if (httpCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        JSONObject data = resp.getJSONObject("data");
                        final boolean currentStatus = data.optBoolean("joined");
                        success = true;

                        mainHandler.post(() -> {
                            updateButtonUI(currentStatus);
                            if (currentStatus) {
                                Intent intent = new Intent(clubterm.this, ClubCommunityActivity.class);
                                intent.putExtra("CLUB_ID", clubId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(clubterm.this, "Successfully Exited!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Toggle join error", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(clubterm.this, "Network Error", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void updateButtonUI(boolean status) {
        isJoined = status;
        if (isJoined) {
            btnJoin.setText("Enter Community");
            btnJoin.setBackgroundColor(Color.parseColor("#C7FB58"));
            btnJoin.setTextColor(Color.parseColor("#1E1F22"));
        } else {
            btnJoin.setText("Join");
            btnJoin.setBackgroundColor(Color.parseColor("#C7FB58"));
            btnJoin.setTextColor(Color.parseColor("#1E1F22"));
        }
    }
}