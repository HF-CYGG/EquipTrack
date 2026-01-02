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
   - 在 GitHub 发布新 Release (Tag)。
   - GitHub Actions 自动构建 APK 并同步发布到 Gitee (携带 APK 附件)。
2. 配置 (Server) :
   - 您在服务器修改 server/data/app_version.json ，更新版本号 (例如 versionCode: 3 )，保持 downloadUrl 为空。
3. 更新 (Client) :
   - 客户端检测到服务器版本号更高。
   - 服务器自动从 Gitee 获取最新的 APK 下载链接返回给客户端。
   - 客户端自动下载并调用系统安装程序进行覆盖安装。