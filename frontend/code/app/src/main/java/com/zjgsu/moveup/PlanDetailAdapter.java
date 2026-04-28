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

    // 🌟 新增：长按监听器接口
    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    private final List<PlanDetailItem> items;
    private OnItemLongClickListener longClickListener; // 监听器变量

    public PlanDetailAdapter(List<PlanDetailItem> items) {
        this.items = items;
    }

    // 🌟 新增：设置监听器的方法
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
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
        holder.tvDetailText.setText(item.time + "  -  " + item.distance);

        // 左右交替显示逻辑
        if (position % 2 == 0) {
            holder.itemContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        } else {
            holder.itemContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        // 🌟 新增：绑定长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(position);
                return true; // 返回 true 表示消费了长按事件
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetailText;
        LinearLayout itemContainer;

        ViewHolder(View view) {
            super(view);
            tvDetailText = view.findViewById(R.id.tvDetailText);
            itemContainer = view.findViewById(R.id.itemContainer);
        }
    }
}