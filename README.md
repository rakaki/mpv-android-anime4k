# FAMPV4K - 本地动漫视频实时超分播放器

**[中文版本](README.md) | [English Version](README_EN.md)**

一个基于 libmpv 的 Android 本地视频播放器，支持多种视频格式、字幕处理、手势控制和实时超分等功能。

## 软件名称1

**FAMPV4K** 是 **For Anime Media Player Video 4K** 的首字母简称：
- **F**or - 为了
- **A**nime - 动画
- **M**edia - 媒体
- **P**layer - 播放器
- **V**ideo - 视频
- **4K** - 超高清分辨率

该名称体现了本软件主要为日本动画爱好者优化的本地高清视频播放器定位。

## 功能截图

| 首页 | 设置页面 | 播放页面 |
|------|---------|---------|
| ![首页](docs/screenshots/Screenshot_20251026_114205.jpg) | ![播放](docs/screenshots/Screenshot_20251026_114217.jpg) | ![设置](docs/screenshots/Screenshot_20251026_114259.jpg) |

| 文件夹扫描 | 视频列表 |
|---------|---------|
| ![字幕](docs/screenshots/Screenshot_20251026_114420.jpg) | ![控制](docs/screenshots/IMG_20251026_114329.jpg) |

## 主要功能

- **视频播放**：支持主流视频格式（MP4、MKV、AVI 等）
- **播放列表**：自动扫描文件夹、支持视频排序和分类
- **字幕管理**：内嵌字幕解析、外部字幕导入、字幕位置和大小调整
- **音频轨道**：多音轨切换
- **手势控制**：
  - 左侧滑动：调节亮度
  - 右侧滑动：调节音量
  - 双指缩放：视频缩放
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

## 已知问题

| 问题 | 现象 |
|------|------|
| 视频列表缩略图丢失 | 下拉刷新后缩略图可能消失 |
| 排序功能延迟 | 排序功能初始需要执行两次才生效 |
| 视频重复加载 | 为修复字幕画面问题，视频开头会重复加载一次 |
| 内嵌+外部字幕冲突 | 内嵌字幕视频不应再导入外部字幕 |
| 字幕导入黑屏 | 导入外部字幕有极小概率导致黑屏，返回重进即可恢复 |

## 功能规划

以下功能已规划但暂未实现：

- 弹幕功能
- 帧插值补帧
- 字幕字体自定义
- 字体选择
- 播放器锁定模式
- 视频缩放功能
- 在线缓存
- 在线视频播放
- UI 主题和深色模式

## 致谢

本项目离不开以下开源项目的支持：

| 项目 | 说明 |
|------|------|
| [mpv-player/mpv](https://github.com/mpv-player/mpv) | 本项目的核心基础，强大的多媒体播放器库 |
| [mpv-android/mpv-android](https://github.com/mpv-android/mpv-android) | Android 移动端实现参考 |
| [jarnedemeulemeester/libmpv-android](https://github.com/jarnedemeulemeester/libmpv-android) | 提供现成可用的 libmpv 库文件 |
| [abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt) | 参考了手势控制、滑动处理、外部字幕导入等多项实现 |
| [bloc97/Anime4K](https://github.com/bloc97/Anime4K) | 超分辨率滤镜 GLSL 文件来源 |
| [Predidit/Kazumi](https://github.com/Predidit/Kazumi) | 项目开发灵感和原始需求 |

## 开发说明

**本项目全程由 AI 完成代码开发**，本人仅负责测试、反馈和方案设计等工作。

由于 AI 生成的特殊性，项目中可能存在代码冗余或不够精简的情况，感谢理解。

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

**Last Updated:** 2025-10-26
