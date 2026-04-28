package com.zjgsu.moveup;

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

    public ClubAdapter(List<Club> clubs) {
        this.clubs = clubs;
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club_card, parent, false);
        return new ClubViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        Club club = clubs.get(position);
        holder.name.setText(club.name);
        holder.location.setText(club.location);
        holder.image.setImageResource(club.imageResId);
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    static final class ClubViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView location;

        ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.clubImage);
            name = itemView.findViewById(R.id.clubName);
            location = itemView.findViewById(R.id.clubLocation);
        }
    }
}
