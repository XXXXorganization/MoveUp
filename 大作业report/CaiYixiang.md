# 蔡燚翔（cyx）MoveUp 项目综合贡献说明

# 「本文本由 AI 辅助生成，经人工审核修改」

**姓名**：蔡燚翔  
**学号**：2312190426  
**角色**：后端架构与开发（兼数据库设计、Docker 容器化、CI/CD、安全审查、云端部署）  
**项目**：MoveUp 跑步运动 App  
**汇总日期**：2026-06-15  

---

## 一、贡献总览

本人主要负责 MoveUp 项目的 **后端 Node.js/Express/TypeScript 全链路开发**，并承担数据库设计与迁移、Docker 容器化、CI/CD 流水线、后端安全审查、Render 云端部署等工作。以下按课程章节汇总各模块贡献。

| 章节 | 主题 | 完成度 | 贡献文档 |
|------|------|--------|---------|
| 02 | UI 设计规范（配色/字体） | ✅ 完成 | `02-ui/caiyixiang.md` |
| 03 | 软件架构（后端） | ✅ 完成 | `03-architecture/caiyixiang.md` |
| 04 | API 设计与 OpenAPI 文档 | ✅ 完成 | `04-api/caiyixiang.md` |
| 06 | 后端核心实现 | ✅ 完成 | `06-backend/caiyixiang.md` |
| 07 | AI 数据总结功能 | ✅ 完成 | `07-ai/caiyixiang.md` |
| 08 | 软件测试（后端） | ✅ 完成 | `08-testing/caiyixiang.md` |
| 09 | CI/CD 与代码规范 | ✅ 完成 | `09-cicd/caiyixiang.md` |
| 10 | 安全审查与加固（后端） | ✅ 完成 | `10-security/caiyixiang.md` |
| 11 | Docker 容器化部署 | ✅ 完成 | `11-docker/caiyixiang.md` |
| 12 | 云服务部署（Render） | ✅ 完成 | `12-cloud/caiyixiang.md` |
| 13 | 监控配置部署 | ✅ 完成 | `13-monitoring/caiyixiang.md` |

---

## 二、各模块详细贡献

### 2.1 数据库设计与迁移（2026-03-24）

**完成工作**：

1. **29 张表完整设计**：从用户核心 → 运动记录 → GPS 轨迹 → 社交关系 → 社团系统 → 训练计划 → 挑战激励 → 会员积分，分层扩展
2. **ER 图绘制**：通过 dbdiagram.io 在线工具生成，链接见参考文献
3. **16 个 Knex 迁移脚本**：版本化管理所有结构变更，每个文件含 `up` / `down`，可正向执行可回滚
4. **设计规范落地**：所有主键 UUID、时间字段 TIMESTAMPTZ、外键按业务选择 CASCADE 或 RESTRICT、高频查询字段建索引
5. **JSONB 字段应用**：GPS 轨迹点批量存入 `sport_records`；社团动态图片 `images` 数组直接存 PostgreSQL JSONB，无需单独 OSS

**PR**：#8 — https://github.com/XXXXorganization/MoveUp/pull/8

---

### 2.2 API 设计与实现（2026-03-31 ~ 2026-04-14）

**API 设计**：

- 统一响应结构 `{ code: 200, message: "success", data: {...} }`
- RESTful 资源命名，统一 `/v1` 前缀
- 57 个端点覆盖 7 大业务模块
- OpenAPI 文档（`docs/api.yaml`）供 Apifox 导入与 Mock

**后端核心实现**（`backend/src/`）：

| 模块 | 文件数 | 功能 |
|------|--------|------|
| user | 5 个（controller/service/repository/model/types） | 手机号注册/登录、JWT 签发、验证码发送、资料管理 |
| sport | 5 个 | 运动开始/实时更新/结束、GPS 批量上传、分段配速分析、卡路里计算 |
| club | 5 个 | 社团 CRUD、加入/退出、成员管理、动态发布与展示 |
| social | 5 个 | 好友系统、社区动态流、点赞评论、排行榜 |
| coaching | 5 个 | AI 推荐计划、今日任务、进度管理、语音指导 |
| challenge | 5 个 | 任务系统、成就徽章、挑战排行、会员积分 |
| ai | 3 个 | DeepSeek 运动总结、历史汇总分析 |

