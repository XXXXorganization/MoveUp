package com.zjgsu.moveup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ClubTermPostAdapter extends RecyclerView.Adapter<ClubTermPostAdapter.PostViewHolder> {

    // 🌟 核心修复：添加 public static 的 BASE_URL，供测试代码动态拦截
    public static String BASE_URL = "http://10.0.2.2:3000";

    private final List<ClubTermPost> posts;
    private final String currentUserId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context context;

    public ClubTermPostAdapter(@NonNull List<ClubTermPost> posts, String currentUserId) {
        this.posts = posts;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View root = LayoutInflater.from(context).inflate(R.layout.item_club_post, parent, false);
        return new PostViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ClubTermPost post = posts.get(position);

        holder.tvAuthor.setText(post.authorName);
        holder.tvTime.setText(post.timeText);
        holder.tvLateTitle.setText(post.lateTitle);
        holder.tvSubLine.setText(post.subLine);
        holder.tvSubDetail.setText(post.subDetail);

        // 渲染点赞状态
        holder.tvLikeCount.setText(post.likeCount + " likes");
        if (post.isLiked) {
            holder.ivLike.setColorFilter(Color.parseColor("#E91E63"));
        } else {
            holder.ivLike.setColorFilter(Color.parseColor("#8B8B8B"));
        }

        // 1. 点赞点击事件
        holder.ivLike.setOnClickListener(v -> toggleLike(post, position));

        // 2. 渲染评论区
        holder.llCommentsList.removeAllViews();
        if (post.comments != null) {
            for (ClubComment c : post.comments) {
                TextView tv = new TextView(context);
                tv.setTextSize(13);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 4, 0, 4);
                tv.setLayoutParams(params);

                String text = "<b><font color='#222222'>" + c.author + "</font></b>  ";
                if (c.replyToId != null && !c.replyToId.isEmpty() && !c.replyToId.equals("null") &&
                        c.replyToName != null && !c.replyToName.isEmpty() && !c.replyToName.equals("null")) {
                    text += "<font color='#8B8B8B'><i>@" + c.replyToName + "</i></font>  ";
                }
                text += "<font color='#4F4F4F'>" + c.content + "</font>";
                tv.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));

                tv.setOnClickListener(v -> {
                    holder.etCommentInput.setHint("Reply to " + c.author + "...");
                    holder.etCommentInput.setTag(c.id);
                    holder.etCommentInput.requestFocus();
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(holder.etCommentInput, InputMethodManager.SHOW_IMPLICIT);
                });
                holder.llCommentsList.addView(tv);
            }
        }

        // 3. 点击卡片非评论区域
        holder.itemView.setOnClickListener(v -> {
            holder.etCommentInput.setTag(null);
            holder.etCommentInput.setHint("Add a comment...");
            holder.etCommentInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(holder.etCommentInput.getWindowToken(), 0);
        });

        // 4. 评论折叠及跳转详情页
        if (post.totalComments > 3) {
            holder.tvViewAllComments.setVisibility(View.VISIBLE);
            holder.tvViewAllComments.setText("View all " + post.totalComments + " comments");
            holder.tvViewAllComments.setOnClickListener(v -> openPostDetail(post));
        } else {
            holder.tvViewAllComments.setVisibility(View.GONE);
        }

        // 5. 发送评论事件
        holder.btnSendComment.setOnClickListener(v -> {
            String content = holder.etCommentInput.getText().toString().trim();
            if (content.isEmpty()) return;

            String replyToId = holder.etCommentInput.getTag() != null ? holder.etCommentInput.getTag().toString() : "";
            sendComment(post, content, replyToId, position, holder.etCommentInput);
        });
    }

    private void openPostDetail(ClubTermPost post) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra("POST_ID", post.id);
        context.startActivity(intent);
    }

    private void toggleLike(ClubTermPost post, int position) {
        new Thread(() -> {
            try {
                // 🌟 修改点：动态拼接 BASE_URL
                URL url = new URL(BASE_URL + "/v1/posts/" + post.id + "/like");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("user_id", currentUserId);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONObject data = res.getJSONObject("data");

                    post.isLiked = data.getBoolean("is_liked");
                    post.likeCount = data.getInt("like_count");

                    mainHandler.post(() -> notifyItemChanged(position));
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Like Error", e);
            }
        }).start();
    }

    private void sendComment(ClubTermPost post, String content, String replyToId, int position, EditText inputField) {
        new Thread(() -> {
            try {
                // 🌟 修改点：动态拼接 BASE_URL
                URL url = new URL(BASE_URL + "/v1/posts/" + post.id + "/comment");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("user_id", currentUserId);
                body.put("content", content);
                body.put("timestamp", System.currentTimeMillis());
                body.put("reply_to_id", replyToId);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONObject data = res.getJSONObject("data");

                    post.totalComments = data.getInt("total_comments");
                    JSONArray arr = data.getJSONArray("comments");

                    List<ClubComment> newComments = new ArrayList<>();
                    for(int i=0; i<arr.length(); i++){
                        JSONObject obj = arr.getJSONObject(i);
                        newComments.add(new ClubComment(
                                obj.getString("id"),
                                obj.getString("author"),
                                obj.getString("content"),
                                obj.getString("time"),
                                obj.optString("reply_to_id", null),
                                obj.optString("reply_to_name", null)
                        ));
                    }
                    post.comments = newComments;

                    mainHandler.post(() -> {
                        inputField.setText("");
                        inputField.setHint("Add a comment...");
                        inputField.setTag(null);

                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(inputField.getWindowToken(), 0);

                        notifyItemChanged(position);
                        Toast.makeText(context, "Comment sent!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Comment Error", e);
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static final class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAuthor, tvTime, tvLateTitle, tvSubLine, tvSubDetail, tvLikeCount, tvViewAllComments, btnSendComment;
        final ImageView ivLike;
        final LinearLayout llCommentsList;
        final EditText etCommentInput;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLateTitle = itemView.findViewById(R.id.tvLateTitle);
            tvSubLine = itemView.findViewById(R.id.tvSubLine);
            tvSubDetail = itemView.findViewById(R.id.tvSubDetail);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            ivLike = itemView.findViewById(R.id.ivLike);

            llCommentsList = itemView.findViewById(R.id.llCommentsList);
            tvViewAllComments = itemView.findViewById(R.id.tvViewAllComments);
            etCommentInput = itemView.findViewById(R.id.etCommentInput);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);
        }
    }
}