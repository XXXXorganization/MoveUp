# 安全审查与加固贡献说明

姓名：蔡燚翔  学号：2312190426  角色：后端  日期：2026-05-10

## 我完成的工作

### AI 安全审查（OWASP Top 10 视角）

- 审查了哪些文件/模块：

| 模块 | 文件 | 审查重点 |
|------|------|---------|
| 认证与鉴权 | `middleware/auth.ts`, `routes/*.ts` | JWT 密钥管理、接口鉴权覆盖 |
| 用户模块 | `modules/user/service.ts`, `controller.ts`, `model.ts`, `repository.ts` | 验证码安全、登录防护、密码存储 |
| 运动模块 | `modules/sport/controller.ts`, `service.ts`, `repository.ts`, `model.ts` | 越权访问、数据归属校验 |
| 社交模块 | `modules/social/service.ts`, `repository.ts`, `model.ts` | SQL 注入、好友关系校验 |
| AI 模块 | `modules/ai/service.ts`, `controller.ts`, `utils/llm.ts` | Prompt 注入、API Key 管理 |
| 基础设施 | `app.ts`, `config/database.ts`, `knexfile.js`, `.env`, `docker/.env` | 安全头、CORS、密钥泄露、依赖安全 |

- AI 发现的主要问题：

| 编号 | 漏洞名称 | OWASP 分类 | 危害等级 |
|------|---------|-----------|---------|
| FIND-01 | JWT 密钥硬编码退路值 `'your-secret-key'` | A02:2021 / A07:2021 | 🔴 高 |
| FIND-02 | 运动记录接口缺少所有权校验（11个接口） | A01:2021 访问控制失效 | 🔴 高 |
| FIND-03 | 真实 API Key 和数据库密码存于本地 `.env` | A02:2021 加密失效 | 🔴 高 |
| FIND-04 | 验证码通过 console.log 明文泄露 | A04:2021 不安全设计 | 🟡 中 |
| FIND-05 | SQL 注入风险评估（Knex 参数化查询） | A03:2021 注入 | 🟢 低（安全） |
| FIND-06 | 无登录暴力破解防护 | A07:2021 认证失效 | 🟡 中 |
| FIND-07 | CORS 允许所有来源 | A01:2021 访问控制失效 | 🟢 低 |
| FIND-08 | Docker Compose 中的 MinIO/Redis 弱口令 | A02:2021 加密失效 | 🟡 中 |

- 我修复了哪些问题：

| 编号 | 修复方式 | 修改文件 |
|------|---------|---------|
| FIND-01 | 移除 `\|\| 'your-secret-key'` 退路值，新增 `requireJwtSecret()` 启动时强制校验 | `middleware/auth.ts`, `app.ts` |
| FIND-02 | 新增 `SportService.verifyOwnership()` 方法，在 11 个 Controller 方法中调用 | `modules/sport/service.ts`, `modules/sport/controller.ts` |
| FIND-04 | 验证码日志脱敏输出，仅显示手机号首 3 位和末 2 位 | `modules/user/service.ts` |
| FIND-06 | 账号级登录锁：5 次失败 → 锁定 10 分钟；IP 级限频：登录 10次/15min | `modules/user/service.ts`, `middleware/rateLimiter.ts`, `routes/user.ts` |
| FIND-07 | CORS 改为白名单模式（`CORS_ORIGINS` 环境变量），限制 methods/headers | `app.ts` |

### 安全检查清单（认证与授权 / 注入防护 / 敏感信息 / 依赖安全）

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 密码存储：使用 bcrypt/argon2 哈希 | 不适用 | 本项目采用手机号+验证码登录，无传统密码 |
| JWT Token 有过期时间 | ✅ 通过 | `JWT_EXPIRES_IN` 配置，默认 7200 秒 |
| 所有需登录接口有权限校验 | ✅ 通过 | 6 个路由文件逐一核查，公开接口为合理设计 |
| 用户只能操作自己的数据 | ✅ 已修复 | Sport 模块已修复；Coaching/Social/Challenge 已有校验 |
| SQL 使用参数化查询 | ✅ 通过 | Knex.js 自动参数化绑定，无字符串拼接 |
| API Key/密码不硬编码 | ✅ 已修复 | JWT Secret 退路值已移除 |
| .env 已在 .gitignore | ✅ 通过 | 根目录 `.gitignore` 包含 `.env` |
| 依赖无高危漏洞 | ⚠️ 已记录 | axios@1.15.0 有 4 个 HIGH 漏洞，可通过升级至 1.15.2 修复 |

### CI 安全扫描

- 配置了哪些：

| 工作流 | 文件 | 功能 | 触发条件 |
|--------|------|------|---------|
| Gitleaks | `.github/workflows/security-scan.yml` | 扫描 Git 历史中的密钥/Token/密码 | Push/PR + 手动 |
| CodeQL | `.github/workflows/codeql.yml` | JavaScript/TypeScript 静态代码分析 | Push/PR + 每周一 8:00 |

- 扫描结果（本地验证）：
  - Gitleaks：已配置，首次运行后将检测是否有历史密钥混入
  - CodeQL：已配置，将在 PR 上自动分析并输出 SARIF 报告至 Security 面板

### 选做完成情况

| 项目 | 状态 | 说明 |
|------|------|------|
| 安全 HTTP 头（CSP/HSTS/X-Frame-Options） | ✅ 已完成 | `app.ts` helmet 配置 |
| CSRF 保护 | ✅ 已评估 | 移动端 JWT API 架构不适用，已写说明 |
| Rate Limiting | ✅ 已完成 | 全局 + 登录 + 短信 三层限频 |
| Prompt 注入防护 | ✅ 已完成 | AI 模块 7 种注入模式检测 + 数值校验 + 字符串净化 |
| 登录失败限频 | ✅ 已完成 | 账号级 5次→10分钟锁 + IP 级限频 |

