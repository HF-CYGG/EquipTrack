# EquipTrack API 规范

本文档为 EquipTrack 应用的后端 API 提供了详细的规范。所有端点都以 `/api` 为前缀。

## 核心数据模型

所有核心数据模型的 TypeScript 类型定义请参考前端 `src/lib/types.ts` 文件。主要模型包括：

*   `Department`: 部门
*   `Category`: 物资类别
*   `EquipmentItem`: 物资
*   `User`: 用户
*   `BorrowHistoryEntry`: 借用历史记录
*   `RegistrationRequest`: 注册申请

---

## 1. 认证 (Auth)

### 1.1 用户登录

*   **Endpoint**: `POST /api/login`
*   **描述**: 验证用户凭据并返回用户信息。
*   **请求体**:
    ```json
    {
      "contact": "string",
      "password": "string"
    }
    ```
*   **成功响应 (200 OK)**:
    ```json
    {
      "user": "User" // 完整的用户对象，不包含密码
    }
    ```
*   **失败响应 (400/401 Unauthorized)**:
    ```json
    {
      "message": "错误信息描述"
    }
    ```

### 1.2 用户注册申请

*   **Endpoint**: `POST /api/signup`
*   **描述**: 提交一个新的用户注册申请，需要有效的邀请码。
*   **请求体**:
    ```json
    {
      "name": "string",
      "contact": "string",
      "departmentName": "string",
      "password": "string",
      "invitationCode": "string"
    }
    ```
*   **成功响应 (200 OK)**:
    ```json
    {
      "message": "注册申请已提交，等待管理员批准。"
    }
    ```
*   **失败响应 (400 Bad Request)**:
    ```json
    {
      "message": "无效的邀请码 / 该联系方式已被注册 / ..."
    }
    ```
*   **后端逻辑**:
    1.  验证 `invitationCode` 是否存在且有效（属于一个角色为 '超级管理员', '管理员', 或 '高级用户' 的用户）。
    2.  检查 `contact` 是否已被注册或已在申请中。
    3.  创建一个新的 `RegistrationRequest` 记录并保存。

---

## 2. 部门管理 (Departments)

### 2.1 获取所有部门

*   **Endpoint**: `GET /api/departments`
*   **描述**: 返回所有部门的列表。
*   **成功响应 (200 OK)**: `Department[]`

### 2.2 添加新部门

*   **Endpoint**: `POST /api/departments`
*   **描述**: 创建一个新部门。
*   **请求体**: `{ "name": "string" }`
*   **成功响应 (200 OK)**: `Department` (新创建的部门对象)
*   **失败响应 (400 Bad Request)**: `{ "message": "该部门名称已存在。" }`

### 2.3 更新部门信息

*   **Endpoint**: `PUT /api/departments/:id`
*   **描述**: 更新指定 ID 的部门名称。
*   **请求体**: `{ "name": "string" }`
*   **成功响应 (200 OK)**: `Department` (更新后的部门对象)
*   **后端逻辑**: 更新部门名称后，需要同步更新所有属于该部门的用户的 `departmentName` 字段。

### 2.4 删除部门

*   **Endpoint**: `DELETE /api/departments/:id`
*   **描述**: 删除指定 ID 的部门。
*   **成功响应 (200 OK)**: `{ "message": "Department deleted" }`
*   **后端逻辑**: 删除部门时，应级联删除该部门下的所有用户、物资及相关的借用历史。

---

## 3. 物资类别管理 (Categories)

### 3.1 获取所有类别

*   **Endpoint**: `GET /api/categories`
*   **描述**: 返回所有物资类别的列表。
*   **成功响应 (200 OK)**: `Category[]`

### 3.2 添加新类别

*   **Endpoint**: `POST /api/categories`
*   **描述**: 创建一个新的物资类别。
*   **请求体**: `{ "name": "string", "color": "string" }`
*   **成功响应 (200 OK)**: `Category` (新创建的类别对象)

---

## 4. 物资管理 (Items)

### 4.1 获取物资列表

*   **Endpoint**: `GET /api/items`
*   **描述**: 根据用户角色和部门 ID 过滤并返回物资列表。
*   **URL 查询参数**:
    *   `userRole: UserRole` (必需)
    *   `departmentId: string` (可选)
    *   `allAvailable: boolean` (可选)
*   **成功响应 (200 OK)**: `EquipmentItem[]`
*   **后端逻辑**:
    *   如果 `allAvailable` 为 `true`，返回所有部门中 `availableQuantity > 0` 的物资。
    *   如果 `userRole` 是 '超级管理员'：
        *   如果提供了 `departmentId`，返回该部门的物资。
        *   否则，返回所有部门的物资。
    *   对于其他角色，必须提供 `departmentId`，返回该部门的物资。

### 4.2 获取单个物资信息

*   **Endpoint**: `GET /api/items/:id`
*   **成功响应 (200 OK)**: `EquipmentItem`

### 4.3 添加新物资

