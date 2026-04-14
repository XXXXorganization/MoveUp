# Sport 模块 API 测试指南 - Apifox

## 一、准备工作

### 1.1 确保后端服务运行
```bash
cd backend
npm run dev
```
服务默认运行在：`http://localhost:3000`

### 1.2 Apifox 环境配置

在 Apifox 中创建环境变量：

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `BASE_URL` | `http://localhost:3000/v1` | API 基础路径 |
| `PHONE` | `13800138000` | 测试手机号 |
| `TOKEN` | `{{token}}` | 认证 Token |

---

## 二、认证流程

### 2.1 发送验证码
```
POST {{BASE_URL}}/auth/code
```

**请求体：**
```json
{
  "phone": "{{PHONE}}",
  "type": "login"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "验证码已发送"
}
```

> **注意：** 查看控制台输出的验证码，格式如：`Verification code for 13800138000 (login): 123456`

### 2.2 登录获取 Token
```
POST {{BASE_URL}}/auth/login
```

**请求体：**
```json
{
  "phone": "{{PHONE}}",
  "code": "控制台输出的验证码"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 7200,
    "user": {
      "id": "uuid",
      "nickname": "用户8000",
      "avatar": null
    }
  }
}
```

> **重要：** 将返回的 `token` 值复制到环境变量 `TOKEN` 中

### 2.3 设置请求头
所有需要认证的接口都需要添加以下请求头：

| Header 名 | 值 |
|-----------|-----|
| `Authorization` | `Bearer {{TOKEN}}` |

---

## 三、运动记录管理 API

### 3.1 开始运动记录
```
POST {{BASE_URL}}/sport/start
```

**请求头：**
```
Authorization: Bearer {{TOKEN}}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "运动记录已创建",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "startTime": "2026-04-13T12:00:00.000Z",
    "distance": 0,
    "duration": 0,
    "calories": 0,
    "status": "active"
  }
}
```

> **保存返回的 `id`，后续接口需要使用**

---

### 3.2 获取运动记录列表
```
GET {{BASE_URL}}/sport
```

**响应示例：**
```json
{
  "code": 200,
  "message": "获取运动记录列表成功",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "550e8400-e29b-41d4-a716-446655440001",
      "startTime": "2026-04-13T12:00:00.000Z",
      "endTime": null,
      "distance": 1500,
      "duration": 600,
      "calories": 120,
      "status": "active"
    }
  ]
}
```

---

### 3.3 获取单个运动记录
```
GET {{BASE_URL}}/sport/:recordId
```

**路径参数：**
- `recordId`: 运动记录 ID

---

### 3.4 停止运动记录
```
PUT {{BASE_URL}}/sport/:recordId/stop
```

**路径参数：**
- `recordId`: 运动记录 ID

**响应示例：**
```json
{
  "code": 200,
  "message": "运动记录已结束",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "completed",
    "endTime": "2026-04-13T12:10:00.000Z",
    "distance": 1500,
    "duration": 600,
    "calories": 120,
    "averagePace": 400
  }
}
```

---

## 四、GPS 轨迹 API

### 4.1 批量上传 GPS 轨迹点
```
POST {{BASE_URL}}/sport/:recordId/gps/batch
```

**路径参数：**
- `recordId`: 运动记录 ID

**请求体：**
```json
{
  "gpsPoints": [
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440000",
      "latitude": 39.90420,
      "longitude": 116.40740,
      "timestamp": "2026-04-13T12:00:00.000Z",
      "speed": 3.5,
      "accuracy": 5
    },
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440000",
      "latitude": 39.90421,
      "longitude": 116.40741,
      "timestamp": "2026-04-13T12:00:01.000Z",
      "speed": 3.6,
      "accuracy": 4
    }
  ]
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "GPS 轨迹点上传成功",
  "data": {
    "count": 2,
    "points": [...]
  }
}
```

---

### 4.2 获取 GPS 轨迹点
```
GET {{BASE_URL}}/sport/:recordId/gps
```

**响应示例：**
```json
{
  "code": 200,
  "message": "获取 GPS 轨迹点成功",
  "data": {
    "count": 10,
    "points": [
      {
        "id": "uuid",
        "recordId": "550e8400-e29b-41d4-a716-446655440000",
        "latitude": 39.90420,
        "longitude": 116.40740,
        "timestamp": "2026-04-13T12:00:00.000Z",
        "speed": 3.5,
        "accuracy": 5
      }
    ]
  }
}
```

---

## 五、心率数据 API

### 5.1 批量上传心率数据
```
POST {{BASE_URL}}/sport/:recordId/heart-rate/batch
```

**请求体：**
```json
{
  "heartRates": [
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440000",
      "timestamp": "2026-04-13T12:00:00.000Z",
      "heartRate": 140
    },
    {
      "recordId": "550e8400-e29b-41d4-a716-446655440000",
      "timestamp": "2026-04-13T12:00:01.000Z",
      "heartRate": 145
    }
  ]
}
```

---

### 5.2 获取心率数据
```
GET {{BASE_URL}}/sport/:recordId/heart-rate
```

---

### 5.3 蓝牙心率设备数据
```
POST {{BASE_URL}}/sport/:recordId/bluetooth/heart-rate
```

**请求体：**
```json
{
  "deviceId": "HRM-001",
  "heartRate": 145,
  "batteryLevel": 80,
  "timestamp": "2026-04-13T12:00:00.000Z"
}
```

---

## 六、实时数据 API

### 6.1 获取实时运动数据
```
GET {{BASE_URL}}/sport/:recordId/realtime
```

