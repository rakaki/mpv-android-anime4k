# 哔哩哔哩番剧解析播放原理

## 简介

本文档详细说明应用如何解析B站番剧链接，获取视频播放地址，并实现在线播放。整个过程分为三个主要步骤：**解析番剧信息** → **获取播放地址** → **在线播放**。

## 第一步：解析番剧链接

### 用户输入的链接格式

B站番剧链接通常有以下几种格式：

```
完整链接：
https://www.bilibili.com/bangumi/play/ss12345
https://www.bilibili.com/bangumi/play/ep67890

短链接：
https://b23.tv/xxxxx
```

### 提取关键ID

应用需要从链接中提取两个关键ID之一：

1. **Season ID (ssid)**：番剧季度ID
   - 格式：`ss12345`
   - 例如：ss12345 代表某个番剧的一季

2. **Episode ID (epid)**：单集ID
   - 格式：`ep67890`
   - 例如：ep67890 代表某个番剧的某一集

```kotlin
// 提取ID的示例代码
fun extractSeasonId(url: String): String? {
    // 从 https://www.bilibili.com/bangumi/play/ss12345 提取 12345
    val ssPattern = "ss(\\d+)".toRegex()
    return ssPattern.find(url)?.groupValues?.get(1)
}

fun extractEpisodeId(url: String): String? {
    // 从 https://www.bilibili.com/bangumi/play/ep67890 提取 67890
    val epPattern = "ep(\\d+)".toRegex()
    return epPattern.find(url)?.groupValues?.get(1)
}
```

## 第二步：获取番剧信息

### 请求番剧详情

使用提取的ID向B站API请求番剧的详细信息：

```
API地址: https://api.bilibili.com/pgc/view/web/season

请求参数:
- season_id: 番剧季度ID（如果有的话）
- ep_id: 单集ID（如果有的话）

必须携带的请求头:
- User-Agent: 模拟浏览器
- Referer: https://www.bilibili.com
- Cookie: 登录凭证（如果需要会员权限）
```

### 返回的番剧信息

B站会返回一个JSON数据，包含：

```json
{
  "code": 0,
  "message": "success",
  "result": {
    "season_id": 12345,
    "title": "番剧名称",
    "cover": "封面图片链接",
    "evaluate": "简介",
    "episodes": [
      {
        "id": 67890,
        "aid": 11111,     // 视频AV号
        "cid": 22222,     // 视频CID号
        "title": "第1话",
        "long_title": "话标题",
        "cover": "封面"
      },
      // ... 更多集数
    ]
  }
}
```

**重要字段说明**：
- `aid` (AV号)：B站视频的唯一标识
- `cid` (CID号)：视频分P的唯一标识，用于获取播放地址
- `episodes`：包含该番剧所有集数的列表

## 第三步：获取播放地址

### 请求视频播放链接

有了`aid`和`cid`后，就可以获取实际的视频播放地址：

```
API地址: https://api.bilibili.com/pgc/player/web/playurl

请求参数:
- avid: 视频AV号
- cid: 视频CID号
- qn: 画质代码（16=360P, 32=480P, 64=720P, 80=1080P, 112=1080P+, 116=1080P60）
- fnval: 视频格式（1=MP4, 16=DASH）
- fourk: 是否请求4K（1=是）

必须携带:
- Cookie: 登录凭证（会员才能获取高画质）
- User-Agent: 浏览器标识
- Referer: https://www.bilibili.com
```

### 返回的播放地址

B站返回两种格式的视频数据：

#### 格式一：MP4直链（较简单）

```json
{
  "code": 0,
  "data": {
    "quality": 64,        // 实际画质
    "format": "mp4",
    "durl": [
      {
        "url": "https://...",  // 直接的视频地址
        "size": 12345678
      }
    ]
  }
}
```

优点：直接播放，兼容性好
缺点：画质较低，通常最高720P

#### 格式二：DASH格式（更常用）

