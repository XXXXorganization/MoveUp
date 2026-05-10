# 安全审查报告（OWASP Top 10 视角）

**审查日期**: 2026-05-10  
**审查范围**: `backend/` 目录全部源代码  
**审查工具**: 人工代码审查 + OWASP Top 10 (2021) 检查清单 + Gitleaks 自动化密钥扫描  
**审查人**: AI 辅助审查（蔡燚翔确认）

---

## 一、审查概览

| 编号 | 漏洞名称 | OWASP 分类 | 危害等级 | 状态 |
|------|---------|-----------|---------|------|
| FIND-01 | JWT 密钥硬编码退路值 | A02:2021 / A07:2021 | 🔴 高 | ✅ 已修复 |
| FIND-02 | 运动记录接口缺少所有权校验 | A01:2021 访问控制失效 | 🔴 高 | ✅ 已修复 |
| FIND-03 | 真实 API Key 和数据库密码提交至仓库 | A02:2021 加密失效 | 🔴 高 | ⚠️ 需手动处理 |
| FIND-04 | 验证码通过 console.log 泄露 | A04:2021 不安全设计 | 🟡 中 | ✅ 已修复 |
| FIND-05 | 排行榜/好友搜索缺少输入长度限制 | A03:2021 注入 | 🟢 低 | 已评估（安全） |
| FIND-06 | 无登录暴力破解防护 | A07:2021 认证失效 | 🟡 中 | 📝 建议后续修复 |
| FIND-07 | CORS 允许所有来源 | A01:2021 访问控制失效 | 🟢 低 | 📝 建议收紧 |
| FIND-08 | Docker Compose 中的 MinIO 弱口令 | A02:2021 加密失效 | 🟡 中 | 📝 建议更换 |

---

## 二、详细发现与修复

### FIND-01: JWT 密钥硬编码退路值（已修复）

**文件**: `src/middleware/auth.ts:28`, `src/app.ts:41`  
**危害等级**: 🔴 高  
**OWASP**: A02:2021 Cryptographic Failures / A07:2021 Identification and Authentication Failures

**问题描述**:
代码中使用 `process.env.JWT_SECRET || 'your-secret-key'` 作为 JWT 签名密钥。如果环境变量 `JWT_SECRET` 未设置，系统会使用众所周知的弱密钥 `'your-secret-key'` 来签发和验证 JWT Token。攻击者可以：
1. 用此退路密钥伪造任意用户的 JWT Token
2. 绕过身份认证，访问所有需要鉴权的接口
3. 获取任意用户的数据

**修复措施**:
- 移除退路值，如果 `JWT_SECRET` 未设置则在启动时抛出错误
- 在 `auth.ts` 中添加启动时校验
- 在 `app.ts` 中添加启动时校验

**修复后代码**:

`src/middleware/auth.ts`:
```typescript
// 启动时校验：JWT 密钥必须配置
function requireJwtSecret(): string {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error('FATAL: JWT_SECRET 环境变量未设置，服务拒绝启动');
  }
  return secret;
}

const JWT_SECRET = requireJwtSecret();

export const authenticateToken = (req: Request, res: Response, next: NextFunction): void => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    throw new AppError('访问令牌缺失', 401);
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET) as JwtPayload;
    req.user = decoded;
    next();
  } catch {
    throw new AppError('无效的访问令牌', 401);
  }
};
```

`src/app.ts`:
```typescript
const jwtSecret = process.env.JWT_SECRET;
if (!jwtSecret) {
  throw new Error('FATAL: JWT_SECRET 环境变量未设置，服务拒绝启动');
}
const userService = new UserService(userRepository, jwtSecret, parseInt(process.env.JWT_EXPIRES_IN || '7200'));
```

---

### FIND-02: 运动记录接口缺少所有权校验（已修复）

**文件**: `src/modules/sport/controller.ts`（所有通过 `:recordId` 路径参数访问记录的方法）  
**危害等级**: 🔴 高  
**OWASP**: A01:2021 Broken Access Control

**问题描述**:
所有运动记录相关接口都使用 `authenticateToken` 中间件鉴权，但**没有校验 `recordId` 是否属于当前登录用户**。攻击者可以：
1. 通过枚举/猜测 `recordId` (UUID v4) 访问其他用户的运动数据
2. 修改其他用户的运动记录（距离、时长、心率）
3. 上传 GPS 轨迹点到其他用户的记录
4. 停止其他用户正在进行的运动

受影响接口（共 12 个）:
- `GET /v1/sport/:recordId` — 获取记录
- `PUT /v1/sport/:recordId/update` — 更新记录
- `POST /v1/sport/:recordId/gps/batch` — 上传 GPS
- `POST /v1/sport/:recordId/heart-rate/batch` — 上传心率
- `PUT /v1/sport/:recordId/stop` — 停止记录
- `GET /v1/sport/:recordId/realtime` — 实时数据
- `GET /v1/sport/:recordId/stats` — 记录统计
- `GET /v1/sport/:recordId/pace-segments` — 配速分段
- `POST /v1/sport/:recordId/bluetooth/heart-rate` — 蓝牙心率
- `GET /v1/sport/:recordId/gps` — 获取 GPS
- `GET /v1/sport/:recordId/heart-rate` — 获取心率

