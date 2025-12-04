后端架构总览（EquipTrack）

技术栈与基础设施
- 运行时：Node.js 20（容器内）
- 框架：Express
- 安全：helmet、cors、express-rate-limit
- 日志：pino + pino-http（结构化）
- 压缩：compression
- 配置：dotenv（`.env`）与模块化 `config`
- 文档：swagger-ui-express + YAML（`server/docs/openapi.yaml`）

模块划分
- `app`：Express 应用初始化，注册通用中间件、健康检查、文档与 API 路由、错误处理。
- `routes`：REST API 路由聚合，按领域分模块（auth、departments、categories、items、users、approvals）。
- `services`：业务服务层，封装具体领域逻辑（过滤、借还流程、审批权限校验等）。
- `models`：数据模型与类型定义（与前端类型契约保持一致）。
- `middlewares`：错误处理、权限校验、请求限流、日志等。
- `config`：环境变量加载与解析（如 `PORT` 与 `NODE_ENV`）。

关键端点
- 健康检查：`GET /health`
- 文档入口：`GET /docs`
- API 前缀：`/api`

部署运行
- Docker Compose：服务名 `equiptrack-server`，端口映射 `3000:3000`，健康检查基于容器内 Node HTTP 请求。
- 数据持久化：`./server/{data,logs,config,.env}` 挂载到容器 `/app`。
- 重启策略：`unless-stopped`。

错误与日志
- 全局错误处理中间件返回统一错误结构。
- 访问日志与结构化应用日志写入到 `server/logs` 卷。