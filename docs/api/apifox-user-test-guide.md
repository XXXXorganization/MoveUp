# User 模块 API 测试指南 - Apifox

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
| `TEST_PHONE` | `13800138000` | 测试手机号 |
| `TOKEN` | `{{token}}` | 认证 Token |

---

## 二、认证 API

### 2.1 发送验证码
```
POST {{BASE_URL}}/auth/code
```

**请求体：**
```json
{
  "phone": "{{TEST_PHONE}}",
  "type": "login"
}
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号（11位） |
| type | string | 是 | 类型：`login`-登录，`register`-注册，`reset`-重置 |

**响应示例：**
```json
{
  "code": 200,
  "message": "验证码发送成功"
}
```

> **注意：** 查看后端控制台输出的验证码，格式如：`Verification code for 13800138000 (login): 123456`

**验证码有效期：** 5 分钟

---

### 2.2 用户登录/注册
```
POST {{BASE_URL}}/auth/login
```

**请求体：**
```json
{
  "phone": "{{TEST_PHONE}}",
  "code": "123456"
}
```

**参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号 |
| code | string | 是 | 6位验证码 |

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 7200,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "nickname": "用户8000",
      "avatar": null
    }
  }
}
```

> **重要：** 将返回的 `token` 值复制到环境变量 `TOKEN` 中

**Token 有效期：** 7200 秒（2 小时）

---

## 三、用户资料 API

所有用户资料 API 都需要认证，请在请求头中添加：

```
Authorization: Bearer {{TOKEN}}
```

---

### 3.1 获取用户资料
```
GET {{BASE_URL}}/user/profile
```

**请求头：**
```
Authorization: Bearer {{TOKEN}}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nickname": "测试用户",
    "avatar": "https://example.com/avatar.jpg",
    "gender": 1,
    "birthday": "1995-06-15",
    "height": 175,
    "weight": 70,
    "total_distance": 15000,
    "total_time": 7200,
    "total_runs": 10,
    "level": 5,
    "preferences": {
      "target_distance": 5,
      "remind_time": "07:00",
      "voice_frequency": "every_km"
    }
  }
}
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 用户 ID |
| nickname | string | 昵称 |
| avatar | string | 头像 URL |
| gender | number | 性别：0-未知，1-男，2-女 |
| birthday | string | 生日（ISO 8601 格式） |
| height | number | 身高（厘米） |
| weight | number | 体重（公斤） |
| total_distance | number | 总跑步距离（米） |
| total_time | number | 总跑步时间（秒） |
| total_runs | number | 跑步次数 |
| level | number | 用户等级（根据总距离计算） |
| preferences.target_distance | number | 目标距离（公里） |
| preferences.remind_time | string | 提醒时间 |
| preferences.voice_frequency | string | 语音播报频率 |

---

### 3.2 更新用户资料
```
PUT {{BASE_URL}}/user/profile
```

**请求头：**
```
Authorization: Bearer {{TOKEN}}
```

**请求体：**
```json
{
  "nickname": "新昵称",
  "avatar": "https://example.com/new-avatar.jpg",
  "gender": 1,
  "birthday": "1995-06-15",
  "height": 175,
  "weight": 70,
  "target_distance": 10000,
  "target_time": 60
}
```

**请求参数说明：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | string | 否 | 昵称 |
| avatar | string | 否 | 头像 URL |
| gender | number | 否 | 性别：0-未知，1-男，2-女 |
| birthday | string | 否 | 生日，格式：YYYY-MM-DD |
| height | number | 否 | 身高（厘米） |
| weight | number | 否 | 体重（公斤） |
| target_distance | number | 否 | 目标距离（米） |
| target_time | number | 否 | 目标时间（分钟） |

**响应示例：**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "phone": "13800138000",
    "nickname": "新昵称",
    "avatar": "https://example.com/new-avatar.jpg",
    "gender": 1,
    "birthday": "1995-06-15",
    "height": 175,
    "weight": 70,
    "target_distance": 10000,
    "target_time": 60,
    "role": "user",
    "created_at": "2026-04-13T10:00:00.000Z",
    "updated_at": "2026-04-13T12:00:00.000Z"
  }
}
```

---

## 四、Apifox 接口配置

### 4.1 接口列表