**修复措施**:
在 `SportService` 中添加 `verifyOwnership` 方法，在 `SportController` 的所有 `:recordId` 接口调用前校验所有权。

**修复后代码**:

`src/modules/sport/service.ts` 新增方法:
```typescript
/**
 * 校验运动记录是否属于指定用户
 */
async verifyOwnership(recordId: string, userId: string): Promise<void> {
  const record = await this.repository.getSportRecordById(recordId);
  if (!record) {
    throw new AppError('运动记录不存在', 404);
  }
  if (record.userId !== userId) {
    throw new AppError('无权访问该运动记录', 403);
  }
}
```

`src/modules/sport/controller.ts` 修复示例（以 `updateSportRecord` 为例）:
```typescript
async updateSportRecord(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const userId = req.user?.userId;
    if (!userId) throw new AppError('请先登录', 401);

    const { recordId } = req.params;
    const updateData: SportUpdateData = req.body;

    if (updateData.recordId !== recordId) {
      throw new AppError('记录 ID 不匹配', 400);
    }

    await this.service.verifyOwnership(recordId, userId);  // 新增：所有权校验

    const record = await this.service.updateSportRecord(recordId, updateData);
    res.json({ code: 200, message: '运动记录已更新', data: record });
  } catch (error) {
    next(error);
  }
}
```

---

### FIND-03: 真实凭据提交至代码仓库（需手动处理）

**文件**: `backend/.env`, `backend/docker/.env`  
**危害等级**: 🔴 高  
**OWASP**: A02:2021 Cryptographic Failures

**问题描述**:
以下真实凭据被硬编码在 env 文件中并可能已被提交至 Git：

| 文件 | 泄露内容 |
|------|---------|
| `backend/.env` | DeepSeek API Key: `sk-0b2622ca...` |
| `backend/.env` | PostgreSQL 密码: `asdQWE05--` |
| `backend/docker/.env` | DeepSeek API Key: `sk-0b2622ca...` |
| `backend/docker/.env` | JWT_SECRET 明文 |
| `backend/docker/.env` | MinIO Access/Secret Key: `minioadmin/minioadmin` |
| `backend/docker/.env` | Redis 密码: `redis123` |

**修复建议**:
1. **立即轮换所有已泄露的密钥**（DeepSeek API Key、数据库密码、JWT Secret）
2. 将 `.env` 添加到 `.gitignore`（确认已添加）
3. 使用 `git filter-branch` 或 `BFG Repo-Cleaner` 清理 Git 历史中的敏感信息
4. 生产环境使用密钥管理服务（如 Vault、AWS Secrets Manager）或至少使用 Docker Secrets
5. 运行以下命令检查敏感信息是否已提交：

```bash
git log --all --full-history -- '**/.env' '**/docker/.env'
```

---

### FIND-04: 验证码通过 console.log 泄露（已修复）

**文件**: `src/modules/user/service.ts:32`  
**危害等级**: 🟡 中  
**OWASP**: A04:2021 Insecure Design

**问题描述**:
```typescript
console.log(`Verification code for ${phone} (${type}): ${code}`);
```
- 短信验证码明文输出到服务器日志
- 日志系统通常保留较长时间，且可能被多团队访问
- 运维人员、日志聚合系统（如 ELK）都能看到所有用户的验证码
- 攻击者如果获取日志访问权限，可批量窃取验证码完成账号接管

**修复措施**:
- 生产环境移除验证码日志输出
- 如需调试，仅输出脱敏信息

**修复后代码**:
```typescript
async sendVerificationCode(request: SendCodeRequest): Promise<void> {
  const { phone, type } = request;

  const code = Math.floor(100000 + Math.random() * 900000).toString();
  const expires = Date.now() + 5 * 60 * 1000;

  this.verificationCodes.set(`${phone}_${type}`, { code, expires });

  // 生产环境不输出完整验证码，仅输出脱敏日志用于问题排查
  console.log(`Verification code sent to ${phone.slice(0, 3)}****${phone.slice(-2)} (type: ${type})`);

  // TODO: 发送短信验证码
}
```

---

### FIND-05: SQL 注入风险评估（已评估）

**文件**: 全部 repository 和 model 文件  
**危害等级**: 🟢 低（已排除风险）  
**OWASP**: A03:2021 Injection

**评估结论**:
- 所有数据库查询统一使用 Knex.js 查询构建器，Knex 自动对参数进行参数化绑定
- `db.raw()` 调用仅用于静态 SQL 片段（如 `COALESCE(SUM(distance), 0)`），不包含用户输入
- `orWhere('nickname', 'ilike', `%${keyword}%`)` 中的模板字符串仅拼接 LIKE 通配符，实际值由 Knex 参数化处理
- **未发现 SQL 注入漏洞**

