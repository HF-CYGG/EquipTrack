# EquipTrack Android 应用

这是 EquipTrack 物资管理系统的 Android 客户端，聚焦“物资台账、智能借还、审计追溯、权限与多部门”的核心能力，与 Web 版本保持一致并支持离线运行与后端对接。本文档重新梳理结构，便于快速上手、调试与集成。

## 简介

EquipTrack Android 通过 Jetpack Compose + MVVM 架构实现现代化移动体验，支持拍照存证、离线数据、本地与服务器双模式运行。应用可在未连接后端时依托本地数据库进行完整演示，接入后端后自动切换为真实数据模式，适合原型验证与生产部署。

## 快速开始

1. 使用 Android Studio 打开 `android` 目录并等待 Gradle 同步。
2. 在模拟器环境下，后端地址使用 `http://10.0.2.2:3000/`（映射宿主机 `localhost`）。
3. 连接真实设备时，将 BASE_URL 替换为局域网可达的服务器 IP（如 `http://192.168.1.100:3000/`）。
4. 启动应用，在设置页配置服务器地址（或开启本地调试模式），即可体验核心流程：浏览→借用→拍照存证→归还→历史追溯。
5. 如需查看后端运行状态与日志，可在后端仓库使用 `GET /health` 与实时日志追踪脚本（见服务器 README）。

## 技术栈

- 开发语言：Kotlin
- UI 框架：Jetpack Compose + Material 3
- 架构模式：MVVM + Repository Pattern
- 依赖注入：Hilt
- 本地数据库：Room（支持离线与数据持久化）
- 网络：Retrofit + OkHttp（支持分页与过滤接口对接）
- 异步与响应式：Kotlin Coroutines + Flow
- 图片加载：Coil
- 相机能力：CameraX（借出/归还拍照存证）

## 项目结构

```
app/src/main/java/com/equiptrack/android/
├── data/                    # 数据层
│   ├── local/              # 本地数据源（Room）
│   │   ├── dao/            # DAO 接口
│   │   ├── EquipTrackDatabase.kt
│   │   └── Converters.kt
│   ├── remote/             # 远程数据源（Retrofit）
│   │   └── api/            # API 接口定义
│   ├── repository/         # 仓库（统一数据读写入口）
│   └── model/              # 数据模型
├── di/                     # 依赖注入模块（Hilt）
├── ui/                     # UI 层
│   ├── auth/               # 认证与注册申请
│   ├── equipment/          # 物资列表/详情/编辑
│   ├── history/            # 借用与归还历史
│   ├── approval/           # 审批中心（预留/迭代中）
│   ├── profile/            # 个人中心与设置入口
│   ├── navigation/         # 导航管理
│   └── theme/              # 主题和样式
├── utils/                  # 工具与辅助
├── MainActivity.kt         # 主入口
└── EquipTrackApplication.kt # 应用类
```

## 核心功能

- 用户认证：登录、注册申请；会话管理与角色识别。
- 物资看板：卡片化展示图片、名称、类别、状态、库存；关键词搜索与多维筛选。
- 借用与归还：填写用途与预计归还时间；拍照存证；管理员强制归还（预留）。
- 历史追溯：时间轴/表格展示借还记录，支持筛选与导出（与后端对接后）。
- 审批中心：统一处理领用/归还/报修/报废（部分功能迭代中）。
- 设置与个性化：后端地址配置、本地调试开关、主题与视觉定制（部分在 Web 端）。

## 配置说明

### 网络配置（BASE_URL）

在网络模块中设置后端地址（示例）：

```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/" // 模拟器映射宿主机
// 真机示例："http://192.168.1.100:3000/"
```

同时可在应用设置页（ServerConfigScreen）填写或修改服务地址；当本地调试/服务器地址变化时，审批与数据模块会自动触发重同步（参考 `ApprovalViewModel` 中的设置监听）。

### 数据库配置（Room）

应用使用 Room 本地数据库支持离线，首次启动自动创建与迁移。推荐在弱网/无网场景下进行盘点与借还记录的临时保存，网络恢复后由仓库层统一推送同步。

## 构建与运行

1. 克隆项目并打开 `android` 目录。
2. 等待 Gradle 同步与依赖下载完成。
3. 启动模拟器或连接真机。
4. 配置 BASE_URL 或在设置页填写服务器地址。
5. 运行应用并按核心流程进行体验与验证。

## 调试与日志

- 网络调试：建议在 Debug 构建开启 OkHttp 拦截器日志（如已配置）；遇到连通性问题，优先检查 BASE_URL、端口与防火墙策略。
- 后端联动：服务器提供 `GET /health` 健康检查与结构化日志（`server/logs/server.log`）；可通过专用脚本实时追踪日志，便于联调与排障。

## 常见问题（FAQ）

- 模拟器无法访问后端：请使用 `10.0.2.2` 替代 `localhost`，并确保后端监听在可达网段；必要时放行防火墙规则。
- 图片/拍照权限：Android 13 及以上使用 `READ_MEDIA_IMAGES`，更低版本使用 `READ_EXTERNAL_STORAGE`；首次拍照需授权相机权限。
- 数据不同步：检查设置页服务器地址与本地调试开关；网络恢复后等待仓库层自动同步或手动刷新。

## 路线图（Roadmap）

- 已完成：认证、物资管理、借还与拍照、历史记录、状态与逾期检测、基础数据层与 UI 架构。
- 迭代中：部门管理、用户管理、注册审批、通知推送、数据同步优化、审批流程可视化与配置化。

## 贡献指南

1. Fork 仓库
2. 创建功能分支（`git checkout -b feature/YourFeature`）
3. 提交更改（`git commit -m 'feat: YourFeature'`）
4. 推送分支（`git push origin feature/YourFeature`）
5. 创建 Pull Request 并说明变更点与影响范围

## 许可证

本项目采用 MIT 许可证。

## 联系方式

- 项目 Issues: [GitHub Issues](https://github.com/your-repo/issues)
- 邮箱: your-email@example.com

---

提示：若需联调后端，请参阅服务器 README（`server/README.md`）中的一键启动脚本与日志监控说明。