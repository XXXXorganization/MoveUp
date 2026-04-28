# MoveUp

[![CI](https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml/badge.svg)](https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/XXXXorganization/MoveUp/graph/badge.svg)](https://codecov.io/gh/XXXXorganization/MoveUp)

这是一个关于运动跑步的软件
## 团队成员
| 姓名 | 学号 | 分工 |
|------|------|------|
| 徐康勒  | 2312190422  | 前端，编写了Android 原生界面开发、页面交互、菜单导航、跑步定位功能实现|
| 蔡燚翔  | 2312190426  | 后端，编写架构设计文档(含架构图)和数据库设计文档(含ER图) |
## 项目简介

Moveup是一款专为**跑步初学者和进阶爱好者**设计的移动端运动软件。其核心目标是利用移动端特性，通过“游戏化激励”与“科学化指导”，降低跑步门槛，帮助用户在趣味中养成运动习惯。

核心功能包括：精准的实时运动数据追踪（配速、心率、路线）、AI智能生成的个性化跑步计划、丰富的实景挑战任务，以及一个强调正向鼓励的社交社区。在这里，用户可以与水平相近的伙伴互相监督、打卡交流。Moveup致力于让每一次迈步都充满动力，成为用户从零开始、不断突破自我的专属移动伙伴。

## 技术栈（初步规划）

### 前端
**开发平台**：Android 原生应用
**开发工具**：Android Studio
**开发语言**：Java
**UI 框架**：Android 原生 XML 布局（ConstraintLayout / DrawerLayout）
**定位服务**：高德地图API的引用
**页面导航**：Activity 跳转 + 侧边抽屉菜单导航
**数据展示**：原生文本、视图组件实时显示运动数据

### 后端
- **应用框架**：Node.js + Express / Python + Django
- **API架构**：RESTful API + GraphQL（灵活的数据查询）
- **实时通信**：WebSocket / Socket.io（实时运动数据同步、社交互动）
- **云服务**：阿里云 / 腾讯云 / AWS
- **第三方集成**：微信/支付宝支付（会员服务）、社交媒体登录

### 数据库
- **主数据库**：PostgreSQL （用户信息、运动记录、社交关系）
- **数据库迁移脚本**：采用Migration来版本控制数据库结构变化
- **时序数据库**：InfluxDB （海量运动轨迹、心率等时序数据）
- **缓存**：Redis（会话管理、实时排行榜、热门动态）
- **对象存储**：OSS / S3（用户头像、跑步路线截图、社区图片）


### UI界面
Figma链接：https://www.figma.com/design/IKpsxQMrrc4alOJIQWFdDe/Move-Up?node-id=0-1&t=b55PvDZSBtFpf062-1

###  dbdiagram.io(ER图)
https://dbdiagram.io/d/69c23dbb78c6c4bc7a5191bf