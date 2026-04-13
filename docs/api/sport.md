# 运动数据模块 API 文档

## 概述

运动数据模块提供完整的运动追踪功能，包括：
- 实时运动追踪
- GPS 轨迹记录与纠偏
- 配速、距离、时长实时计算
- 卡路里消耗算法（基于心率、体重、配速）
- 心率数据接入（支持蓝牙设备）

## 认证

所有接口都需要在请求头中携带 JWT Token：
```
Authorization: Bearer <token>
```

## API 端点

### 1. 开始运动记录

**请求**
```
POST /v1/sport/start
```

**响应**
```json
{
  "code": 200,
  "message": "运动记录已创建",
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "startTime": "2026-04-13T10:00:00.000Z",
    "distance": 0,
    "duration": 0,
    "calories": 0,
    "status": "active"
  }
}
```

### 2. 更新运动记录（实时上传）

**请求**
```
PUT /v1/sport/:recordId/update
```

**请求体**
```json
{
  "recordId": "uuid",
  "gpsPoints": [
    {
      "recordId": "uuid",
      "latitude": 39.9042,
      "longitude": 116.4074,
      "timestamp": "2026-04-13T10:00:00.000Z",
      "speed": 3.5,
      "accuracy": 5
    }
  ],
  "heartRates": [
    {
      "recordId": "uuid",
      "timestamp": "2026-04-13T10:00:00.000Z",
      "heartRate": 145
    }
  ]
}
```

**响应**
```json
{
  "code": 200,
  "message": "运动记录已更新",
  "data": {
    "id": "uuid",
    "distance": 150.5,
    "duration": 60,
    "averagePace": 398,
    "averageHeartRate": 145,
    "calories": 15.2
  }
}
```

### 3. 批量上传 GPS 轨迹点

**请求**
```
POST /v1/sport/:recordId/gps/batch
```

**请求体**
```json
{
  "gpsPoints": [
    {
      "recordId": "uuid",
      "latitude": 39.9042,
      "longitude": 116.4074,
      "timestamp": "2026-04-13T10:00:00.000Z"
    }
  ]
}
```

**说明**
- 自动进行 GPS 纠偏，移除漂移点和异常点
- 纠偏规则：
  - 精度 > 50 米的点视为漂移点
  - 速度 > 8 m/s (28.8 km/h) 的点视为异常点

**响应**
```json
{
  "code": 200,
  "message": "GPS 轨迹点上传成功",
  "data": {
    "count": 10,
    "points": [...]
  }
}
```

### 4. 批量上传心率数据

**请求**
```
POST /v1/sport/:recordId/heart-rate/batch
```

**请求体**
```json
{
  "heartRates": [
    {
      "recordId": "uuid",
      "timestamp": "2026-04-13T10:00:00.000Z",
      "heartRate": 145
    }
  ]
}
```

**响应**
```json
{
  "code": 200,
  "message": "心率数据上传成功",
  "data": {
    "count": 10,
    "rates": [...]
  }
}
```

### 5. 停止运动记录

**请求**
```
PUT /v1/sport/:recordId/stop
```

**响应**
```json
{
  "code": 200,
  "message": "运动记录已结束",
  "data": {
    "id": "uuid",
    "endTime": "2026-04-13T10:30:00.000Z",
    "distance": 5000,
    "duration": 1800,
    "averagePace": 360,
    "maxHeartRate": 175,
    "averageHeartRate": 155,
    "calories": 450.5,
    "status": "completed"
  }
}
```

### 6. 获取实时运动数据

**请求**
```
GET /v1/sport/:recordId/realtime
```

**响应**
```json
{
  "code": 200,
  "message": "获取实时数据成功",
  "data": {
    "distance": 2500,
    "duration": 900,
    "pace": 360,
    "speed": 2.78,
    "calories": 225.5,
    "heartRate": 152,
    "lastGpsPoint": {
      "latitude": 39.9082,
      "longitude": 116.4114,
      "timestamp": "2026-04-13T10:15:00.000Z"
    }
  }
}
```

### 7. 获取运动记录统计

**请求**
```
GET /v1/sport/:recordId/stats
```

**响应**
```json
{
  "code": 200,
  "message": "获取统计数据成功",
  "data": {
    "totalDistance": 5000,
    "totalTime": 1800,
    "totalCalories": 450.5,
    "averagePace": 360,
    "bestPace": 340,
    "maxHeartRate": 175,
    "averageHeartRate": 155
  }
}
```

### 8. 获取用户运动统计

**请求**
```
GET /v1/sport/stats
```

**响应**
```json
{
  "code": 200,
  "message": "获取用户统计成功",
  "data": {
    "totalDistance": 50000,
    "totalTime": 18000,
    "totalCalories": 4500,
    "averagePace": 360,
    "bestPace": 320,
    "maxHeartRate": 180,
    "averageHeartRate": 150
  }
}
```

### 9. 获取运动记录列表

