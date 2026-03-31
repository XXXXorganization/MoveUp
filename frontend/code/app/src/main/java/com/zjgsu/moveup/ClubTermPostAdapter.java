package com.zjgsu.moveup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClubTermPostAdapter extends RecyclerView.Adapter<ClubTermPostAdapter.PostViewHolder> {

    private final List<ClubTermPost> posts;

    public ClubTermPostAdapter(@NonNull List<ClubTermPost> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club_post, parent, false);
        return new PostViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ClubTermPost post = posts.get(position);

        holder.tvAuthor.setText(post.authorName);
        holder.tvTime.setText(post.timeText);
        holder.tvLateTitle.setText(post.lateTitle);

        holder.ivPostImage.setImageResource(post.postImageResId);
        holder.tvPostBadge.setText(post.postBadgeText);

        holder.tvSubLine.setText(post.subLine);
        holder.tvSubDetail.setText(post.subDetail);

        holder.tvLikeCount.setText(post.likesText);
        holder.avatar.setImageResource(post.avatarResId);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static final class PostViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView tvAuthor;
        final TextView tvTime;

        final TextView tvLateTitle;

        final ImageView ivPostImage;
        final TextView tvPostBadge;

        final TextView tvSubLine;
        final TextView tvSubDetail;

        final TextView tvLikeCount;
        final ImageView ivLike;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);

            tvLateTitle = itemView.findViewById(R.id.tvLateTitle);

            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvPostBadge = itemView.findViewById(R.id.tvPostBadge);

            tvSubLine = itemView.findViewById(R.id.tvSubLine);
            tvSubDetail = itemView.findViewById(R.id.tvSubDetail);

            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            ivLike = itemView.findViewById(R.id.ivLike);
        }
    }
}

