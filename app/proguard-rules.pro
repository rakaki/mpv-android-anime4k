# ProGuard rules for release build

# ============================================
# 核心规则：保护 MPV 和 Native 调用
# ============================================

# 1. 保护 MPV 库（最重要！）
# 注意：使用的是 is.xyz.mpv，不是 dev.jdtech.mpv
-keep class is.xyz.mpv.** { *; }
-keep interface is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# 特别保护 MPVLib 和 BaseMPVView
-keep class is.xyz.mpv.MPVLib { *; }
-keep class is.xyz.mpv.BaseMPVView { *; }
-keep class is.xyz.mpv.MPVLib$** { *; }

# 2. 保护所有 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. 保护 Parcelable（用于 Intent 传递数据）
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# 4. 保护自定义 View（防止 CustomMPVView 被混淆）
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保护所有 Activity（确保 findViewById 和 Intent 正常工作）
-keep public class * extends androidx.appcompat.app.AppCompatActivity {
    public <methods>;
}
-keep public class * extends android.app.Activity {
    public <methods>;
}

# 5. 保护项目中的关键类
-keep class com.fam4k007.videoplayer.player.CustomMPVView { *; }
-keep class com.fam4k007.videoplayer.player.PlaybackEngine { *; }
-keep class com.fam4k007.videoplayer.player.PlayerControlsManager { *; }
-keep class com.fam4k007.videoplayer.player.GestureHandler { *; }

# 6. 保护设置管理和常量类（防止 SharedPreferences 键名被混淆）
-keep class com.fam4k007.videoplayer.AppConstants { *; }
-keep class com.fam4k007.videoplayer.AppConstants$** { *; }
-keep class com.fam4k007.videoplayer.manager.PreferencesManager { *; }

# 7. 保护数据类
-keep class com.fam4k007.videoplayer.VideoFolder { *; }
-keep class com.fam4k007.videoplayer.VideoFile { *; }
-keep class com.fam4k007.videoplayer.VideoFileParcelable { *; }

# ============================================
# 其他库规则
# ============================================

# Glide 图片加载库
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
-keepclassmembers class com.bumptech.glide.** { *; }

# Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ============================================
# WebDAV 相关规则
# ============================================

# Sardine WebDAV 客户端
-keep class com.xyoye.sardine.** { *; }
-keepclassmembers class com.xyoye.sardine.** { *; }
-dontwarn com.xyoye.sardine.**

# Simple XML - XML 解析库
-keep class org.simpleframework.xml.** { *; }
-keepclassmembers class org.simpleframework.xml.** { *; }
-dontwarn org.simpleframework.xml.**

# 忽略 javax.xml.stream (Android 不包含这些类，但 Simple XML 可以用其他方式解析)
-dontwarn javax.xml.stream.**

# 保护 WebDAV 相关类
-keep class com.fam4k007.videoplayer.webdav.** { *; }
-keepclassmembers class com.fam4k007.videoplayer.webdav.** { *; }

# OkHttp (WebDAV 依赖)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================
# Bilibili 相关规则
# ============================================

# Jsoup HTML 解析
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Gson JSON 解析
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Gson 使用泛型和反射进行序列化/反序列化，需要保护相关特性
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# 保护所有使用 @SerializedName 的字段
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保护 Bilibili 相关数据类（完整保护，防止混淆）
-keep class com.fam4k007.videoplayer.bilibili.** { *; }
-keepclassmembers class com.fam4k007.videoplayer.bilibili.** { *; }

# 保护所有 sealed class 和它们的子类（防止反射和序列化问题）
-keep class com.fam4k007.videoplayer.bilibili.model.LoginResult { *; }
-keep class com.fam4k007.videoplayer.bilibili.model.LoginResult$** { *; }

# 保护 BiliBiliPlayActivity 中的所有内部类和数据类
-keep class com.fam4k007.videoplayer.BiliBiliPlayActivity$** { *; }
-keep class com.fam4k007.videoplayer.PlayUiState { *; }
-keep class com.fam4k007.videoplayer.PlayUiState$** { *; }
-keep class com.fam4k007.videoplayer.SimpleBangumiInfo { *; }
-keep class com.fam4k007.videoplayer.SimpleEpisode { *; }
-keep class com.fam4k007.videoplayer.BangumiDetailResponse { *; }
-keep class com.fam4k007.videoplayer.BangumiDetailResult { *; }
-keep class com.fam4k007.videoplayer.EpisodeItem { *; }

# 保护 BiliBiliLoginActivity 中的 sealed class
-keep class com.fanchen.fam4k007.manager.compose.LoginUiState { *; }
-keep class com.fanchen.fam4k007.manager.compose.LoginUiState$** { *; }

# 保护弹幕下载管理器中的 sealed class
-keep class com.fam4k007.videoplayer.danmaku.BiliBiliDanmakuDownloadManager$DownloadResult { *; }
-keep class com.fam4k007.videoplayer.danmaku.BiliBiliDanmakuDownloadManager$DownloadResult$** { *; }

# 保护所有数据类的构造函数和字段（Gson 需要）
-keepclassmembers class com.fam4k007.videoplayer.bilibili.model.** {
    <init>(...);
    <fields>;
}

# 防止 data class 的 copy 方法被移除
-keepclassmembers class com.fam4k007.videoplayer.bilibili.model.** {
    public ** copy(...);
    public ** component*();
}

# ZXing 二维码
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Coil 图片加载
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================
# Room Database
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.migration.Migration

# 保持调试信息（方便定位崩溃）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
