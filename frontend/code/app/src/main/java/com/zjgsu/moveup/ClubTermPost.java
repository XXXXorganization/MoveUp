package com.zjgsu.moveup;

import androidx.annotation.DrawableRes;

/**
 * 俱乐部详情页帖子/评论区条目数据。
 * 目前使用示例数据，后续可替换成接口返回的数据结构。
 */
public final class ClubTermPost {

    public final String authorName;
    public final String timeText;
    public final String lateTitle;

    public final @DrawableRes int postImageResId;
    public final String postBadgeText;

    public final String subLine;
    public final String subDetail;

    public final String likesText;
    public final @DrawableRes int avatarResId;

    public ClubTermPost(
            String authorName,
            String timeText,
            String lateTitle,
            @DrawableRes int postImageResId,
            String postBadgeText,
            String subLine,
            String subDetail,
            String likesText,
            @DrawableRes int avatarResId) {
        this.authorName = authorName;
        this.timeText = timeText;
        this.lateTitle = lateTitle;
        this.postImageResId = postImageResId;
        this.postBadgeText = postBadgeText;
        this.subLine = subLine;
        this.subDetail = subDetail;
        this.likesText = likesText;
        this.avatarResId = avatarResId;
    }
}

