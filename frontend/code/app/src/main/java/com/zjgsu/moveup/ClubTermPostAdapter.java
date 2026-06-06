package com.zjgsu.moveup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.bumptech.glide.Glide;

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

    public static String BASE_URL = "http://192.168.25.47:3000";

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

        if (post.images != null && !post.images.isEmpty()) {
            holder.postImageWrap.setVisibility(View.VISIBLE);
            String firstImage = post.images.get(0);
            if (firstImage.startsWith("data:")) {
                try {
                    byte[] decodedBytes = android.util.Base64.decode(
                            firstImage.substring(firstImage.indexOf(",") + 1),
                            android.util.Base64.DEFAULT);
                    holder.ivPostImage.setImageBitmap(
                            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                } catch (Exception e) {
                    holder.ivPostImage.setImageResource(R.drawable.term1);
                }
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(firstImage)
                        .centerCrop()
                        .placeholder(R.drawable.term1)
                        .error(R.drawable.term1)
                        .into(holder.ivPostImage);
            }
            holder.tvPostBadge.setText(post.images.size() + " pics");
            holder.tvPostBadge.setVisibility(View.VISIBLE);
        } else {
            holder.postImageWrap.setVisibility(View.GONE);
        }

        holder.tvLikeCount.setText(post.likeCount + " likes");
        if (post.isLiked) {
            holder.ivLike.setColorFilter(Color.parseColor("#E91E63"));
        } else {
            holder.ivLike.setColorFilter(Color.parseColor("#8B8B8B"));
        }

        holder.ivLike.setOnClickListener(v -> toggleLike(post, position));

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

        holder.itemView.setOnClickListener(v -> {
            holder.etCommentInput.setTag(null);
            holder.etCommentInput.setHint("Add a comment...");
            holder.etCommentInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(holder.etCommentInput.getWindowToken(), 0);
        });

        if (post.totalComments > 3) {
            holder.tvViewAllComments.setVisibility(View.VISIBLE);
            holder.tvViewAllComments.setText("View all " + post.totalComments + " comments");
            holder.tvViewAllComments.setOnClickListener(v -> openPostDetail(post));
        } else {
            holder.tvViewAllComments.setVisibility(View.GONE);
        }

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

    // ------------------- toggleLike -------------------
    private void toggleLike(ClubTermPost post, int position) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/posts/" + post.id + "/like";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                if (context != null) {
                    SharedPreferences prefs = context.getSharedPreferences("moveup_auth", Context.MODE_PRIVATE);
                    String token = prefs.getString("jwt", "");
                    if (!token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                }

                JSONObject body = new JSONObject();
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONObject data = res.getJSONObject("data");

                    post.isLiked = data.optBoolean("is_liked", !post.isLiked);
                    post.likeCount = data.optInt("like_count", post.likeCount);
                    success = true;

                    mainHandler.post(() -> notifyItemChanged(position));
                }
            } catch (Exception e) {
                Log.e("API_TEST", "Like Error", e);
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // ------------------- sendComment -------------------
    private void sendComment(ClubTermPost post, String content, String replyToId, int position, EditText inputField) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/posts/" + post.id + "/comment";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                if (context != null) {
                    SharedPreferences prefs = context.getSharedPreferences("moveup_auth", Context.MODE_PRIVATE);
                    String token = prefs.getString("jwt", "");
                    if (!token.isEmpty()) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                }

                JSONObject body = new JSONObject();
                body.put("content", content);
                body.put("timestamp", System.currentTimeMillis());
                if (replyToId != null && !replyToId.isEmpty()) {
                    body.put("reply_to_id", replyToId);
                }

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONObject data = res.getJSONObject("data");

                    String commentAuthor = "Unknown";
                    JSONObject authorObj = data.optJSONObject("author");
                    if (authorObj != null) {
                        commentAuthor = authorObj.optString("nickname", "Unknown");
                    }

                    ClubComment newComment = new ClubComment(
                            data.optString("id", ""),
                            commentAuthor,
                            data.optString("content", content),
                            data.optString("created_at", ""),
                            data.optString("reply_to_id", null),
                            ""
                    );
                    if (post.comments == null) {
                        post.comments = new ArrayList<>();
                    }
                    post.comments.add(newComment);
                    post.totalComments = post.comments.size();

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
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static final class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAuthor, tvTime, tvLateTitle, tvSubLine, tvSubDetail, tvLikeCount, tvViewAllComments, btnSendComment, tvPostBadge;
        final ImageView ivLike, ivPostImage;
        final LinearLayout llCommentsList;
        final EditText etCommentInput;
        final View postImageWrap;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLateTitle = itemView.findViewById(R.id.tvLateTitle);
            tvSubLine = itemView.findViewById(R.id.tvSubLine);
            tvSubDetail = itemView.findViewById(R.id.tvSubDetail);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            postImageWrap = itemView.findViewById(R.id.postImageWrap);
            tvPostBadge = itemView.findViewById(R.id.tvPostBadge);
            llCommentsList = itemView.findViewById(R.id.llCommentsList);
            tvViewAllComments = itemView.findViewById(R.id.tvViewAllComments);
            etCommentInput = itemView.findViewById(R.id.etCommentInput);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);
        }
    }
}