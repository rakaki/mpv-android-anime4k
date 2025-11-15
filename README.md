# 小喵player - 本地动漫视频实时超分播放器

**[中文版本](README.md) | [English Version](README_EN.md)**

一个基于 libmpv 的 Android 本地视频播放器，支持多种视频格式、字幕处理、手势控制和实时超分等功能。

本项目旨在将二次元动漫/动画/番剧风格的视频进行优化超分，当然你也可以当作普通的播放器使用。

## 功能截图

### 应用主界面（竖屏）

| 主页 | 文件夹列表 | 视频列表 | WebDAV功能 |
|------|---------|---------|------------|
| <img src="docs/screenshots/主页.jpg" width="280"/> | <img src="docs/screenshots/文件夹列表.jpg" width="280"/> | <img src="docs/screenshots/视频列表.jpg" width="280"/> | <img src="docs/screenshots/webdav功能.jpg" width="280"/> |

| 番剧解析 |
|---------|
| <img src="docs/screenshots/番剧解析.jpg" width="280"/> |

### 播放器界面（横屏）

| 播放器主界面 | 弹幕功能 |
|-----------|---------|
| <img src="docs/screenshots/播放器主界面.jpg" width="280"/> | <img src="docs/screenshots/弹幕功能.jpg" width="280"/> |

| 弹幕样式设置 | 字幕样式设置 |
|------------|------------|
| <img src="docs/screenshots/弹幕样式设置.jpg" width="280"/> | <img src="docs/screenshots/字幕样式设置.jpg" width="280"/> |

| 超分功能 |
|---------|
| <img src="docs/screenshots/超分功能.jpg" width="280"/> |

## 主要功能

- **视频播放**：支持主流视频格式（MP4、MKV、AVI 等）
- **哔哩哔哩番剧支持**：支持登录B站账号、在线播放番剧（详见[登录实现说明](docs/bilibili_login.md)和[番剧解析原理](docs/bilibili_bangumi.md)）
- **WebDAV 网络存储**：支持连接 WebDAV 服务器，直接播放云端视频文件（详见[WebDAV 使用说明](docs/webdav使用说明.md)）
- **播放列表**：自动扫描文件夹、支持视频排序和分类
- **字幕管理**：内嵌字幕解析、外部字幕导入、字幕位置和大小调整
- **音频轨道**：多音轨切换
- **音量增强**：支持开关增强功能，且可精细到0.1%调整
- **弹幕功能**：
  - 支持导入本地 XML 格式弹幕文件
  - **支持从哔哩哔哩下载弹幕**（详见[弹幕下载原理说明](docs/bilibili_danmaku_download.md)）
    - 使用B站分段弹幕API，获取完整弹幕数据
    - 支持普通视频和番剧整季批量下载
    - 并发下载技术，速度提升10-20倍
    - 自动携带登录Cookie，获取会员专属弹幕
  - 弹幕样式自定义（大小、速度、透明度、描边等）
  - 弹幕轨道管理，支持显示/隐藏不同类型弹幕
  - 自动记忆弹幕文件和显示状态
  - 高刷新率屏幕适配（支持 90Hz/120Hz/144Hz）
  - 弹幕与视频进度同步，支持章节跳转
- **手势控制**：
  - 左侧滑动：调节亮度
  - 右侧滑动：调节音量
  - 左右滑动：快进/快退
  - 双击：暂停/播放
  - 长按：倍速播放
  - 进度条拖动：精确定位
- **播放控制**：快进/快退、倍速播放、字幕延迟调整
- **超分辨率**：集成 Anime4K，支持实时视频超分
- **播放进度恢复**：自动保存播放进度，下次打开自动续播
- **截图功能**：支持视频截图保存

## 技术架构

- **视频引擎**：libmpv（开源多媒体播放器库）
- **UI 框架**：Android AppCompat
- **编程语言**：Kotlin + Java
- **最低 SDK**：26 (Android 8.0)
- **编译 SDK**：34 (Android 14)

## 功能规划

以下功能已规划但暂未实现：

- 帧插值补帧
- 字幕字体自定义
- 字体选择

## 致谢

本项目离不开以下开源项目的支持：

