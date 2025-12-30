# 🚀 版本更新功能配置说明

## ✅ 已完成的功能

1. ✅ 创建了 `UpdateManager.kt` 工具类
2. ✅ 在设置页面添加了"检查更新"选项
3. ✅ 实现了更新提示对话框
4. ✅ 支持跳转浏览器下载新版本

---

## 📝 配置步骤

### 1️⃣ 修改 GitHub API 地址

打开文件：`app/src/main/java/com/fam4k007/videoplayer/utils/UpdateManager.kt`

找到第 **18 行**，修改为你的 GitHub 仓库地址：

```kotlin
// 修改前：
private const val GITHUB_API_URL = "https://api.github.com/repos/YOUR_USERNAME/YOUR_REPO/releases/latest"

// 修改后（示例）：
private const val GITHUB_API_URL = "https://api.github.com/repos/username/FAM4K007/releases/latest"
```

**格式说明**：
- `YOUR_USERNAME` = 你的 GitHub 用户名
- `YOUR_REPO` = 你的仓库名称

---

### 2️⃣ 发布 GitHub Release

#### 步骤：
1. 进入你的 GitHub 仓库
2. 点击右侧的 **"Releases"**
3. 点击 **"Create a new release"**
4. 填写信息：
   - **Tag version**: `v1.1.4`（版本号，建议以 v 开头）
   - **Release title**: `版本 1.1.4`
   - **Describe this release**: 填写更新内容
   - **Attach binaries**: 上传编译好的 APK 文件
5. 点击 **"Publish release"**

#### 版本号规则：
- 使用 `v` 前缀：`v1.1.4`
- 主版本.次版本.修订号
- 示例：`v1.0.0` → `v1.0.1` → `v1.1.0` → `v2.0.0`

---

## 🎯 使用方式

### 用户操作：
1. 打开应用，进入**设置页面**
2. 找到**"检查更新"**选项（在"其他"分类下）
3. 点击后会自动检查最新版本
4. 如果有新版本，会弹出提示框
5. 点击**"立即下载"**跳转浏览器下载

---

## 🔧 工作原理

```
用户点击检查更新
    ↓
请求 GitHub API
    ↓
获取最新版本信息
    ↓
对比版本号（当前: 1.1.3）
    ↓
如果有新版本 → 弹出对话框
如果是最新版 → 提示"已是最新版本"
    ↓
用户点击下载 → 跳转浏览器
```

---

## 📌 版本号对比逻辑

```kotlin
// 当前版本（build.gradle）
versionCode = 13
versionName = "1.1.3"

// GitHub Release 版本
tag_name = "v1.1.4"

// 转换规则：
1.1.3 → 113
1.1.4 → 114

// 114 > 113 → 提示更新
```

---

## 🛠️ 可选优化

如果不想使用 GitHub，可以自建服务器，返回 JSON 格式：

```json
{
  "tag_name": "v1.1.4",
  "name": "版本 1.1.4",
  "body": "1. 修复了排序bug\n2. 优化了帧率检测",
  "html_url": "https://yourwebsite.com/download",
  "assets": [
    {
      "name": "app-release.apk",
      "browser_download_url": "https://yourwebsite.com/app-v1.1.4.apk"
    }
  ]
}
```

---

## ✨ 功能特点

- ✅ **轻量级**：只需一个网络请求
- ✅ **无需应用内下载**：节省存储空间
- ✅ **安全性高**：浏览器下载，用户可控
- ✅ **维护简单**：只需在 GitHub 发布 Release
- ✅ **无需额外服务器**：免费使用 GitHub

---

## ⚠️ 注意事项

1. **网络权限**：已自动添加（INTERNET）
2. **API 限制**：GitHub API 有请求频率限制（匿名 60次/小时）
3. **版本规范**：确保 GitHub Release 的 tag 格式为 `vX.X.X`
4. **APK 上传**：记得在 Release 中上传 APK 文件

---

## 🎉 完成！

现在你的应用已经支持版本更新检查功能了！

测试步骤：
1. 确保已配置 GitHub API 地址
2. 在 GitHub 发布一个高版本的 Release
3. 打开应用，进入设置 → 检查更新
4. 查看是否能正常检测到新版本