**分层架构**：每个模块遵循 controller → service → repository → model 四层，`app.ts` 中手动依赖注入组装。

**Android 兼容层**：`routes/compatibility.ts` 将旧 Android 客户端的 `/runs/*`、`/plan/*`、`/ai/chat` 路径映射到新后端模块，避免前端大规模改动。

**PR**：#11、#15 — https://github.com/XXXXorganization/MoveUp/pull/11, #15

---

### 2.3 AI 功能集成——后端侧（2026-04-19）

**功能类型**：运动数据 AI 总结

**使用模型**：DeepSeek（`deepseek-chat`）

**实现内容**：

| 层级 | 实现 |
|------|------|
| LLM 客户端 | `src/utils/llm.ts` 封装 DeepSeek / Qwen API 调用，含超时保护与 JSON 容错 |
| AI Service | `summarizeSportRecord`（单次运动分析）、`summarizeHistory`（历史汇总） |
| Prompt 设计 | System Prompt 限定角色为运动健康教练 + 输出 JSON 格式约束 + 字数/条数限制 |
| 容错处理 | JSON 正则提取（兼容 markdown 代码块）、解析失败异常抛出 |

**PR**：#17 — https://github.com/XXXXorganization/MoveUp/pull/17

---

### 2.4 软件测试——后端（2026-04-28）

**测试框架**：Jest + Supertest + PostgreSQL（CI 服务容器）

**测试文件**（17 个测试套件，300+ 用例）：

| 类型 | 文件 |
|------|------|
| API 集成测试 | `user.test.ts`, `sport.test.ts`, `club.test.ts`, `social.test.ts`, `coaching.test.ts`, `challenge.test.ts`, `ai.test.ts`, `compatibility.test.ts` |
| 单元测试 | `unit/user.service.test.ts`, `unit/sport.service.test.ts`, `unit/coaching.service.test.ts`, `unit/challenge.service.test.ts`, `unit/social.service.test.ts`, `unit/ai.service.test.ts`, `unit/llm.test.ts`, `unit/utils.test.ts`, `unit/user.password.test.ts` |
| 测试辅助 | `helpers/auth-helper.ts`（JWT Token 生成） |

**测试统计**：

- API 正常流程测试：> 50 个
- 边界/异常情况测试：> 30 个
- 鉴权验证测试：Token 缺失、Token 无效、Token 过期
- **后端核心模块覆盖率 > 80%**（Codecov 上报）

**CI 流程**：GitHub Actions 启动 PostgreSQL 15 服务容器 → 运行迁移 → 执行 `npm test` → 生成 `coverage.xml` → 上传 Codecov。

**PR**：#19 — https://github.com/XXXXorganization/MoveUp/pull/19

---

### 2.5 CI/CD 配置与代码规范治理（2026-05-04）

**完成工作**：

- [√] 编写 `.github/workflows/ci.yml`（后端测试任务 + PostgreSQL 服务容器）
- [√] 配置 Codecov 覆盖率上传（backend flag）
- [√] 添加 README 状态徽章（CI / Codecov）
- [√] 配置 Dependabot 自动更新依赖
- [√] ESLint 配置与 700+ 条 lint 错误修复（测试文件规则分层、路径别名声明、Express 类型扩展）

**核心问题与解决**：

| 问题 | 解决方案 |
|------|---------|
| ESLint 对测试文件报 700+ 条错误 | 为测试文件配置独立 globals 与规则 |
| `require('@knexfile')` 编译后运行时无法解析 | `database.ts` 改为直接读取环境变量构建配置 |
| `(req as any).user` 引发 no-explicit-any | 创建 `src/types/express.d.ts` 扩展 Request 接口 |
| Jest 不识别 TypeScript paths 别名 | `jest.config.js` 中添加 `moduleNameMapper` |

