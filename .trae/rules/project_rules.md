使用命令.venv\Scripts\node.exe -r dotenv/config dist/index.js启动后端服务器
使用.venv\Scripts\node.exe node_modules/typescript/bin/tsc编译文件
必须在.venv的虚拟环境下使用命令npm run build 
node相关命令必须在.venv的虚拟环境下执行
自动构建并安装到虚拟机
使用中文编写代码内注释内容
尽量不更改后端服务器代码

### 自动更新流程说明
现在，您的完整自动化更新流程如下：

1. 发布 (GitHub) :
   - 开发者手动在 GitHub 发布新 Release (Tag)。
   - 开发者手动构建并上传 APK 附件到 GitHub Release。
   - GitHub Actions (sync-gitee) 自动同步 Release 信息及 APK 到 Gitee。
2. 配置 (Server) :
   - 您在服务器修改 server/data/app_version.json ，更新版本号 (例如 versionCode: 3 )，保持 downloadUrl 为空。
3. 更新 (Client) :
   - 客户端检测到服务器版本号更高。
   - 服务器自动从 Gitee 获取最新的 APK 下载链接返回给客户端。
   - 客户端自动下载并调用系统安装程序进行覆盖安装。

### AI 构建与开发规则
1. **构建流程遵从性**：
   - AI 在协助构建时，应遵循上述“手动发布 + 自动同步”的混合流程。
   - 不应主动建议更改为全自动发布（除非用户明确要求），以保持现有工作流的稳定性。
2. **版本一致性检查**：
   - 在协助发布新版本时，AI 应提醒用户同时更新 `android/app/build.gradle` (versionCode/versionName) 和 `server/data/app_version.json`。
3. **环境约束**：
   - 严格遵守 `.venv` 虚拟环境和绝对路径的使用规则。
   - Android 构建命令应在 `android/` 目录下执行。