**响应示例：**
```json
{
  "code": 200,
  "message": "获取实时数据成功",
  "data": {
    "distance": 1500,
    "duration": 600,
    "pace": 400,
    "speed": 2.5,
    "calories": 120,
    "heartRate": 145,
    "lastGpsPoint": {
      "latitude": 39.90420,
      "longitude": 116.40740
    }
  }
}
```

---

### 6.2 获取运动记录统计
```
GET {{BASE_URL}}/sport/:recordId/stats
```

---

### 6.3 获取配速分段统计
```
GET {{BASE_URL}}/sport/:recordId/pace-segments
```

---

## 七、用户统计 API

### 7.1 获取用户运动统计
```
GET {{BASE_URL}}/sport/stats
```

**响应示例：**
```json
{
  "code": 200,
  "message": "获取用户统计成功",
  "data": {
    "totalDistance": 15000,
    "totalTime": 7200,
    "totalCalories": 1200,
    "averagePace": 480,
    "bestPace": 420,
    "maxHeartRate": 165,
    "averageHeartRate": 145
  }
}
```

---

## 八、工具接口

### 8.1 计算卡路里
```
POST {{BASE_URL}}/sport/calculate-calories
```

**请求体：**
```json
{
  "weight": 70,
  "duration": 3600,
  "averagePace": 360,
  "averageHeartRate": 150,
  "age": 25,
  "gender": 1
}
```

**参数说明：**
| 参数 | 类型 | 说明 |
|------|------|------|
| weight | number | 体重 (kg) |
| duration | number | 运动时长 (秒) |
| averagePace | number | 平均配速 (秒/公里) |
| averageHeartRate | number | 平均心率 (bpm)，可选 |
| age | number | 年龄，默认 25 |
| gender | number | 性别：1-男，2-女，默认 1 |

---

## 九、Apifox 测试步骤

### 9.1 创建 API 文档

1. **新建项目**
   - 打开 Apifox，点击「新建项目」
   - 项目名称：`MoveUp Sport API`
   - 选择「HTTP 项目」

2. **导入接口**
   - 点击「导入数据」
   - 选择「手动输入」或「URL 导入」
   - 按照上述 API 定义逐个创建接口

### 9.2 创建接口分组

建议创建以下分组：

```
MoveUp Sport API
├── 认证 (Auth)
│   ├── 发送验证码
│   └── 登录
├── 运动记录管理
│   ├── 开始运动记录
│   ├── 停止运动记录
│   ├── 获取记录列表
│   └── 获取单个记录
├── GPS 轨迹
│   ├── 上传轨迹点
│   └── 获取轨迹点
├── 心率数据
│   ├── 上传心率数据
│   ├── 获取心率数据
│   └── 蓝牙心率数据
├── 实时数据
│   ├── 获取实时数据
│   ├── 获取记录统计
│   └── 获取配速分段
└── 用户统计
    └── 获取用户统计
```

### 9.3 设置环境变量

1. 点击项目设置 → 环境管理
2. 添加环境变量：
   ```
   BASE_URL=http://localhost:3000/v1
   PHONE=13800138000
   TOKEN=
   ```

### 9.4 创建自动化测试用例

推荐创建以下测试流程：

```
测试流程：完整的运动记录流程
1. 发送验证码
2. 登录获取 Token
3. 开始运动记录
4. 上传 GPS 轨迹点
5. 上传心率数据
6. 获取实时数据
7. 停止运动记录
8. 获取运动记录统计
9. 获取用户统计
```

---

## 十、常见问题

### Q1: 401 未授权错误
**解决方案：** 确保 `Authorization` 请求头正确设置，格式为 `Bearer {{TOKEN}}`

### Q2: 验证码无效
**解决方案：** 查看后端控制台输出的验证码，验证码 5 分钟内有效

### Q3: 400 运动记录已结束
**解决方案：** 已完成的记录不能更新数据，需要开始新的运动记录

### Q4: GPS 点上传失败
**解决方案：** 确保 GPS 点之间的速度不超过阈值（约 28.8 km/h）

---

## 十一、快速测试数据

### 运动开始 → 结束完整流程测试数据

```json
// 1. 开始运动（POST /sport/start）
// 无需请求体，返回 recordId

// 2. 上传 GPS 点（POST /sport/{recordId}/gps/batch）
{
  "gpsPoints": [
    {
      "recordId": "{{recordId}}",
      "latitude": 39.90420,
      "longitude": 116.40740,
      "timestamp": "2026-04-13T12:00:00.000Z",
      "speed": 3.5,
      "accuracy": 5
    },
    {
      "recordId": "{{recordId}}",
      "latitude": 39.90421,
      "longitude": 116.40741,
      "timestamp": "2026-04-13T12:00:01.000Z",
      "speed": 3.6,
      "accuracy": 4
    },
    {
      "recordId": "{{recordId}}",
      "latitude": 39.90422,
      "longitude": 116.40742,
      "timestamp": "2026-04-13T12:00:02.000Z",
      "speed": 3.7,
      "accuracy": 4
    }
  ]
}

// 3. 上传心率数据（POST /sport/{recordId}/heart-rate/batch）
{
  "heartRates": [
    {
      "recordId": "{{recordId}}",
      "timestamp": "2026-04-13T12:00:00.000Z",
      "heartRate": 140
    },
    {
      "recordId": "{{recordId}}",
      "timestamp": "2026-04-13T12:00:01.000Z",
      "heartRate": 145
    },
    {
      "recordId": "{{recordId}}",
      "timestamp": "2026-04-13T12:00:02.000Z",
      "heartRate": 150
    }
  ]
}

// 4. 停止运动（PUT /sport/{recordId}/stop）
// 无需请求体
```

---

**测试完成后，你可以：**
1. 导出 API 文档为 Markdown/PDF 格式
2. 分享 API 文档链接给团队成员
3. 生成自动化测试报告
4. 导出接口定义给前端开发
