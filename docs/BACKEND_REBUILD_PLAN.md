后端重建计划（EquipTrack）

目标
- 以 `/api` 为前缀提供稳定、可测试、文档化的 REST 服务。
- 对齐前端类型契约与 `API_SPEC.md`，确保终端应用功能全覆盖。

阶段划分
1) 基础框架
- 初始化 Express 应用（安全中间件、压缩、CORS、限流、日志）。
- 加载配置（dotenv + `config`），暴露 `GET /health` 与 `/docs`。
- 注册全局错误处理与结构化日志。

2) 核心 API
- Auth：`POST /api/login`，`POST /api/signup`。
- Departments：`GET/POST/PUT/DELETE /api/departments[/:id]`。
- Categories：`GET/POST /api/categories`。
- Items：`GET /api/items`，`GET/POST/PUT/DELETE /api/items/:id`，借还流程：
  - `POST /api/items/:id/borrow`
  - `POST /api/items/:itemId/return/:historyEntryId`
- Users：`GET /api/users`，`GET/POST/PUT /api/users/:id`。
- Approvals：`GET /api/approvals`（角色权限过滤）。

3) 辅助功能
- 审计日志与操作记录（借还、审批）。
- 统一响应封装与错误码规范。
- 图片上传（Data URI 解析与存储抽象）。
- 分页/筛选与通用查询参数解析。
- 健康/指标端点扩展（可选）。

测试策略
- 单元测试：服务层纯逻辑函数。
- 集成测试：使用 `supertest` 覆盖关键路由与错误路径。
- 合同测试：对照 `API_SPEC.md` 的示例请求/响应进行断言。
- 端到端验证：Android 终端联调，关注登录、列表、借还、审批全链路。

文档与交付
- OpenAPI：维护 `server/docs/openapi.yaml`，在 `/docs` 渲染。
- 架构与接口文档：本计划 + `docs/BACKEND_ARCHITECTURE.md` 与 `API_SPEC.md`。
- 运维：延用 `server/scripts` 与 Docker Compose 部署，健康检查一致。

验收标准
- `GET /health` 返回 200，Compose 健康检查稳定通过。
- 所有核心端点返回结构与语义符合 `API_SPEC.md`。
- 日志完整、错误处理一致、接口具备幂等性或明确的副作用说明。