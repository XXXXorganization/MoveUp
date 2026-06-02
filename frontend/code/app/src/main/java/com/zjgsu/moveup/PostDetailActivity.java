package com.zjgsu.moveup;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    public static String BASE_URL = "http://192.168.25.47:3000";

    private RecyclerView rvAllComments;
    private String postId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra("POST_ID");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvAllComments = findViewById(R.id.rvAllComments);
        rvAllComments.setLayoutManager(new LinearLayoutManager(this));

        if (postId != null) {
            fetchAllComments();
        } else {
            Toast.makeText(this, "Error: Post ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------- fetchAllComments -------------------
    private void fetchAllComments() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/posts/" + postId + "/comments";
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
                    JSONObject res = new JSONObject(br.readLine());
                    JSONArray list = res.getJSONObject("data").getJSONArray("list");

                    List<ClubComment> comments = new ArrayList<>();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);
                        String commentAuthor = "Unknown";
                        JSONObject authorObj = obj.optJSONObject("author");
                        if (authorObj != null) {
                            commentAuthor = authorObj.optString("nickname", "Unknown");
                        }
                        comments.add(new ClubComment(
                                obj.optString("id", ""),
                                commentAuthor,
                                obj.optString("content", ""),
                                obj.optString("created_at", ""),
                                obj.optString("reply_to_id", null),
                                ""
                        ));
                    }

                    mainHandler.post(() -> rvAllComments.setAdapter(new AllCommentsAdapter(comments)));
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Failed to load all comments", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(PostDetailActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static class AllCommentsAdapter extends RecyclerView.Adapter<AllCommentsAdapter.CommentViewHolder> {
        private final List<ClubComment> comments;

        AllCommentsAdapter(List<ClubComment> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment_detail, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            ClubComment c = comments.get(position);
            holder.tvAuthor.setText(c.author);
            holder.tvTime.setText(c.time);
            String text = "";
            if (c.replyToId != null && !c.replyToId.isEmpty() && !c.replyToId.equals("null") &&
                    c.replyToName != null && !c.replyToName.isEmpty() && !c.replyToName.equals("null")) {
                text += "<font color='#8B8B8B'><i>@" + c.replyToName + "</i></font>  ";
            }
            text += c.content;
            holder.tvContent.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvAuthor, tvTime, tvContent;
            CommentViewHolder(View itemView) {
                super(itemView);
                tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
                tvTime = itemView.findViewById(R.id.tvCommentTime);
                tvContent = itemView.findViewById(R.id.tvCommentContent);
            }
        }
    }
}