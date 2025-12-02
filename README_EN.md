# XiaoMiao Player - Local Anime Video Real-time Upscaling Player

**[中文版本](README.md) | [English Version](README_EN.md)**

An Android local video player based on libmpv, supporting multiple video formats, subtitle processing, gesture controls, and real-time upscaling features.

This project aims to optimize and upscale anime-style videos, though it can also be used as a regular video player.

---

## License

> [!IMPORTANT]
> This project is licensed under [GPL-3.0-or-later](LICENSE) and is free and open source.
> 
> **Any form of redistribution must remain open source, follow the same license, and retain the original author and copyright information.**

---

## ⚠️ Important Notice

**This project is intended for learning and testing purposes only. Please do not abuse it!**

We strongly oppose and do not condone any form of piracy, illegal distribution, dark web activities, or other illegal uses or behaviors.

- Any consequences arising from the use of this project (including but not limited to illegal purposes, account restrictions, or other losses) shall be borne by the user personally and have nothing to do with [me](https://github.com/azxcvn). I am not responsible.
- This project is **free and open source**, and the author has not obtained any economic benefits from it.
- This project does not bypass authentication mechanisms, crack paid resources, or engage in other illegal activities.
- "哔哩哔哩" and "Bilibili" names, logos, and related graphics are registered trademarks or trademarks of Shanghai Huandian Information Technology Co., Ltd.
- This project is an independent third-party tool and has no affiliation, cooperation, authorization, or endorsement with Bilibili or its affiliated companies.
- Content obtained using this project is copyrighted by the original rights holders. Please comply with relevant laws, regulations, and platform service agreements.
- If there is any infringement, please feel free to [contact](https://github.com/azxcvn) for resolution.

---

## Screenshots

### Application Interface (Portrait)

| Home | Folder List | Video List |
|------|------------|------------|
| <img src="docs/screenshots/软件主页2.jpg" width="280"/> | <img src="docs/screenshots/文件夹列表2.jpg" width="280"/> | <img src="docs/screenshots/视频列表2.jpg" width="280"/> |

| WebDAV Feature | Bangumi Parsing | Settings Page |
|----------------|----------------|----------------|
| <img src="docs/screenshots/webdav支持.jpg" width="280"/> | <img src="docs/screenshots/番剧解析.jpg" width="280"/> | <img src="docs/screenshots/设置页面.jpg" width="280"/> |

---

### Player Interface (Landscape)

| Player Main Interface | Danmaku Styling | Subtitle Styling |
|----------------------|----------------|------------------|
| <img src="docs/screenshots/播放器主界面.jpg" width="280"/> | <img src="docs/screenshots/弹幕样式设置.jpg" width="280"/> | <img src="docs/screenshots/字幕样式设置.jpg" width="280"/> |

| Resume Playback | More Menu | Super-Resolution |
|----------------|-----------|------------------|
| <img src="docs/screenshots/记忆播放.jpg" width="280"/> | <img src="docs/screenshots/更多菜单.jpg" width="280"/> | <img src="docs/screenshots/超分功能.jpg" width="280"/> |

## Key Features

- **Video Playback**: Support for mainstream video formats (MP4, MKV, AVI, etc.)
- **Web Sniffing Feature**: Built-in WebView for sniffing web videos, automatically selecting the best quality, and one-click playback
- **Bilibili Bangumi Support**: Login to Bilibili account, stream bangumi online (see [Login Implementation](docs/bilibili_login.md) and [Bangumi Parsing Principle](docs/bilibili_bangumi.md))
- **Bilibili Video/Bangumi Download**: Download Bilibili videos and bangumi to local storage (see [Download Implementation Principle](docs/bilibili_download_principle.md))
  - Support for full URLs, short links (b23.tv), and text-embedded share links
  - Automatic video information parsing, support for multi-episode bangumi selection
  - Automatic audio-video merging into MP4 format
  - Support for pause, resume, and cancel downloads
  - Real-time download progress display
  - ⚠️ **For personal learning use only, commercial use is strictly prohibited**
- **WebDAV Network Storage**: Connect to WebDAV servers and stream cloud-hosted videos directly (see [WebDAV Usage Guide](docs/webdav使用说明.md))
- **Playlist Management**: Automatic folder scanning, video sorting and categorization
- **Subtitle Handling**: Built-in subtitle parsing, external subtitle import, subtitle position and size adjustment
- **Auto-load Subtitles**: Automatically load subtitle files with the same name in the same folder
- **Audio Tracks**: Multi-track audio switching
- **Volume Boost**: Toggle volume boost feature with fine-grained 0.1% adjustment
- **Danmaku Features**:
  - Support for importing local XML format danmaku files
  - **Support for downloading danmaku from Bilibili** (see [Danmaku Download Principle](docs/bilibili_danmaku_download.md))
    - Uses Bilibili's segmented danmaku API for complete data retrieval
    - Supports batch downloads for regular videos and entire bangumi seasons
    - Concurrent download technology, 10-20x speed improvement
    - Automatically includes login cookies to access premium danmaku
  - Customizable danmaku styles (size, speed, transparency, stroke, etc.)
  - Danmaku track management, show/hide different types of danmaku
  - Auto-remember danmaku files and display status
  - High refresh rate screen adaptation (supports 90Hz/120Hz/144Hz)
  - Danmaku synchronized with video progress, supports chapter jumping
- **Gesture Controls**:
  - Left swipe: Adjust brightness
  - Right swipe: Adjust volume
  - Horizontal swipe: Fast forward/rewind
  - Double tap: Pause/play
  - Long press: Speed playback
  - Progress bar drag: Precise positioning
- **Playback Controls**: Fast forward/rewind, speed playback, subtitle delay adjustment
- **Super Resolution**: Integrated Anime4K, supports real-time video upscaling
- **Playback Progress Resume**: Automatically save playback progress, resume on next open
- **Screenshot Feature**: Support for video screenshot saving

## Technical Architecture

- **Video Engine**: libmpv (open-source multimedia player library)
- **UI Framework**: Android AppCompat
- **Programming Languages**: Kotlin + Java
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 34 (Android 14)

## Planned Features

The following features are planned but not yet implemented:

- Frame interpolation
- Subtitle font customization
- Font selection

## Acknowledgments

This project would not be possible without the support of the following open-source projects:

- **[mpv-player/mpv](https://github.com/mpv-player/mpv)**  
  The core foundation of this project, a powerful multimedia player library

- **[mpv-android/mpv-android](https://github.com/mpv-android/mpv-android)**  
  Reference implementation for Android mobile

- **[abdallahmehiz/mpv-android](https://github.com/abdallahmehiz/mpv-android/releases)**  
  Provides ready-to-use libmpv library files

- **[abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt)**  
  Reference for gesture controls, swipe handling, and external subtitle import

- **[bloc97/Anime4K](https://github.com/bloc97/Anime4K)**  
  Source of super-resolution GLSL shader files

- **[Predidit/Kazumi](https://github.com/Predidit/Kazumi)**  
  Project development inspiration and original requirements

- **[xyoye/DanDanPlayForAndroid](https://github.com/xyoye/DanDanPlayForAndroid)**  
  Referenced danmaku implementation and refactoring, WebDAV functionality implementation, and many other features

- **[bilibili/DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)**  
  The danmaku core engine for this project is Bilibili's open-source danmaku parsing and rendering engine

- **[SocialSisterYi/bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)**  
  Thanks for collecting public APIs and centralizing scattered APIs. This project referenced the usage methods

- **[the1812/Bilibili-Evolved](https://github.com/the1812/Bilibili-Evolved)**  
  Bilibili enhancement script, referenced concurrent optimization strategies and API calling methods for danmaku downloads

- **[btjawa/BiliTools](https://github.com/btjawa/BiliTools)**  
  Referenced the implementation principles of video/bangumi download from this project

- **[qiusunshine/hikerView](https://github.com/qiusunshine/hikerView)**  
  Referenced the sniffing feature implementation and specific anti-sniffing algorithm logic for certain scenarios

- **[thegrizzlylabs/sardine-android](https://github.com/thegrizzlylabs/sardine-android)**  
  Provides Android WebDAV client implementation, supporting file browsing, uploading, and downloading operations

- **[ngallagher/simplexml](https://github.com/ngallagher/simplexml)**  
  Provides a lightweight XML serialization framework for parsing XML format response data from WebDAV servers

---

Thanks to all the above open-source projects and developers for their selfless contributions. Without your efforts, this project would not have been possible!

## Third-Party Service Disclosure

This application uses public APIs from the following third-party services:

- **Bilibili** - For login, parsing bangumi links for online streaming, downloading danmaku, and downloading videos/bangumi
  - Login API: `https://passport.bilibili.com/x/passport-login/web/qrcode/*`
  - Bangumi Info API: `https://api.bilibili.com/pgc/view/web/season`
  - Bangumi Playback API: `https://api.bilibili.com/pgc/player/web/playurl`
  - Danmaku Download API: `https://api.bilibili.com/x/v1/dm/list.so`
  - Video Info API: `https://api.bilibili.com/x/web-interface/view`
  - Video Download API: `https://api.bilibili.com/x/player/playurl`
  - Bangumi Download API: `https://api.bilibili.com/pgc/player/web/playurl`
  - Usage Scenarios:
    - User actively scans QR code to login to Bilibili account
    - User inputs bangumi link to watch online bangumi
    - User actively inputs Bilibili video link to download danmaku
  - Data Processing:
    - Login credentials are encrypted with AES-256 and stored locally, see [Security Documentation](docs/bilibili_security_analysis.md)
    - Downloaded danmaku data is only saved on the user's local device
    - All data will not be uploaded or shared with third parties
  - Disclaimer: This application has no official affiliation with Bilibili and only uses its public APIs

**Privacy Statement**:

This application highly values user privacy protection. Here is our statement:

### Data Collection
- ❌ **Does NOT collect** any user personal information
- ❌ **Does NOT upload** any data to our servers (we don't have servers)
- ❌ **Does NOT share** user data with any third parties
- ✅ All features run **locally on device**

### Bilibili Login Feature
- Login credentials are encrypted with **AES-256 military-grade encryption** and stored locally (see [Security Analysis](docs/bilibili_security_analysis.md))
- Login keys are protected by Android KeyStore hardware, **cannot be exported by the app**
- Login information is only used for Bilibili API calls, **not uploaded anywhere else**
- Users can **logout with one click** in settings to completely clear all login data
- After app uninstallation, all login data will be **automatically and permanently destroyed**

### Danmaku and Bangumi Data
- Danmaku files and bangumi data are saved in **user-specified local folders**
- Download features are entirely **user-initiated**
- Data is only stored locally, **not synced or backed up to cloud**

### Video/Bangumi Download Feature
- ⚠️ **Important Reminder**: The video download feature is **for personal learning and technical exchange only**
- Downloaded video content is **copyrighted by the original authors**, please delete within 24 hours after download
- **Commercial use is strictly prohibited**, including but not limited to:
  - Redistribution, resale, watermark removal and re-upload
  - Commercial screening, advertising profit
  - Any behavior that infringes on copyright owners' rights
- **Legal liability arising from the use of the download feature is borne by the user**, and has nothing to do with this project
- This project is not responsible for the abuse of the download feature, please comply with relevant laws and regulations
- It is recommended to only download content you own the copyright to or have created, respecting the labor of UP主 and copyright owners

### Permission Statement
The app only requests the following necessary permissions:
- **Storage Permission (Manage All Files)**: Read and save local videos, subtitles, and danmaku files, auto-load subtitle files with the same name
- **Network Permission**: For Bilibili bangumi online streaming, video/bangumi downloads, and danmaku downloads (user-initiated)

### Open Source Transparency
- ✅ Project is **completely open source**, all code is publicly auditable
- ✅ Welcome security experts to conduct code audits
- ✅ If security issues are found, please report them promptly

**Commitment**: This application will never sell or share user data, because we don't collect any in the first place!

## Development Notes

**This project was entirely developed by AI** for code implementation, with the author responsible for testing, feedback, and solution design.

Due to the nature of AI-generated code, the project may contain issues such as **code redundancy and less streamlined structure**. Although the project has undergone **two major code structure optimizations** with comprehensive refactoring and reorganization, the following situations may still exist:
- Some code logic may be complex
- Deprecated code that has been commented out but not removed
- Some implementation approaches may not be elegant

We apologize for any optimization shortcomings! The project is still under continuous improvement, and suggestions and feedback are welcome.

## System Requirements

- Android 9.0 or higher
- At least 64GB storage space
- Recommended 8GB or more RAM

## Usage

1. Install the application
2. Grant file access permissions
3. Open the app and browse local video files
4. Click on a video to start playback

## Feedback and Suggestions

If you encounter any issues or have suggestions, please feel free to report them!

---

**Last Updated:** 2025-12-02
