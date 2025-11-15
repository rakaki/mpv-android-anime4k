package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.fam4k007.videoplayer.compose.FeedbackScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 建议反馈页面
 */
class FeedbackActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = "2297065843@qq.com"
        val githubUrl = AppConstants.URLs.GITHUB_ISSUES_URL
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
                FeedbackScreen(
                    email = email,
                    githubUrl = githubUrl,
                    onBack = {
                        activity.finish()
                        activity.overridePendingTransition(R.anim.no_anim, R.anim.slide_out_down)
                    },
                    onOpenEmail = { openEmail(email) },
                    onOpenGithub = { openUrl(githubUrl) }
                )
            }
        }
    }
    
    /**
     * 打开URL（使用系统默认浏览器）
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "无法打开浏览器",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "FAM4K007 播放器反馈")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "未找到可用的邮件应用",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