**PR**：#42 — https://github.com/XXXXorganization/MoveUp/pull/42  
**CI 链接**：https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml

---

### 2.6 安全审查与加固——后端（2026-05-10）

**审查范围**：认证鉴权、用户模块、运动模块、社交模块、AI 模块、基础设施（6 大领域）

**发现并修复的主要问题**：

| 编号 | 漏洞 | 危害 | 修复方案 |
|------|------|------|---------|
| FIND-01 | JWT 弱密钥退路 `'your-secret-key'` | 🔴 高 | 移除退路值，启动时强制校验环境变量 |
| FIND-02 | 运动记录 11 个接口缺少所有权校验（IDOR） | 🔴 高 | 新增 `verifyOwnership()` 方法统一校验 |
| FIND-03 | API Key 存于本地 .env | 🔴 高 | 确认 .gitignore 已保护；Render 使用环境变量面板 |
| FIND-04 | 验证码通过 console.log 明文泄露 | 🟡 中 | 手机号脱敏 + 移除验证码明文输出 |
| FIND-06 | 登录接口无暴力破解防护 | 🟡 中 | IP 级限频（10次/15min）+ 账号锁（5次→10min） |
| FIND-07 | CORS 允许所有来源 | 🟢 低 | 改为环境变量白名单模式 |

**安全扫描配置**：

| 工具 | 用途 | 触发 |
|------|------|------|
| Gitleaks | 扫描 Git 历史密钥 | Push/PR |
| CodeQL | JavaScript/TS 静态分析 | Push/PR + 每周一 |
| Trivy | Docker 镜像安全扫描 | docker-publish workflow |

**安全检查清单全部通过**：JWT 过期机制、bcrypt 密码哈希、Knex 参数化查询防 SQL 注入、helmet 安全头、Rate Limiting 三层限频。

**PR**：#46 — https://github.com/XXXXorganization/MoveUp/pull/46

---

### 2.7 Docker 容器化与 Render 云端部署（2026-05-19 ~ 2026-06-02）

#### Docker 容器化

**完成工作**：

| 类别 | 内容 |
|------|------|
| Dockerfile | 多阶段构建（构建→运行）、dumb-init 信号处理、非 root 用户（nodejs:nodejs）、HEALTHCHECK |
| Docker Compose | 开发（`docker-compose.yml`）与生产（`compose.prod.yaml`）双环境；资源限制（CPU/内存上限）、cap_drop 最小化权限、只读文件系统、日志轮转 |
| .env 管理 | `.env.example` / `.env.prod.example` 模板 + `${VAR:?required}` 强制校验 |
| GHCR 发布 | `docker-publish.yml`：Build → Trivy 扫描 → Push to GHCR |

**核心问题与解决**（7 个累积配置问题）：

| 问题 | 解决方案 |
|------|---------|
| 国内镜像源 500，无法拉取 node:18-alpine | Docker Engine 配置 `docker.1ms.run` 等国内镜像 |
| Windows 端口 3000 被系统保留 | 改用 `BACKEND_PORT=3500` |
| .dockerignore 中 `*.ts` 排除导致 tsc 无源文件 | 移除 `*.ts`，改为排除 `dist` |
| TS 路径别名 `@knexfile` 运行时解析失败 | 源码层面改为环境变量构建配置 |
| PostgreSQL `cap_drop: ALL` 导致 chmod 失败 | PG 仅保留 `no-new-privileges` |
| express-rate-limit 未写入 lock 文件 | 宿主机 `npm install --save` 同步 lock |
| 容器反复重启，每次报错不同 | 逐轮排查 7 个问题，五容器全部 Healthy |

#### Render 云端部署

**部署架构**：

