# MoveUp API 接口文档

**维护人**：蔡燚翔  **版本**：v1.0  **日期**：2026-05-11

---

## 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://<host>/v1` |
| 请求格式 | `application/json` |
| 认证方式 | `Authorization: Bearer <token>` |
| 响应格式 | `{ code: number, message: string, data?: any }` |

---

## 状态码

| 状态码 | 含义 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或 Token 失效 |
| 403 | 无权访问该资源 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁 / 账号已锁定 |
| 500 | 服务器错误 |

---

## 1. 认证模块

所有接口挂载于 `/v1` 下。

### POST /auth/code — 发送短信验证码
```
Body: { phone: string, type: "login" | "register" | "reset" }
200:  { code: 200, message: "验证码发送成功" }
限频: 每号码每分钟 1 次
```

### POST /auth/login — 验证码登录（新用户自动注册）
```
Body: { phone: string, code: string }
200:  { code: 200, message: "登录成功", data: { token, expires_in, user: { id, nickname, avatar } } }
400:  验证码无效或已过期
429:  账号已锁定（连续 5 次错误码锁定 10 分钟）
```

### POST /auth/register — 密码注册（兼容旧版 App）
```
Body: { phone: string, username: string, password: string }
200:  { code: 200, message: "注册成功" }
400:  信息不完整
409:  手机号已注册
```

### GET /user/profile — 获取个人资料 🔒
```
200: { code: 200, data: { id, nickname, avatar, gender, birthday, height, weight,
      total_distance, total_time, total_runs, level, preferences } }
```

### PUT /user/profile — 更新个人资料 🔒
```
Body: { nickname?, avatar?, gender?, birthday?, height?, weight?, target_distance?, target_time? }
200:  { code: 200, message: "更新成功", data: User }
```

---

## 2. 运动模块

### POST /sport/start — 开始跑步 🔒
```
Body: 无
200:  { code: 201, message: "运动记录已创建", data: SportRecord }
400:  已有进行中的记录
```

### GET /sport — 获取运动记录列表 🔒
```
200: { code: 200, data: SportRecord[] }
```

### GET /sport/stats — 获取用户运动总统计 🔒
```
200: { code: 200, data: { totalDistance, totalTime, totalCalories, averagePace, bestPace,
      maxHeartRate, averageHeartRate } }
```

### GET /sport/:recordId — 获取单条记录 🔒
### PUT /sport/:recordId/update — 实时更新记录 🔒
```
Body: { recordId, gpsPoints?, heartRates? }
校验: 记录所有权（非本人记录返回 403）
```
### PUT /sport/:recordId/stop — 结束运动 🔒
### GET /sport/:recordId/realtime — 获取实时数据 🔒
```
200: { code: 200, data: { distance, duration, pace, speed, calories, heartRate, lastGpsPoint } }
```

### GET /sport/:recordId/stats — 记录级统计 🔒
### GET /sport/:recordId/pace-segments — 配速分段 🔒
### POST /sport/:recordId/gps/batch — 批量上传 GPS 轨迹点 🔒
```
Body: { gpsPoints: [{ latitude, longitude, timestamp, speed?, altitude?, accuracy? }] }
```
### GET /sport/:recordId/gps — 获取 GPS 轨迹 🔒
### POST /sport/:recordId/heart-rate/batch — 批量上传心率 🔒
```
Body: { heartRates: [{ timestamp, heartRate }] }
```
### GET /sport/:recordId/heart-rate — 获取心率数据 🔒
### POST /sport/:recordId/bluetooth/heart-rate — 蓝牙心率上报 🔒
```
Body: { deviceId: string, heartRate: number, timestamp?: string }
```
### POST /calculate-calories — 卡路里计算工具
```
Body: { weight, duration, averagePace, averageHeartRate?, age?, gender? }
```

### 兼容路由（映射到新路径）
```
POST /runs/start     → POST /sport/start        🔒
GET  /runs           → GET  /sport              🔒
```

---

## 3. 社交模块

全部 🔒

