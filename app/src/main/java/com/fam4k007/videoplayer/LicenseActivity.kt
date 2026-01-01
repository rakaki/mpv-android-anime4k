package com.fam4k007.videoplayer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.fam4k007.videoplayer.compose.LicenseScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

/**
 * 许可证书页面 - 使用 AboutLibraries 自动化管理
 */
class LicenseActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                LicenseScreen(
                    onBack = {
                        activity.finish()
                        activity.overridePendingTransition(R.anim.no_anim, R.anim.slide_out_down)
                    }
                )
            }
        }
    }
}
