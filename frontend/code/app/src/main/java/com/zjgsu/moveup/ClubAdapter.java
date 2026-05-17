package com.zjgsu.moveup;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubViewHolder> {

    private final List<Club> clubs;
    private final int layoutResId;

    public ClubAdapter(List<Club> clubs) {
        this.clubs = clubs;
        this.layoutResId = R.layout.item_club_card;
    }

    public ClubAdapter(List<Club> clubs, int layoutResId) {
        this.clubs = clubs;
        this.layoutResId = layoutResId;
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext())
                .inflate(layoutResId, parent, false);
        return new ClubViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        Club club = clubs.get(position);

        if (holder.name != null) holder.name.setText(club.name);
        if (holder.location != null) holder.location.setText(club.location);
        if (holder.flag != null) holder.flag.setText(club.flag);

        // 🌟 核心修改：动态加载后端网络图片
        if (holder.image != null) {
            if (club.imageUrl != null && !club.imageUrl.isEmpty()) {
                // 如果后端传了 URL，就用 Glide 进行异步加载
                Glide.with(holder.itemView.getContext())
                        .load(club.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.moveup) // 加载时的占位图
                        .error(R.drawable.moveup)       // 如果网络错误，显示默认图
                        .into(holder.image);
            } else if (club.imageResId != 0) {
                // 如果没有 URL，兼容之前传了本地资源 ID 的逻辑（供测试类使用）
                holder.image.setImageResource(club.imageResId);
            }
        }

        // 点击整个卡片跳转到详情页 (clubterm)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), clubterm.class);
            intent.putExtra("CLUB_ID", club.id);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    static final class ClubViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView location;
        final TextView flag;

        ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.clubImage);
            name = itemView.findViewById(R.id.clubName);
            location = itemView.findViewById(R.id.clubLocation);
            flag = itemView.findViewById(R.id.clubFlag);
        }
    }
}