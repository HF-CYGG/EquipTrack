# EquipTrack 微信小程序后端对接指南

本文档专门为微信小程序开发团队准备，详细说明了 EquipTrack 后端服务（基于 Node.js/Express）的接入方式、接口规范、资源访问逻辑及核心业务流程。

---

## 1. 服务基础信息 (Server Basics)

在进行接口调用前，请配置好基础环境参数。

*   **服务器 Base URL**:
    *   **开发环境**: `http://<局域网IP>:3000` (需确保手机/模拟器与服务器在同一网络)
    *   **生产环境**: `https://api.your-domain.com` (建议配置 SSL)
*   **接口前缀 (API Prefix)**: `/api`
    *   示例: `http://192.168.1.100:3000/api/login`
*   **静态资源前缀**: `/uploads`
    *   示例: `http://192.168.1.100:3000/uploads/items/thumbs/abc.jpg`
*   **通信协议**: JSON
    *   请求头建议包含: `Content-Type: application/json`

---

## 2. 认证与鉴权 (Authentication)

后端采用 **JWT (JSON Web Token)** 进行无状态认证。

### 2.1 获取 Token
目前系统采用**账号密码**模式（暂未集成微信 `jscode2session` 一键登录）。小程序端需提供登录页面。

*   **接口**: `POST /api/login`
*   **参数**: `{ "contact": "手机号", "password": "密码" }`
*   **响应**:
    ```json
    {
      "user": { ...用户信息... },
      "token": "eyJhbGciOiJIUzI1NiIsIn..."
    }
    ```

### 2.2 使用 Token
在除登录、注册外的所有接口请求中，必须在 **HTTP Header** 中携带 Token：

```http
Authorization: Bearer <your_token_string>
```

> **注意**: Token 有效期默认为 7 天。若接口返回 `401 Unauthorized`，请引导用户重新登录。

---

## 3. 图片与文件资源处理 (Resource Handling)

### 3.1 图片显示
后端数据库中存储的图片字段（如 `image`, `photo`, `avatar`）均为**相对路径**。
**小程序端显示逻辑**:
```javascript
const BASE_URL = "http://192.168.1.100:3000";
const imagePath = item.image; // 例如 "/uploads/items/thumbs/123.jpg"
const fullUrl = imagePath.startsWith("http") ? imagePath : BASE_URL + imagePath;
// <image src="{{fullUrl}}" />
```

### 3.2 图片上传
小程序需使用 `wx.uploadFile` API。

*   **接口**: `POST /api/upload`
*   **Header**: 需携带 `Authorization` Token
*   **关键参数**:
    *   `name`: 必须为 **`file`** (后端 multer 中间件指定字段名)
    *   `formData`: 必须包含 **`type`** 字段，用于指定存储目录
*   **Type 字段取值**:
    *   `item_thumb`: 物资缩略图 (存入 `uploads/items/thumbs`)
    *   `item_full`: 物资大图 (存入 `uploads/items/full`)
    *   `borrow`: 借用凭证 (存入 `uploads/returns`)
    *   `return`: 归还凭证 (存入 `uploads/returns`)
    *   其他: 存入 `uploads/others`

**小程序代码示例**:
```javascript
wx.uploadFile({
  url: `${BASE_URL}/api/upload`,
  filePath: tempFilePath,
  name: 'file', // 必填，固定为 file
  header: {
    'Authorization': 'Bearer ' + token
  },
  formData: {
    'type': 'borrow' // 根据业务场景修改
  },
  success (res) {
    const data = JSON.parse(res.data);
    const serverFilePath = data.url; // 拿到相对路径，提交给业务接口
  }
})
```

---

## 4. 核心业务流程对接 (Core Workflows)

### 4.1 物资列表与筛选
*   **接口**: `GET /api/items`
*   **场景**: 首页展示、搜索。
*   **参数**:
    *   `departmentId`: (可选) 部门ID。不传则根据用户角色默认显示（普通用户看本部门，超管看所有）。
    *   `allAvailable`: (可选) 传 `true` 仅显示 `availableQuantity > 0` 的物资。
*   **数据结构**:
    ```json
    [
      {
        "id": "item_123",
        "name": "Sony A7M4",
        "totalQuantity": 5,
        "availableQuantity": 3,
        "image": "/uploads/items/thumbs/sony.jpg",
        "borrowHistory": [...] // 包含当前借用状态
      }
    ]
    ```

### 4.2 借用申请 (Borrow)
*   **接口**: `POST /api/items/:id/borrow`
*   **流程**:
    1. 用户在小程序填写表单，拍照。
    2. 调用上传接口上传照片，获得 `photoUrl`。
    3. 调用本接口提交申请。
*   **Body 参数**:
    ```json
    {
      "expectedReturnDate": "2023-12-31",
      "photo": "/uploads/returns/xxx.jpg", // 上传接口返回的地址
      "borrower": { // 可选。普通用户后端会自动填充为当前登录人
         "name": "张三",
         "phone": "13800000000"
      }
    }
    ```

### 4.3 物资归还 (Return)
*   **接口**: `POST /api/items/:itemId/return/:historyEntryId`
*   **参数**:
    *   `itemId`: 物资 ID
    *   `historyEntryId`: 借用记录 ID (从 `item.borrowHistory` 或 `/api/history` 获取)
*   **Body 参数**:
    ```json
    {
      "photo": "/uploads/returns/return_proof.jpg",
      "isForced": false // 是否强制归还（仅管理员可用）
    }
    ```

### 4.4 历史记录 (History)
*   **接口**: `GET /api/history`
*   **逻辑**: 后端已将复杂的嵌套结构扁平化，适合列表展示。
*   **权限**: 普通用户只能看到自己的借还记录；管理员可以看到本部门记录。

---

## 5. 常见问题 (FAQ)

1.  **小程序请求失败 (Request Failed)**
    *   检查开发工具是否勾选了“不校验合法域名”。
    *   检查 Base URL 是否正确，真机调试需保证手机能 ping 通服务器 IP。

2.  **图片加载失败 (404 Not Found)**
    *   确认图片 URL 是否拼接了 Base URL。
    *   确认后端 `uploads` 文件夹下确实存在该文件。

3.  **日期格式**
    *   后端均使用 ISO 8601 字符串 (如 `2023-10-27T10:00:00.000Z`)。小程序端需自行格式化显示（建议使用 dayjs）。

4.  **枚举值定义**
    *   **UserRole**: `超级管理员`, `管理员`, `高级用户`, `普通用户`
    *   **BorrowStatus**: `借用中`, `逾期未归还`, `已归还`, `逾期归还`

---

## 6. 数据字典参考

**EquipmentItem (物资)**
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 唯一标识 |
| name | string | 物资名称 |
| categoryId | string | 类别ID |
| departmentId | string | 所属部门ID |
| totalQuantity | number | 总数量 |
| availableQuantity | number | 当前可用数量 |
| image | string | 缩略图路径 |
| imageFull | string | 原图路径 |

**BorrowHistoryEntry (借还记录)**
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 记录ID |
| borrower | object | `{ name, phone }` |
| borrowDate | string | 借出时间 |
| expectedReturnDate | string | 预计归还时间 |
| returnDate | string | 实际归还时间 (未还为 null) |
| status | string | 状态枚举 |
| photo | string | 借出拍照 |
| returnPhoto | string | 归还拍照 |
