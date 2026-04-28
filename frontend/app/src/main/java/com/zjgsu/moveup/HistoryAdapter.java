package com.zjgsu.moveup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnShareClickListener {
        void onShareClick(@NonNull HistoryRun run, int position);
    }

    private final List<HistoryRun> runs;
    @Nullable
    private final OnShareClickListener shareListener;

    public HistoryAdapter(List<HistoryRun> runs, @Nullable OnShareClickListener shareListener) {
        this.runs = runs;
        this.shareListener = shareListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_history_run, parent, false);
        return new HistoryViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryRun run = runs.get(position);
        holder.date.setText(run.date);
        holder.title.setText(run.title);
        holder.timeValue.setText(run.timeValue);
        holder.paceValue.setText(run.paceValue);
        holder.distanceValue.setText(run.distanceValue);

        holder.share.setOnClickListener(
                v -> {
                    if (shareListener != null) {
                        shareListener.onShareClick(run, holder.getBindingAdapterPosition());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return runs.size();
    }

    static final class HistoryViewHolder extends RecyclerView.ViewHolder {
        final TextView date;
        final TextView title;
        final TextView timeValue;
        final TextView paceValue;
        final TextView distanceValue;
        final ImageButton share;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.activityDate);
            title = itemView.findViewById(R.id.activityTitle);
            timeValue = itemView.findViewById(R.id.timeValue);
            paceValue = itemView.findViewById(R.id.paceValue);
            distanceValue = itemView.findViewById(R.id.distanceValue);
            share = itemView.findViewById(R.id.btnShare);
        }
    }
}