```
GitHub push → Render Web Service（Node.js 18）
                  ├── Express Backend（端口 3000）
                  └── Render PostgreSQL（托管数据库）
```

**配置项**：

| 环境变量 | 说明 |
|---------|------|
| `DATABASE_URL` | Render PostgreSQL 连接串（自动注入） |
| `JWT_SECRET` | JWT 签名密钥 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `QWEN_API_KEY` | 通义千问 API 密钥 |

**线上地址**：`https://moveup-v7mf.onrender.com`（自动 HTTPS、健康检查 `/health`）

---

### 2.8 UI 设计规范（2026-03-16）

**完成工作**：

- [x] 配色方案制定（主色 #FFFFFF、辅助色 #B4E6D9/#FFC7B3、强调色 #FF6B4A/#4A7AFF）
- [x] 字体选择（标题 Fugaz One、正文 Poppins/Outfit）
- [x] 设计说明文档 `design-spec.md` 全部内容
- [x] 暗色模式 UI 参考方案

---

## 三、PR 汇总

| PR | 主题 | 链接 |
|----|------|------|
| #8 | 数据库设计与迁移脚本 | https://github.com/XXXXorganization/MoveUp/pull/8 |
| #11 | API 设计与 OpenAPI 文档 | https://github.com/XXXXorganization/MoveUp/pull/11 |
| #15 | 后端核心 API 实现 | https://github.com/XXXXorganization/MoveUp/pull/15 |
| #17 | AI 数据总结功能 | https://github.com/XXXXorganization/MoveUp/pull/17 |
| #19 | 后端测试（300+ 用例） | https://github.com/XXXXorganization/MoveUp/pull/19 |
| #42 | CI/CD 与代码规范治理 | https://github.com/XXXXorganization/MoveUp/pull/42 |
| #46 | 后端安全审查与加固 | https://github.com/XXXXorganization/MoveUp/pull/46 |
| #XX | Docker 容器化 + Render 部署 | https://github.com/XXXXorganization/MoveUp/pull/XX |

---

## 四、核心代码产出清单

### 后端源文件（`backend/src/`）

**业务模块**（7 个模块，每个含 controller/service/repository/model/types 共 5 文件）：

| 模块 | 路径 | 核心功能 |
|------|------|---------|
| user | `modules/user/` | 注册登录、JWT 签发、资料管理 |
| sport | `modules/sport/` | 运动记录生命周期、GPS 批量上传、配速分析 |
| club | `modules/club/` | 社团 CRUD、成员管理、动态系统 |
| social | `modules/social/` | 好友系统、社区动态、排行榜 |
| coaching | `modules/coaching/` | 训练计划、今日任务、语音指导 |
| challenge | `modules/challenge/` | 任务、徽章、挑战、会员积分 |
| ai | `modules/ai/` | DeepSeek 运动总结 |

**基础设施**：

| 文件 | 功能 |
|------|------|
| `app.ts` | Express 入口 + 依赖注入 |
| `server.ts` | HTTP 启动 + 自动迁移 |
| `config/database.ts` | Knex 数据库连接 |
| `middleware/auth.ts` | JWT 认证中间件 |
| `middleware/errorHandler.ts` | 全局错误处理 |
| `utils/errors.ts` | AppError 自定义错误类 |
| `utils/llm.ts` | LLM 客户端（DeepSeek + Qwen） |
| `routes/*.ts` | 8 组路由定义（含 compatibility.ts） |

**测试**（`backend/tests/`）：17 个测试套件，300+ 用例

**迁移**（`backend/migrations/`）：16 个 Knex 迁移脚本

### 工程化配置