```json
{
  "code": 0,
  "data": {
    "quality": 80,
    "format": "dash",
    "dash": {
      "video": [
        {
          "id": 80,           // 画质ID
          "base_url": "https://...",  // 视频流地址
          "codecid": 7        // 编码格式
        }
      ],
      "audio": [
        {
          "id": 30280,
          "base_url": "https://...",  // 音频流地址
          "codecid": 0
        }
      ]
    }
  }
}
```

优点：支持更高画质（1080P+）
特点：视频和音频分开，需要播放器合并

## 第四步：在线播放

### 使用libmpv播放器

应用使用`libmpv`这个强大的播放器库来播放视频：

```kotlin
// 播放DASH格式（最常见）
fun playDashVideo(videoUrl: String, audioUrl: String) {
    // 1. 设置视频流
    mpv.command(arrayOf("loadfile", videoUrl))
    
    // 2. 添加音频流
    mpv.command(arrayOf("audio-add", audioUrl))
    
    // 3. 设置必要的请求头（重要！）
    mpv.setOptionString("http-header-fields", 
        "Referer: https://www.bilibili.com,User-Agent: Mozilla/5.0..."
    )
}

// 播放MP4格式（较简单）
fun playMp4Video(videoUrl: String) {
    mpv.command(arrayOf("loadfile", videoUrl))
    mpv.setOptionString("http-header-fields", 
        "Referer: https://www.bilibili.com,User-Agent: Mozilla/5.0..."
    )
}
```

### 关键技术点

1. **HTTP请求头**：
   - 必须设置`Referer`为`https://www.bilibili.com`
   - 必须设置`User-Agent`模拟浏览器
   - 否则B站服务器会拒绝请求（返回403错误）

2. **Cookie处理**：
   - 如果是会员专享内容，必须携带登录Cookie
   - Cookie需要包含：SESSDATA、bili_jct、DedeUserID等字段
   - 未登录或Cookie过期会导致只能观看低画质或无法播放

3. **画质选择**：
   ```
   不同会员等级可用的画质：
   - 未登录/普通用户: 最高480P
   - 大会员: 可选1080P、1080P+、1080P60
   - 年度大会员: 可选4K（需要视频支持）
   ```

## 完整流程示意图

```
用户输入番剧链接
    ↓
提取Season ID或Episode ID
    ↓
调用番剧信息API → 获取episodes列表（含aid和cid）
    ↓
用户选择要看的集数
    ↓
调用播放地址API → 获取视频/音频URL
    ↓
libmpv加载视频和音频流 → 开始播放
```

## 常见问题

### Q: 为什么有的番剧播放不了？

A: 可能的原因：
1. **需要大会员**：部分番剧是会员专享，需要登录大会员账号
2. **地区限制**：某些番剧有地区限制（如港澳台限定）
3. **下架或过期**：版权到期的番剧会被下架

### Q: 为什么画质不高？

A: 可能的原因：
1. **未登录**：未登录只能看480P以下
2. **不是大会员**：普通用户最高720P
3. **视频源本身画质**：老番剧可能只有720P
4. **网络带宽限制**：B站会根据网速自动降低画质

### Q: DASH格式是什么？

A: DASH（Dynamic Adaptive Streaming over HTTP）是一种流媒体技术：
- 将视频和音频分开存储和传输
- 可以根据网速动态切换画质
- 支持更高的视频质量
- 现代视频网站的标准格式

### Q: 为什么有时候播放卡顿？

A: 可能的原因：
1. **网络速度慢**：建议切换到较低画质
2. **B站CDN节点问题**：可以尝试重新获取播放地址
3. **设备性能不足**：高画质视频需要较好的解码能力

## 技术优势

使用libmpv播放器的优势：
1. **强大的格式支持**：几乎支持所有视频格式
2. **优秀的性能**：高效的硬件解码
3. **稳定可靠**：成熟的开源项目
4. **丰富的功能**：支持字幕、音轨切换、倍速等

## 相关文档

- [登录实现说明](bilibili_login.md) - 了解如何登录B站账号
- [安全说明](bilibili_security_analysis.md) - 了解登录信息的安全保护措施

## 声明

本应用仅使用哔哩哔哩的公开API，与B站官方无任何关联。所有API调用均符合B站的使用规范。
