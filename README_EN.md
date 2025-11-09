# XiaoMiao Player - Local Video Player

**[中文版本](README.md) | [English Version](README_EN.md)**

An Android local video player based on libmpv, supporting multiple video formats, subtitle processing, gesture controls, and real-time upscaling features.

This project aims to optimize and upscale anime-style videos, though it can also be used as a regular video player.

## Screenshots

### Application Interface (Portrait)

| Home | Video List & Sorting | Playback History | Settings |
|------|---------------------|------------------|----------|
| <img src="docs/screenshots/主页.jpg" width="200"/> | <img src="docs/screenshots/排序功能与视频列表.jpg" width="200"/> | <img src="docs/screenshots/播放历史.jpg" width="200"/> | <img src="docs/screenshots/设置页.jpg" width="200"/> |

### Player Interface (Landscape)

| Danmaku Feature | Danmaku Styling |
|----------------|----------------|
| <img src="docs/screenshots/弹幕功能.jpg" width="400"/> | <img src="docs/screenshots/弹幕样式设置.jpg" width="400"/> |

| Subtitle Styling | Super-Resolution |
|-----------------|------------------|
| <img src="docs/screenshots/字幕样式设置.jpg" width="400"/> | <img src="docs/screenshots/超分功能.jpg" width="400"/> |

## Key Features

- **Video Playback**: Support for mainstream video formats (MP4, MKV, AVI, etc.)
- **Playlist Management**: Automatic folder scanning, video sorting and categorization
- **Subtitle Handling**: Built-in subtitle parsing, external subtitle import, subtitle position and size adjustment
- **Audio Tracks**: Multi-track audio switching
- **Volume Boost**: Toggle volume boost feature with fine-grained 0.1% adjustment
- **Danmaku Features**:
  - Support for importing local XML format danmaku files
  - **Support for downloading danmaku from Bilibili** (using Bilibili's public API)
  - Customizable danmaku styles (size, speed, transparency, stroke, etc.)
  - Danmaku track management, show/hide different types of danmaku
  - Auto-save danmaku files and display states
  - High refresh rate screen adaptation (supports 90Hz/120Hz/144Hz)
  - Danmaku synchronization with video progress, supports chapter jumping
- **Gesture Controls**:
  - Left swipe: Adjust brightness
  - Right swipe: Adjust volume
  - Horizontal swipe: Fast forward/rewind
  - Double tap: Pause/play
  - Long press: Speed up playback
  - Progress bar drag: Precise positioning
- **Playback Controls**: Fast forward/rewind, speed control, subtitle delay adjustment
- **Super-Resolution**: Integrated Anime4K for real-time video upscaling
- **Resume Playback**: Automatically saves playback progress and resumes from where you left off
- **Screenshot**: Support for video screenshot capture

## Technical Architecture

- **Video Engine**: libmpv (Open-source multimedia player library)
- **UI Framework**: Android AppCompat
- **Programming Language**: Kotlin + Java
- **Minimum SDK**: 26 (Android 8.0)
- **Compile SDK**: 34 (Android 14)

## Planned Features

The following features are planned but not yet implemented:

- Frame interpolation
- Subtitle font customization
- Font selection
- Player lock mode
- Video zoom functionality
- Online caching
- Online video playback
- UI themes and dark mode

## Acknowledgments

This project would not be possible without the support of the following open-source projects:

| Project | Description |
|---------|-------------|
| [mpv-player/mpv](https://github.com/mpv-player/mpv) | The core foundation of this project, a powerful multimedia player library |
| [mpv-android/mpv-android](https://github.com/mpv-android/mpv-android) | Reference implementation for Android mobile |
| [abdallahmehiz/mpv-android](https://github.com/abdallahmehiz/mpv-android/releases) | Provides ready-to-use libmpv library files |
| [abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt) | Reference for gesture controls, swipe handling, and external subtitle import |
| [bloc97/Anime4K](https://github.com/bloc97/Anime4K) | Source of super-resolution GLSL shader files |
| [Predidit/Kazumi](https://github.com/Predidit/Kazumi) | Project development inspiration and original requirements |
| [xyoye/DanDanPlayForAndroid](https://github.com/xyoye/DanDanPlayForAndroid) | This project extensively references the danmaku implementation from this project. Many thanks! |
| [bilibili/DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster) | The danmaku core engine for this project is Bilibili's open-source danmaku library. Many thanks! |

## Third-Party Service Disclosure

This application uses public APIs from the following third-party services:

- **Bilibili** - For downloading video and bangumi danmaku
  - Video Info API: `https://www.bilibili.com/video/*`
  - Bangumi Info API: `https://api.bilibili.com/pgc/view/web/season`
  - Danmaku Download API: `https://api.bilibili.com/x/v1/dm/list.so`
  - Usage: When users actively input a Bilibili video or bangumi link, the app accesses these APIs to retrieve danmaku data
  - Data Processing: Downloaded danmaku data is only stored on the user's local device and is not uploaded or shared
  - Disclaimer: This application has no official affiliation with Bilibili and only uses its public APIs

**Privacy Statement**:
- The app does not collect or upload any user personal information
- Danmaku download feature is entirely user-initiated
- All downloaded data is saved to user-specified local folders

## Development Notes

**This project was entirely developed by AI** for code implementation, with the author responsible for testing, feedback, and solution design.

Due to the nature of AI-generated code, the project may contain issues such as **code redundancy and less streamlined structure**. Although the project has undergone **two major code structure optimizations** with comprehensive refactoring and reorganization, the following situations may still exist:
- Some code logic may be complex
- Deprecated code that has been commented out but not removed
- Some implementation approaches may not be elegant

We apologize for any optimization shortcomings! The project is still under continuous improvement, and suggestions and feedback are welcome.

## System Requirements

- Android 8.0 or higher
- At least 100MB storage space
- Recommended 2GB or more RAM

## Usage

1. Install the application
2. Grant file access permissions
3. Open the app and browse local video files
4. Click on a video to start playback

## Feedback and Suggestions

If you encounter any issues or have suggestions, please feel free to report them!

---

**Last Updated:** 2025-11-08

**[中文版本](README.md) | [English Version](README_EN.md)**
