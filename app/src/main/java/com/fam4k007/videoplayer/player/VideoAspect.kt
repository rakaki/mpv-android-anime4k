package com.fam4k007.videoplayer.player

/**
 * 视频画面比例模式
 */
enum class VideoAspect(val displayName: String) {
    FIT("适应屏幕"),      // 原始比例，完整显示
    STRETCH("拉伸"),     // 拉伸填充屏幕
    CROP("裁剪");        // 裁剪填充屏幕
    
    /**
     * 获取下一个模式（循环切换）
     */
    fun next(): VideoAspect {
        return when (this) {
            FIT -> STRETCH
            STRETCH -> CROP
            CROP -> FIT
        }
    }
}
