# 哔哩哔哩视频/番剧下载实现原理

## 一、核心流程

```
用户输入链接 → 解析链接 → 获取视频信息 → 下载音视频流 → 合并文件 → 完成
```

## 二、关键步骤详解

### 1. 链接解析
- **短链处理**：`b23.tv` 短链通过HTTP重定向获取真实URL
- **ID提取**：从URL中提取BV号（如`BV1eRYfzxEJj`）或AV号（如`av123456`）
- **类型判断**：区分普通视频(`/video/`)和番剧(`/bangumi/play/`)

### 2. 获取视频详情
使用B站API获取视频基本信息：
```
API: https://api.bilibili.com/x/web-interface/view
参数: bvid=BV号 或 aid=AV号
返回: 标题、cid（内容ID）、时长等
```

### 3. 获取下载地址
通过cid获取实际的音视频流地址：
```
API: https://api.bilibili.com/x/player/playurl
参数: bvid/aid + cid + qn(清晰度) + fnval=4048(DASH格式)
返回: 音频流URL + 视频流URL
```

**关键点**：
- B站使用DASH格式，音频和视频是分离的
- 需要携带Cookie（SESSDATA）才能获取高清视频
- fnval=4048 表示请求DASH格式的流媒体

### 4. 分段下载
音频和视频分别下载，使用多线程提高速度：
- 通过`Range`请求头实现断点续传
- 每个片段独立下载，失败可重试
- 实时更新下载进度

### 5. 文件合并
使用Android的`MediaMuxer`合并音视频：
```kotlin
1. 创建MediaExtractor分别读取音频和视频
2. 创建MediaMuxer作为输出
3. 添加音频和视频轨道
4. 逐帧写入数据
5. 生成最终的MP4文件
```

## 三、技术要点

### BV号与AV号转换
- **BV号**：Base58编码，如`BV1eRYfzxEJj`（12位）
- **AV号**：纯数字，如`av123456`
- 算法基于B站官方公式，使用位置映射和异或运算

### Cookie管理
- 存储在`SharedPreferences`
- 关键字段：`SESSDATA`（登录凭证）
- 每次API请求需携带完整Cookie

### 番剧特殊处理
- 需要获取完整剧集列表
- 使用`season_id`或`ep_id`查询
- API：`https://api.bilibili.com/pgc/view/web/season`

### 存储路径选择
- 使用`DocumentFile` API访问外部存储
- 支持SAF（Storage Access Framework）
- 持久化URI权限，避免重复授权

## 四、代码结构

```
download/
├── BilibiliDownloadManager.kt    # 核心下载逻辑
├── BilibiliDownloadViewModel.kt  # UI状态管理
├── DownloadItem.kt               # 下载项数据类
└── MediaParseResult.kt           # 解析结果

DownloadActivity.kt                # 下载界面（Compose）
utils/CookieManager.kt             # Cookie存储管理
```

## 五、用户使用流程

1. **输入链接**：支持完整URL、短链、带文本的分享链接
2. **解析链接**：自动识别视频/番剧类型
3. **选择集数**：番剧可多选集数下载
4. **选择路径**：首次使用需选择下载目录
5. **开始下载**：后台下载，支持暂停/恢复/取消
6. **自动合并**：下载完成后自动合并音视频

## 六、注意事项

- **混淆保护**：Release版本需在ProGuard中添加keep规则
- **权限要求**：需要网络权限和存储权限
- **API限制**：下载速度可能受B站服务器限制
- **版权声明**：下载内容仅供个人学习使用

---

**实现语言**：Kotlin + Jetpack Compose  
**主要依赖**：OkHttp、Coroutines、MediaMuxer
