package com.zjgsu.moveup;

import java.io.Serializable;

public class ClubComment implements Serializable {
    public String id;
    public String author;
    public String content;
    public String time;
    public String replyToId; // 回复目标的评论ID（如果是顶级评论则为空）
    public String replyToName; // 🌟 新增：被回复者的名字

    public ClubComment(String id, String author, String content, String time, String replyToId, String replyToName) {
        this.id = id;
        this.author = author;
        this.content = content;
        this.time = time;
        this.replyToId = replyToId;
        this.replyToName = replyToName;
    }
}