| 接口名称 | 方法 | 路径 | 是否需要认证 |
|---------|------|------|------------|
| 发送验证码 | POST | `/auth/code` | 否 |
| 用户登录/注册 | POST | `/auth/login` | 否 |
| 获取用户资料 | GET | `/user/profile` | 是 |
| 更新用户资料 | PUT | `/user/profile` | 是 |

### 4.2 创建接口分组

在 Apifox 中建议创建以下分组：

```
MoveUp User API
├── 认证 (Auth)
│   ├── 发送验证码
│   └── 用户登录/注册
└── 用户资料 (Profile)
    ├── 获取用户资料
    └── 更新用户资料
```

---

## 五、完整测试流程

### 5.1 新用户注册流程

```
1. 发送验证码（type: "register"）
   ↓
2. 使用验证码登录（自动创建新用户）
   ↓
3. 获取用户资料（查看默认信息）
   ↓
4. 更新用户资料（完善个人信息）
```

### 5.2 老用户登录流程

```
1. 发送验证码（type: "login"）
   ↓
2. 使用验证码登录
   ↓
3. 获取用户资料
```

---

## 六、Apifox 快速配置

### 6.1 发送验证码接口配置

**基础信息：**
- 接口名称：`发送验证码`
- 请求方法：`POST`
- 接口路径：`/auth/code`

**Query 参数：**
- `BASE_URL`: `{{BASE_URL}}`

**Body 参数（application/json）：**
```
{
  "phone": "{{TEST_PHONE}}",
  "type": "login"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "验证码发送成功"
}
```

---

### 6.2 用户登录接口配置

**基础信息：**
- 接口名称：`用户登录/注册`
- 请求方法：`POST`
- 接口路径：`/auth/login`

**Body 参数（application/json）：**
```
{
  "phone": "{{TEST_PHONE}}",
  "code": "123456"
}
```

**后置操作（自动提取 token）：**
```javascript
// 在 Apifox 的"后置操作"中添加
if (response.body.data && response.body.data.token) {
  pm.environment.set("TOKEN", response.body.data.token);
}
```

---

### 6.3 获取用户资料接口配置

**基础信息：**
- 接口名称：`获取用户资料`
- 请求方法：`GET`
- 接口路径：`/user/profile`

**请求头：**
```
Authorization: Bearer {{TOKEN}}
```

---

### 6.4 更新用户资料接口配置

**基础信息：**
- 接口名称：`更新用户资料`
- 请求方法：`PUT`
- 接口路径：`/user/profile`

**请求头：**
```
Authorization: Bearer {{TOKEN}}
```

**Body 参数（application/json）：**
```
{
  "nickname": "运动达人",
  "gender": 1,
  "height": 178,
  "weight": 72
}
```

---

## 七、测试用例建议

### 7.1 发送验证码测试

| 用例 | 描述 | 预期结果 |
|------|------|---------|
| 正常发送 | 输入有效手机号 | 返回 200，验证码发送成功 |
| 无效手机号 | 手机号格式错误 | 返回 400，错误提示 |
| 空手机号 | 不输入手机号 | 返回 400，错误提示 |

### 7.2 用户登录测试

| 用例 | 描述 | 预期结果 |
|------|------|---------|
| 正常登录 | 输入正确验证码 | 返回 200，获取 token |
| 验证码错误 | 输入错误验证码 | 返回 400，验证码无效 |
| 验证码过期 | 使用过期验证码 | 返回 400，验证码过期 |
| 新用户注册 | 首次登录 | 自动创建用户，返回 token |

### 7.3 获取用户资料测试

| 用例 | 描述 | 预期结果 |
|------|------|---------|
| 正常获取 | 使用有效 token | 返回 200，用户资料完整 |
| 无 token | 不带 token | 返回 401，未授权 |
| 无效 token | 使用无效 token | 返回 401，无效令牌 |

### 7.4 更新用户资料测试

| 用例 | 描述 | 预期结果 |
|------|------|---------|
| 更新昵称 | 修改昵称 | 返回 200，昵称更新成功 |
| 更新性别 | 设置性别 | 返回 200，性别更新成功 |
| 更新身高体重 | 设置身体数据 | 返回 200，数据更新成功 |
| 部分更新 | 只更新部分字段 | 返回 200，只更新指定字段 |

---

## 八、快速测试数据

### 8.1 完整测试流程示例