- `.github/workflows/ci.yml`（后端测试任务 + PostgreSQL 服务容器）
- `.github/workflows/docker-publish.yml`（镜像构建 + Trivy 扫描 + GHCR 推送）
- `.github/workflows/security-scan.yml`（Gitleaks 密钥扫描）
- `.github/workflows/codeql.yml`（CodeQL 静态分析）
- `backend/docker/compose.prod.yaml`（生产 Docker Compose）
- `backend/docker/docker-compose.yml`（开发 Docker Compose）
- `backend/Dockerfile`（多阶段构建）
- `backend/eslint.config.mts`（ESLint 配置）
- `backend/jest.config.js`（Jest + moduleNameMapper）
- `backend/tsconfig.json`（TypeScript 编译配置）

---

## 五、问题与解决——综合归纳

| 类别 | 代表性问题 | 解决思路 |
|------|-----------|---------|
| 数据库 | 表结构迭代管理混乱 | Knex 迁移脚本版本化管理，每变更一个文件 |
| API 设计 | 不了解 OpenAPI 规范 | AI 辅助了解 + Apifox 实操学习 |
| Docker | 7 个累积配置问题致容器反复重启 | `docker compose logs` 逐轮排查：镜像源、端口、.dockerignore、TS 别名、cap_drop、lock 文件、CMD 路径 |
| TypeScript | 路径别名 `@knexfile` 运行时解析失败 | 源码层面避开别名，直接读取环境变量构建配置 |
| ESLint | 700+ 条 lint 错误 | 测试文件规则分层 + Express Request 类型扩展 + moduleNameMapper |
| 安全 | 11 个运动接口存在 IDOR 漏洞 | 新增 `verifyOwnership()` 统一校验资源所有权 |
| 安全 | JWT 弱密钥退路值可被攻击者利用 | fail-fast 原则——启动时强制校验环境变量 |
| AI | DeepSeek API Token 消耗大 | System Prompt 优化角色设定与输出字数限制 |
| CI/CD | Codecov 路径不匹配 | codecov.yml 配置 flags 与路径映射 |
| Render | 免费套餐 15 分钟休眠 | 演示前 ping `/health` 预热 |

---

## 六、技术收获与心得

### 6.1 后端架构

- 掌握 Node.js/TypeScript 分层架构设计：controller → service → repository → model，理解每层职责边界与依赖注入
- 熟悉 Express 中间件模式（auth、errorHandler、rateLimiter），能灵活组合实现认证、错误兜底、限流
- 深入理解 RESTful API 设计原则：资源命名、HTTP 动词语义、统一响应格式、版本管理

### 6.2 数据库工程

- 掌握 Knex.js 查询构建器 + 迁移脚本的工作流：参数化查询防注入、种子数据、可回滚变更
- 理解 PostgreSQL JSONB 字段的适用场景——GPS 轨迹序列与动态图片数组无需单独 OSS，简化了 MVP 阶段的部署拓扑
- 学会索引设计：在高频查询字段（`user_id`、`start_time`）建索引以提升性能

### 6.3 Docker 与 DevOps

- 多阶段构建（build → production）实现镜像瘦身
- Docker Compose 双环境设计（dev vs prod）：环境差异全部在 compose 文件层面体现，Dockerfile 复用
- `docker compose logs` 逐轮排查是解决容器反复重启的核心方法论

### 6.4 安全工程

- 理解 OWASP Top 10 的系统性视角：安全不是单一环节，而是认证、授权、校验、加密、密钥管理多层纵深防御
- 掌握了 IDOR 漏洞的发现与修复模式：在每个数据访问点显式校验所有权
- AI Prompt 注入是新兴攻击面，需数值校验 + 字符串净化 + 模式检测三层组合防护

### 6.5 总体感悟

MoveUp 项目中，我从零开始搭建了一个完整的 Node.js/Express/TypeScript 后端系统，涵盖数据库设计 → API 实现 → 测试 → CI/CD → Docker 容器化 → 安全审查 → 云端部署的全流程。最大的体会是：**分层架构降低复杂度，迁移脚本管理数据库变更，Docker 保证环境一致性，安全审查为代码兜底**。

---

## 七、Git 协作数据

| 成员标识 | 提交次数（约） |
|---------|--------------|
| 蔡燚翔 | 321 |

