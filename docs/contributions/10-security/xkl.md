# 安全审查贡献说明

**姓名:** [徐康勒]
**学号:** [2312190422]
**日期:** 2026-05-11

## 我完成的工作

### AI 前端代码安全审查
- **审查了哪些文件/模块:** - 核心认证模块：`Login.java`, `Register.java`
  - 语音交互模块：`VoiceCoachManager.java`, `Runing.java`
  - 模拟后端接口：`index.js`, `server.js`
- **AI 发现的主要问题:**
  1. **缺少限流处理导致潜在的滥用/暴力破解风险**：前端登录页面没有对后端返回的 429 (Too Many Requests) 状态码进行针对性处理。
  2. **系统资源未正确释放/申请导致的本地 DoS 风险**：`VoiceCoachManager` 在申请音频焦点时，针对 Android 8.0+ 系统缺少强制的 `OnAudioFocusChangeListener`，导致程序直接 Crash（崩溃退出）。
  3. **数据解析异常导致的应用崩溃**：网络请求返回的数据如果格式异常，直接解析会导致应用闪退，暴露内部运行状态。
- **我修复了哪些问题:**
  1. 在 `Login.java` 中完善了对 HTTP 状态码（特别是 `429` 账号锁定/限流 和 `401/400`）的捕获与处理，增强了认证接口防暴力破解的前端交互逻辑。
  2. 修复了 `VoiceCoachManager` 中的音频焦点监听漏洞，通过添加 `.setOnAudioFocusChangeListener(focusChange -> {})` 防止了应用级拒绝服务（Crash）。
  3. 在前后端的数据传输中，严格使用 `JSONObject` 构造请求体，避免了手动拼接字符串可能导致的 JSON 注入；并对解析过程增加了 `try-catch` 包裹。

### 安全检查清单
- [x] **认证与授权**
  - [x] JWT/Session: 前端登录成功后正确解析并安全存储 JWT Token 到 `SharedPreferences`。
  - [x] 接口鉴权: 规划了 `/v1/sport/*` 等路径的鉴权拦截。
  - [x] 越权访问: 前端请求中显式传递并校验 `user_id`，避免越权。
- [x] **注入防护**
  - [x] SQL: Mock环境目前使用安全的 JS 对象映射。
  - [x] XSS: Android 前端 `TextView` 天然防范 XSS，不涉及 Web 端的 `innerHTML` 问题。
- [x] **依赖安全**
  - [x] 运行依赖扫描,无高危漏洞(或已记录已知漏洞原因)



### 选做完成情况
- **登录失败限频:** 配合后端限频策略，在 `Login.java` 中实现了对 `HTTP 429`（同一 IP/账号 多次失败后锁定）的捕获，并友好提示用户“请求过于频繁或账号已锁定，请稍后再试”。
- **Prompt 注入防护:** 在 `/ai/chat` 接口中，对大模型（Qwen）的 Prompt 进行了严格的格式限制与角色设定（System Prompt），规定其只能作为“跑步助理教练”回答，并严格校验其输出的 JSON 格式计划，防止用户的闲聊或恶意注入破坏系统功能。

## PR 链接
- PR #X: https://github.com/XXXXorganization/MoveUp/pull/47

## 遇到的问题和解决
1. **问题:** Android 模拟器测试语音识别时，频繁抛出 `IllegalStateException` 导致 App 崩溃，且测试环境报网络或客户端错误。
   **解决:** 借助 AI 排查发现是 Android 8.0 之后 `setAcceptsDelayedFocusGain(true)` 强制要求监听器。补充空监听器修复了崩溃漏洞；网络错误则通过完善错误码翻译（细化 ERROR_CLIENT 和 ERROR_NETWORK）并在代码中增加对设备是否支持语音的防御性检查 (`isRecognitionAvailable`) 来避免应用发生不可控异常。
2. **问题:** 后端直接返回 429 状态码时，原有的网络请求逻辑没有正确捕获，导致统一走到 Exception。
   **解决:** 重构了 `HttpURLConnection` 的 `InputStream` 获取逻辑，针对 `>= 400` 的状态码读取 `ErrorStream`，精准识别限流拦截并给予安全提示。

## 心得体会
在 Vibe Coding（AI 辅助编程）场景下，开发效率确实得到了极大的提升，尤其是在快速搭建语音交互、定位追踪等复杂业务逻辑时。但是，AI 生成的初版代码往往侧重于“跑通”，容易忽略边界条件和安全防护（例如异常状态码捕获、资源释放、并发访问等）。
这要求我们在使用 AI 编程时，不能盲目 Copy-Paste，而必须加入**“安全审查的二次 Prompt”**环节。让 AI 扮演安全专家的角色，重新审视刚写完的代码。通过这次实践，我学会了如何引导 AI 发现潜在的拒绝服务（Crash）、限流绕过等问题，在“极速开发”和“防御性编程”之间找到了一个很好的平衡点。
