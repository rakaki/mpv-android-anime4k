# ProGuard rules for release build

# ============================================
# 核心规则：保护 MPV 和 Native 调用
# ============================================

# 1. 保护 MPV 库（最重要！）
-keep class dev.jdtech.mpv.** { *; }
-keep interface dev.jdtech.mpv.** { *; }
-keepclassmembers class dev.jdtech.mpv.** { *; }

# 2. 保护所有 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. 保护 Parcelable（用于 Intent 传递数据）
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ============================================
# 可选规则：如果 Release 崩溃再启用
# ============================================

# Glide 图片加载库（如果截图或缩略图失败，启用下面）
# -keep public class * implements com.bumptech.glide.module.GlideModule
# -keep class com.bumptech.glide.** { *; }

# 保护所有自定义 View（如果界面显示异常，启用下面）
# -keep public class * extends android.view.View {
#     public <init>(android.content.Context);
#     public <init>(android.content.Context, android.util.AttributeSet);
# }

# 保护数据类（如果视频列表传递失败，启用下面）
# -keep class com.fam4k007.videoplayer.VideoFolder { *; }
# -keep class com.fam4k007.videoplayer.VideoFile { *; }
# -keep class com.fam4k007.videoplayer.VideoFileParcelable { *; }
