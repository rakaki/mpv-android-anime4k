# 弹弹play 弹幕 API 接入技术方案

## 1. 概述
本方案旨在为播放器接入弹弹play (DanDanPlay) 的开放 API，实现通过文件名或哈希匹配番剧，并下载加载弹幕的功能。

## 2. 核心组件设计

### 2.1 网络层 (Network Layer)
利用现有的 `OkHttp` + `Gson` 技术栈，不引入新的网络库 (如 Retrofit)，保持项目一致性。

#### 2.1.1 `DanDanApiManager` (单例)
*   **职责**: 管理 OkHttpClient 实例，提供 API 方法入口。
*   **位置**: `com.fam4k007.videoplayer.dandan.DanDanApiManager`
*   **主要成员**:
    *   `client`: 配置了 `DanDanAuthInterceptor` 的 OkHttpClient。
    *   `gson`: 用于 JSON 解析。

#### 2.1.2 `DanDanAuthInterceptor` (OkHttp Interceptor)
*   **职责**: 处理 API 签名验证。
*   **逻辑**:
    1.  拦截请求。
    2.  获取当前时间戳和 API Key。
    3.  收集所有 URL Query 参数。
    4.  按 Key 字母顺序排序参数。
    5.  拼接参数字符串：`key1=value1&key2=value2...`。
    6.  在末尾拼接 AppSecret。
    7.  计算 UTF-8 编码的 MD5 哈希值作为签名。
    8.  将 `api_signature` 添加到请求参数中。

### 2.2 数据模型 (Data Models)
定义与 API 响应对应的 Kotlin Data Classes。
*   **位置**: `com.fam4k007.videoplayer.dandan.model`
*   **主要模型**:
    *   `DanDanApiResponse<T>`: 通用响应包装 (errorCode, success, result)。
    *   `DanDanMatchResponse`: 匹配结果 (包含 episodeId, animeTitle, episodeTitle 等)。
    *   `DanDanCommentResponse`: 弹幕数据 (包含 time, mode, color, message)。

### 2.3 业务逻辑层 (Business Logic)

#### 2.3.1 `DanDanDanmakuService`
*   **职责**: 封装具体的业务操作。
*   **方法**:
    *   `matchVideo(fileName: String, fileHash: String, length: Long)`: 匹配视频。
    *   `getComments(episodeId: Long)`: 获取指定集数的弹幕。
    *   `searchAnime(keyword: String)`: 手动搜索番剧 (可选后续实现)。

#### 2.3.2 `DanDanConverter`
*   **职责**: 将弹弹play的弹幕格式转换为播放器内核 (DanmakuFlameMaster) 可用的格式。
*   **逻辑**:
    *   输入: `List<DanDanComment>`
    *   输出: B站格式 XML 字符串 或 直接生成 `BaseDanmaku` 对象流。
    *   *考虑到现有 `DanmakuManager` 主要通过文件加载，建议将转换后的弹幕保存为临时 XML 文件，复用现有的加载逻辑。*

## 3. 详细实施步骤

### 阶段一：基础架构搭建
1.  **创建包结构**: `com.fam4k007.videoplayer.dandan` 及其子包 `model`, `service`, `utils`。
2.  **定义数据模型**: 编写 Kotlin data classes。
3.  **实现签名工具**: 编写 MD5 计算和签名生成工具类。
4.  **实现拦截器**: 编写 `DanDanAuthInterceptor`。
5.  **实现 API 管理器**: 编写 `DanDanApiManager`。

### 阶段二：弹幕功能实现
1.  **实现匹配逻辑**: 在 `DanDanApiManager` 中添加 `match` 方法。
2.  **实现弹幕下载**: 在 `DanDanApiManager` 中添加 `getComments` 方法。
3.  **实现格式转换**: 编写 `DanDanConverter`，支持将 JSON 弹幕转存为 XML 文件。
4.  **集成到播放器**:
    *   在视频播放开始时，异步触发匹配请求。
    *   匹配成功后，自动下载弹幕。
    *   转换并保存为缓存文件。
    *   调用 `DanmakuManager.loadDanmakuFile()` 加载弹幕。

## 4. 关键代码示例 (伪代码)

### 4.1 签名生成
```kotlin
fun generateSignature(params: Map<String, String>, appSecret: String): String {
    val sortedParams = params.toSortedMap()
    val sb = StringBuilder()
    for ((k, v) in sortedParams) {
        sb.append(k).append("=").append(v).append("&")
    }
    sb.append(appSecret) // 只要直接拼接，不需要 key
    return md5(sb.toString())
}
```

### 4.2 弹幕转换 (JSON -> XML)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<i>
    <chatserver>chat.bilibili.com</chatserver>
    <chatid>12345</chatid>
    <mission>0</mission>
    <maxlimit>3000</maxlimit>
    <source>k-v</source>
    <!-- time,mode,size,color,timestamp,pool,senderId,rowId -->
    <d p="23.82600,1,25,16777215,1422201084,0,b3a2459c,0">弹幕内容</d>
    ...
</i>
```

## 5. UI/UX 规划 (后续阶段)
*   **匹配结果确认**: 如果自动匹配置信度低，弹出对话框让用户选择正确的剧集。
*   **手动搜索**: 提供界面允许用户手动搜索番剧并绑定。

## 6. 注意事项
*   **API 限流**: 注意处理 API 请求频率。
*   **错误处理**: 网络失败、解析失败、无匹配结果等情况的优雅处理。
*   **AppId/Secret**: 需要申请或配置 Key。代码中应预留配置入口。
