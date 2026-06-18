# 监控与可观测性贡献说明

**姓名**：蔡燚翔
**学号**：2312190426
**角色**：后端
**日期**：2026-06-02

---

## 我完成的工作

### 1. 健康检查端点

- [√] `/health` 端点实现
- [√] Docker HEALTHCHECK 配置
- [√] Render 健康探活对接

**实现**（`backend/src/app.ts`）：

```typescript
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});
```

| 消费方 | 用途 | 路径 |
|--------|------|------|
| Render | 自动探活，异常重启 | `GET /health` |
| Docker | HEALTHCHECK 指令 | `wget http://localhost:3000/health` |
| 开发者 | 手动验证服务状态 | `curl /health` |

Docker HEALTHCHECK 配置（`backend/Dockerfile`）：

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000/health', ...)"
```

### 2. 结构化错误响应

- [] 统一错误响应格式 `{ code, message, data }`
- [] 全局错误处理中间件
- [] 自定义 `AppError` 类（含 HTTP 状态码）

**实现文件**：

| 文件 | 功能 |
|------|------|
| `src/utils/errors.ts` | `AppError` 自定义错误类 |
| `src/middleware/errorHandler.ts` | Express 全局错误处理中间件 |
| 各 Controller | `try { ... } catch (e) { next(e); }` 统一传递错误到中间件 |

**错误响应示例**：

```json
// 401 未认证
{ "code": 401, "message": "访问令牌缺失", "data": null }

// 400 业务错误
{ "code": 400, "message": "验证码无效或已过期", "data": null }
```

### 3. Docker 日志管理

- [x] Docker 日志驱动配置（`json-file`）
- [x] 日志轮转策略（大小限制 + 文件数限制）
- [x] 生产环境 Compose 全服务日志配置

**生产 Compose 配置**（`compose.prod.yaml`）：

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "50m"   # 单文件最大 50MB
    max-file: "5"      # 保留 5 个轮转文件
```

各服务限制：

| 服务 | max-size | max-file |
|------|----------|----------|
| Backend | 50m | 5 |
| PostgreSQL | 50m | 3 |

### 4. CI 覆盖率监控

- [x] Codecov 覆盖率上报（backend flag）
- [x] GitHub Actions CI 自动生成 `coverage.xml`
- [x] README 覆盖率徽章

**工作流**：`npm test` → 生成 `coverage.xml` → `codecov-action` 上传

**Codecov 面板**：实时追踪后端覆盖率趋势（当前 > 80%），PR 自动评论覆盖率变化。

### 5. 启动时健康校验

- [x] `server.ts` 启动前执行 `db.raw('SELECT 1')` 验证数据库连接
- [x] 数据库不可用则服务拒绝启动（fail-fast）
- [x] 自动运行 `db.migrate.latest()` 确保数据库结构最新

---

## PR 链接

- PR #XX: https://github.com/XXXXorganization/MoveUp/pull/XX

---

## 架构说明——后端可观测性三支柱

```
┌──────────────────────────────────────────────────┐
│                  后端可观测性                       │
├────────────────┬───────────────┬──────────────────┤
│  日志 (Logs)    │ 指标 (Metrics) │ 健康检查 (Health)  │
├────────────────┼───────────────┼──────────────────┤
│ 结构化错误响应    │ Codecov 覆盖率  │ GET /health      │
│ Docker 日志轮转  │               │ Docker HEALTHCHECK│
│ console 脱敏    │               │ Render 探活       │
│                │               │ 数据库连通性校验    │
└────────────────┴───────────────┴──────────────────┘
```

---

## 遇到的问题和解决

1. **问题**：Codecov 本地路径与 CI 环境路径不一致，覆盖率报告无法正确映射到源文件

   **解决**：在 `jest.config.js` 中配置 `coverageReporters: ['json', 'lcov', 'text', 'clover']`，确保 CI 环境生成的 `coverage.xml` 使用相对路径；在 Codecov 面板验证文件路径映射正确。

2. **问题**：Docker 容器崩溃后无日志留存，无法排查原因

   **解决**：Docker Compose 中配置 `json-file` 日志驱动 + 轮转策略，即使容器重启历史日志也不丢失；通过 `docker compose logs <service>` 查看最近日志。

3. **问题**：Express 未捕获的异常导致进程退出，Render 频繁重启

   **解决**：在 `app.ts` 末尾注册全局错误处理中间件 `app.use(errorHandler)`，确保所有未捕获异常被兜底为 500 响应，避免进程崩溃。

4. **问题**：健康检查端点仅返回静态文本，无法反映数据库连接状态

   **解决**：在 `server.ts` 启动流程中先执行 `db.raw('SELECT 1')` 验证数据库连接成功后，再启动 HTTP 服务。如果数据库不可用，服务直接退出（fail-fast），避免 Render 路由流量到不健康的实例。

---

## 心得体会

通过本次监控配置实践，我认识到可观测性不是运维的"附加品"，而是系统设计的"必需品"。一个无法快速定位问题的系统，即使功能再完善，也难以在生产环境中稳定运行。

后端的三支柱——健康检查（知道服务是否活着）、结构化错误响应（知道出了什么问题）、日志管理（追溯问题根因）——构成了最基本的运维闭环。`/health` 端点的价值不仅在于告诉 Render 容器是否健康，更在于它连接了部署平台与业务服务：平台通过探活决定是否路由流量，开发者通过响应判断数据库是否连通。

Docker 日志轮转的配置看似琐碎，但在实际运维中极为关键——一个忘记配置轮转的容器可能在几周内撑满服务器磁盘，导致所有服务不可用。提前预设资源限制和日志限制，是容器化部署的安全底线。

Codecov 覆盖率监控将"代码质量"从主观感受变成了客观数字，虽然覆盖率不等于测试质量，但它是防止测试退化的有效门禁——新提交导致覆盖率骤降时，团队能立刻发现并修复。

最后，fail-fast 是最简但最有效的监控策略——在启动阶段发现问题并拒绝启动，远比在运行中返回错误的体验更好，也远比静默失败更难排查。
