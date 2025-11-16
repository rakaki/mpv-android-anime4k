# .nomedia 支持功能说明

## 功能概述

已为播放器项目添加了对 `.nomedia` 文件的完整支持。当文件夹中存在 `.nomedia` 文件时，该文件夹及其子文件夹中的视频将不会被扫描和显示。

## 实现详情

### 1. 新增工具类：`NoMediaChecker.kt`

位置：`app/src/main/java/com/fam4k007/videoplayer/utils/NoMediaChecker.kt`

提供三个主要方法：
- `containsNoMedia(path: String?)`: 检查路径或其任何父目录是否包含 .nomedia 文件
- `folderHasNoMedia(folderPath: String?)`: 检查指定文件夹是否直接包含 .nomedia 文件
- `fileInNoMediaFolder(filePath: String?)`: 检查文件所在文件夹或其父目录是否包含 .nomedia 文件

### 2. 修改的文件

#### VideoBrowserActivity.kt
- 在 `scanVideoFiles()` 方法中添加 .nomedia 检测
- 扫描视频时会自动跳过包含 .nomedia 的文件夹

#### VideoListActivity.kt
- 在 `refreshVideoList()` 方法中添加 .nomedia 检测
- 在 `scanFolderVideos()` 方法中添加 .nomedia 检测
- 刷新视频列表时会跳过包含 .nomedia 的文件夹

#### VideoScanner.kt
- 在 `getAllVideos()` 异步方法中添加 .nomedia 过滤
- 在 `getAllVideosSync()` 同步方法中添加 .nomedia 过滤

#### SeriesManager.kt
- 在 `getVideosFromMediaStore()` 方法中添加 .nomedia 检测
- 在 `getVideosFromFile()` 方法中添加 .nomedia 检测
- 连播功能会自动跳过包含 .nomedia 的文件夹

## 使用方法

### 如何标记文件夹不被扫描

1. 在任何文件夹中创建一个名为 `.nomedia` 的空文件（注意：文件名以点开头）
2. 该文件夹及其所有子文件夹中的视频将不会被播放器扫描和显示
3. 重新扫描视频列表后生效

### 测试步骤

1. 在某个包含视频的文件夹中创建 `.nomedia` 文件：
   ```bash
   # 使用文件管理器或命令行
   touch /sdcard/Videos/TestFolder/.nomedia
   ```

2. 在播放器中下拉刷新视频列表

3. 验证该文件夹中的视频不再显示

4. 删除 `.nomedia` 文件后，再次刷新，视频应重新出现

## 技术特点

- **高效检测**：只在扫描视频时进行检测，不影响正常播放性能
- **递归检查**：支持检查父目录层次结构，符合 Android 系统行为
- **日志记录**：跳过的视频会在 Logcat 中记录，便于调试
- **兼容性好**：兼容不同的文件访问方式（MediaStore、DocumentFile、File）

## 注意事项

1. `.nomedia` 文件是 Android 系统的标准约定，系统媒体扫描器也会遵守此规则
2. 该功能主要用于排除不希望在媒体库中显示的私密文件或临时文件
3. 已被 MediaStore 索引的文件可能需要重启应用或重新扫描才能完全生效
4. 删除 `.nomedia` 文件后，需要手动刷新视频列表
