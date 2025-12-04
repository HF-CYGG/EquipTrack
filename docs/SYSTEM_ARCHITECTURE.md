系统完整架构（EquipTrack）

系统总览
- 终端应用：Android 客户端（已含 UI/导航与后端联动说明）。
- 后端 API：Node.js 20 + Express，统一前缀 `/api`，健康检查 `GET /health`，文档入口 `/docs`。
- 部署形态：Docker 多阶段镜像 + Docker Compose，一键安装与运维脚本（`server/scripts/install.sh`、`server/scripts/manage.sh`、`server/scripts/tail-logs.sh`）。
- 数据根路径：`server/` 作为所有数据与配置的根存储路径（包含 `data/`、`logs/`、`config/`、`.env`）。

运行时与关键依赖
- 运行时：`Node.js 20`（容器内统一版本）。
- 框架：`express`。
- 中间件（计划/推荐）：`helmet`（安全头）、`cors`（跨域）、`compression`（压缩）、`express-rate-limit`（限流）。
- 日志：`pino` + `pino-http`（结构化访问日志与应用日志）。
- 配置：`dotenv`，统一读取 `.env`。
- 文档：`swagger-ui-express` 渲染 `OpenAPI` 规范（源：`API_SPEC.md` 与后续 `server/docs/openapi.yaml`）。

目录结构（后端）
- `server/src/index.ts`：进程入口，启动监听并处理优雅关机。
- `server/src/app.ts`：Express 应用初始化，注册通用中间件、健康检查 `/health`、文档 `/docs` 与 `/api` 占位、错误处理。
- `server/src/config/env.ts`：环境配置读取（`PORT`、`NODE_ENV`）。
- `server/src/middlewares/error.ts`：统一 404 与错误处理。
- `server/src/routes/`：按领域划分路由模块（待实现：`auth`、`departments`、`categories`、`items`、`users`、`approvals`）。
- `server/src/services/`：业务服务层（过滤、借还流程、审批权限校验等）。
- `server/src/models/`：领域模型与类型定义（与前端契约一致）。
- `server/docs/`：OpenAPI/接口文档源文件。

请求生命周期
1. `Android 客户端` → `Express`（`/api`）。
2. 通用中间件：安全头、CORS、压缩、限流、请求体解析、访问日志。
3. 路由层：领域路由分发。
4. 服务层：业务逻辑（校验、过滤、状态变更、审计）。
5. 数据层：读写存储（初期文件存储，后续可切换数据库）。
6. 响应封装：统一成功/错误输出结构。
7. 错误处理：全局错误中间件收敛。
8. 结构化日志：应用日志与访问日志写入 `server/logs`。

数据存储策略
- 根路径：`server/`（宿主机与容器共享）。
- 推荐结构：
  - `server/data/`：领域数据（初期可 JSON/轻量文件，后续可迁移数据库）。
  - `server/logs/`：结构化日志（访问日志、应用日志）。
  - `server/config/`：额外配置（如策略、白名单）。
  - `server/.env`：环境变量文件（参考 `server/.env.example`）。
- 迁移计划：当数据规模或并发上升时，升级为数据库（如 PostgreSQL + Prisma），保留数据迁移脚本与回滚策略。

API 端点（对齐 `API_SPEC.md`）
- 认证（Auth）：`POST /api/login`、`POST /api/signup`。
- 部门（Departments）：`GET/POST/PUT/DELETE /api/departments[/:id]`。
- 类别（Categories）：`GET/POST /api/categories`。
- 物资（Items）：
  - 列表/详情：`GET /api/items`、`GET /api/items/:id`
  - CRUD：`POST/PUT/DELETE /api/items[/:id]`
  - 借还：`POST /api/items/:id/borrow`、`POST /api/items/:itemId/return/:historyEntryId`
- 用户（Users）：`GET /api/users`、`GET/POST/PUT /api/users/:id`。
- 审批（Approvals）：`GET /api/approvals`（按角色过滤）。
- 健康检查：`GET /health`。
- 文档入口：`GET /docs`。

统一响应与错误处理
- 成功：语义化 JSON，返回核心数据或确认消息。
- 错误：`{ message: string }`，HTTP 状态码对齐语义（400/401/403/404/409/422/500）。
- 未匹配路径：统一 404。
- 日志：错误打点包含请求上下文（方法、路径、用户、部门、角色）。

安全策略
- CORS：后端允许来自前端/终端的合法域名；生产环境限制来源并附带凭证策略。
- 速率限制：公共端点限流，防止暴力调用；管理员操作适当放宽并审计。
- 安全头：`helmet` 默认策略；禁用 `x-powered-by`（已启用）。
- 身份认证：
  - 初期：基于会话/简单令牌或在受控环境中直接返回用户信息（对齐现有规范）。
  - 演进：JWT（访问/刷新）或服务端会话（含 CSRF、防重放），视需求与环境决定。

配置管理
- `.env`：`PORT`（默认 `3000`）、`NODE_ENV`（默认 `production`）。
- Compose：通过 `.env` 与卷挂载将 `server/` 映射进容器 `/app`，环境与数据一致。
- 多环境：`development`、`staging`、`production` 三种模式（日志与安全策略随模式调整）。

日志与观测性
- 访问日志：记录方法、路径、状态码、耗时、客户端信息。
- 应用日志：业务事件（借还、审批、用户变更）与错误堆栈。
- 健康检查：`GET /health`；Compose 健康探针基于容器内 `node` 直接发起 HTTP 检测（无 `curl/wget` 依赖）。
- 监控扩展：可选 Prometheus 指标端点与告警（后续阶段）。

部署与运维
- Docker Compose：
  - 服务名：`equiptrack-server`
  - 端口映射：`3000:3000`
  - 重启策略：`unless-stopped`
  - 健康检查：容器内 Node HTTP 检测 `/health`
  - 卷：`./server` → `/app`
- 脚本：
  - 安装：`server/scripts/install.sh`
  - 管理：`server/scripts/manage.sh`（`start|stop|restart|status|logs`）
  - 日志追踪：`server/scripts/tail-logs.sh`
- 常用命令：
  - 构建并启动：`docker compose up -d --build`
  - 状态与日志：`docker compose ps`、`docker compose logs -f equiptrack-server`

开发与测试流程
- 单元测试：服务层纯逻辑（借还、过滤、审批权限）。
- 集成测试：`supertest` 覆盖关键路由与错误路径，对照 `API_SPEC.md` 断言请求/响应。
- 端到端：Android 客户端联调，验证登录、列表、借还、审批全链路。
- CI/CD（可选）：构建镜像、运行测试、推送产物与版本标签。

扩展与演进
- 数据库存储：引入 `PostgreSQL + Prisma`，提供迁移脚本与回滚策略。
- 图片/附件：Data URI 解析与持久化（对象存储或本地卷），带审计与访问控制。
- 指标与告警：`/metrics` 暴露核心指标，结合告警通道（邮件/IM）。
- 弹性扩容：多实例部署 + 负载均衡（如 Nginx/Traefik），会话/令牌策略随之调整。

当前进度对齐
- 已完成：基础框架（`server/src/index.ts`、`server/src/app.ts`、`server/src/config/env.ts`、`server/src/middlewares/error.ts`）、部署/运维脚本与 Compose。
- 进行中（Phase 2）：核心领域路由与服务层按 `API_SPEC.md` 落地，先从 `Auth` 与 `Departments` 开始，随后 `Items` 借还流程与 `Approvals`。