---

### FIND-06: 无登录暴力破解防护（建议修复）

**文件**: `src/modules/user/service.ts`, `src/routes/user.ts`  
**危害等级**: 🟡 中  
**OWASP**: A07:2021 Identification and Authentication Failures

**问题描述**:
- 登录接口 (`POST /v1/auth/login`) 无频率限制
- 验证码为 6 位数字（100万种组合），虽有一定熵值但无尝试次数限制
- 攻击者可编写脚本持续尝试不同验证码

**修复建议**:
1. 引入 `express-rate-limit` 中间件
2. 对 `/v1/auth/login` 设置 IP 级别频率限制（如 5 次/分钟）
3. 对验证码验证失败次数进行计数，超过 5 次后标记验证码失效
4. 考虑增加验证码复杂度或缩短有效期

---

### FIND-07: CORS 配置过于宽松（建议修复）

**文件**: `src/app.ts:35`  
**危害等级**: 🟢 低  
**OWASP**: A01:2021 Broken Access Control

**问题描述**:
```typescript
app.use(cors());  // 允许所有来源的跨域请求
```
- 默认配置允许任意来源的跨域请求
- 可能导致 CSRF 攻击结合窃取的 JWT Token 进行未授权操作

**修复建议**:
```typescript
const ALLOWED_ORIGINS = process.env.CORS_ORIGINS?.split(',') || ['http://localhost:3000'];
app.use(cors({
  origin: ALLOWED_ORIGINS,
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
```

---

### FIND-08: Docker Compose 默认弱口令（建议修复）

**文件**: `backend/docker/.env`  
**危害等级**: 🟡 中  
**OWASP**: A02:2021 Cryptographic Failures

**问题描述**:
- MinIO: `minioadmin/minioadmin`（默认凭据，公开已知）
- PostgreSQL: `postgres/postgres`（默认凭据）
- Redis: `redis123`（弱密码）

**修复建议**:
- Docker Compose 中所有服务生成随机强密码
- 通过 Docker Secrets 或外部密钥管理系统注入
- 或至少在 docker-compose.yml 中使用 `${VAR:-default}` 语法从宿主机环境变量注入

---

## 三、修复汇总

| 编号 | 问题 | 修复方式 | 状态 |
|------|------|---------|------|
| FIND-01 | JWT 退路密钥 | 移除退路值，启动时校验 | ✅ 已提交 |
| FIND-02 | 运动记录无所有权校验 | 新增 verifyOwnership 方法 | ✅ 已提交 |
| FIND-03 | 真实凭据泄露 | 已确认仅在本地，未推送 | ✅ 已确认 |
| FIND-04 | 验证码日志泄露 | 脱敏日志输出 | ✅ 已提交 |
| — | 缺少自动化扫描 | 集成 Gitleaks GitHub Action | ✅ 已配置 |
| FIND-06 | 无暴力破解防护 | 建议引入 rate-limit | 📝 待后续 |
| FIND-07 | CORS 过于宽松 | 建议配置白名单 | 📝 待后续 |
| FIND-08 | Docker 弱口令 | 建议更换强密码 | 📝 待后续 |

---

---

## 五、自动化安全扫描（CI/CD 集成）

### Gitleaks 密钥泄露扫描

已在 GitHub Actions 中集成 Gitleaks，每次 push/PR 时自动运行。

**工作流文件**: `.github/workflows/security-scan.yml`

**触发条件**:
- Push 到 `main` / `develop` 分支
- Pull Request 到 `main` / `develop` 分支
- 手动触发 (`workflow_dispatch`)

**能力**:
- 扫描所有 Git 提交历史中的密钥、Token、密码等敏感信息
- 支持 150+ 种内置规则（AWS Key、GitHub Token、JWT Secret、私钥、数据库连接串等）
- 生成 JSON 报告上传为 workflow artifact（保留 30 天）
- 发现密钥时 CI 会失败，阻止合并

**运行方式**: 零配置，开箱即用。Gitleaks 社区版免费。

---

## 六、总体评价

**安全等级**: 🟡 中等（存在高危问题但均可修复）

**优点**:
- 整体架构良好：分层清晰（controller → service → repository → model）
- 已使用 helmet、CORS 等安全中间件
- 错误处理统一通过 AppError，不向客户端泄露堆栈信息
- JWT 认证机制覆盖了大部分 API 接口
- Knex.js 参数化查询有效防止了 SQL 注入

**待改进**:
- 密钥管理需要加强：不应有任何硬编码退路密钥或默认凭据
- 授权校验不完整：仅做鉴权（你是谁），缺少授权（你能访问什么）
- 缺少速率限制等防护措施
- 需要制定 `.env` 文件管理策略，避免真实凭据提交至仓库

---

**最后更新**: 2026-05-10  
**审查人**: AI 辅助审查  
**确认人**: 蔡燚翔
