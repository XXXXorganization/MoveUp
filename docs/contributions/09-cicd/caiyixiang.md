# CI/CD 配置贡献说明

姓名：蔡燚翔  学号：2312190426  角色：后端  日期：2026-05-05

## 完成的工作

### 工作流相关
- [√] 参与编写 / 审查 `.github/workflows/ci.yml`
- [√] 配置 Codecov 覆盖率上传（backend flag）
- [√] 添加 README 状态徽章

### 代码适配
- [√] 本地测试命令与 CI 一致，无需额外配置
- [√] 代码通过 Lint 检查（ruff）
- [√] 核心覆盖率达标（> 80%）

### 可选项
- [√] 配置 Dependabot 自动更新依赖
- [ ] 集成 CodeRabbit AI 代码审查
- [ ] 使用 act 本地验证工作流

## PR 链接- PR #X: https://github.com/XXXXorganization/MoveUp/pull/42

## CI 运行链接- https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml

## 遇到的问题和解决
1.问题：ESLint 对测试文件报大量 no-undef 和 no-require-imports 错误（700+ 条）

解决：在 eslint.config.js 中为测试文件配置独立的环境变量（globals.jest、globals.node），并关闭测试文件内的 no-require-imports、no-explicit-any 和 no-unused-vars 规则，同时添加 dist/**、coverage/** 等编译产物目录到忽略列表，使测试文件与源代码的规则强度合理分离。

2.问题：src/config/database.ts 中 require('../../knexfile') 被 ESLint 的 no-require-imports 规则禁止，改为 import 后 TypeScript 报 ts(7016) 无法找到模块声明

解决：在 tsconfig.json 中配置 paths 别名 @knexfile，在 src/types/knexfile.d.ts 中声明模块类型，并在 jest.config.js 中添加 moduleNameMapper 映射别名到真实文件路径，确保 TypeScript 编译和 Jest 运行时都能正常解析。

3.问题：修改 tsconfig.json 添加路径别名后，Jest 测试报 Cannot find module '@knexfile'

解决：Jest 默认不识别 TypeScript 的 paths 别名，需在 jest.config.js 的 moduleNameMapper 中手动声明 '^@knexfile$': '<rootDir>/knexfile.js'，使 Jest 能够正确解析别名路径。

4.问题：(req as any).user 写法引发 no-explicit-any 错误，且全局替换 any 为 unknown 后引发大量类型兼容性问题

解决：创建 src/types/express.d.ts，通过 declare namespace Express 扩展 Request 接口，为 user 属性添加明确的类型声明，业务代码改为 req.user 直接访问，消除对 any 的依赖。

5.问题：Express 中间件中未使用的参数 error、next 引发 no-unused-vars 错误

解决：对 catch 块中未使用的 error 参数改为无参数的 catch 写法；对 Express 错误处理中间件中未使用的 next 参数直接在函数签名中移除。

## 心得体会
通过本次 CI/CD 配置与代码规范治理工作，我对前端/后端工程化实践有了更深入的理解。首先，ESLint 的规则配置不是越严格越好，而是需要根据代码目录的职责合理分层——测试文件可以适度放宽 any 和 require 的限制以提高编写效率，而核心源码则应保持严格以保障代码质量。其次，TypeScript 的路径别名虽然提升了代码可读性，但必须兼顾运行时（Jest）和编译时（tsc）两套模块解析机制的一致性，moduleNameMapper 的配置是连接二者的关键桥梁。此外，通过创建全局类型声明文件（*.d.ts）扩展第三方库的类型定义，既能消除 any 类型带来的安全隐患，又能获得完整的智能提示，是 TypeScript 项目中的最佳实践。最后，CI/CD 的自动化检查（Lint + Test + Coverage）能在代码合入前尽早发现问题，有效提升了团队的开发效率和代码可维护性。