package com.zjgsu.moveup;

import java.io.Serializable;
import java.util.List;

public final class ClubTermPost implements Serializable {
    public String id;
    public String authorName;
    public String timeText;
    public String lateTitle;
    public int postImageResId;
    public String postBadgeText;
    public String subLine;
    public String subDetail;
    public int avatarResId;

    public boolean isLiked;
    public int likeCount;
    public int totalComments;
    public List<ClubComment> comments;
    public List<String> images;
    public String runDistance;
    public String runDuration;
    public String runPace;
    public boolean hasRunData;

    public ClubTermPost(String id, String authorName, String timeText, String lateTitle,
                        int postImageResId, String postBadgeText, String subLine,
                        String subDetail, int avatarResId, boolean isLiked, int likeCount,
                        int totalComments, List<ClubComment> comments) {
        this(id, authorName, timeText, lateTitle, postImageResId, postBadgeText, subLine,
             subDetail, avatarResId, isLiked, likeCount, totalComments, comments, null);
    }

    public ClubTermPost(String id, String authorName, String timeText, String lateTitle,
                        int postImageResId, String postBadgeText, String subLine,
                        String subDetail, int avatarResId, boolean isLiked, int likeCount,
                        int totalComments, List<ClubComment> comments, List<String> images) {
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
        this.images = images;
    }
}