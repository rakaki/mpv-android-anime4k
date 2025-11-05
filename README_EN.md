# FAMPV4K - Local Video Player

An Android local video player based on libmpv, supporting multiple video formats, subtitle processing, gesture controls, and real-time upscaling features.

## About the Name

**FAMPV4K** is an abbreviation for **For Anime Media Player Video 4K**:
- **F**or - For
- **A**nime - Anime
- **M**edia - Media
- **P**layer - Player
- **V**ideo - Video
- **4K** - Ultra high definition resolution

This project aims to optimize and upscale anime-style videos, though it can also be used as a regular video player.

## Screenshots

| Home | Settings | Playback |
|------|----------|----------|
| ![Home](docs/screenshots/Screenshot_20251026_114205.jpg) | ![Playback](docs/screenshots/Screenshot_20251026_114217.jpg) | ![Settings](docs/screenshots/Screenshot_20251026_114259.jpg) |

| Subtitle Management | Playback Control |
|-------------------|------------------|
| ![Subtitles](docs/screenshots/Screenshot_20251026_114420.jpg) | ![Control](docs/screenshots/IMG_20251026_114329.jpg) |

## Key Features

- **Video Playback**: Support for mainstream video formats (MP4, MKV, AVI, etc.)
- **Playlist Management**: Automatic folder scanning, video sorting and categorization
- **Subtitle Handling**: Built-in subtitle parsing, external subtitle import, subtitle position and size adjustment
- **Audio Tracks**: Multi-track audio switching
- **Volume Boost**: Toggle volume boost feature with fine-grained 0.1% adjustment
- **Danmaku Support**: Load XML format danmaku files, toggle danmaku display, adjust danmaku styles
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

## Development Notes

**This project was entirely developed by AI** for code implementation, with the author responsible for testing, feedback, and solution design.

Due to the nature of AI-generated code, the project may contain code redundancy or less optimized implementations. Thank you for your understanding.

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

**Last Updated:** 2025-11-05

**[中文版本](README.md) | [English Version](README_EN.md)**
