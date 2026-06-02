package com.zjgsu.moveup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlanDetailAdapter extends RecyclerView.Adapter<PlanDetailAdapter.ViewHolder> {

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemCheckListener {
        void onItemCheck(int position);
    }

    private final List<PlanDetailItem> items;
    private OnItemLongClickListener longClickListener;
    private OnItemCheckListener checkListener;

    public PlanDetailAdapter(List<PlanDetailItem> items) {
        this.items = items;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemCheckListener(OnItemCheckListener listener) {
        this.checkListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlanDetailItem item = items.get(position);
        String distStr = item.distance;
        if (!distStr.contains("Km") && !distStr.isEmpty()) distStr += " Km";
        holder.tvDetailText.setText(item.time + "  -  " + distStr);

        // 完成状态
        holder.tvCheckmark.setVisibility(item.isCompleted ? View.VISIBLE : View.GONE);
        if (item.isCompleted) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        // 点击切换完成状态
        holder.itemView.setOnClickListener(v -> {
            if (checkListener != null) {
                checkListener.onItemCheck(position);
            }
        });

        // 左右交替显示逻辑
        if (position % 2 == 0) {
            holder.itemContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        } else {
            holder.itemContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetailText, tvCheckmark;
        LinearLayout itemContainer;

        ViewHolder(View view) {
            super(view);
            tvDetailText = view.findViewById(R.id.tvDetailText);
            tvCheckmark = view.findViewById(R.id.tvCheckmark);
            itemContainer = view.findViewById(R.id.itemContainer);
        }
    }
}