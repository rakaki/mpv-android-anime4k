package com.fam4k007.videoplayer.utils

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * 对话框工具类 - 集中管理所有 Toast 和 AlertDialog
 * 单例模式确保全局共用，便于统一管理和维护
 */
object DialogUtils {

    /**
     * 显示 Toast 消息（通用方法）
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    /**
     * 显示短时 Toast（2 秒）
     */
    fun showToastShort(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_SHORT)
    }

    /**
     * 显示长时 Toast（4 秒）
     */
    fun showToastLong(context: Context, message: String) {
        showToast(context, message, Toast.LENGTH_LONG)
    }

    /**
     * 显示确认对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param message 对话框内容
     * @param positiveButtonText 确定按钮文本
     * @param negativeButtonText 取消按钮文本
     * @param onPositive 确定按钮点击回调
     * @param onNegative 取消按钮点击回调（可选）
     */
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "确定",
        negativeButtonText: String = "取消",
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onPositive?.invoke()
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                onNegative?.invoke()
            }
            .create()
        
        // 应用圆角风格
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        
        // 为对话框添加圆角背景
        try {
            val decorView = dialog.window?.decorView as? android.view.ViewGroup
            decorView?.findViewById<android.view.View>(android.R.id.content)?.let { contentView ->
                val background = android.graphics.drawable.GradientDrawable()
                background.setColor(android.graphics.Color.WHITE)
                background.cornerRadius = 16f * context.resources.displayMetrics.density
                contentView.background = background
            }
        } catch (e: Exception) {
            // 降级处理
        }
    }

    /**
     * 显示单个按钮对话框（仅确定）
     */
    fun showAlertDialog(
        context: Context,
        title: String,
        message: String,
        buttonText: String = "确定",
        onConfirm: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { _, _ ->
                onConfirm?.invoke()
            }
            .show()
    }

    /**
     * 显示列表选择对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param items 列表项数组
     * @param onItemClick 项目点击回调（参数：position, item）
     */
    fun showListDialog(
        context: Context,
        title: String,
        items: Array<String>,
        onItemClick: (position: Int, item: String) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { _, position ->
                onItemClick(position, items[position])
            }
            .show()
    }

    /**
     * 显示带有取消按钮的列表选择对话框
     */
    fun showListDialogWithCancel(
        context: Context,
        title: String,
        items: Array<String>,
        onItemClick: (position: Int, item: String) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { _, position ->
                onItemClick(position, items[position])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示带有正/负按钮和列表的对话框
     * @param context 上下文
     * @param title 对话框标题
     * @param items 列表项数组
     * @param positiveButtonText 确定按钮文本
     * @param negativeButtonText 取消按钮文本
     * @param onItemClick 项目点击回调
     * @param onPositive 确定按钮回调（可选）
     * @param onNegative 取消按钮回调（可选）
     */
    fun showListDialogWithButtons(
        context: Context,
        title: String,
        items: Array<String>,
        positiveButtonText: String = "确定",
        negativeButtonText: String = "取消",
        onItemClick: (position: Int, item: String) -> Unit,
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { _, position ->
                onItemClick(position, items[position])
            }
            .setPositiveButton(positiveButtonText) { _, _ ->
                onPositive?.invoke()
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                onNegative?.invoke()
            }
            .show()
    }

    /**
     * 显示错误对话框
     */
    fun showErrorDialog(
        context: Context,
        title: String = "错误",
        message: String,
        onConfirm: (() -> Unit)? = null
    ) {
        showAlertDialog(context, title, message, "确定", onConfirm)
    }

    /**
     * 显示信息对话框
     */
    fun showInfoDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: (() -> Unit)? = null
    ) {
        showAlertDialog(context, title, message, "确定", onConfirm)
    }

    /**
     * 显示 Toast 和对话框组合（先 Toast，再对话框）
     */
    fun showToastAndDialog(
        context: Context,
        toastMessage: String,
        title: String,
        dialogMessage: String,
        positiveButtonText: String = "确定",
        onConfirm: (() -> Unit)? = null
    ) {
        showToastShort(context, toastMessage)
        showAlertDialog(context, title, dialogMessage, positiveButtonText, onConfirm)
    }
}
