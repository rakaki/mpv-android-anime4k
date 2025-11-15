# B站弹幕下载原理

## 📖 简介

本项目实现了从B站视频/番剧下载弹幕的功能，支持XML格式的弹幕文件导出。

## 🔧 核心原理

### 1. 弹幕API

B站提供了两套弹幕API：

#### 分段弹幕API（主要）
```
https://api.bilibili.com/x/v2/dm/web/view?type=1&oid={cid}
https://api.bilibili.com/x/v2/dm/web/seg.so?type=1&oid={cid}&segment_index={index}
```

- **优点**：获取完整弹幕，数据结构完整
- **格式**：Protobuf 二进制格式
- **分段**：长视频会分成多个段（每段约6分钟）

#### 普通弹幕API（降级）
```
https://comment.bilibili.com/{cid}.xml
```

- **用途**：当分段API失败时的备用方案
- **格式**：Deflate 压缩的 XML
- **限制**：可能不完整

### 2. 下载流程

```
用户输入视频链接
    ↓
提取 CID (视频ID)
    ↓
获取弹幕元数据（总分段数）
    ↓
并发下载所有分段 ← 关键优化点
    ↓
解析 Protobuf 数据
    ↓
合并、去重、排序
    ↓
转换为 XML 格式
    ↓
保存到本地
```

### 3. 性能优化

**并发下载策略**：
- 传统方式：串行下载，一个接一个（慢）
- 优化方式：**并发下载所有分段**（快 10-20倍）

```kotlin
// 并发下载所有分段
(1..totalSegments).map { segmentIndex ->
    async { downloadSegment(segmentIndex) }
}.awaitAll()
```

### 4. Cookie 支持

**为什么需要 Cookie？**
- ✅ 获取会员专属弹幕
- ✅ 提高 API 调用频率限制
- ✅ 访问需要登录的内容

**实现方式**：
```kotlin
// 自动携带用户登录 Cookie
builder.addHeader("Cookie", authManager.getCookieString())
```

## 📊 支持功能

| 功能 | 支持 |
|------|------|
| 普通视频弹幕 | ✅ |
| 番剧单集弹幕 | ✅ |
| 番剧整季批量下载 | ✅ |
| 短链接解析 | ✅ |
| 登录状态（Cookie） | ✅ |
| 并发下载 | ✅ |

## 🔑 关键参数

- **CID**：视频的唯一标识，用于获取弹幕
- **分段索引**：从 1 开始的分段编号
- **Protobuf**：Google 的二进制序列化格式，需要手动解析

## 📝 XML 格式

生成的弹幕 XML 格式：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<i>
  <chatserver>chat.bilibili.com</chatserver>
  <chatid>0</chatid>
  <d p="时间,类型,字号,颜色,时间戳,池,用户hash,弹幕ID">弹幕内容</d>
  <d p="5.234,1,25,16777215,1699999999,0,abc123,12345">这是一条弹幕</d>
</i>
```

**p 属性说明**：
1. 时间（秒）
2. 类型（1-3滚动 4底部 5顶部）
3. 字号（18/25等）
4. 颜色（十进制RGB）
5. 发送时间戳
6. 弹幕池（0普通）
7. 用户ID的hash
8. 弹幕ID

## 🚀 性能数据

| 场景 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 单视频（10分段） | ~5秒 | ~0.5秒 | 10倍 |
| 番剧12集 | ~2分钟 | ~10秒 | 12倍 |

## 🔗 参考

- 核心实现：`BiliBiliDanmakuDownloadManager.kt`
- 参考项目：[Bilibili-Evolved](https://github.com/the1812/Bilibili-Evolved)
- API文档：B站开放API（非官方）
