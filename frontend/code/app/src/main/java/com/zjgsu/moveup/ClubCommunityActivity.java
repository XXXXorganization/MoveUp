package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ClubCommunityActivity extends AppCompatActivity {

    public static String BASE_URL = "http://192.168.25.47:3000";

    private String clubId;
    private String currentUserId;
    private int memberCount = 0;

    private TextView tvClubName, tvClubLocation, tvMemberCount, tvCommunityTitle;
    private ImageView ivClubAvatar;
    private MaterialButton btnExit;
    private EditText etNewPost;
    private ImageButton btnSendPost, btnPickImage;
    private RecyclerView recyclerPosts;
    private ImageButton btnBack;

    private Handler mainHandler;
    private List<ClubTermPost> postList = new ArrayList<>();
    private ClubTermPostAdapter postAdapter;
    private List<String> pendingImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_club_community);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        clubId = getIntent().getStringExtra("CLUB_ID");
        if (clubId == null) clubId = "c1";

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        initViews();
        setupListeners();
        setupRecyclerView();

        fetchClubDetails();
        fetchPosts();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCommunityTitle = findViewById(R.id.tvCommunityTitle);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvClubName = findViewById(R.id.tvClubName);
        tvClubLocation = findViewById(R.id.tvClubLocation);
        ivClubAvatar = findViewById(R.id.ivClubAvatar);
        btnExit = findViewById(R.id.btnExit);
        etNewPost = findViewById(R.id.etNewPost);
        btnSendPost = findViewById(R.id.btnSendPost);
        btnPickImage = findViewById(R.id.btnPickImage);
        recyclerPosts = findViewById(R.id.recyclerPosts);
    }

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                        if (bitmap != null) {
                            int maxWidth = 800;
                            if (bitmap.getWidth() > maxWidth) {
                                int newHeight = bitmap.getHeight() * maxWidth / bitmap.getWidth();
                                bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true);
                            }
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                            pendingImages.add("data:image/jpeg;base64," + base64);
                            Toast.makeText(this, "Image added (" + (pendingImages.size()) + ")", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("IMAGE", "Failed to load image", e);
                    }
                }
            });

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnExit.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Exit Club")
                    .setMessage("Are you sure you want to exit this club?")
                    .setPositiveButton("Yes", (dialog, which) -> toggleExit())
                    .setNegativeButton("No", null)
                    .show();
        });

        btnPickImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnSendPost.setOnClickListener(v -> {
            String content = etNewPost.getText().toString().trim();
            if (content.isEmpty() && pendingImages.isEmpty()) {
                Toast.makeText(this, "Please write something or add an image", Toast.LENGTH_SHORT).show();
                return;
            }
            createPost(content);
        });
    }

    private void setupRecyclerView() {
        recyclerPosts.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new ClubTermPostAdapter(postList, currentUserId);
        recyclerPosts.setAdapter(postAdapter);
    }

    // ------------------- fetchClubDetails -------------------
    private void fetchClubDetails() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/clubs/" + clubId;
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        JSONObject data = resp.getJSONObject("data");
                        String name = data.optString("name", "Club");
                        String location = data.optString("location", "");
                        String imageUrl = data.optString("image_url", "");
                        memberCount = data.optInt("member_count", 0);
                        boolean isMember = data.optBoolean("is_member", true);

                        mainHandler.post(() -> {
                            tvClubName.setText(name);
                            tvCommunityTitle.setText(name);
                            tvClubLocation.setText(location);
                            tvMemberCount.setText(memberCount + " members");
                            if (!imageUrl.isEmpty()) {
                                Glide.with(ClubCommunityActivity.this).load(imageUrl).centerCrop().into(ivClubAvatar);
                            }
                            if (!isMember) {
                                Toast.makeText(ClubCommunityActivity.this, "You have left this club", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Fetch club details error", e);
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ------------------- fetchPosts -------------------
    private void fetchPosts() {
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
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject res = new JSONObject(sb.toString());
                    JSONArray list = res.getJSONObject("data").getJSONArray("list");

                    List<ClubTermPost> newPosts = new ArrayList<>();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject p = list.getJSONObject(i);

                        JSONArray cmts = p.optJSONArray("comments");
                        List<ClubComment> cList = new ArrayList<>();
                        if (cmts != null) {
                            for (int j = 0; j < cmts.length(); j++) {
                                JSONObject c = cmts.getJSONObject(j);
                                String commentAuthor = "Unknown";
                                JSONObject ca = c.optJSONObject("author");
                                if (ca != null) commentAuthor = ca.optString("nickname", "Unknown");
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
                        if (authorObj != null) postAuthor = authorObj.optString("nickname", "Unknown");
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
                        newPosts.add(ctp);
                    }

                    mainHandler.post(() -> {
                        postList.clear();
                        postList.addAll(newPosts);
                        postAdapter.notifyDataSetChanged();
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

    // ------------------- createPost -------------------
    private void createPost(String content) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/clubs/" + clubId + "/posts";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                JSONObject body = new JSONObject();
                body.put("content", content);
                body.put("timestamp", System.currentTimeMillis());
                if (!pendingImages.isEmpty()) {
                    JSONArray imgArray = new JSONArray();
                    for (String img : pendingImages) {
                        imgArray.put(img);
                    }
                    body.put("images", imgArray);
                }

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    mainHandler.post(() -> {
                        etNewPost.setText("");
                        pendingImages.clear();
                        Toast.makeText(ClubCommunityActivity.this, "Post shared!", Toast.LENGTH_SHORT).show();
                        fetchPosts();
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Create post error", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(ClubCommunityActivity.this, "Failed to post", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ------------------- toggleExit -------------------
    private void toggleExit() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/clubs/" + clubId + "/toggle";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                JSONObject body = new JSONObject();
                body.put("timestamp", System.currentTimeMillis());
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    mainHandler.post(() -> {
                        Toast.makeText(ClubCommunityActivity.this, "Left the club", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ClubCommunityActivity.this, clubterm.class);
                        intent.putExtra("CLUB_ID", clubId);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Toggle exit error", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(ClubCommunityActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}