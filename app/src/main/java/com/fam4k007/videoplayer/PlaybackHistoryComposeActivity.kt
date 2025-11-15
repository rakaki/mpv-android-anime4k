package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.fam4k007.videoplayer.compose.PlaybackHistoryScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.ThemeManager

class PlaybackHistoryComposeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val historyManager = PlaybackHistoryManager(this)

        setContent {
            val themeColors = getThemeColors(ThemeManager.getCurrentTheme(this).themeName)
            
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
                PlaybackHistoryScreen(
                    historyManager = historyManager,
                    onBack = {
                        finish()
                        overridePendingTransition(R.anim.no_anim, R.anim.slide_out_down)
                    },
                    onPlayVideo = { uri, startPosition ->
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            data = uri
                            putExtra("lastPosition", startPosition)
                        }
                        startActivity(intent)
                        startActivityWithDefaultTransition()
                    }
                )
            }
        }
    }
}