## PR 链接
- PR #XX: https://github.com/XXXXorganization/MoveUp/pull/46

## 遇到的问题和解决

1. 问题：`auth.ts` 中 `process.env.JWT_SECRET || 'your-secret-key'` 的退路设计是为了本地开发便利，但该字符串是公开已知的弱密钥。如果生产环境忘记配置环境变量，攻击者可直接用此密钥伪造任意用户的 JWT Token

解决：新增 `requireJwtSecret()` 函数，在模块加载时校验环境变量是否存在，不存在则抛出 Fatal 错误阻止启动。同时在 `app.ts` 的依赖注入处也加入同样的校验，确保两个 JWT 签名入口都得到保护。这遵循 fail-fast 原则——宁可启动失败，也不静默降级到不安全状态。

2. 问题：Sport 模块的 Controller 中只做了鉴权（`authenticateToken` 中间件验证用户身份），但没有校验 `:recordId` 路径参数对应的记录是否属于当前用户。这是典型的 IDOR（Insecure Direct Object Reference）漏洞

解决：在 `SportService` 中新增 `verifyOwnership(recordId, userId)` 方法，先查记录是否存在（不存在返回 404 而非 403，避免信息泄露），再比对 `record.userId` 与当前用户是否一致（不一致返回 403）。在 Controller 层的 11 个 `:recordId` 接口中统一调用此方法。同时提取 `getUserId(req)` 私有方法减少 `req.user?.userId` 的重复检查。

3. 问题：登录接口同时缺少 IP 级别和账号级别的暴力破解防护。IP 限频能防止分布式攻击，但无法防护同一个 IP 下针对多个账号的尝试；账号锁能防护针对特定账号的攻击，但需要与 IP 限频互补

解决：采用双层防护——IP 层用 `express-rate-limit` 中间件限制登录接口每 IP 15 分钟 10 次；账号层在 `UserService.login()` 中维护 `loginFailures` Map，同一手机号连续 5 次验证码错误则锁定 10 分钟。两层同时生效，互不干扰。

4. 问题：AI 模块中 `summarizeSportRecord` 和 `summarizeHistory` 将用户传入的运动数据直接拼接到 LLM prompt 中，攻击者可通过构造特殊的 `date` 或 `periodLabel` 字段注入 system prompt 覆盖指令

解决：组合防护——(1) 对 `date` 和 `periodLabel` 字符串进行净化（移除不可见控制字符、截断至合理长度）；(2) 对净化后的字符串检测 7 种注入模式（system prompt override、role switching、`<|im_start|>` 等特殊标记）；(3) 对所有数值字段（distance、duration、calories、heartRate、pace）进行合理范围校验。检测到异常时抛出错误拒绝请求而非尝试修复。

5. 问题：`console.log` 输出完整验证码 `Verification code for ${phone} (${type}): ${code}`，可能被日志聚合系统（ELK/Loki）采集并被运维或开发人员查看

解决：改为 `phone.slice(0, 3) + '****' + phone.slice(-2)` 脱敏输出手机号，移除验证码明文。虽然验证码在内存 Map 中仅存活 5 分钟，但日志通常会保留数周甚至数月，必须同步防护。

## 心得体会

本次安全审查与加固工作让我对 Web 应用安全有了系统性的认识。首先，安全不是单一环节的事，而是贯穿认证（你是谁）、授权（你能做什么）、输入校验、输出编码、密钥管理、依赖管理等多个层次的纵深防御体系。任何一个层次的缺失都可能导致整个系统出现漏洞。

其次，框架和库的能力边界需要清晰认知。Knex.js 能自动防止 SQL 注入，但无法防止 IDOR（越权访问）这类业务逻辑层漏洞。后者需要开发者在每个数据访问点显式校验所有权——Sport 模块的 11 个接口就是典型例子，全部加上了 `verifyOwnership` 后才算安全。

再次，JWT 密钥的硬编码退路值是设计上的反模式。"方便开发"不应以牺牲安全为代价，正确的做法是在启动阶段快速失败（fail-fast）而非静默降级。同样，验证码明文日志虽然方便调试，但日志的留存周期远长于验证码的有效期，必须在源头控制。

此外，AI 模块的 Prompt 注入是新兴的攻击面。大模型调用链条中，用户数据 → prompt 拼接 → LLM 的每个环节都可能被注入。需要在数值校验（拒绝异常值）、字符串净化（移除控制字符）、模式检测（拦截已知注入语法）三个层面组合防护，且应在 controller 层（HTTP 入口）和 service 层（业务逻辑）双重校验。

最后，自动化安全扫描（Gitleaks + CodeQL）能将安全检查从一次性的人工审查转变为持续性的 CI 门禁。人工审查能发现设计层面的问题（如 IDOR、架构决策），而自动化扫描擅长发现已知模式的问题（如密钥泄露、漏洞代码模式），二者互补而非替代。

（Vibe Coding 场景下，如何平衡开发效率和安全？在 AI 辅助开发中，快速生成功能代码的同时，安全配置（Helmet/CORS/Rate Limit）应作为项目模板的一部分预设好，让 AI 在生成业务代码时自动继承安全基线。关键的安全决策（如认证策略、密钥管理、授权校验点）则需要人工设计后由 AI 按模式批量应用。）
