使用命令.venv\Scripts\node.exe -r dotenv/config dist/index.js启动后端服务器
使用.venv\Scripts\node.exe node_modules/typescript/bin/tsc编译文件
必须在.venv的虚拟环境下使用命令npm run build 
node相关命令必须在.venv的虚拟环境下执行
自动构建并安装到虚拟机
使用中文编写代码内注释内容
尽量不更改后端服务器代码

### 版本发布与自动更新工作流
为了确保客户端能够正确检测并提示更新，请严格遵守以下发布流程：

1. **构建发行版 (Build Release)**:
   - 在 `android/` 目录下构建正式版 APK (Release Build)。
   - 确保 `android/app/build.gradle` 中的 `versionCode` 和 `versionName` 已更新。

2. **发布托管 (Publish)**:
   - 将构建好的 APK 上传至 Gitee 或 GitHub 的 Releases 页面进行发布。
   - 发布页链接：`https://gitee.com/YeMiao_cats/EquipTrack/releases`

3. **配置服务端 (Server Config)**:
   - **必须步骤**：修改服务器端文件 `server/data/app_version.json`。
   - 更新 `versionCode` (必须大于当前版本) 和 `versionName`。
   - 更新 `updateContent` (更新日志)。
   - **关键设置**：设置 `downloadUrl` 为 `https://gitee.com/YeMiao_cats/EquipTrack/releases`。
   - 示例：
     ```json
     {
       "versionCode": 7,
       "versionName": "1.0.0",
       "updateContent": "1. 新增功能A\n2. 修复问题B",
       "downloadUrl": "https://gitee.com/YeMiao_cats/EquipTrack/releases",
       "forceUpdate": false,
       "releaseDate": "2026-01-05T00:00:00.000Z"
     }
     ```

4. **客户端行为 (Client Behavior)**:
   - 客户端会自动检测服务器上的 `versionCode`。
   - 若发现新版本，弹出更新提示对话框。
   - 用户点击“下载更新”时，应用将跳转到系统浏览器并打开 Gitee Releases 页面供用户手动下载。

### AI 构建与开发规则
1. **环境约束**：
   - 严格遵守 `.venv` 虚拟环境和绝对路径的使用规则。
   - Android 构建命令应在 `android/` 目录下执行。
2. **版本一致性**：
   - 在协助发布新版本时，必须检查 `android/app/build.gradle` 与 `server/data/app_version.json` 的版本号是否同步更新。
3. **更新机制维护**：
   - 任何涉及更新逻辑的修改，必须确保 `MainViewModel` 和 `UpdateManager` 正确处理 `downloadUrl` 的跳转逻辑（即 HTTP 链接应跳转浏览器）。