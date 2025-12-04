# EquipTrack - 企业级物资追踪与管理系统

EquipTrack 是一套现代化的全栈物资管理解决方案，专为企业和组织设计，旨在解决设备资产管理混乱、借还记录不透明、审批流程繁琐等痛点。

本系统由两部分组成：
1.  **Android 移动客户端**：为一线员工和管理员提供便捷的操作界面。
2.  **Node.js 后端服务器**：提供稳定、高效的数据接口和业务逻辑处理。

---

## 📂 目录结构深度解析

```text
studio-main/
├── android/              # Android 客户端原生工程
│   ├── app/              # 核心应用模块
│   └── build.gradle      # Gradle 构建配置
├── server/               # 后端服务器工程
│   ├── src/              # TypeScript 源代码
│   │   ├── config/       # 环境变量与系统配置
│   │   ├── middlewares/  # 认证、上传、错误处理中间件
│   │   ├── models/       # TypeScript 类型定义与数据模型
│   │   ├── routes/       # API 路由分发
│   │   ├── services/     # 核心业务逻辑 (Service Layer)
│   │   ├── utils/        # 工具函数 (数据持久化等)
│   │   ├── app.ts        # Express 应用实例配置
│   │   └── index.ts      # 服务器启动入口
│   ├── data/             # (运行时生成) 本地 JSON 数据库文件
│   ├── uploads/          # (运行时生成) 图片上传存储目录
│   ├── Dockerfile        # Docker 容器构建描述文件
│   └── package.json      # Node.js 依赖管理
└── API_SPEC.md           # 详细的 RESTful API 接口文档
```

---

## ✨ 核心功能模块

### 1. � 用户与权限管理 (User & Auth)
*   **多角色体系**：支持三种角色权限：
    *   **超级管理员 (Super Admin)**：拥有系统最高权限，可管理所有部门、审批所有注册。
    *   **部门管理员 (Admin)**：管理本部门物资和人员，审批本部门借用。
    *   **普通用户 (User)**：仅可查询物资、发起借用申请、查看个人记录。
*   **安全认证**：基于 JWT (JSON Web Token) 的无状态身份验证。
*   **注册审批流**：新用户注册需填写邀请码，并经由管理员审批通过后方可登录。

### 2. 📦 物资全生命周期管理 (Equipment)
*   **物资录入**：支持图片上传、详细参数记录、分类归档。
*   **状态追踪**：实时监控物资状态（`可用`、`借出`、`维修中`、`报废`）。
*   **分类管理**：自定义物资类别（如“IT设备”、“办公用品”等），支持颜色标签。
*   **数据持久化**：所有数据均通过 JSON 文件本地持久化，无需安装额外数据库。

### 3. 🔄 借还与审批流程 (Borrow & Approval)
*   **借用申请**：用户发起借用，填写用途和归还时间。
*   **多级审批**：根据用户角色自动路由审批任务（部门内审批或跨部门审批）。
*   **归还流程**：用户归还物资，管理员确认入库，形成闭环。
*   **历史记录**：完整的借还操作日志，随时可追溯。

### 4. 🏢 组织架构管理 (Department)
*   **部门维护**：灵活创建和编辑部门信息。
*   **人员关联**：用户与部门深度绑定，实现基于部门的数据隔离与权限控制。

---

## 🚀 后端部署指南 (Server Deployment)

### 环境准备
*   **运行环境**: Node.js v18+
*   **包管理器**: npm 或 yarn

### 方式一：本地开发/部署

1.  **进入服务器目录**
    ```bash
    cd server
    ```

2.  **安装依赖**
    ```bash
    npm install
    ```
    *(国内用户推荐使用淘宝镜像: `npm config set registry https://registry.npmmirror.com`)*

3.  **配置环境变量** (可选)
    复制 `.env.example` 为 `.env` 并修改配置：
    ```bash
    cp .env.example .env
    ```

4.  **启动服务**
    *   **开发模式 (热重载)**:
        ```bash
        npm run dev
        ```
    *   **生产模式**:
        ```bash
        npm run build
        npm start
        ```

### 方式二：Docker 容器化部署 (强烈推荐)

本项目已深度优化 Docker 支持，集成国内镜像源加速构建。

1.  **构建镜像**
    ```bash
    # 在 server 目录下执行
    docker build -t equiptrack-server .
    ```

2.  **启动容器** (包含数据持久化挂载)
    ```bash
    docker run -d \
      --name equiptrack-server \
      --restart always \
      -p 3000:3000 \
      -e TZ=Asia/Shanghai \
      -v /opt/equiptrack/data:/app/data \
      -v /opt/equiptrack/uploads:/app/uploads \
      equiptrack-server
    ```

    *   **端口映射**: 将容器 3000 端口映射到主机 3000 端口。
    *   **数据挂载**: 确保重启容器后数据库和图片不丢失。
    *   **时区设置**: 设置为中国标准时间。

---

## 📱 Android 客户端开发指南

1.  **开发工具**: 推荐使用最新版 Android Studio (Koala 或更高)。
2.  **编译步骤**:
    *   打开 Android Studio，选择 `Import Project` 并指向 `android` 目录。
    *   等待 Gradle Sync 完成。
    *   连接真机或启动模拟器。
    *   点击 Run 按钮 (绿色三角形) 即可安装运行。
3.  **API 地址配置**:
    *   请确保 Android 设备与后端服务器在同一局域网，或后端已部署到公网。
    *   在 App 代码配置中修改 `BASE_URL` 指向你的服务器地址。

---

## 🤝 贡献与支持

欢迎提交 Issue 或 Pull Request 来改进本项目。

*   **API 文档**: 详见 [API_SPEC.md](./API_SPEC.md)
*   **维护者**: EquipTrack Team
