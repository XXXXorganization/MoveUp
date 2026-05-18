package com.zjgsu.moveup;

public class Club {
    public String id;
    public String name;
    public String location;

    // 兼容原有的本地图片资源
    public int imageResId;

    // 🌟 新增：用于接收后端的真实网络图片链接
    public String imageUrl;

    public String flag;

    public Club() {
    }

    // 以前用于测试或本地占位图的构造函数（保留以防报错）
    public Club(String id, String name, String location, int imageResId, String flag) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.imageResId = imageResId;
        this.flag = flag;
    }

    // 🌟 新增：用于从网络后端创建数据的构造函数
    public Club(String id, String name, String location, String imageUrl, String flag) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.imageUrl = imageUrl;
        this.flag = flag;
    }
}