- **[mpv-player/mpv](https://github.com/mpv-player/mpv)**  
  本项目的核心基础，强大的多媒体播放器库

- **[mpv-android/mpv-android](https://github.com/mpv-android/mpv-android)**  
  Android 移动端实现参考

- **[abdallahmehiz/mpv-android](https://github.com/abdallahmehiz/mpv-android/releases)**  
  提供现成可用的 libmpv 库文件

- **[abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt)**  
  参考了手势控制、滑动处理、外部字幕导入等多项实现

- **[bloc97/Anime4K](https://github.com/bloc97/Anime4K)**  
  超分辨率滤镜 GLSL 文件来源

- **[Predidit/Kazumi](https://github.com/Predidit/Kazumi)**  
  项目开发灵感和原始需求

- **[xyoye/DanDanPlayForAndroid](https://github.com/xyoye/DanDanPlayForAndroid)**  
  参考了弹幕实现与重构以及webdav功能的实现等其他诸多功能

- **[bilibili/DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)**  
  此项目的弹幕底层核心库为哔哩哔哩的Android开源弹幕解析绘制引擎项目

- **[SocialSisterYi/bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)**  
  感谢此项目收集的公开API，将散落的API集中起来，本项目参考了其中的使用方法

- **[the1812/Bilibili-Evolved](https://github.com/the1812/Bilibili-Evolved)**  
  哔哩哔哩增强脚本，参考了弹幕下载的并发优化策略和API调用方式

- **[thegrizzlylabs/sardine-android](https://github.com/thegrizzlylabs/sardine-android)**  
  提供了 WebDAV 协议的 Android 客户端实现，支持文件浏览、上传、下载等操作

- **[ngallagher/simplexml](https://github.com/ngallagher/simplexml)**  
  提供了轻量级的 XML 序列化框架，用于解析 WebDAV 服务器返回的 XML 格式响应数据

---

感谢以上所有开源项目和开发者的无私贡献，没有你们的努力就没有这个项目的诞生！

## 第三方服务声明

本应用使用了以下第三方服务的公开API：

- **哔哩哔哩 (Bilibili)** - 用于登录、解析番剧链接并在线播放、下载弹幕
  - 登录API: `https://passport.bilibili.com/x/passport-login/web/qrcode/*`
  - 番剧信息API: `https://api.bilibili.com/pgc/view/web/season`
  - 番剧播放API: `https://api.bilibili.com/pgc/player/web/playurl`
  - 弹幕下载API: `https://api.bilibili.com/x/v1/dm/list.so`
  - 使用场景：
    - 用户主动扫码登录B站账号
    - 用户输入番剧链接观看在线番剧
    - 用户主动输入B站视频链接下载弹幕
  - 数据处理：
    - 登录凭证使用AES-256加密存储在本地，详见[安全说明](docs/bilibili_security_analysis.md)
    - 下载的弹幕数据仅保存在用户本地设备
    - 所有数据不会上传或分享给第三方
  - 声明：本应用与哔哩哔哩无任何官方关联，仅使用其公开API

**隐私说明**：

本应用高度重视用户隐私保护，特此说明：

### 数据收集
- ❌ **不收集**用户的任何个人信息
- ❌ **不上传**任何数据到我们的服务器（我们没有服务器）
- ❌ **不分享**用户数据给任何第三方
- ✅ 所有功能均在**本地设备**上运行

### 哔哩哔哩登录功能
- 登录凭证使用 **AES-256 军事级加密** 存储在本地（详见[安全分析](docs/bilibili_security_analysis.md)）
- 登录密钥由 Android KeyStore 硬件保护，**应用无法导出**
- 登录信息仅用于调用B站API，**不会上传到任何其他地方**
- 用户可随时在设置中**一键退出登录**，彻底清除所有登录数据
- 应用卸载后，所有登录数据将**自动永久销毁**

### 弹幕与番剧数据
- 弹幕文件和番剧数据保存在**用户指定的本地文件夹**
- 下载功能完全由**用户主动触发**
- 数据仅存储在本地，**不会同步或备份到云端**

### 权限说明
应用仅请求以下必要权限：
- **存储权限**：读取和保存本地视频、字幕、弹幕文件
- **网络权限**：用于哔哩哔哩番剧在线播放和弹幕下载（用户主动触发）

### 开源透明
- ✅ 项目**完全开源**，所有代码公开可审查
- ✅ 欢迎安全专家进行代码审计
- ✅ 如发现安全问题，请及时反馈

**承诺**：本应用永远不会售卖或共享用户数据，因为我们根本不收集！

## 开发说明

**本项目全程由 AI 完成代码开发**，本人仅负责测试、反馈和方案设计等工作。

由于 AI 生成代码的特殊性，项目中可能存在**代码冗余、结构不够精简**等问题。虽然项目已经历**两次大版本的代码结构优化**，对大部分代码进行了归类与重构，但仍可能存在以下情况：
- 部分代码逻辑较为复杂
- 存在已注释但未删除的废弃代码
- 某些实现方式不够优雅

对于优化不足之处，还望各位开发者见谅！项目仍在持续改进中，欢迎提出建议和反馈。

## 系统要求

- Android 8.0 及以上
- 至少 100MB 存储空间
- 建议 2GB 以上 RAM

## 使用方式

1. 安装应用
2. 授予文件访问权限
3. 打开应用，浏览本地视频文件
4. 点击视频即可播放

## 反馈与建议

如遇到问题或有建议，欢迎提出！

---

**Last Updated:** 2025-11-15