*   **Endpoint**: `POST /api/items`
*   **请求体**: `Omit<EquipmentItem, 'id'>`
*   **成功响应 (200 OK)**: `EquipmentItem`

### 4.4 更新物资信息

*   **Endpoint**: `PUT /api/items/:id`
*   **请求体**: `Partial<EquipmentItem>`
*   **成功响应 (200 OK)**: `EquipmentItem`

### 4.5 删除物资

*   **Endpoint**: `DELETE /api/items/:id`
*   **成功响应 (200 OK)**: `{ "message": "Item deleted" }`
*   **后端逻辑**: 删除物资时，也应删除相关的借用历史。

### 4.6 借出物资

*   **Endpoint**: `POST /api/items/:id/borrow`
*   **描述**: 处理物资借出操作。
*   **请求体**:
    ```json
    {
      "borrower": { "name": "string", "phone": "string" },
      "expectedReturnDate": "Date | string",
      "photo": "string" // Data URI 格式的图片
    }
    ```
*   **成功响应 (200 OK)**: `EquipmentItem` (更新后的物资对象)
*   **后端逻辑**:
    1.  检查物资可用数量是否大于0。
    2.  减少 `availableQuantity`。
    3.  创建一个新的 `BorrowHistoryEntry` 记录，状态为 '借用中'。

### 4.7 归还物资

*   **Endpoint**: `POST /api/items/:itemId/return/:historyEntryId`
*   **描述**: 处理物资归还操作。
*   **请求体**:
    ```json
    {
      "photo": "string", // Data URI 格式的图片
      "isForced": "boolean", // 是否为管理员强制归还
      "adminName": "string" // 如果是强制归还，操作的管理员名称
    }
    ```
*   **成功响应 (200 OK)**: `EquipmentItem` (更新后的物资对象)
*   **后端逻辑**:
    1.  验证 `historyEntryId` 对应的记录是否存在且状态为 '借用中' 或 '逾期未归还'。
    2.  增加 `availableQuantity`。
    3.  更新 `BorrowHistoryEntry` 记录，设置 `returnDate` 和 `status` ('已归还' 或 '逾期归还')。
    4.  如果 `isForced` 为 `true`，记录 `forcedReturnBy`。

---

## 5. 用户管理 (Users)

### 5.1 获取用户列表

*   **Endpoint**: `GET /api/users`
*   **URL 查询参数**:
    *   `userRole: UserRole` (必需)
    *   `departmentId: string` (可选)
*   **成功响应 (200 OK)**: `User[]` (不包含密码)
*   **后端逻辑**: 逻辑与获取物资列表类似，根据角色和部门 ID 进行过滤。

### 5.2 获取单个用户信息

*   **Endpoint**: `GET /api/users/:id`
*   **成功响应 (200 OK)**: `User` (不包含密码)

### 5.3 添加用户

*   **Endpoint**: `POST /api/users`
*   **请求体**: `Omit<User, 'id' | 'status'>` (包含初始密码)
*   **成功响应 (200 OK)**: `User` (不包含密码)

### 5.4 更新用户信息

*   **Endpoint**: `PUT /api/users/:id`
*   **请求体**: `Partial<User>` (可包含 `name`, `contact`, `departmentId`, `role`, `status`, `password`, `invitationCode`)
*   **成功响应 (200 OK)**: `User` (不包含密码)

---

## 6. 注册审批 (Approvals)

### 6.1 获取注册申请列表

*   **Endpoint**: `GET /api/approvals`
*   **URL 查询参数**:
    *   `userId: string`
    *   `userRole: UserRole`
    *   `departmentId: string`
*   **成功响应 (200 OK)**: `RegistrationRequest[]`
*   **后端逻辑**:
    *   `超级管理员`可以查看所有申请。
    *   `管理员`可以查看其所在部门的所有申请。
    *   `高级用户`只能查看由其邀请码发起的申请。

### 6.2 批准注册申请

*   **Endpoint**: `POST /api/approvals/:requestId`
*   **成功响应 (200 OK)**: `User` (新创建的用户对象, 不含密码)
*   **后端逻辑**:
    1.  删除对应的 `RegistrationRequest` 记录。
    2.  创建一个新的 `User` 记录，角色默认为 '普通用户'，状态为 '正常'。

### 6.3 拒绝注册申请

*   **Endpoint**: `DELETE /api/approvals/:requestId`
*   **成功响应 (200 OK)**: `{ "message": "申请已拒绝" }`

---

## 7. 借用历史 (History)

### 7.1 获取历史记录

*   **Endpoint**: `GET /api/history`
*   **URL 查询参数**:
    *   `userRole: UserRole` (必需)
    *   `departmentId: string` (可选)
*   **成功响应 (200 OK)**: `BorrowHistoryEntry[]`
*   **后端逻辑**:
    1.  根据角色和部门 ID 过滤历史记录。
    2.  在返回前，检查所有 '借用中' 的记录，如果当前时间已超过 `expectedReturnDate`，则将其状态动态更新为 '逾期未归还'。
