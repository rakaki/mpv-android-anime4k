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

# 保持调试信息（方便定位崩溃）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
