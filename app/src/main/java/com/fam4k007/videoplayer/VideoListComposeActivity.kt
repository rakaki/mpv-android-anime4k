package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.compose.VideoListScreen
import com.fam4k007.videoplayer.ui.theme.getThemeColors
import com.fam4k007.videoplayer.utils.NoMediaChecker
import com.fam4k007.videoplayer.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoListComposeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "VideoListCompose"
    }

    private lateinit var preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager
    private var folderPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 应用主题
        val currentTheme = ThemeManager.getCurrentTheme(this)
        setTheme(currentTheme.styleRes)

        preferencesManager = com.fam4k007.videoplayer.manager.PreferencesManager.getInstance(this)

        val folderName = intent.getStringExtra("folder_name") ?: "视频列表"
        folderPath = intent.getStringExtra("folder_path") ?: ""
        val videos = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list") ?: arrayListOf()

        setupContent(folderName, videos)
    }

    private fun setupContent(folderName: String, videos: ArrayList<VideoFileParcelable>) {
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
                    onBackground = Color(0xFF212121),
                    surface = themeColors.background,
                    surfaceVariant = themeColors.surfaceVariant,
                    onSurface = Color(0xFF212121)
                )
            ) {
                VideoListScreen(
                    folderName = folderName,
                    initialVideos = videos,
                    onNavigateBack = { finish() },
                    onOpenVideo = { video, index, allVideos -> 
                        openVideoPlayer(video, index, folderName, allVideos)
                    },
                    onRescanFolder = { callback -> rescanFolder(callback) },
                    preferencesManager = preferencesManager
                )
            }
        }
    }

    private fun openVideoPlayer(
        video: VideoFileParcelable, 
        currentIndex: Int, 
        folderName: String, 
        allVideos: List<VideoFileParcelable>
    ) {
        Log.d(TAG, "播放视频: ${video.name}, 索引: $currentIndex")

        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.data = Uri.parse(video.uri)
        intent.putExtra("video_name", video.name)
        intent.putExtra("current_index", currentIndex)
        intent.putExtra("folderName", folderName)
        intent.putParcelableArrayListExtra("video_list", ArrayList(allVideos))
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun rescanFolder(callback: (List<VideoFileParcelable>) -> Unit) {
        if (folderPath.isEmpty()) {
            callback(emptyList())
            return
        }
        
        lifecycleScope.launch {
            val newVideos = withContext(Dispatchers.IO) {
                scanVideosInFolder(folderPath)
            }
            callback(newVideos)
        }
    }
    
    private fun scanVideosInFolder(folderPath: String): List<VideoFileParcelable> {
        val videos = mutableListOf<VideoFileParcelable>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath%")
        
        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn)
                    if (path.substringBeforeLast("/") == folderPath) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val dateAdded = cursor.getLong(dateColumn)
                        val uri = Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        ).toString()
                        
                        videos.add(
                            VideoFileParcelable(
                                uri = uri,
                                name = name,
                                path = path,
                                size = size,
                                duration = duration,
                                dateAdded = dateAdded
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescanning folder", e)
        }
        
        return videos
    }
}
