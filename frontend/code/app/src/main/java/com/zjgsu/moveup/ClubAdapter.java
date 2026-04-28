package com.zjgsu.moveup;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        if (holder.image != null) holder.image.setImageResource(club.imageResId);
        if (holder.flag != null) holder.flag.setText(club.flag);

        // 🌟 新增：点击整个卡片跳转到详情页 (clubterm)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), clubterm.class);
            // 将社团 ID 传递过去
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