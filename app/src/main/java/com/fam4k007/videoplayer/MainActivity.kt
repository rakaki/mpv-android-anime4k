package com.fam4k007.videoplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.compose.HomeScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.utils.UpdateManager
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    
    private var historyManager: PlaybackHistoryManager? = null  // 改为可空类型，延迟初始化
    private var lastThemeName: String? = null
    private var needsRefresh = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        // 检查用户是否已同意协议
        if (!UserAgreementActivity.isAgreed(this)) {
            // 未同意，跳转到协议页面
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // 使用IdleHandler在主线程空闲时初始化历史记录管理器
        android.os.Looper.myQueue().addIdleHandler {
            historyManager = PlaybackHistoryManager(this)
            com.fam4k007.videoplayer.utils.Logger.d("MainActivity", "PlaybackHistoryManager initialized in idle")
            false  // 返回false表示只执行一次
        }
        
        lastThemeName = ThemeManager.getCurrentTheme(this).themeName
        
        // 延迟检查更新（5秒后，避免阻塞启动）
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            checkForUpdateSilently()
        }, 5000)
        
        setupContent()
    }
    
    override fun onResume() {
        super.onResume()
        // 检查主题是否改变
        val currentThemeName = ThemeManager.getCurrentTheme(this).themeName
        if (lastThemeName != null && lastThemeName != currentThemeName) {
            lastThemeName = currentThemeName
            needsRefresh = false
            recreate() // 主题改变，重新创建 Activity
        } else if (needsRefresh) {
            // 播放记录可能已更新，刷新界面
            needsRefresh = false
            setupContent()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 离开主页时标记需要刷新
        needsRefresh = true
    }
    
    private fun setupContent() {
        val activity = this
        
        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(activity).themeName)

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = themeColors.primary,
                    onPrimary = themeColors.onPrimary,
                    primaryContainer = themeColors.primaryVariant,
                    secondary = themeColors.secondary,
                    background = themeColors.background,
                    onBackground = themeColors.onBackground,
                    surface = themeColors.surface,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = themeColors.onSurface
                )
            ) {
                HomeScreen(
                    historyManager = historyManager ?: PlaybackHistoryManager(activity),
                    onNavigateToSettings = {
                        startActivity(Intent(activity, SettingsComposeActivity::class.java))
                        overridePendingTransition(R.anim.scale_in, R.anim.scale_out)
                    }
                )
            }
        }
    }
    
    /**
     * 静默检查更新（后台执行，有新版本时弹窗提示）
     */
    private fun checkForUpdateSilently() {
        lifecycleScope.launch {
            try {
                val updateInfo = UpdateManager.checkForUpdate(this@MainActivity)
                if (updateInfo != null) {
                    // 有新版本，显示更新对话框
                    showUpdateDialog(updateInfo)
                }
                // 没有更新或检查失败时，不做任何提示
            } catch (e: Exception) {
                // 静默失败，不打扰用户
            }
        }
    }
    
    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(updateInfo: UpdateManager.UpdateInfo) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("发现新版本 ${updateInfo.versionName}")
        
        val message = if (updateInfo.releaseNotes.isNotEmpty()) {
            "更新内容：\n${updateInfo.releaseNotes}"
        } else {
            "发现新版本，是否立即下载？"
        }
        builder.setMessage(message)
        
        builder.setPositiveButton("立即下载") { _, _ ->
            UpdateManager.openDownloadPage(this, updateInfo.downloadUrl)
        }
        builder.setNegativeButton("稍后提醒", null)
        builder.setCancelable(true)
        
        builder.create().show()
    }
}

