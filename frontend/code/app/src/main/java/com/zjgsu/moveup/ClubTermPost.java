package com.zjgsu.moveup;

import java.io.Serializable;
import java.util.List;

public final class ClubTermPost implements Serializable {
    public String id; // 帖子ID
    public String authorName;
    public String timeText;
    public String lateTitle;
    public int postImageResId;
    public String postBadgeText;
    public String subLine;
    public String subDetail;
    public int avatarResId;

    // 动态交互数据
    public boolean isLiked;
    public int likeCount;
    public int totalComments;
    public List<ClubComment> comments; // 列表页仅包含最多3条

    public ClubTermPost(String id, String authorName, String timeText, String lateTitle,
                        int postImageResId, String postBadgeText, String subLine,
                        String subDetail, int avatarResId, boolean isLiked, int likeCount,
                        int totalComments, List<ClubComment> comments) {
        this.id = id;
        this.authorName = authorName;
        this.timeText = timeText;
        this.lateTitle = lateTitle;
        this.postImageResId = postImageResId;
        this.postBadgeText = postBadgeText;
        this.subLine = subLine;
        this.subDetail = subDetail;
        this.avatarResId = avatarResId;
        this.isLiked = isLiked;
        this.likeCount = likeCount;
        this.totalComments = totalComments;
        this.comments = comments;
    }
}