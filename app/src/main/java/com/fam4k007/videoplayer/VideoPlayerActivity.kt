package com.fam4k007.videoplayer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.Anime4KManager
import com.fam4k007.videoplayer.manager.PreferencesManager
import com.fam4k007.videoplayer.manager.SubtitleManager
import com.fam4k007.videoplayer.player.GestureHandler
import com.fam4k007.videoplayer.player.PlaybackEngine
import com.fam4k007.videoplayer.player.PlayerControlsManager
import com.fam4k007.videoplayer.player.SeriesManager
import com.fam4k007.videoplayer.utils.FormatUtils
import com.fam4k007.videoplayer.utils.UriUtils.resolveUri
import com.fam4k007.videoplayer.utils.DialogUtils
import dev.jdtech.mpv.MPVLib
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * 视频播放器 Activity (重构版)
 * 使用管理器模式进行职责分离，防止内存泄漏
 */
class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoPlayerActivity"
        private const val SEEK_DEBUG = "SEEK_DEBUG"  // 快进调试专用日志标签
    }

    // ========== 核心管理器 ==========
    private lateinit var playbackEngine: PlaybackEngine
    private lateinit var controlsManager: PlayerControlsManager
    private lateinit var gestureHandler: GestureHandler
    private lateinit var seriesManager: SeriesManager
    private lateinit var anime4KManager: Anime4KManager

    // ========== UI 组件（仅保留必需的引用）==========
    private lateinit var surfaceView: SurfaceView
    private lateinit var clickArea: View
    
    // 截图状态指示器
    private var screenshotStatusCard: androidx.cardview.widget.CardView? = null
    private var screenshotProgressBar: android.widget.ProgressBar? = null
    private var screenshotSuccessIcon: TextView? = null
    private var screenshotStatusText: TextView? = null
    
    // 进度恢复提示框
    private var resumeProgressPrompt: LinearLayout? = null
    private var btnResumePromptConfirm: TextView? = null
    private var btnResumePromptClose: TextView? = null
    private val resumePromptHandler = Handler(Looper.getMainLooper())

    // ========== 数据 ==========
    private var videoUri: Uri? = null
    private lateinit var preferencesManager: PreferencesManager
    private val subtitleManager = SubtitleManager()
    private var savedPosition = 0.0
    private var hasRestoredPosition = false
    private var hasShownPrompt = false
    private var lastPlaybackPosition = 0L  // 从列表传入的播放位置
    
    // 播放状态
    private var currentPosition = 0.0
    private var duration = 0.0
    private var isPlaying = false
    private var currentSpeed = 1.0
    private var isHardwareDecoding = true
    
    // 快进/快退时长（从设置读取，默认5秒）
    private var seekTimeSeconds = 5
    
    // 系列视频相关
    private var currentSeries: List<Uri> = emptyList()
    private var currentVideoIndex = -1

    // Anime4K 状态
    private var anime4KDialog: android.app.Dialog? = null
    private var anime4KEnabled = false
    private var anime4KMode = Anime4KManager.Mode.OFF
    private var anime4KQuality = Anime4KManager.Quality.BALANCED
    
    // Surface准备状态标志
    private var isSurfaceReady = false
    private var pendingVideoLoad = false
    
    // 手势提示View
    private lateinit var seekHint: TextView
    private lateinit var speedHint: LinearLayout
    private lateinit var speedHintText: TextView
    
    // 字幕文件选择器
    private lateinit var subtitlePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    // 标志：是否正在打开字幕选择器（用于区分正常返回和选择器返回）
    private var isSelectingSubtitle = false
    
    // 双击进度累积（用于连续双击）
    private var seekAccumulator = 0
    private var isSeekingForward = true
    private val seekAccumulatorHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var seekAccumulatorRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_video_player)

        // 初始化PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)
        
        // 初始化字幕文件选择器
        initializeSubtitleFilePicker()
        
        // 读取用户设置
        loadUserSettings()

        // 获取视频URI
        videoUri = intent.data
        if (videoUri == null) {
            DialogUtils.showToastShort(this, "无效的视频路径")
            finish()
            return
        }

        Log.d(TAG, "Video URI: $videoUri")

        // 读取保存的播放进度
        savedPosition = preferencesManager.getPlaybackPosition(videoUri.toString())
        Log.d(TAG, "Saved position: $savedPosition seconds")

        // 初始化UI组件
        surfaceView = findViewById(R.id.surfaceView)
        clickArea = findViewById(R.id.clickArea)
        
        // 初始化进度恢复提示框
        resumeProgressPrompt = findViewById(R.id.resumeProgressPrompt)
        btnResumePromptConfirm = findViewById(R.id.btnResumePromptConfirm)
        btnResumePromptClose = findViewById(R.id.btnResumePromptClose)
        
        // 设置确定按钮点击事件
        btnResumePromptConfirm?.setOnClickListener {
            onResumePromptConfirm()
        }
        
        // 设置关闭按钮点击事件
        btnResumePromptClose?.setOnClickListener {
            hideResumeProgressPrompt()
        }
        
        // 初始化手势提示View
        seekHint = findViewById(R.id.seekHint)
        speedHint = findViewById(R.id.speedHint)
        speedHintText = findViewById(R.id.speedHintText)
        
        // 初始化所有管理器
        initializeManagers()
        
        // 获取从列表传入的视频数据
        handleVideoListIntent()
        
        // 标记等待Surface准备完成后加载视频
        pendingVideoLoad = true
        // loadVideo() 现在会在surfaceCreated回调中调用
    }

    /**
     * 初始化所有管理器
     */
    private fun initializeManagers() {
        // 初始化 PlaybackEngine
        playbackEngine = PlaybackEngine(
            WeakReference(this),
            object : PlaybackEngine.PlaybackEventCallback {
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    this@VideoPlayerActivity.isPlaying = isPlaying
                    // 更新播放/暂停按钮状态
                    controlsManager?.updatePlayPauseButton(isPlaying)
                }
                
                override fun onProgressUpdate(position: Double, duration: Double) {
                    this@VideoPlayerActivity.currentPosition = position
                    this@VideoPlayerActivity.duration = duration
                    // 更新进度条和时间显示
                    controlsManager?.updateProgress(position, duration)
                }
                
                override fun onFileLoaded() {
                    // 视频加载完成
                    isPlaying = true
                    controlsManager?.updatePlayPauseButton(true)
                }
                
                override fun onEndOfFile() {
                    // 视频播放结束，清除保存的位置
                    videoUri?.let { uri ->
                        preferencesManager.clearPlaybackPosition(uri.toString())
                        Log.d(TAG, "Video ended, position reset to 0")
                    }
                    
                    // 已禁用自动连播功能
                    Log.d(TAG, "Video playback ended, auto-play disabled")
                }
                
                override fun onError(message: String) {
                    DialogUtils.showToastLong(this@VideoPlayerActivity, message)
                }
                
                override fun onSurfaceReady() {
                    // Surface准备完成，可以安全加载视频了
                    Log.d(TAG, "Surface ready callback received")
                    if (pendingVideoLoad) {
                        isSurfaceReady = true
                        pendingVideoLoad = false
                        Log.d(TAG, "Loading video after surface ready")
                        loadVideo()
                    }
                }
            }
        )
        
        // 初始化 PlaybackEngine
        if (!playbackEngine.initialize()) {
            DialogUtils.showToastLong(this, "播放器初始化失败")
            finish()
            return
        }
        
        // 绑定 Surface
        playbackEngine.attachSurface(surfaceView)
        
        // 初始化 GestureHandler (需要先于PlayerControlsManager创建)
        gestureHandler = GestureHandler(
            WeakReference(this),
            WeakReference(window),
            object : GestureHandler.GestureCallback {
                override fun onGestureStart() {
                    // 手势开始
                }
                
                override fun onGestureEnd() {
                    // 手势结束，隐藏提示（带渐出动画）
                    if (seekHint.visibility == View.VISIBLE) {
                        seekHint.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction { seekHint.visibility = View.GONE }
                            .start()
                    }
                    
                    if (speedHint.visibility == View.VISIBLE) {
                        speedHint.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction { speedHint.visibility = View.GONE }
                            .start()
                    }
                }
                
                override fun onLongPressRelease() {
                    // 长按松手时恢复正常速度
                    if (currentSpeed != 1.0) {
                        currentSpeed = 1.0
                        playbackEngine?.setSpeed(1.0)
                    }
                }
                
                override fun onSingleTap() {
                    // 单击切换控制面板显示/隐藏
                    controlsManager?.toggleControls()
                }
                
                override fun onDoubleTap() {
                    // 双击暂停/播放
                    playbackEngine?.togglePlayPause()
                }
                
                override fun onLongPress() {
                    // 长按使用设置中的倍速播放，显示提示（带动画）
                    if (currentSpeed == 1.0) {
                        // 从PreferencesManager读取用户设置的倍速
                        val longPressSpeed = preferencesManager.getLongPressSpeed()
                        
                        currentSpeed = longPressSpeed.toDouble()
                        playbackEngine?.setSpeed(longPressSpeed.toDouble())
                        
                        speedHintText.text = "正在${String.format("%.1f", longPressSpeed)}倍速播放"
                        speedHint.visibility = View.VISIBLE
                        speedHint.alpha = 0f
                        speedHint.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                }
                
                override fun onSeekGesture(seekSeconds: Int) {
                    // 左右滑动调整进度（固定步进）
                    if (duration > 0) {
                        val newPos = (currentPosition + seekSeconds).coerceIn(0.0, duration).toInt()
                        val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                        playbackEngine?.seekTo(newPos, usePrecise)
                        
                        // 显示进度提示（带动画）
                        val currentTime = FormatUtils.formatProgressTime(newPos.toDouble())
                        val sign = if (seekSeconds >= 0) "+" else ""
                        val seekTime = FormatUtils.formatProgressTime(seekSeconds.toDouble())
                        seekHint.text = "$currentTime\n[$sign$seekTime]"
                        
                        // 渐入动画
                        if (seekHint.visibility != View.VISIBLE) {
                            seekHint.visibility = View.VISIBLE
                            seekHint.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start()
                        } else {
                            seekHint.alpha = 1f
                        }
                    }
                }
            }
        )
        
        // 初始化 PlayerControlsManager (传入GestureHandler引用)
        controlsManager = PlayerControlsManager(
            WeakReference(this),
            object : PlayerControlsManager.ControlsCallback {
                override fun onPlayPauseClick() {
                    playbackEngine.togglePlayPause()
                }
                
                override fun onPreviousClick() {
                    playPreviousVideo()
                }
                
                override fun onNextClick() {
                    playNextVideo()
                }
                
                override fun onRewindClick() {
                    Log.d(SEEK_DEBUG, "onRewindClick: seekTimeSeconds = $seekTimeSeconds, currentPosition = $currentPosition, seekBy = -$seekTimeSeconds")
                    playbackEngine.seekBy(-seekTimeSeconds)
                }
                
                override fun onForwardClick() {
                    Log.d(SEEK_DEBUG, "onForwardClick: seekTimeSeconds = $seekTimeSeconds, currentPosition = $currentPosition, seekBy = $seekTimeSeconds")
                    playbackEngine.seekBy(seekTimeSeconds)
                }
                
                override fun onSubtitleClick() {
                    showSubtitleDialog()
                }
                
                override fun onAudioTrackClick() {
                    showAudioTrackDialog()
                }
                
                override fun onDecoderClick() {
                    toggleDecoder()
                }
                
                override fun onAnime4KClick() {
                    showAnime4KModeDialog()
                }
                
                override fun onMoreClick() {
                    showMoreOptionsDialog()
                }
                
                override fun onSpeedClick() {
                    showSpeedDialog()
                }
                
                override fun onSeekBarChange(position: Double) {
                    val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                    playbackEngine.seekTo(position.toInt(), usePrecise)
                }
                
                override fun onBackClick() {
                    // 退出前恢复原始设置
                    gestureHandler?.restoreOriginalSettings()
                    finish()
                }
            },
            WeakReference(gestureHandler)  // 传入GestureHandler引用
        )
        
        // 初始化 SeriesManager
        seriesManager = SeriesManager()
        
        // 检查是否从 VideoListActivity 传入了视频列表
        val videoListParcelable = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list")
        
        if (videoListParcelable != null && videoListParcelable.isNotEmpty()) {
            // 从列表传入，使用完整的视频列表（按列表顺序）
            val uriList = videoListParcelable.map { Uri.parse(it.uri) }
            videoUri?.let { uri ->
                seriesManager.setVideoList(uriList, uri)
                Log.d(TAG, "Video list from intent: ${uriList.size} videos, currentIndex: ${seriesManager.currentIndex}")
            }
        } else {
            // 单独打开视频，尝试扫描同目录（已废弃的功能，保留以防万一）
            videoUri?.let { uri ->
                seriesManager.identifySeries(this, uri) { videoUri ->
                    getFileNameFromUri(videoUri)
                }
            }
        }
        
        Log.d(TAG, "Series list size: ${seriesManager.getVideoList().size}, currentIndex: ${seriesManager.currentIndex}")
        Log.d(TAG, "hasPrevious: ${seriesManager.hasPrevious}, hasNext: ${seriesManager.hasNext}")
        
        // 初始化 Anime4KManager
        anime4KManager = Anime4KManager(this)
        if (anime4KManager.initialize()) {
            Log.d(TAG, "Anime4K initialized successfully")
        } else {
            Log.w(TAG, "Anime4K initialization failed")
        }
        
        // 初始化截图状态指示器
        screenshotStatusCard = findViewById(R.id.cardScreenshotStatus)
        screenshotProgressBar = findViewById(R.id.progressLoading)
        screenshotSuccessIcon = findViewById(R.id.iconSuccess)
        screenshotStatusText = findViewById(R.id.tvStatusText)
        
        // 绑定View到管理器
        bindViewsToManagers()
    }
    
    /**
     * 绑定所有View到对应的管理器
     */
    private fun bindViewsToManagers() {
        // 绑定控制器View
        controlsManager.bindViews(
            topInfoPanel = findViewById(R.id.topInfoPanel),
            controlPanel = findViewById(R.id.controlPanel),
            tvFileName = findViewById(R.id.tvFileName),
            tvBattery = findViewById(R.id.tvBattery),
            tvTime = findViewById(R.id.tvTime),
            tvTimeInfo = findViewById(R.id.tvTimeInfo),
            btnPlayPause = findViewById(R.id.btnPlayPause),
            btnPrevious = findViewById(R.id.btnPrevious),
            btnNext = findViewById(R.id.btnNext),
            btnRewind = findViewById(R.id.btnRewind),
            btnForward = findViewById(R.id.btnForward),
            btnBack = findViewById(R.id.btnBack),
            btnSubtitle = findViewById(R.id.btnSubtitle),  // 新增字幕按钮
            btnAudioTrack = findViewById(R.id.btnAudioTrack),
            btnDecoder = findViewById(R.id.btnDecoder),
            btnMore = findViewById(R.id.btnMore),
            btnSpeed = findViewById(R.id.btnSpeed),
            btnAnime4K = findViewById(R.id.btnAnime4K),
            seekBar = findViewById(R.id.seekBar),
            resumePlaybackPrompt = findViewById(R.id.resumePlaybackPrompt),
            tvResumeConfirm = findViewById(R.id.tvResumeConfirm)
        )
        
        // 初始化控制管理器
        controlsManager.initialize()
        
        // 设置文件名
        videoUri?.let { uri ->
            val fileName = getFileNameFromUri(uri)
            controlsManager.setFileName(fileName)
        }
        
        // 初始化手势处理器
        gestureHandler.initialize()
        
        // 绑定手势指示器View
        gestureHandler.bindIndicatorViews(
            brightnessIndicator = findViewById(R.id.brightnessIndicator),
            volumeIndicator = findViewById(R.id.volumeIndicator),
            brightnessBar = findViewById(R.id.brightnessBar),
            volumeBar = findViewById(R.id.volumeBar),
            brightnessText = findViewById(R.id.brightnessText),
            volumeText = findViewById(R.id.volumeText)
        )
        
        // 绑定触摸事件
        clickArea.setOnTouchListener { _, event ->
            gestureHandler.onTouchEvent(event)
        }
        
        // 更新上下集按钮状态（必须在 bindViewsToManagers 之后）
        updateEpisodeButtons()
    }
    
    /**
     * 加载视频
     */
    private fun loadVideo() {
        videoUri?.let { uri ->
            // 对于短视频（<30秒），不使用保存的位置，从头播放
            val position = if (duration > 0 && duration < 30) {
                Log.d(TAG, "Short video detected, starting from 0")
                0.0
            } else {
                savedPosition
            }
            
            // 如果有保存的播放进度（大于5秒），显示恢复提示
            if (position > 5.0) {
                Log.d(TAG, "Saved position detected: $position seconds - showing resume prompt")
                showResumeProgressPrompt()
            }
            
            playbackEngine?.loadVideo(uri, position)
        }
    }
    
    /**
     * 显示进度恢复提示框
     */
    private fun showResumeProgressPrompt() {
        resumeProgressPrompt?.visibility = View.VISIBLE
        
        // 5秒后自动隐藏
        resumePromptHandler.postDelayed({
            hideResumeProgressPrompt()
        }, 5000)
    }
    
    /**
     * 加载用户设置
     */
    private fun loadUserSettings() {
        // 读取快进/快退时长
        seekTimeSeconds = preferencesManager.getSeekTime()
        Log.d(SEEK_DEBUG, "loadUserSettings: seekTimeSeconds loaded = $seekTimeSeconds seconds")
    }
    
    /**
     * 隐藏进度恢复提示框
     */
    private fun hideResumeProgressPrompt() {
        resumeProgressPrompt?.visibility = View.GONE
        resumePromptHandler.removeCallbacksAndMessages(null)
    }
    
    /**
     * 用户点击确定按钮，从头开始播放
     */
    private fun onResumePromptConfirm() {
        Log.d(TAG, "User confirmed to restart from beginning")
        hideResumeProgressPrompt()
        
        // 立即跳转到开头(同步操作)
        playbackEngine?.seekTo(0)
        
        // 清除保存的进度(异步操作,不阻塞)
        videoUri?.let { uri ->
            Thread {
                preferencesManager.clearPlaybackPosition(uri.toString())
            }.start()
        }
    }
    
    /**
     * 处理从列表传入的视频数据
     */
    private fun handleVideoListIntent() {
        val lastPosition = intent.getLongExtra("lastPosition", -1L)
        if (lastPosition > 0) {
            this.lastPlaybackPosition = lastPosition
        }
    }
    
    /**
     * 更新上一集/下一集按钮状态
     */
    private fun updateEpisodeButtons() {
        Log.d(TAG, "updateEpisodeButtons - hasPrevious: ${seriesManager.hasPrevious}, hasNext: ${seriesManager.hasNext}")
        controlsManager?.updateEpisodeButtons(seriesManager.hasPrevious, seriesManager.hasNext)
    }
    
    /**
     * 播放上一集
     */
    private fun playPreviousVideo() {
        Log.d(TAG, "playPreviousVideo - hasPrevious: ${seriesManager.hasPrevious}")
        if (seriesManager.hasPrevious) {
            val previousUri = seriesManager.previous()
            if (previousUri != null) {
                Log.d(TAG, "Playing previous video: $previousUri")
                playVideo(previousUri)
                updateEpisodeButtons()
            }
        } else {
            DialogUtils.showToastShort(this, "已经是第一集了")
        }
    }
    
    /**
     * 播放下一集
     */
    private fun playNextVideo() {
        Log.d(TAG, "playNextVideo - hasNext: ${seriesManager.hasNext}")
        if (seriesManager.hasNext) {
            val nextUri = seriesManager.next()
            if (nextUri != null) {
                Log.d(TAG, "Playing next video: $nextUri")
                playVideo(nextUri)
                updateEpisodeButtons()
            }
        } else {
            DialogUtils.showToastShort(this, "已经是最后一集了")
        }
    }
    
    /**
     * 播放指定视频
     */
    private fun playVideo(uri: Uri) {
        videoUri = uri
        
        // 更新文件名
        val fileName = getFileNameFromUri(uri)
        controlsManager?.setFileName(fileName)
        
        // 读取保存的播放进度
        val position = preferencesManager.getPlaybackPosition(uri.toString())
        
        // 加载视频（会自动开始播放）
        playbackEngine?.loadVideo(uri, position)
        
        // 更新按钮状态
        updateEpisodeButtons()
    }

    // Surface 回调已由 PlaybackEngine 处理
    // togglePlayPause、seekBy 已由管理器处理
    
    private fun resetAutoHideTimer() {
        // 该方法已被 PlayerControlsManager 接管，这里保留空方法以兼容旧代码引用
        controlsManager?.resetAutoHideTimer()
    }
    
    // updateProgress、toggleControls、showControls、hideControls 已由 PlayerControlsManager 处理
    

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "未知文件"
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = it.getString(displayNameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name", e)
            // 从路径中提取文件�?
            fileName = uri.lastPathSegment ?: "未知文件"
        }
        return fileName
    }

    private fun showAudioTrackDialog() {
        playbackEngine?.let { engine ->
            try {
                val audioTracks = engine.getAudioTracks()
                
                if (audioTracks.isEmpty()) {
                    DialogUtils.showToastShort(this, "未找到音轨")
                    return
                }
                
                val items = audioTracks.map { it.second }
                val currentTrackIndex = audioTracks.indexOfFirst { it.third }
                
                val dialog = android.app.Dialog(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_audio_track, null)
                val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
                val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
                val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                
                dialogTitle.text = "音轨"
                recyclerView.layoutManager = LinearLayoutManager(this)
                
                val adapter = SelectionAdapter(items, currentTrackIndex) { position ->
                    val trackId = audioTracks[position].first
                    engine.selectAudioTrack(trackId)
                    DialogUtils.showToastShort(this@VideoPlayerActivity, "已切换到: ${items[position]}")
                    dialog.dismiss()
                }
                recyclerView.adapter = adapter
                
                dialog.setContentView(dialogView)
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.window?.setLayout(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.setCanceledOnTouchOutside(true)
                
                btnCancel.setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing audio track dialog", e)
                DialogUtils.showToastShort(this, "获取音轨失败")
            }
        }
    }

    /**
     * 初始化字幕文件选择器
     */
    private fun initializeSubtitleFilePicker() {
        // 创建文件选择器 Launcher
        subtitlePickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            // 必须在 finally 块中重置标志，确保即使发生异常也会重置
            try {
                if (uri != null) {
                    Log.d(TAG, "Subtitle file selected: $uri")
                    // 检查文件是否是支持的字幕格式
                    val displayName = subtitleManager.getSubtitleDisplayName(this, uri)
                    Log.d(TAG, "Subtitle file name: $displayName")
                    
                    if (subtitleManager.isSupportedSubtitleFormat(displayName)) {
                        // 添加外挂字幕，通过回调注册到 PlaybackEngine 以便后续重新加载
                        if (subtitleManager.addExternalSubtitle(this, uri) { subtitlePath ->
                            // 字幕添加成功，注册到 PlaybackEngine
                            playbackEngine?.registerExternalSubtitle(subtitlePath)
                        }) {
                            DialogUtils.showToastShort(this, "已添加字幕: $displayName")
                            Log.d(TAG, "External subtitle added successfully: $displayName")
                            
                            // 延迟后尝试自动选中新添加的字幕
                            // MPV 需要足够的时间来处理新添加的字幕
                            Thread {
                                try {
                                    // 增加延迟到 2 秒，确保 MPV 完全处理外挂字幕
                                    Thread.sleep(2000)
                                    runOnUiThread {
                                        try {
                                            // 获取更新后的字幕轨道列表
                                            val subtitleTracks = playbackEngine?.getSubtitleTracks()
                                            Log.d(TAG, "Auto-select: Got subtitle tracks, count = ${subtitleTracks?.size}")
                                            
                                            if (subtitleTracks != null) {
                                                Log.d(TAG, "Auto-select: Available tracks: $subtitleTracks")
                                                
                                                // 详细打印每个轨道
                                                for ((index, track) in subtitleTracks.withIndex()) {
                                                    Log.d(TAG, "Auto-select: Track[$index]: id=${track.first}, name=${track.second}, current=${track.third}")
                                                }
                                                
                                                if (subtitleTracks.size > 1) {
                                                    // 通常新添加的字幕会成为最后一个轨道
                                                    val lastTrackIndex = subtitleTracks.size - 1
                                                    val trackId = subtitleTracks[lastTrackIndex].first
                                                    Log.d(TAG, "Auto-select: Selecting track at index $lastTrackIndex with ID $trackId")
                                                    playbackEngine?.selectSubtitleTrack(trackId)
                                                    Log.d(TAG, "Auto-selected new external subtitle: track $trackId")
                                                } else {
                                                    Log.w(TAG, "Auto-select: Not enough tracks to select (size=${subtitleTracks.size}), will retry in 1s")
                                                    // 再次延迟尝试
                                                    Thread {
                                                        try {
                                                            Thread.sleep(1000)
                                                            runOnUiThread {
                                                                val retryTracks = playbackEngine?.getSubtitleTracks()
                                                                Log.d(TAG, "Auto-select: Retry - Got subtitle tracks, count = ${retryTracks?.size}")
                                                                Log.d(TAG, "Auto-select: Retry - Available tracks: $retryTracks")
                                                                
                                                                if (retryTracks != null) {
                                                                    for ((index, track) in retryTracks.withIndex()) {
                                                                        Log.d(TAG, "Auto-select: Retry Track[$index]: id=${track.first}, name=${track.second}, current=${track.third}")
                                                                    }
                                                                }
                                                                
                                                                if (retryTracks != null && retryTracks.size > 1) {
                                                                    val lastIdx = retryTracks.size - 1
                                                                    val tid = retryTracks[lastIdx].first
                                                                    Log.d(TAG, "Auto-select: Retry - Selecting track at index $lastIdx with ID $tid")
                                                                    playbackEngine?.selectSubtitleTrack(tid)
                                                                    Log.d(TAG, "Auto-selected new external subtitle (retry): track $tid")
                                                                } else {
                                                                    Log.w(TAG, "Auto-select: Retry still failed (count=${retryTracks?.size})")
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "Error in retry auto-select", e)
                                                        }
                                                    }.start()
                                                }
                                            } else {
                                                Log.w(TAG, "Auto-select: Failed to get subtitle tracks (null)")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error in auto-select runOnUiThread", e)
                                            e.printStackTrace()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error auto-selecting new subtitle", e)
                                    e.printStackTrace()
                                }
                            }.start()
                        } else {
                            DialogUtils.showToastShort(this, "添加字幕失败")
                            Log.w(TAG, "Failed to add external subtitle: $displayName")
                        }
                    } else {
                        DialogUtils.showToastShort(this, "不支持的字幕格式: $displayName")
                        Log.w(TAG, "Unsupported subtitle format: $displayName")
                    }
                } else {
                    // 用户取消选择文件
                    Log.d(TAG, "Subtitle file selection cancelled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling subtitle file selection", e)
                e.printStackTrace()
                DialogUtils.showToastShort(this, "添加字幕出错: ${e.message}")
            } finally {
                // 选择完成，重置标志（必须）
                isSelectingSubtitle = false
                Log.d(TAG, "Subtitle file picker closed, isSelectingSubtitle reset to false")
                
                // 延迟后检查是否需要恢复播放（防止黑屏）
                resumePromptHandler.postDelayed({
                    try {
                        Log.d(TAG, "File picker callback finally block: checking playback state")
                        val isPaused = playbackEngine?.let { MPVLib.getPropertyBoolean("pause") } ?: true
                        val hasVideo = playbackEngine?.let { MPVLib.getPropertyString("filename") != null } ?: false
                        Log.d(TAG, "Playback state after file picker: isPaused=$isPaused, hasVideo=$hasVideo")
                        
                        if (hasVideo && isPaused) {
                            Log.d(TAG, "文件选择器关闭：检测到视频已暂停，尝试恢复播放以防止黑屏...")
                            playbackEngine?.play()
                            Log.d(TAG, "已发送播放恢复命令")
                        }
                        
                        // 强制刷新渲染（解决Surface销毁后的黑屏问题）
                        if (hasVideo) {
                            Log.d(TAG, "强制刷新视频渲染...")
                            try {
                                // 暂停-继续循环，强制重新渲染（不使用seeking，避免触发视频重新加载）
                                val wasPaused = isPaused
                                Log.d(TAG, "执行渲染刷新: 先暂停")
                                MPVLib.setPropertyBoolean("pause", true)
                                Thread.sleep(100)
                                
                                if (!wasPaused) {
                                    Log.d(TAG, "执行渲染刷新: 恢复播放")
                                    MPVLib.setPropertyBoolean("pause", false)
                                }
                                
                                Log.d(TAG, "已执行渲染刷新命令")
                            } catch (e: Exception) {
                                Log.e(TAG, "渲染刷新失败", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recovering playback in finally block", e)
                    }
                }, 100)
            }
        }
    }

    /**
     * 打开字幕文件选择器
     */
    private fun openSubtitlePicker() {
        if (::subtitlePickerLauncher.isInitialized) {
            // 标记正在选择字幕
            isSelectingSubtitle = true
            // 可以指定只选择特定类型的文件
            subtitlePickerLauncher.launch(
                arrayOf(
                    "application/x-subrip",
                    "text/plain",
                    "application/x-ssa",
                    "application/x-ass",
                    "*/*"
                )
            )
        }
    }

    private fun showSubtitleDialog() {
        playbackEngine?.let { engine ->
            try {
                val subtitleTracks = engine.getSubtitleTracks()
                
                // 构建菜单选项
                val menuItems = mutableListOf<String>()
                if (subtitleTracks.isNotEmpty()) {
                    menuItems.add("字幕轨道")
                    menuItems.add("字幕延迟")
                    menuItems.add("字幕大小")
                    menuItems.add("字幕位置")
                }
                menuItems.add("外挂字幕")
                
                val dialog = android.app.Dialog(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_option_list, null)
                val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
                val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
                val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                val scrollHint = dialogView.findViewById<TextView>(R.id.scrollHint)
                
                dialogTitle.text = "字幕设置"
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.isVerticalScrollBarEnabled = false
                
                val adapter = MenuItemAdapter(menuItems) { position ->
                    when {
                        position == menuItems.size - 1 -> {
                            openSubtitlePicker()
                        }
                        subtitleTracks.isNotEmpty() -> {
                            when (position) {
                                0 -> showSubtitleTrackSelection(engine, subtitleTracks)
                                1 -> showSubtitleDelayDialog(engine)
                                2 -> showSubtitleSizeDialog(engine)
                                3 -> showSubtitlePositionDialog(engine)
                            }
                        }
                    }
                    dialog.dismiss()
                }
                recyclerView.adapter = adapter
                
                dialog.setContentView(dialogView)
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.setCanceledOnTouchOutside(true)
                
                btnCancel.setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing subtitle dialog", e)
                DialogUtils.showToastShort(this, "获取字幕失败")
            }
        }
    }

    private fun showSubtitleTrackSelection(engine: PlaybackEngine, subtitleTracks: List<Triple<Int, String, Boolean>>) {
        val items = subtitleTracks.map { it.second }
        val currentTrackIndex = subtitleTracks.indexOfFirst { it.third }
        
        val dialog = android.app.Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_subtitle_track, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        dialogTitle.text = "字幕轨道"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isVerticalScrollBarEnabled = false
        
        val adapter = SelectionAdapter(items, currentTrackIndex) { position ->
            val trackId = subtitleTracks[position].first
            engine.selectSubtitleTrack(trackId)
            DialogUtils.showToastShort(this@VideoPlayerActivity, "已切换到: ${items[position]}")
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showSubtitleDelayDialog(engine: PlaybackEngine) {
        val currentDelay = engine.getSubtitleDelay()
        val defaultDelay = 0.0
        val stepValue = 0.2  // 单位步进
        
        // 使用普通Dialog而不是AlertDialog，避免默认宽度限制
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_subtitle_delay, null)
        
        val etDelayValue = view.findViewById<EditText>(R.id.etDelayValue)
        val btnDecrease = view.findViewById<Button>(R.id.btnDecreaseDelay)
        val btnIncrease = view.findViewById<Button>(R.id.btnIncreaseDelay)
        val btnReset = view.findViewById<Button>(R.id.btnResetDelay)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmDelay)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelDelay)
        
        // 设置初始值
        etDelayValue.setText(String.format("%.1f", currentDelay))
        etDelayValue.setSelection(etDelayValue.text.length)
        
        // 更新按钮状态
        fun updateResetButton() {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            val isDefault = Math.abs(currentValue - defaultDelay) < 0.01
            btnReset.isEnabled = !isDefault
            btnReset.setTextColor(if (isDefault) 
                getColor(android.R.color.darker_gray) else 
                getColor(android.R.color.white))
        }
        
        // 更新编辑框和引擎
        fun updateDelayValue(newValue: Double) {
            val clampedValue = newValue.coerceIn(-10.0, 10.0)
            val roundedValue = Math.round(clampedValue * 10.0) / 10.0
            etDelayValue.setText(String.format("%.1f", roundedValue))
            etDelayValue.setSelection(etDelayValue.text.length)
            engine.setSubtitleDelay(roundedValue)
            updateResetButton()
        }
        
        updateResetButton()
        
        // 减号按钮
        btnDecrease.setOnClickListener {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            updateDelayValue(currentValue - stepValue)
        }
        
        // 加号按钮
        btnIncrease.setOnClickListener {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            updateDelayValue(currentValue + stepValue)
        }
        
        // 监听输入变化
        etDelayValue.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateResetButton()
                
                // 实时应用延迟
                val value = s.toString().toDoubleOrNull()
                if (value != null && value >= -10.0 && value <= 10.0) {
                    engine.setSubtitleDelay(value)
                }
            }
        })
        
        // 设置对话框内容和样式
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        
        btnReset.setOnClickListener {
            updateDelayValue(defaultDelay)
        }
        
        btnConfirm.setOnClickListener {
            val value = etDelayValue.text.toString().toDoubleOrNull()
            if (value != null) {
                val clampedValue = value.coerceIn(-10.0, 10.0)
                val roundedValue = Math.round(clampedValue * 10.0) / 10.0
                engine.setSubtitleDelay(roundedValue)
                DialogUtils.showToastShort(this, "字幕延迟：${String.format("%.1f", roundedValue)}秒")
            }
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            engine.setSubtitleDelay(currentDelay)  // 恢复原始值
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showSubtitleSizeDialog(engine: PlaybackEngine) {
        val currentScale = engine.getSubtitleScale()
        val scaleLabels = listOf("极小 (0.5)", "较小 (0.7)", "小 (0.8)", "正常 (1.0)", "大 (1.2)", "较大 (1.5)", "极大 (2.0)")
        val scaleValues = listOf(0.5, 0.7, 0.8, 1.0, 1.2, 1.5, 2.0)
        
        // 找到最接近当前值的选项
        val currentSelection = scaleValues.indices.minByOrNull { 
            Math.abs(scaleValues[it] - currentScale) 
        } ?: 3
        
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_subtitle_size, null)
        
        val tvCurrentSize = view.findViewById<TextView>(R.id.tvCurrentSize)
        val seekBarSize = view.findViewById<android.widget.SeekBar>(R.id.seekBarSize)
        val btnReset = view.findViewById<Button>(R.id.btnResetSize)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelSize)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmSize)
        
        // 设置初始值
        seekBarSize.max = 6
        seekBarSize.progress = currentSelection
        tvCurrentSize.text = scaleLabels[currentSelection]
        
        val defaultPosition = 3 // 正常 (1.0)
        
        // 更新重置按钮状态
        fun updateResetButton(position: Int) {
            val isDefault = position == defaultPosition
            btnReset.isEnabled = !isDefault
            btnReset.setTextColor(if (isDefault) 
                getColor(android.R.color.darker_gray) else 
                getColor(android.R.color.white))
        }
        
        updateResetButton(currentSelection)
        
        // SeekBar 监听器
        seekBarSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvCurrentSize.text = scaleLabels[progress]
                engine.setSubtitleScale(scaleValues[progress])
                updateResetButton(progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 设置对话框
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        
        btnReset.setOnClickListener {
            seekBarSize.progress = defaultPosition
            tvCurrentSize.text = scaleLabels[defaultPosition]
            engine.setSubtitleScale(scaleValues[defaultPosition])
            updateResetButton(defaultPosition)
        }
        
        btnConfirm.setOnClickListener {
            val position = seekBarSize.progress
            DialogUtils.showToastShort(this, "字幕大小：${scaleLabels[position]}")
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            engine.setSubtitleScale(currentScale)  // 恢复原始值
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showSubtitlePositionDialog(engine: PlaybackEngine) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_subtitle_position)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val seekBarVertical = dialog.findViewById<android.widget.SeekBar>(R.id.seekBarVertical)
        val tvVerticalValue = dialog.findViewById<TextView>(R.id.tvVerticalValue)
        val btnReset = dialog.findViewById<Button>(R.id.btnReset)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)
        
        // 获取当前垂直位置 (MPV 默认: sub-pos=100 表示底部, 0表示顶部)
        val currentVertical = engine.getSubtitleVerticalPosition()
        val defaultVertical = 100
        
        seekBarVertical.progress = currentVertical
        tvVerticalValue.text = "$currentVertical"
        
        // 更新重置按钮状态
        fun updateResetButton(position: Int) {
            val isDefault = position == defaultVertical
            btnReset.isEnabled = !isDefault
            btnReset.setTextColor(if (isDefault) 
                getColor(android.R.color.darker_gray) else 
                getColor(android.R.color.white))
        }
        
        updateResetButton(currentVertical)
        
        seekBarVertical.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvVerticalValue.text = "$progress"
                engine.setSubtitleVerticalPosition(progress)
                updateResetButton(progress)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        btnReset.setOnClickListener {
            seekBarVertical.progress = defaultVertical
            engine.setSubtitleVerticalPosition(defaultVertical)
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun toggleDecoder() {
        val items = listOf("硬件解码", "软件解码")
        val currentSelection = if (isHardwareDecoding) 0 else 1
        
        val dialog = android.app.Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_decoder, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        dialogTitle.text = "解码方式"
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val adapter = SelectionAdapter(items, currentSelection) { position ->
            isHardwareDecoding = (position == 0)
            playbackEngine?.setHardwareDecoding(isHardwareDecoding)
            DialogUtils.showToastShort(this@VideoPlayerActivity, "已切换到${if (isHardwareDecoding) "硬件" else "软件"}解码")
            Log.d(TAG, "Decoder switched to: ${if (isHardwareDecoding) "auto" else "no"}")
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(true)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showSpeedDialog() {
        val speeds = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "2.5x", "3.0x")
        val speedValues = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0)
        val currentSelection = speedValues.indexOf(currentSpeed)
        
        val dialog = android.app.Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_speed, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val scrollHint = dialogView.findViewById<TextView>(R.id.scrollHint)
        
        dialogTitle.text = "播放速度"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isVerticalScrollBarEnabled = false
        
        val adapter = SelectionAdapter(speeds, currentSelection) { position ->
            currentSpeed = speedValues[position]
            playbackEngine?.setSpeed(currentSpeed)
            DialogUtils.showToastShort(this@VideoPlayerActivity, "播放速度：${speeds[position]}")
            Log.d(TAG, "Speed changed to: $currentSpeed")
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    // resetAutoHideTimer、hideSystemUI、showSystemUI、applyNavigationBarPadding 已由 PlayerControlsManager 处理

        
    
        // 获取选中的模�?
        
    
    private fun applyAnime4K() {
        try {
            val shaderChain = if (anime4KEnabled) {
                anime4KManager.getShaderChain(anime4KMode, anime4KQuality)
            } else {
                ""
            }
            
            MPVLib.setOptionString("glsl-shaders", shaderChain)
            Log.d(TAG, "Applied Anime4K: enabled=$anime4KEnabled, mode=$anime4KMode, quality=$anime4KQuality")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply Anime4K", e)
            DialogUtils.showToastShort(this, "应用Anime4K失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
        
        // 恢复原始系统设置(音量、亮度)
        gestureHandler?.restoreOriginalSettings()
        
        // 清理进度恢复提示框的handler
        resumePromptHandler.removeCallbacksAndMessages(null)
        
        // 清理所有管理器
        playbackEngine?.destroy()
        controlsManager?.cleanup()
        gestureHandler?.cleanup()
    }

    // MPVLib.EventObserver 实现
    // MPV 事件回调已由 PlaybackEngine 处理
    // eventProperty 和 event 方法已移除



    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed, isSelectingSubtitle: $isSelectingSubtitle")
        
        // 检查播放状态，确保从后台返回后能正确恢复
        resumePromptHandler.postDelayed({
            try {
                val isPaused = playbackEngine?.let { MPVLib.getPropertyBoolean("pause") } ?: true
                val hasVideo = playbackEngine?.let { MPVLib.getPropertyString("filename") != null } ?: false
                Log.d(TAG, "Resume check: isPaused=$isPaused, hasVideo=$hasVideo, isSelectingSubtitle=$isSelectingSubtitle")
                
                // 如果有视频但是黑屏（暂停），尝试恢复
                if (hasVideo && isPaused && isSelectingSubtitle) {
                    Log.d(TAG, "检测到从字幕选择器返回，视频已暂停，尝试恢复播放...")
                    playbackEngine?.play()
                    Log.d(TAG, "已发送播放命令")
                }
                
                // 强制刷新渲染（解决Surface销毁后的黑屏问题）
                if (hasVideo && isSelectingSubtitle) {
                    Log.d(TAG, "从字幕选择器返回，强制刷新视频渲染...")
                    try {
                        // 暂停-继续循环，强制重新渲染
                        val wasPaused = isPaused
                        Log.d(TAG, "执行渲染刷新: 先暂停")
                        MPVLib.setPropertyBoolean("pause", true)
                        Thread.sleep(100)
                        
                        if (!wasPaused) {
                            Log.d(TAG, "执行渲染刷新: 恢复播放")
                            MPVLib.setPropertyBoolean("pause", false)
                        }
                        
                        Log.d(TAG, "已执行渲染刷新命令")
                    } catch (e: Exception) {
                        Log.e(TAG, "渲染刷新失败", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Resume recovery error", e)
            }
        }, 300)
    }

    override fun onPause() {
        super.onPause()
        // 保存当前播放进度
        savePlaybackPosition()
        // 保存播放历史
        savePlaybackHistory()
        
        Log.d(TAG, "Activity paused, isFinishing: $isFinishing")
    }

    override fun onStop() {
        super.onStop()
        // 保存当前播放进度
        savePlaybackPosition()
        // 保存播放历史
        savePlaybackHistory()
        
        // 用户切换到其他应用、返回桌面、锁屏、小窗等操作时，自动退出播放界面
        // 但如果正在选择字幕，不自动退出（因为文件选择器会让Activity进入onStop）
        if (!isFinishing && !isSelectingSubtitle) {
            Log.d(TAG, "检测到用户离开播放界面（返回桌面/切换任务/锁屏/小窗），自动退出")
            finish()
        }
        
        Log.d(TAG, "Activity stopped, isFinishing: $isFinishing, isSelectingSubtitle: $isSelectingSubtitle")
    }

    private fun savePlaybackPosition() {
        videoUri?.let { uri ->
            val videoKey = uri.toString()
            
            // 如果播放进度超过95%，保存为0（下次从头开始）
            val positionToSave = if (duration > 0 && currentPosition / duration > 0.95) {
                Log.d(TAG, "Video almost finished (${(currentPosition/duration*100).toInt()}%), resetting position to 0")
                0.0
            } else {
                currentPosition
            }
            
            preferencesManager.setPlaybackPosition(videoKey, positionToSave)
            Log.d(TAG, "Saved playback position: $positionToSave seconds")
        }
    }

    private fun savePlaybackHistory() {
        videoUri?.let { uri ->
            if (duration > 5.0) {  // 只保存时长大于5秒的视频
                val historyManager = PlaybackHistoryManager(this)
                val fileName = getFileNameFromUri(uri)
                val folderName = intent.getStringExtra("folderName") ?: "未知文件夹"
                
                historyManager.addHistory(
                    uri = uri,
                    fileName = fileName,
                    position = (currentPosition * 1000).toLong(),  // 转换为毫秒
                    duration = (duration * 1000).toLong(),
                    folderName = folderName
                )
                Log.d(TAG, "Saved to playback history: $fileName")
            }
        }
    }

    private fun showAnime4KModeDialog() {
        if (anime4KDialog != null && anime4KDialog!!.isShowing) return
        
        val dialog = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
        dialog.setContentView(R.layout.dialog_anime4k_mode)
        
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 设置固定位置：左上角，带固定边距
        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.START or android.view.Gravity.TOP
        layoutParams?.x = 20 // 距离左边缘20dp
        layoutParams?.y = 100 // 距离顶部100dp（避开状态栏和顶部信息栏）
        layoutParams?.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams?.flags = layoutParams?.flags?.or(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) ?: 0
        window?.attributes = layoutParams
        
        val rvMode = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAnime4KMode)
        val btnClose = dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnAnime4KClose)
        
        val modes = listOf(
            Anime4KManager.Mode.OFF,
            Anime4KManager.Mode.A,
            Anime4KManager.Mode.B,
            Anime4KManager.Mode.C,
            Anime4KManager.Mode.A_PLUS,
            Anime4KManager.Mode.B_PLUS,
            Anime4KManager.Mode.C_PLUS
        )
        
        rvMode.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        rvMode.adapter = com.fam4k007.videoplayer.adapter.Anime4KModeAdapter(modes, anime4KMode) { mode ->
            anime4KMode = mode
            anime4KEnabled = mode != Anime4KManager.Mode.OFF
            applyAnime4K()
            dialog.dismiss()
        }
        
        btnClose.setOnClickListener { dialog.dismiss() }
        anime4KDialog = dialog
        dialog.show()
    }

    // 识别系列视频
    // identifySeriesVideos 已由 SeriesManager 处理

    // extractSeriesName 和 getVideosInSameFolder 已由 SeriesManager 处理
    
    //  保留 getFileNameFromUri - 在多处使用
    // 重复的 getFileNameFromUri 已删除（保留第一个）
        
    // hideBrightnessIndicatorRunnable 和 hideVolumeIndicatorRunnable 已由 GestureHandler 处理
    // initBatteryMonitor 和 updateTime 已由 PlayerControlsManager 处理
    
    // 显示现代化的选项对话框
    
    // 用于单选的适配器（音频、解码、更多）
    inner class SelectionAdapter(
        private val items: List<String>,
        private var selectedPosition: Int,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<SelectionAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val itemText: TextView = view.findViewById(R.id.itemText)
            val innerLayout: android.widget.LinearLayout = view.findViewById(R.id.innerLayout)
            
            fun bind(position: Int) {
                itemText.text = items[position]
                
                // 如果 selectedPosition 为 -1，表示所有项都显示为蓝色（如"更多"菜单）
                if (selectedPosition == -1) {
                    innerLayout.setBackgroundResource(R.drawable.bg_rounded_item)
                    itemText.setTextColor(android.graphics.Color.WHITE)
                } else {
                    // 正常的选中/未选中状态（如字幕轨道）
                    val isSelected = position == selectedPosition
                    if (isSelected) {
                        innerLayout.setBackgroundResource(R.drawable.bg_rounded_item)
                        itemText.setTextColor(android.graphics.Color.WHITE)
                    } else {
                        innerLayout.setBackgroundResource(R.drawable.bg_rounded_item_unselected)
                        itemText.setTextColor(android.graphics.Color.BLACK)
                    }
                }
                
                itemView.setOnClickListener {
                    onItemClick(position)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.dialog_selection_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }
        
        override fun getItemCount() = items.size
    }
    
    // 字幕菜单适配器（用于字幕设置菜单）
    inner class MenuItemAdapter(
        private val items: List<String>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MenuItemAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val itemText: TextView = view.findViewById(R.id.itemText)
            
            fun bind(position: Int) {
                itemText.text = items[position]
                
                itemView.setOnClickListener {
                    onItemClick(position)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.dialog_selection_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }
        
        override fun getItemCount() = items.size
    }
    
    // 适配器类
    
    // 显示更多选项对话框
    private fun showMoreOptionsDialog() {
        val options = mutableListOf<String>()
        
        // 检查是否有章节
        val hasChapters = playbackEngine?.hasChapters() ?: false
        
        if (hasChapters) {
            options.add("章节")
        }
        
        // 添加截图选项
        options.add("截图")
        
        if (options.isEmpty()) {
            DialogUtils.showToastShort(this, "暂无可用选项")
            return
        }
        
        val dialog = android.app.Dialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_more, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        dialogTitle.text = "更多"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isVerticalScrollBarEnabled = false
        
        val adapter = SelectionAdapter(options, -1) { position ->
            when (options[position]) {
                "章节" -> showChapterDialog()
                "截图" -> takeScreenshot()
            }
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    // 显示章节选择对话框
    private fun showChapterDialog() {
        playbackEngine?.let { engine ->
            try {
                val chapters = engine.getChapters()
                
                if (chapters.isEmpty()) {
                    DialogUtils.showToastShort(this, "该视频没有章节")
                    return
                }
                
                showChapterListDialog(chapters)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing chapter dialog", e)
                DialogUtils.showToastShort(this, "获取章节失败")
            }
        }
    }
    
    // 显示章节列表对话框（自定义布局）
    private fun showChapterListDialog(chapters: List<Pair<String, Double>>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_option_list, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)
        val scrollHint = dialogView.findViewById<TextView>(R.id.scrollHint)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        dialogTitle.text = "章节"
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // 计算章节文本的最大宽度
        val paint = android.graphics.Paint()
        paint.textSize = 15 * resources.displayMetrics.scaledDensity
        var maxTextWidth = 0f
        chapters.forEach { chapter ->
            val titleWidth = paint.measureText(chapter.first)
            val timeWidth = paint.measureText(formatChapterTime(chapter.second))
            maxTextWidth = maxOf(maxTextWidth, maxOf(titleWidth, timeWidth))
        }
        
        val titlePaint = android.graphics.Paint()
        titlePaint.textSize = 16 * resources.displayMetrics.scaledDensity
        val titleWidth = titlePaint.measureText("章节")
        
        // 计算对话框宽度
        val iconWidth = 20 * resources.displayMetrics.density
        val spacing = 12 * resources.displayMetrics.density
        val itemPadding = 32 * resources.displayMetrics.density
        val dialogPadding = 40 * resources.displayMetrics.density
        
        val contentWidth = maxOf(maxTextWidth, titleWidth)
        val totalWidth = (contentWidth + iconWidth + spacing + itemPadding + dialogPadding).toInt()
        val minWidth = (240 * resources.displayMetrics.density).toInt()
        val finalWidth = maxOf(totalWidth, minWidth)
        
        recyclerView.layoutParams.width = (finalWidth - dialogPadding).toInt()
        
        // 限制最多显示3行
        if (chapters.size > 3) {
            val itemHeight = (60 * resources.displayMetrics.density).toInt()
            val maxHeight = itemHeight * 3
            recyclerView.layoutParams.height = maxHeight
            scrollHint.visibility = android.view.View.VISIBLE
        }
        
        val adapter = ChapterAdapter(chapters) { position ->
            val chapter = chapters[position]
            // 立即跳转，不显示提示
            playbackEngine?.seekTo(chapter.second.toInt())
        }
        recyclerView.adapter = adapter
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        
        // 设置对话框宽度
        dialog.window?.setLayout(
            finalWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        adapter.onItemSelected = { dialog.dismiss() }
    }
    
    // 格式化章节时间
    private fun formatChapterTime(seconds: Double): String {
        val hours = (seconds / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    // 章节适配器
    inner class ChapterAdapter(
        private val chapters: List<Pair<String, Double>>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ChapterAdapter.ViewHolder>() {
        
        var onItemSelected: (() -> Unit)? = null
        
        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val chapterIcon: ImageView = view.findViewById(R.id.chapterIcon)
            val chapterTitle: TextView = view.findViewById(R.id.chapterTitle)
            val chapterTime: TextView = view.findViewById(R.id.chapterTime)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.dialog_chapter_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val chapter = chapters[position]
            holder.chapterTitle.text = chapter.first
            holder.chapterTime.text = formatChapterTime(chapter.second)
            
            holder.itemView.setOnClickListener {
                // 先关闭对话框，再跳转，减少卡顿感
                onItemSelected?.invoke()
                
                // 使用 postDelayed 让关闭动画先执行
                Handler(Looper.getMainLooper()).postDelayed({
                    onItemClick(holder.adapterPosition)
                }, 50)
            }
        }
        
        override fun getItemCount() = chapters.size
    }
    
    // 截图功能 - 直接从SurfaceView抓取当前画面
    private fun takeScreenshot() {
        try {
            Log.d(TAG, "takeScreenshot() called")
            
            // 显示加载状态
            showScreenshotStatus(loading = true)
            
            // 从SurfaceView抓取当前画面
            val bitmap = Bitmap.createBitmap(
                surfaceView.width,
                surfaceView.height,
                Bitmap.Config.ARGB_8888
            )
            
            // 使用PixelCopy API (Android 7.0+) 从SurfaceView抓取内容
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val location = IntArray(2)
                surfaceView.getLocationInWindow(location)
                
                android.view.PixelCopy.request(
                    surfaceView.holder.surface,
                    bitmap,
                    { copyResult ->
                        if (copyResult == android.view.PixelCopy.SUCCESS) {
                            Log.d(TAG, "PixelCopy successful")
                            saveBitmapToFile(bitmap)
                        } else {
                            Log.e(TAG, "PixelCopy failed with result: $copyResult")
                            runOnUiThread {
                                hideScreenshotStatus()
                                DialogUtils.showToastShort(this, "截图失败: 无法捕获画面")
                            }
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                // Android 7.0以下使用备用方法
                hideScreenshotStatus()
                DialogUtils.showToastShort(this, "您的Android版本不支持截图")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot failed", e)
            hideScreenshotStatus()
            DialogUtils.showToastShort(this, "截图失败: ${e.message}")
        }
    }
    
    // 显示截图状态
    private fun showScreenshotStatus(loading: Boolean) {
        runOnUiThread {
            screenshotStatusCard?.visibility = View.VISIBLE
            screenshotStatusCard?.alpha = 0f
            screenshotStatusCard?.scaleX = 0.8f
            screenshotStatusCard?.scaleY = 0.8f
            
            if (loading) {
                screenshotProgressBar?.visibility = View.VISIBLE
                screenshotSuccessIcon?.visibility = View.GONE
                screenshotStatusText?.text = "正在保存..."
            } else {
                screenshotProgressBar?.visibility = View.GONE
                screenshotSuccessIcon?.visibility = View.VISIBLE
                screenshotStatusText?.text = "已保存"
            }
            
            // 入场动画
            screenshotStatusCard?.animate()
                ?.alpha(1f)
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setDuration(200)
                ?.setInterpolator(android.view.animation.DecelerateInterpolator())
                ?.start()
        }
    }
    
    // 隐藏截图状态
    private fun hideScreenshotStatus() {
        runOnUiThread {
            screenshotStatusCard?.animate()
                ?.alpha(0f)
                ?.scaleX(0.8f)
                ?.scaleY(0.8f)
                ?.setDuration(200)
                ?.setInterpolator(android.view.animation.AccelerateInterpolator())
                ?.withEndAction {
                    screenshotStatusCard?.visibility = View.GONE
                }
                ?.start()
        }
    }
    
    // 保存Bitmap到文件
    private fun saveBitmapToFile(bitmap: Bitmap) {
        try {
            val filename = "Screenshot_${FormatUtils.generateTimestamp()}.png"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore
                Log.d(TAG, "Saving to MediaStore")
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FAM4K007")
                }
                
                val resolver = contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    
                    runOnUiThread {
                        // 切换到成功状态
                        showScreenshotStatus(loading = false)
                        // 1秒后自动隐藏
                        Handler(Looper.getMainLooper()).postDelayed({
                            hideScreenshotStatus()
                        }, 1000)
                    }
                    Log.d(TAG, "Screenshot saved to MediaStore: $filename")
                } else {
                    Log.e(TAG, "Failed to create MediaStore URI")
                    runOnUiThread {
                        hideScreenshotStatus()
                        DialogUtils.showToastShort(this, "截图保存失败")
                    }
                }
            } else {
                // Android 10以下直接保存到Pictures目录
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FAM4K007")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                val file = File(dir, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                
                // 通知系统扫描新文件
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                
                runOnUiThread {
                    // 切换到成功状态
                    showScreenshotStatus(loading = false)
                    // 1秒后自动隐藏
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideScreenshotStatus()
                    }, 1000)
                }
                Log.d(TAG, "Screenshot saved: ${file.absolutePath}")
            }
            
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap", e)
            runOnUiThread {
                DialogUtils.showToastShort(this, "保存截图失败: ${e.message}")
            }
        }
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}