```json
// ===== 步骤1：发送验证码 =====
POST {{BASE_URL}}/auth/code
{
  "phone": "13800138000",
  "type": "login"
}
// 响应：{"code": 200, "message": "验证码发送成功"}

// ===== 步骤2：登录（控制台查看验证码）=====
POST {{BASE_URL}}/auth/login
{
  "phone": "13800138000",
  "code": "123456"
}
// 响应：返回 token，复制到环境变量

// ===== 步骤3：获取用户资料 =====
GET {{BASE_URL}}/user/profile
Headers: {
  "Authorization": "Bearer {{TOKEN}}"
}
// 响应：用户资料，nickname默认为"用户8000"

// ===== 步骤4：更新用户资料 =====
PUT {{BASE_URL}}/user/profile
Headers: {
  "Authorization": "Bearer {{TOKEN}}"
}
{
  "nickname": "跑步爱好者",
  "gender": 1,
  "birthday": "1998-05-20",
  "height": 175,
  "weight": 68,
  "target_distance": 10000,
  "target_time": 50
}
// 响应：{"code": 200, "message": "更新成功", "data": {...}}

// ===== 步骤5：再次获取用户资料验证 =====
GET {{BASE_URL}}/user/profile
Headers: {
  "Authorization": "Bearer {{TOKEN}}"
}
// 验证：nickname 变为 "跑步爱好者"，其他字段也已更新
```

---

## 九、常见问题

### Q1: 验证码在哪里查看？
**解决方案：** 查看后端控制台输出，格式如：
```
Verification code for 13800138000 (login): 123456
```

### Q2: 返回 401 未授权错误
**解决方案：**
1. 确保已执行登录接口获取 token
2. 检查请求头格式：`Authorization: Bearer {{TOKEN}}`
3. 确认 token 未过期（2小时有效期）

### Q3: 更新用户资料失败
**解决方案：**
- 检查请求方法是否为 `PUT`
- 确认 Content-Type 为 `application/json`
- 验证字段类型正确（如 height 是数字不是字符串）

### Q4: 新用户登录后默认昵称是什么？
**答案：** 默认昵称为 `用户xxxx`，其中 xxxx 是手机号后 4 位

### Q5: 如何计算用户等级？
**答案：** 用户等级 = Math.floor(总距离 / 100) + 1
- 跑 0-100 米 = 1 级
- 跑 100-200 米 = 2 级
- 以此类推...

---

## 十、自动化测试脚本

在 Apifox 中创建自动化测试套件：

```javascript
// Apifox 测试脚本示例

// 测试1：发送验证码
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// 测试2：登录并保存 token
pm.test("Login successful and save token", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql(200);
    pm.expect(jsonData.data.token).to.exist;
    pm.environment.set("TOKEN", jsonData.data.token);
});

// 测试3：获取用户资料
pm.test("Get profile successful", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql(200);
    pm.expect(jsonData.data.id).to.exist;
    pm.expect(jsonData.data.nickname).to.exist;
});

// 测试4：更新用户资料
pm.test("Update profile successful", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql(200);
    pm.expect(jsonData.data.nickname).to.eql("新昵称");
});
```

---

## 十一、导入到 Apifox 的步骤

### 方式一：手动创建接口

1. 打开 Apifox，创建新项目
2. 创建分组「认证」和「用户资料」
3. 按照上述配置逐个创建接口
4. 配置环境变量
5. 开始测试

### 方式二：使用 Apifox CLI 导入（如果有 OpenAPI 文档）

```bash
# 如果有 OpenAPI/Swagger 文档
apifox-cli import -f user-api.yaml
```

### 方式三：复制接口配置

1. 在 Apifox 中创建新接口
2. 复制本文档中的接口配置
3. 粘贴到对应字段中

---

## 十二、导出文档

测试完成后，可以：

1. **导出为 Markdown**
   - 点击项目设置 → 导出数据
   - 选择 Markdown 格式
   - 保存为 `user-api-guide.md`

2. **生成在线文档**
   - 使用 Apifox 的在线分享功能
   - 生成分享链接发给团队成员

3. **导出为 Postman 集合**
   - 选择导出 Postman 格式
   - 在 Postman 中导入使用

---

**测试完成后，你可以：**
- ✅ 验证用户注册/登录功能
- ✅ 测试用户资料获取和更新
- ✅ 验证认证机制是否正常
- ✅ 生成 API 测试报告
- ✅ 与前端开发人员共享接口文档