### 好友
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/social/users/search?keyword=` | — |
| GET | `/social/friends` | — |
| GET | `/social/friends/requests` | — |
| POST | `/social/friends/request` | `{ friendId }` |
| PUT | `/social/friends/respond` | `{ requesterId, action: "accept"\|"reject" }` |
| DELETE | `/social/friends/:friendId` | — |
| GET | `/social/friends/:friendId/activities` | — |

### 社区动态
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/social/posts?type=&limit=&offset=` | type: following\|recommend |
| POST | `/social/posts` | `{ content, images?, sportRecordId?, location?, tags? }` |
| GET | `/social/posts/:postId` | — |
| DELETE | `/social/posts/:postId` | — |
| POST | `/social/posts/:postId/like` | — |
| POST | `/social/posts/:postId/comments` | `{ content, parentId? }` |
| GET | `/social/posts/:postId/comments?limit=&offset=` | — |
| DELETE | `/social/posts/:postId/comments/:commentId` | — |

### 排行榜
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/social/leaderboard?type=&scope=` | type: weekly\|monthly, scope: friends\|global |

---

## 4. 训练指导模块

全部 🔒

### 训练计划
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/coaching/plans?difficulty=` | beginner/intermediate/advanced |
| GET | `/coaching/plans/:planId` | — |
| POST | `/coaching/plans/recommend` | `{ fitnessLevel, targetDistance, goalType }` |
| POST | `/coaching/plans/:planId/adopt` | — |

### 我的计划
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/coaching/my-plan` | — |
| GET | `/coaching/today-task` | — |
| PUT | `/coaching/my-plan/:userPlanId/progress` | `{ week, day }` |
| DELETE | `/coaching/my-plan/:userPlanId` | — |

### 语音指导
| Method | Path | Body / Query |
|--------|------|-------------|
| POST | `/coaching/voice-guidance` | `{ distanceM, currentPace, currentHeartRate?, config? }` |
| POST | `/coaching/segment-advice` | `{ currentHeartRate, age? }` |

**心率区间**: warmup(<60%) → fat_burn(60-70%) → aerobic(70-80%) → anaerobic(80-90%) → max(>90%)

### 健康建议
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/coaching/stretching?runDuration=` | — |
| GET | `/coaching/injury-prevention?weeklyDistance=&recentRunCount=` | — |

---

## 5. 挑战激励模块

全部 🔒

### 任务系统
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/challenge/tasks?type=` | — |
| GET | `/challenge/tasks/my` | — |
| PUT | `/challenge/tasks/progress` | `{ taskId, progress }` |

### 成就徽章
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/challenge/badges` | — |
| GET | `/challenge/badges/my` | — |

### 挑战赛
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/challenge/challenges?status=` | ongoing\|upcoming\|completed |
| GET | `/challenge/challenges/my` | — |
| GET | `/challenge/challenges/:challengeId` | — |
| POST | `/challenge/challenges/:challengeId/join` | — |
| PUT | `/challenge/challenges/:challengeId/progress` | `{ progress }` |
| GET | `/challenge/challenges/:challengeId/ranking` | — |

### 会员服务
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/challenge/membership/plans` | — |
| GET | `/challenge/membership/my` | — |
| POST | `/challenge/membership/purchase` | `{ planId }` |

### 积分
| Method | Path | Body / Query |
|--------|------|-------------|
| GET | `/challenge/points` | — |
| GET | `/challenge/points/logs?limit=&offset=` | — |

---

## 6. AI 智能模块

全部 🔒

### POST /ai/sport-summary — 单次运动 AI 总结
```
Body: { distance, duration, calories, avgPace?, avgHeartRate?, maxHeartRate?, date? }
校验: 距离 0–200km, 时长 0–24h, 卡路里 0–20000, 心率 30–250bpm
200:  { code: 200, data: { summary: string, suggestions: string[], generatedAt } }
```

### POST /ai/history-summary — 历史运动 AI 总结
```
Body: { records: SportSummaryInput[], periodLabel: string }
限制: records 最多 100 条
200:  { code: 200, data: { summary: string, suggestions: string[], generatedAt } }
```

---

## 7. 系统

### GET /health — 健康检查
```
200: { status: "healthy", timestamp: "ISO8601", uptime: number }
```

---

## 接口总览

| 模块 | 端点数 | 需鉴权 |
|------|--------|--------|
| 认证 | 5 | 2 |
| 运动 | 16 + 2 兼容 | 15 + 2 |
| 社交 | 16 | 16 |
| 训练 | 12 | 12 |
| 挑战 | 16 | 16 |
| AI | 2 | 2 |
| 系统 | 1 | 0 |
| **合计** | **70** | **63** |

---

## 安全注意事项

- 所有数值输入均有范围校验
- AI 模块有 Prompt 注入检测
- 登录有 IP 级和账号级双重限频
- 运动记录有所有权校验（非本人记录返回 403）
- 敏感接口有速率限制
