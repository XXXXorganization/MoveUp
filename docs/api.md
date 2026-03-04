## api.md - Moveup API 接口文档 by 蔡燚翔

# Moveup API 接口文档

## 📌 接口规范

### 基础信息
- **Base URL**: `https://api.moveup.com/v1`
- **请求格式**: `application/json`
- **响应格式**: `application/json`

### 通用响应结构
```json
{
  "code": 200,          // 状态码：200成功，4xx客户端错误，5xx服务器错误
  "message": "success",  // 提示信息
  "data": {}            // 响应数据
}
```

### 认证方式
在请求头中添加 JWT Token：
```
Authorization: Bearer <your_token>
```

---

## 👤 用户模块 API

### 1. 用户注册/登录

#### 发送验证码
```
POST /auth/code
```
**请求参数**：
```json
{
  "phone": "13800138000",
  "type": "login"  // login/register/reset
}
```
**响应**：`{"code": 200, "message": "验证码发送成功"}`

#### 手机号登录
```
POST /auth/login
```
**请求参数**：
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```
**响应**：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGc...",
    "expires_in": 7200,
    "user": {
      "id": 10001,
      "nickname": "跑步小白",
      "avatar": "https://.../avatar.jpg"
    }
  }
}
```

### 2. 用户资料管理

#### 获取个人资料
```
GET /user/profile
```
**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 10001,
    "nickname": "跑步小白",
    "avatar": "url",
    "gender": 1,  // 1男 2女 0未知
    "birthday": "1995-01-01",
    "height": 175,
    "weight": 70,
    "total_distance": 1250.5,  // 累计公里
    "total_time": 36000,        // 累计分钟
    "total_runs": 156,          // 累计次数
    "level": 5,                 // 用户等级
    "preferences": {
      "target_distance": 10,     // 目标距离km
      "remind_time": "07:00",    // 提醒时间
      "voice_frequency": "every_km"  // 语音播报频率
    }
  }
}
```

#### 更新个人资料
```
PUT /user/profile
```
**请求参数**：
```json
{
  "nickname": "进阶跑者",
  "height": 176,
  "weight": 68,
  "birthday": "1995-06-15"
}
```

#### 上传头像
```
POST /user/avatar
Content-Type: multipart/form-data
```
**参数**：`file` (图片文件)

---

## 🏃 运动模块 API

### 1. 运动记录管理

#### 开始运动
```
POST /runs/start
```
**请求参数**：
```json
{
  "run_type": "outdoor",  // outdoor/indoor
  "target_distance": 5,   // 目标距离(km)
  "target_time": 30       // 目标时间(min)
}
```
**响应**：
```json
{
  "code": 200,
  "data": {
    "run_id": "RUN20241101123456",
    "start_time": "2024-11-01T08:00:00Z"
  }
}
```

#### 上传运动轨迹点
```
POST /runs/{run_id}/points
```
**请求参数**（批量上传）：
```json
{
  "points": [
    {
      "lat": 39.9087,
      "lng": 116.3974,
      "altitude": 50,
      "timestamp": "2024-11-01T08:01:00Z",
      "speed": 3.5,      // 当前速度 m/s
      "heart_rate": 145  // 心率
    }
  ]
}
```

#### 结束运动
```
POST /runs/{run_id}/finish
```
**请求参数**：
```json
{
  "feelings": 4,  // 感受评分 1-5
  "notes": "今天状态不错"
}
```
**响应**：返回完整的运动总结数据

#### 获取运动记录列表
```
GET /runs?page=1&size=20&start_date=2024-01-01&end_date=2024-12-31
```
**响应**：
```json
{
  "code": 200,
  "data": {
    "total": 156,
    "list": [
      {
        "run_id": "RUN20241101123456",
        "date": "2024-11-01",
        "distance": 5.23,
        "duration": 1845,
        "pace_avg": "5'30\"",  // 平均配速
        "calories": 380,
        "route_image": "url"
      }
    ]
  }
}
```

#### 获取单次运动详情
```
GET /runs/{run_id}
```
**响应**：包含完整的运动数据、分段数据、轨迹点列表

#### 获取运动统计
```
GET /stats?type=week&date=2024-11-01
```
**支持类型**：week/month/year
**响应**：
```json
{
  "code": 200,
  "data": {
    "total_distance": 35.6,
    "total_duration": 10800,
    "total_calories": 2450,
    "avg_pace": "5'15\"",
    "runs_count": 5,
    "daily_data": [
      {"date": "11-01", "distance": 5.2},
      {"date": "11-02", "distance": 8.5}
    ]
  }
}
```

### 2. 路线管理

#### 保存路线
```
POST /routes
```
**请求参数**：
```json
{
  "name": "朝阳公园晨跑路线",
  "points": [...],  // 轨迹点列表
  "distance": 3.2,
  "city": "北京"
}
```

#### 获取推荐路线
```
GET /routes/recommended?lat=39.9&lng=116.3&radius=5000
```

---

## 👥 社交模块 API

### 1. 好友关系

#### 搜索用户
```
GET /users/search?keyword=138****
```

#### 发送好友请求
```
POST /friends/request
```
```json
{
  "user_id": 10002,
  "message": "一起跑步吧"
}
```

#### 处理好友请求
```
POST /friends/respond
```
```json
{
  "request_id": 123,
  "action": "accept"  // accept/reject
}
```

#### 获取好友列表
```
GET /friends
```

### 2. 动态社区

#### 发布动态
```
POST /feeds
Content-Type: multipart/form-data
```
**参数**：
- `content`: "今日完成10公里！"
- `run_id`: "RUN20241101123456" (可选，关联运动)
- `images`: 图片文件数组 (最多9张)
- `topics`: ["晨跑打卡", "10公里挑战"]

#### 获取动态列表
```
GET /feeds?type=following&page=1&size=20
```
**类型**：following（关注）/recommend（推荐）/friends（好友）

#### 互动操作
```
POST /feeds/{feed_id}/like
POST /feeds/{feed_id}/comment
```
评论参数：
```json
{
  "content": "太厉害了！"
}
```

#### 获取排行榜
```
GET /leaderboard?type=weekly&scope=friends
```
**类型**：weekly/monthly，**范围**：friends/city/global

---

## 🏆 挑战模块 API

#### 获取挑战列表
```
GET /challenges?status=ongoing
```

#### 参与挑战
```
POST /challenges/{challenge_id}/join
```

#### 获取成就徽章
```
GET /badges
```

---

## ⚙️ 其他通用 API

#### 上传图片（通用）
```
POST /upload/image
Content-Type: multipart/form-data
```
**参数**：`file`，`type` (avatar/feed/route)

#### 获取系统配置
```
GET /config
```
返回语音包列表、版本更新信息、隐私协议等

---

## 📝 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证/Token失效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如已存在） |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

---

## 🔄 WebSocket 接口

用于实时运动数据同步和消息推送

**连接地址**：`wss://api.moveup.com/ws?token=<jwt_token>`

### 消息类型

#### 发送实时运动数据
```json
{
  "type": "run_data",
  "run_id": "RUN20241101123456",
  "data": {
    "lat": 39.9087,
    "lng": 116.3974,
    "heart_rate": 148,
    "timestamp": "2024-11-01T08:05:00Z"
  }
}
```

#### 接收推送通知
```json
{
  "type": "notification",
  "data": {
    "id": 12345,
    "category": "like",
    "content": "张三赞了你的动态",
    "create_time": "2024-11-01T09:00:00Z"
  }
}
```

---

*文档版本：v1.0.0 | 最后更新：2024-11-01*