**请求**
```
GET /v1/sport
```

**响应**
```json
{
  "code": 200,
  "message": "获取运动记录列表成功",
  "data": [
    {
      "id": "uuid",
      "startTime": "2026-04-13T10:00:00.000Z",
      "endTime": "2026-04-13T10:30:00.000Z",
      "distance": 5000,
      "duration": 1800,
      "calories": 450.5,
      "status": "completed"
    }
  ]
}
```

### 10. 获取单个运动记录

**请求**
```
GET /v1/sport/:recordId
```

**响应**
```json
{
  "code": 200,
  "message": "获取运动记录成功",
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "startTime": "2026-04-13T10:00:00.000Z",
    "endTime": "2026-04-13T10:30:00.000Z",
    "distance": 5000,
    "duration": 1800,
    "calories": 450.5,
    "status": "completed"
  }
}
```

### 11. 获取 GPS 轨迹点

**请求**
```
GET /v1/sport/:recordId/gps
```

**响应**
```json
{
  "code": 200,
  "message": "获取 GPS 轨迹点成功",
  "data": {
    "count": 100,
    "points": [
      {
        "id": "uuid",
        "recordId": "uuid",
        "latitude": 39.9042,
        "longitude": 116.4074,
        "timestamp": "2026-04-13T10:00:00.000Z",
        "speed": 3.5
      }
    ]
  }
}
```

### 12. 获取心率数据

**请求**
```
GET /v1/sport/:recordId/heart-rate
```

**响应**
```json
{
  "code": 200,
  "message": "获取心率数据成功",
  "data": {
    "count": 50,
    "rates": [
      {
        "id": "uuid",
        "recordId": "uuid",
        "timestamp": "2026-04-13T10:00:00.000Z",
        "heartRate": 145
      }
    ]
  }
}
```

### 13. 接入蓝牙心率设备数据

**请求**
```
POST /v1/sport/:recordId/bluetooth/heart-rate
```

**请求体**
```json
{
  "deviceId": "HRM-001",
  "heartRate": 145,
  "batteryLevel": 80,
  "timestamp": "2026-04-13T10:00:00.000Z"
}
```

**响应**
```json
{
  "code": 200,
  "message": "蓝牙心率数据已记录",
  "data": {
    "id": "uuid",
    "recordId": "uuid",
    "timestamp": "2026-04-13T10:00:00.000Z",
    "heartRate": 145
  }
}
```

### 14. 获取配速分段统计

**请求**
```
GET /v1/sport/:recordId/pace-segments
```

**响应**
```json
{
  "code": 200,
  "message": "获取配速分段成功",
  "data": {
    "recordId": "uuid",
    "totalDistance": 5000,
    "averagePace": 360
  }
}
```

## 卡路里计算算法

卡路里消耗基于以下因素计算：

### 1. 基础代谢当量 (MET)
根据配速确定基础 MET 值：
- 配速 < 4:00/km: MET = 12.0（高强度）
- 配速 4:00-5:00/km: MET = 10.0（中高强度）
- 配速 5:00-6:00/km: MET = 8.3（中等强度）
- 配速 6:00-7:00/km: MET = 7.0（中低强度）
- 配速 > 7:00/km: MET = 5.5（低强度）

### 2. 心率区间系数
根据平均心率与最大心率（220 - 年龄）的比值调整：
- > 90%: 系数 1.3（无氧区）
- 80-90%: 系数 1.2（乳酸阈值区）
- 70-80%: 系数 1.1（有氧区）
- 60-70%: 系数 1.0（燃脂区）
- < 60%: 系数 0.9（热身区）

### 3. 性别系数
- 男性：1.0
- 女性：0.9

### 4. 计算公式
```
卡路里 = MET × 心率系数 × 性别系数 × 体重 (kg) × 时间 (小时)
```

## GPS 纠偏算法

### 1. 精度过滤
移除精度 > 50 米的 GPS 点

### 2. 速度过滤
移除计算速度 > 8 m/s (28.8 km/h) 的点

### 3. 距离计算
使用 Haversine 公式计算两点之间的球面距离

## 错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证/ token 无效 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 使用示例

### 开始一次跑步

```javascript
// 1. 开始运动记录
const startRes = await fetch('/v1/sport/start', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});
const { id: recordId } = startRes.body.data;

// 2. 定期上传 GPS 和心率数据（例如每 5 秒）
setInterval(async () => {
  const gpsPoints = [/* 获取 GPS 数据 */];
  const heartRates = [/* 获取心率数据 */];
  
  await fetch(`/v1/sport/${recordId}/update`, {
    method: 'PUT',
    headers: { 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      recordId,
      gpsPoints,
      heartRates
    })
  });
}, 5000);

// 3. 结束运动
const endRes = await fetch(`/v1/sport/${recordId}/stop`, {
  method: 'PUT',
  headers: { 'Authorization': `Bearer ${token}` }
});
const stats = endRes.body.data;
```
