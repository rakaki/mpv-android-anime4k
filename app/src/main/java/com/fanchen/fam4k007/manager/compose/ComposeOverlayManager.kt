package com.fanchen.fam4k007.manager.compose

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner

/**
 * 通用Compose覆盖层管理器
 * 负责管理Activity中的ComposeView容器,提供统一的Compose内容展示接口
 */
class ComposeOverlayManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val rootView: ViewGroup
) {
    private var composeView: ComposeView? = null

    /**
     * 设置Compose内容
     * @param content Composable内容
     */
    fun setContent(content: @Composable () -> Unit) {
        if (composeView == null) {
            composeView = ComposeView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setContent(content)
            }
            rootView.addView(composeView)
        } else {
            composeView?.setContent(content)
        }
    }

    /**
     * 清空内容并移除视图
     */
    fun clearContent() {
        composeView?.let {
            rootView.removeView(it)
        }
        composeView = null
    }

    /**
     * 检查是否有内容正在显示
     */
    fun hasContent(): Boolean = composeView != null

    /**
     * 释放资源
     */
    fun release() {
        clearContent()
    }

    // ===== 字幕对话框相关 =====

    /**
     * 显示字幕设置抽屉（统一入口）
     */
    fun showSubtitleSettingsDrawer(
        currentDelay: Double,
        currentScale: Float,
        currentPosition: Int,
        currentBorderSize: Int,
        currentTextColor: String,
        currentBorderColor: String,
        currentBackColor: String,
        currentBorderStyle: String,
        onDelayChange: (Double) -> Unit,
        onScaleChange: (Float) -> Unit,
        onPositionChange: (Int) -> Unit,
        onBorderSizeChange: (Int) -> Unit,
        onTextColorChange: (String) -> Unit,
        onBorderColorChange: (String) -> Unit,
        onBackColorChange: (String) -> Unit,
        onBorderStyleChange: (String) -> Unit
    ) {
        setContent {
            SubtitleSettingsDrawer(
                currentDelay = currentDelay,
                currentScale = currentScale,
                currentPosition = currentPosition,
                currentBorderSize = currentBorderSize,
                currentTextColor = currentTextColor,
                currentBorderColor = currentBorderColor,
                currentBackColor = currentBackColor,
                currentBorderStyle = currentBorderStyle,
                onDelayChange = onDelayChange,
                onScaleChange = onScaleChange,
                onPositionChange = onPositionChange,
                onBorderSizeChange = onBorderSizeChange,
                onTextColorChange = onTextColorChange,
                onBorderColorChange = onBorderColorChange,
                onBackColorChange = onBackColorChange,
                onBorderStyleChange = onBorderStyleChange,
                onDismiss = { clearContent() }
            )
        }
    }
    
    /**
     * 显示字幕延迟对话框（已废弃，保留兼容）
     */
    @Deprecated("使用 showSubtitleSettingsDrawer 替代")
    fun showSubtitleDelayDialog(
        currentDelay: Double,
        onDelayChange: (Double) -> Unit
    ) {
        setContent {
            SubtitleDelayDialog(
                currentDelay = currentDelay,
                onDelayChange = onDelayChange,
                onDismiss = { clearContent() }
            )
        }
    }

    /**
     * 显示字幕杂项对话框（已废弃，保留兼容）
     */
    @Deprecated("使用 showSubtitleSettingsDrawer 替代")
    fun showSubtitleMiscDialog(
        currentScale: Float,
        currentPosition: Int,
        onScaleChange: (Float) -> Unit,
        onPositionChange: (Int) -> Unit
    ) {
        setContent {
            SubtitleMiscDialog(
                currentScale = currentScale,
                currentPosition = currentPosition,
                onScaleChange = onScaleChange,
                onPositionChange = onPositionChange,
                onDismiss = { clearContent() }
            )
        }
    }

    /**
     * 显示字幕样式对话框（已废弃，保留兼容）
     */
    @Deprecated("使用 showSubtitleSettingsDrawer 替代")
    fun showSubtitleStyleDialog(
        currentBorderSize: Int,
        onBorderSizeChange: (Int) -> Unit
    ) {
        setContent {
            SubtitleStyleDialog(
                currentBorderSize = currentBorderSize,
                onBorderSizeChange = onBorderSizeChange,
                onDismiss = { clearContent() }
            )
        }
    }
}
