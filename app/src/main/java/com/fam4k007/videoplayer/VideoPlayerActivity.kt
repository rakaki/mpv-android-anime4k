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
import android.view.View
import com.fam4k007.videoplayer.player.CustomMPVView
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
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.utils.getThemeAttrColor
import `is`.xyz.mpv.MPVLib
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
    private lateinit var danmakuManager: com.fam4k007.videoplayer.danmaku.DanmakuManager

    // ========== UI 组件（仅保留必需的引用）==========
    private lateinit var mpvView: CustomMPVView
    private lateinit var danmakuView: com.fam4k007.videoplayer.danmaku.DanmakuPlayerView
    private lateinit var clickArea: View
    
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
    
    // 手势提示View
    private lateinit var seekHint: TextView
    private lateinit var speedHint: LinearLayout
    private lateinit var speedHintText: TextView
    
    // 字幕文件选择器
    private lateinit var subtitlePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    // 弹幕文件选择器
    private lateinit var danmakuPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    // 记录打开字幕选择器前的播放状态
    private var wasPlayingBeforeSubtitlePicker = false
    
    // 记录打开弹幕选择器前的播放状态
    private var wasPlayingBeforeDanmakuPicker = false
    
    // 双击进度累积（用于连续双击）
    private var seekAccumulator = 0
    private var isSeekingForward = true
    private val seekAccumulatorHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var seekAccumulatorRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用主题（必须在 super.onCreate 之前）
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_video_player)
        
        // ========== 保持屏幕常亮 ==========
        // 在播放视频时防止屏幕自动锁定
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "Screen keep-on enabled")

        // 初始化PreferencesManager
        preferencesManager = PreferencesManager.getInstance(this)
        
        // 初始化字幕文件选择器
        initializeSubtitleFilePicker()
        
        // 初始化弹幕文件选择器
        initializeDanmakuFilePicker()
        
        // 初始化字体文件选择器
        
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
        mpvView = findViewById(R.id.surfaceView)
        danmakuView = findViewById(R.id.danmakuView)
        clickArea = findViewById(R.id.clickArea)
        
        // ========== 关键修复: 在Activity中初始化MPV ==========
        // 参考 mpvKt: player.initialize(filesDir.path, cacheDir.path)
        // BaseMPVView.initialize() 会调用 MPVLib.create() 和 init()
        Log.d(TAG, "Initializing MPV in Activity...")
        try {
            mpvView.initialize(filesDir.path, cacheDir.path)
            Log.d(TAG, "MPV initialized successfully")
            
            // MPV 初始化完成后加载视频
            mpvView.postDelayed({
                Log.d(TAG, "Loading video after MPV init")
                loadVideo()
            }, 100) // 延迟 100ms 确保 MPV 完全就绪
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MPV", e)
            DialogUtils.showToastShort(this, "MPV 初始化失败: ${e.message}")
            finish()
            return
        }
        
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
        
        // 初始化弹幕管理器
        danmakuManager = com.fam4k007.videoplayer.danmaku.DanmakuManager(this, danmakuView)
        danmakuManager.initialize()
        
        // 初始化所有管理器
        initializeManagers()
        
        // 获取从列表传入的视频数据
        handleVideoListIntent()
        
        // 注意：loadVideo() 现在在 MPV 初始化完成后的回调中调用
    }

    /**
     * 初始化所有管理器
     */
    private fun initializeManagers() {
        // 初始化 PlaybackEngine
        playbackEngine = PlaybackEngine(
            mpvView,
            WeakReference(this),
            object : PlaybackEngine.PlaybackEventCallback {
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    this@VideoPlayerActivity.isPlaying = isPlaying
                    // 更新播放/暂停按钮状态
                    controlsManager?.updatePlayPauseButton(isPlaying)
                    
                    // 同步弹幕播放状态
                    if (isPlaying) {
                        danmakuManager.resume()
                    } else {
                        danmakuManager.pause()
                    }
                }
                
                override fun onProgressUpdate(position: Double, duration: Double) {
                    this@VideoPlayerActivity.currentPosition = position
                    this@VideoPlayerActivity.duration = duration
                    // 更新进度条和时间显示
                    controlsManager?.updateProgress(position, duration)
                    
                    // 参考 DanDanPlay: 弹幕会自动跟随时间轴播放
                    // 不需要在每次进度更新时调用 seekTo
                    // 只在用户手动 seek 或恢复进度时才需要同步（已在 prepared() 回调中处理）
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
                    // Surface 已准备完成（由 BaseMPVView 自动管理）
                    Log.d(TAG, "Surface ready callback received")
                    // BaseMPVView 会自动处理 Surface 生命周期，无需手动加载视频
                }
            }
        )
        
        // 初始化 PlaybackEngine
        if (!playbackEngine.initialize()) {
            DialogUtils.showToastLong(this, "播放器初始化失败")
            finish()
            return
        }
        
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
                        danmakuManager.setSpeed(1.0f)
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
                        danmakuManager.setSpeed(longPressSpeed)
                        
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
                        // 用户手势 seek 时同步弹幕进度
                        danmakuManager.seekTo((newPos * 1000).toLong())
                        
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
                    // 快退时同步弹幕
                    val newPos = (currentPosition - seekTimeSeconds).coerceAtLeast(0.0)
                    danmakuManager.seekTo((newPos * 1000).toLong())
                }
                
                override fun onForwardClick() {
                    Log.d(SEEK_DEBUG, "onForwardClick: seekTimeSeconds = $seekTimeSeconds, currentPosition = $currentPosition, seekBy = $seekTimeSeconds")
                    playbackEngine.seekBy(seekTimeSeconds)
                    // 快进时同步弹幕
                    val newPos = (currentPosition + seekTimeSeconds).coerceAtMost(duration)
                    danmakuManager.seekTo((newPos * 1000).toLong())
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
                    // 用户手动 seek 时同步弹幕进度
                    danmakuManager.seekTo((position * 1000).toLong())
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
        
        // 绑定弹幕按钮点击事件
        val btnDanmaku = findViewById<ImageView>(R.id.btnDanmaku)
        btnDanmaku.setOnClickListener {
            showDanmakuDialog()
        }
        
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
            
            // 加载弹幕文件
            loadDanmakuForVideo(uri)
            
            // 延迟恢复字幕设置（等待视频加载完成）
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                restoreSubtitlePreferences(uri)
            }, 500)
        }
    }
    
    /**
     * 加载视频对应的弹幕文件
     */
    private fun loadDanmakuForVideo(videoUri: android.net.Uri) {
        try {
            val videoPath = videoUri.resolveUri(this)
            if (videoPath != null) {
                Log.d(TAG, "Loading danmaku for video: $videoPath")
                danmakuManager.loadDanmakuForVideo(videoUri.toString(), videoPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading danmaku", e)
        }
    }
    
    /**
     * 恢复视频的字幕偏好设置
     */
    private fun restoreSubtitlePreferences(videoUri: android.net.Uri) {
        try {
            val uriString = videoUri.toString()
            Log.d(TAG, "Restoring subtitle preferences for: $uriString")
            
            playbackEngine?.let { engine ->
                // 恢复 ASS 覆盖设置
                val assOverride = preferencesManager.isAssOverrideEnabled(uriString)
                if (assOverride) {
                    // 延迟设置，避免在 MPV 未准备好时设置
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        engine.setAssOverride(assOverride)
                        Log.d(TAG, "Restored ASS override: $assOverride")
                    }, 300)
                }
                
                // 恢复字幕大小
                val savedScale = preferencesManager.getSubtitleScale(uriString)
                if (savedScale != 1.0) {
                    engine.setSubtitleScale(savedScale)
                    Log.d(TAG, "Restored subtitle scale: $savedScale")
                }
                
                // 恢复字幕位置
                val savedPosition = preferencesManager.getSubtitlePosition(uriString)
                if (savedPosition != 100) {
                    engine.setSubtitleVerticalPosition(savedPosition)
                    Log.d(TAG, "Restored subtitle position: $savedPosition")
                }
                
                // 恢复字幕延迟
                val savedDelay = preferencesManager.getSubtitleDelay(uriString)
                if (savedDelay != 0.0) {
                    engine.setSubtitleDelay(savedDelay)
                    Log.d(TAG, "Restored subtitle delay: $savedDelay")
                }
                
                // 恢复外部字幕（现在保存的是本地文件路径）
                val savedSubtitlePath = preferencesManager.getExternalSubtitle(uriString)
                if (savedSubtitlePath != null) {
                    if (File(savedSubtitlePath).exists()) {
                        try {
                            // 使用本地文件路径直接添加字幕
                            MPVLib.command("sub-add", savedSubtitlePath, "select")
                            Log.d(TAG, "Restored external subtitle from path: $savedSubtitlePath")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to restore external subtitle", e)
                        }
                    } else {
                        Log.w(TAG, "Saved subtitle file not found: $savedSubtitlePath")
                    }
                }
                
                // 恢复字幕轨道选择
                val savedTrackId = preferencesManager.getSubtitleTrackId(uriString)
                if (savedTrackId != -1) {
                    // 延迟恢复，确保轨道已加载
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            engine.selectSubtitleTrack(savedTrackId)
                            Log.d(TAG, "Restored subtitle track: $savedTrackId")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to restore subtitle track", e)
                        }
                    }, 600)
                }
                
                // 恢复字幕样式设置
                val savedTextColor = preferencesManager.getSubtitleTextColor(uriString)
                if (savedTextColor != "#FFFFFF") {
                    engine.setSubtitleTextColor(savedTextColor)
                    Log.d(TAG, "Restored subtitle text color: $savedTextColor")
                }
                
                val savedBorderSize = preferencesManager.getSubtitleBorderSize(uriString)
                if (savedBorderSize != 3) {
                    engine.setSubtitleBorderSize(savedBorderSize)
                    Log.d(TAG, "Restored subtitle border size: $savedBorderSize")
                }
                
                val savedBorderColor = preferencesManager.getSubtitleBorderColor(uriString)
                if (savedBorderColor != "#000000") {
                    engine.setSubtitleBorderColor(savedBorderColor)
                    Log.d(TAG, "Restored subtitle border color: $savedBorderColor")
                }
                
                val savedBackColor = preferencesManager.getSubtitleBackColor(uriString)
                if (savedBackColor != "#00000000") {
                    engine.setSubtitleBackColor(savedBackColor)
                    Log.d(TAG, "Restored subtitle back color: $savedBackColor")
                }
                
                val savedBorderStyle = preferencesManager.getSubtitleBorderStyle(uriString)
                if (savedBorderStyle != "outline-and-shadow") {
                    engine.setSubtitleBorderStyle(savedBorderStyle)
                    Log.d(TAG, "Restored subtitle border style: $savedBorderStyle")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring subtitle preferences", e)
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
                
                // 获取音轨按钮（在顶部，对话框显示在下方）
                val btnAudioTrack = findViewById<ImageView>(R.id.btnAudioTrack)
                
                showPopupDialog(btnAudioTrack, items, currentTrackIndex, showAbove = false) { position ->
                    val trackId = audioTracks[position].first
                    engine.selectAudioTrack(trackId)
                    DialogUtils.showToastShort(this@VideoPlayerActivity, "已切换到: ${items[position]}")
                }
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
            try {
                if (uri != null) {
                    Log.d(TAG, "Subtitle file selected: $uri")
                    val displayName = subtitleManager.getSubtitleDisplayName(this, uri)
                    Log.d(TAG, "Subtitle file name: $displayName")
                    
                    if (subtitleManager.isSupportedSubtitleFormat(displayName)) {
                        // 添加外挂字幕（sub-add 命令会自动选中）
                        if (subtitleManager.addExternalSubtitle(this, uri)) {
                            // 保存外部字幕的本地文件路径（不是URI）
                            videoUri?.let { vUri ->
                                val subtitlePath = subtitleManager.getLastAddedSubtitlePath()
                                if (subtitlePath != null) {
                                    preferencesManager.setExternalSubtitle(vUri.toString(), subtitlePath)
                                    Log.d(TAG, "Saved subtitle path: $subtitlePath")
                                }
                            }
                            DialogUtils.showToastShort(this, "已添加字幕: $displayName")
                            Log.d(TAG, "External subtitle added successfully: $displayName")
                        } else {
                            DialogUtils.showToastShort(this, "添加字幕失败")
                            Log.w(TAG, "Failed to add external subtitle: $displayName")
                        }
                    } else {
                        DialogUtils.showToastShort(this, "不支持的字幕格式: $displayName")
                        Log.w(TAG, "Unsupported subtitle format: $displayName")
                    }
                } else {
                    Log.d(TAG, "Subtitle file selection cancelled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling subtitle file selection", e)
                DialogUtils.showToastShort(this, "添加字幕出错: ${e.message}")
            } finally {
                // 恢复之前的播放状态
                if (wasPlayingBeforeSubtitlePicker) {
                    Log.d(TAG, "Resuming playback after subtitle picker")
                    playbackEngine?.play()
                    wasPlayingBeforeSubtitlePicker = false
                }
            }
        }
    }
    
    /**
     * 初始化弹幕文件选择器
     */
    private fun initializeDanmakuFilePicker() {
        // 创建文件选择器 Launcher
        danmakuPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            try {
                if (uri != null) {
                    Log.d(TAG, "Danmaku file selected: $uri")
                    
                    // 获取文件名
                    val fileName = getFileNameFromUri(uri)
                    Log.d(TAG, "Danmaku file name: $fileName")
                    
                    // 检查是否为XML文件
                    if (fileName.endsWith(".xml", ignoreCase = true)) {
                        // 复制到缓存目录
                        val cachedFile = copyDanmakuToCache(uri, fileName)
                        if (cachedFile != null) {
                            // 加载弹幕文件
                            val loaded = danmakuManager.loadDanmakuFile(cachedFile.absolutePath)
                            if (loaded) {
                                DialogUtils.showToastShort(this, "弹幕加载成功: $fileName")
                                Log.d(TAG, "Danmaku loaded successfully: $fileName")
                                
                                // 启动弹幕
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    danmakuManager.start()
                                }, 500)
                            } else {
                                DialogUtils.showToastShort(this, "弹幕加载失败")
                                Log.w(TAG, "Failed to load danmaku: $fileName")
                            }
                        } else {
                            DialogUtils.showToastShort(this, "无法读取弹幕文件")
                            Log.w(TAG, "Failed to copy danmaku file")
                        }
                    } else {
                        DialogUtils.showToastShort(this, "请选择 XML 格式的弹幕文件")
                        Log.w(TAG, "Unsupported danmaku format: $fileName")
                    }
                } else {
                    Log.d(TAG, "Danmaku file selection cancelled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling danmaku file selection", e)
                DialogUtils.showToastShort(this, "加载弹幕出错: ${e.message}")
            } finally {
                // 恢复之前的播放状态
                if (wasPlayingBeforeDanmakuPicker) {
                    Log.d(TAG, "Resuming playback after danmaku picker")
                    playbackEngine?.play()
                    wasPlayingBeforeDanmakuPicker = false
                }
            }
        }
    }
    
    /**
     * 复制弹幕文件到缓存目录
     */
    private fun copyDanmakuToCache(uri: Uri, fileName: String): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val cacheFile = File(cacheDir, "danmaku_$fileName")
            
            cacheFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            Log.d(TAG, "Danmaku file copied to: ${cacheFile.absolutePath}")
            return cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying danmaku file", e)
            return null
        }
    }
    
    /**
     * 初始化字体文件选择器
     */
    /**
     * 打开字幕选择器
     */
    private fun openSubtitlePicker() {
        if (::subtitlePickerLauncher.isInitialized) {
            // 记录当前播放状态
            wasPlayingBeforeSubtitlePicker = isPlaying
            
            // 如果正在播放，暂停视频
            if (isPlaying) {
                Log.d(TAG, "Pausing playback before opening subtitle picker")
                playbackEngine?.pause()
            }
            
            // 启动文件选择器
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

    /**
     * 打开弹幕选择器
     */
    private fun openDanmakuPicker() {
        if (::danmakuPickerLauncher.isInitialized) {
            // 记录当前播放状态
            wasPlayingBeforeDanmakuPicker = isPlaying
            
            // 如果正在播放，暂停视频
            if (isPlaying) {
                Log.d(TAG, "Pausing playback before opening danmaku picker")
                playbackEngine?.pause()
            }
            
            // 启动文件选择器，只允许选择 XML 文件
            danmakuPickerLauncher.launch(
                arrayOf(
                    "text/xml",
                    "application/xml",
                    "*/*"
                )
            )
        }
    }

    /**
     * 显示弹出式对话框（在按钮上方或下方显示）
     * @param anchorView 锚点视图（按钮）
     * @param items 选项列表
     * @param selectedPosition 当前选中位置（-1表示无选中状态）
     * @param showAbove 是否显示在按钮上方（true=上方，false=下方）
     * @param useFixedHeight 是否使用固定高度（true=固定144dp，false=自适应）
     * @param showScrollHint 是否显示滑动提示（当项目数量 > 3时）
     * @param onItemClick 点击回调
     */
    private fun showPopupDialog(
        anchorView: View,
        items: List<String>,
        selectedPosition: Int = -1,
        showAbove: Boolean = true,
        useFixedHeight: Boolean = false,
        showScrollHint: Boolean = false,
        onItemClick: (Int) -> Unit
    ) {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        // 根据是否需要固定高度选择不同的布局文件
        val layoutRes = if (useFixedHeight) R.layout.dialog_popup_menu_fixed else R.layout.dialog_popup_menu
        val dialogView = layoutInflater.inflate(layoutRes, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewPopup)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.isVerticalScrollBarEnabled = false
        
        // 如果使用固定高度且需要显示滑动提示
        if (useFixedHeight && showScrollHint && items.size > 3) {
            val scrollHint = dialogView.findViewById<TextView>(R.id.scrollHint)
            scrollHint?.visibility = View.VISIBLE
        }
        
        val adapter = PopupMenuAdapter(items, selectedPosition) { position ->
            onItemClick(position)
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)
        
        // 获取锚点视图在屏幕上的位置
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        
        // 测量对话框大小
        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val dialogWidth = dialogView.measuredWidth.coerceAtLeast(anchorView.width)
        val dialogHeight = dialogView.measuredHeight
        
        // 计算对话框位置
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        layoutParams?.x = anchorX + (anchorView.width - dialogWidth) / 2
        
        // 根据参数决定显示在上方还是下方
        layoutParams?.y = if (showAbove) {
            // 显示在按钮上方，不遮挡按钮
            anchorY - dialogHeight - 10
        } else {
            // 显示在按钮下方，不遮挡按钮
            anchorY + anchorView.height + 10
        }
        
        layoutParams?.width = dialogWidth
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams
        
        // 设置进场和出场动画
        window?.setWindowAnimations(R.style.PopupAnimation)
        
        dialog.show()
        
        // 如果有选中项且使用固定高度，自动滚动到选中位置
        if (selectedPosition >= 0 && useFixedHeight) {
            val nestedScrollView = dialogView.findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollViewPopup)
            nestedScrollView?.post {
                // 计算选中项的位置
                val itemHeight = 48.dpToPx() // 每个选项的高度
                val scrollViewHeight = nestedScrollView.height
                val targetY = selectedPosition * itemHeight - (scrollViewHeight / 2) + (itemHeight / 2)
                
                // 滚动到目标位置，让选中项居中
                nestedScrollView.smoothScrollTo(0, targetY.coerceAtLeast(0))
            }
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
                    menuItems.add("字幕杂项")
                }
                menuItems.add("样式设置")
                menuItems.add("外挂字幕")
                menuItems.add("更改字体")
                
                // 获取字幕按钮（在顶部，对话框显示在下方）
                val btnSubtitle = findViewById<ImageView>(R.id.btnSubtitle)
                
                showPopupDialog(btnSubtitle, menuItems, showAbove = false, useFixedHeight = true, showScrollHint = true) { position ->
                    when {
                        position == menuItems.size - 3 -> {
                            // 样式设置
                            showSubtitleStyleDialog(engine)
                        }
                        position == menuItems.size - 2 -> {
                            // 外挂字幕
                            openSubtitlePicker()
                        }
                        position == menuItems.size - 1 -> {
                            // 更改字体 - 待开发
                            DialogUtils.showToastShort(this@VideoPlayerActivity, "字体更改功能开发中...")
                        }
                        subtitleTracks.isNotEmpty() -> {
                            when (position) {
                                0 -> showSubtitleTrackSelection(engine, subtitleTracks)
                                1 -> showSubtitleDelayDialog(engine)
                                2 -> showSubtitleMiscDialog(engine)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing subtitle dialog", e)
                DialogUtils.showToastShort(this, "获取字幕失败")
            }
        }
    }

    private fun showSubtitleTrackSelection(engine: PlaybackEngine, subtitleTracks: List<Triple<Int, String, Boolean>>) {
        val items = subtitleTracks.map { it.second }
        val currentTrackIndex = subtitleTracks.indexOfFirst { it.third }
        
        // 获取字幕按钮（在顶部，对话框显示在下方）
        val btnSubtitle = findViewById<ImageView>(R.id.btnSubtitle)
        
        showPopupDialog(btnSubtitle, items, currentTrackIndex, showAbove = false, useFixedHeight = false) { position ->
            val trackId = subtitleTracks[position].first
            engine.selectSubtitleTrack(trackId)
            DialogUtils.showToastShort(this@VideoPlayerActivity, "已切换到: ${items[position]}")
            
            // 保存选中的字幕轨道
            videoUri?.let { uri ->
                preferencesManager.setSubtitleTrackId(uri.toString(), trackId)
                Log.d(TAG, "Saved subtitle track ID: $trackId")
            }
        }
    }

    private fun showSubtitleDelayDialog(engine: PlaybackEngine) {
        val currentDelay = engine.getSubtitleDelay()
        val stepValue = 0.2  // 每次步进0.2秒
        val defaultDelay = 0.0  // 默认延迟值
        
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_subtitle_delay, null)
        
        val etDelayValue = view.findViewById<EditText>(R.id.etDelayValue)
        val btnDecrease = view.findViewById<Button>(R.id.btnDecreaseDelay)
        val btnIncrease = view.findViewById<Button>(R.id.btnIncreaseDelay)
        val btnReset = view.findViewById<Button>(R.id.btnResetDelay)
        
        // 设置初始值，只显示数字
        etDelayValue.setText(String.format("%.1f", currentDelay))
        
        // 防止进入时自动弹出输入法
        etDelayValue.clearFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        
        // 更新重置按钮状态
        fun updateResetButton() {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            btnReset.isEnabled = Math.abs(currentValue - defaultDelay) > 0.01
        }
        updateResetButton()
        
        // 更新延迟值
        fun updateDelayValue(newValue: Double) {
            val clampedValue = newValue.coerceIn(-10.0, 10.0)
            val roundedValue = (Math.round(clampedValue * 10.0) / 10.0)
            etDelayValue.setText(String.format("%.1f", roundedValue))
            engine.setSubtitleDelay(roundedValue)
            updateResetButton()
            
            // 保存设置
            videoUri?.let { uri ->
                preferencesManager.setSubtitleDelay(uri.toString(), roundedValue)
            }
        }
        
        // 减号按钮 - 减0.2秒
        btnDecrease.setOnClickListener {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            updateDelayValue(currentValue - stepValue)
        }
        
        // 加号按钮 - 加0.2秒
        btnIncrease.setOnClickListener {
            val currentValue = etDelayValue.text.toString().toDoubleOrNull() ?: 0.0
            updateDelayValue(currentValue + stepValue)
        }
        
        // 重置按钮 - 恢复到默认值
        btnReset.setOnClickListener {
            updateDelayValue(defaultDelay)
        }
        
        // 监听手动输入
        etDelayValue.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val value = s.toString().toDoubleOrNull()
                if (value != null && value >= -10.0 && value <= 10.0) {
                    engine.setSubtitleDelay(value)
                    updateResetButton()
                    // 保存设置
                    videoUri?.let { uri ->
                        preferencesManager.setSubtitleDelay(uri.toString(), value)
                    }
                }
            }
        })
        
        // 配置对话框 - 与字幕杂项保持一致的位置
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 获取屏幕宽度，设置对话框宽度
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val dialogWidth = (screenWidth * 0.4).toInt().coerceIn(
            (200 * displayMetrics.density).toInt(),
            (300 * displayMetrics.density).toInt()
        )
        
        // 将 dp 转换为 px
        val marginRightDp = 65
        val marginRightPx = (marginRightDp * displayMetrics.density).toInt()
        
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // 设置对话框位置在右边界中部（与字幕杂项一致）
        dialog.window?.setGravity(android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.RIGHT)
        
        val params = dialog.window?.attributes
        params?.x = marginRightPx
        dialog.window?.attributes = params
        
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    /**
     * 显示字幕杂项设置对话框（包含字幕大小和位置）
     */
    private fun showSubtitleMiscDialog(engine: PlaybackEngine) {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_subtitle_misc, null)
        
        // 获取UI组件
        val tvSizeLabel = view.findViewById<TextView>(R.id.tvSizeLabel)
        val tvSizeValue = view.findViewById<TextView>(R.id.tvSizeValue)
        val seekBarSize = view.findViewById<SeekBar>(R.id.seekBarSize)
        
        val tvPositionLabel = view.findViewById<TextView>(R.id.tvPositionLabel)
        val tvPositionValue = view.findViewById<TextView>(R.id.tvPositionValue)
        val seekBarPosition = view.findViewById<SeekBar>(R.id.seekBarPosition)
        
        val btnReset = view.findViewById<Button>(R.id.btnReset)
        
        // 获取当前值
        val currentScale = engine.getSubtitleScale()
        val currentPosition = engine.getSubtitleVerticalPosition()
        
        // 定义默认值（这些是MPV的默认值）
        val defaultScale = 1.0
        val defaultPosition = 100
        
        // 配置字幕大小SeekBar (0-50, 对应 0.0-5.0)
        seekBarSize.max = 50
        val currentSizeProgress = (currentScale * 10).toInt().coerceIn(0, 50)
        seekBarSize.progress = currentSizeProgress
        tvSizeValue.text = String.format("%.1f", currentScale)
        
        // 配置字幕位置SeekBar (0-150)
        seekBarPosition.max = 150
        seekBarPosition.progress = currentPosition
        tvPositionValue.text = "$currentPosition"
        
        // 设置主题颜色
        val primaryColor = ThemeManager.getThemeColor(this, com.google.android.material.R.attr.colorPrimary)
        tvSizeLabel.setTextColor(primaryColor)
        tvPositionLabel.setTextColor(primaryColor)
        tvSizeValue.setTextColor(primaryColor)
        tvPositionValue.setTextColor(primaryColor)
        
        // 设置SeekBar颜色
        fun setSeekBarColor(seekBar: SeekBar) {
            seekBar.progressDrawable?.setColorFilter(primaryColor, android.graphics.PorterDuff.Mode.SRC_IN)
            seekBar.thumb?.setColorFilter(primaryColor, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        setSeekBarColor(seekBarSize)
        setSeekBarColor(seekBarPosition)
        
        // 更新重置按钮状态
        fun updateResetButton() {
            val defaultSizeProgress = (defaultScale * 10).toInt()
            val isSizeAtDefault = seekBarSize.progress == defaultSizeProgress
            val isPositionAtDefault = seekBarPosition.progress == defaultPosition
            val isAllDefault = isSizeAtDefault && isPositionAtDefault
            
            btnReset.isEnabled = !isAllDefault
        }
        updateResetButton()
        
        // 字幕大小SeekBar监听器
        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = progress / 10.0
                tvSizeValue.text = String.format("%.1f", scale)
                engine.setSubtitleScale(scale)
                updateResetButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 保存字幕大小设置
                val scale = seekBar?.progress?.let { it / 10.0 } ?: 1.0
                videoUri?.let { uri ->
                    preferencesManager.setSubtitleScale(uri.toString(), scale)
                }
            }
        })
        
        // 字幕位置SeekBar监听器
        seekBarPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPositionValue.text = "$progress"
                engine.setSubtitleVerticalPosition(progress)
                updateResetButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 保存字幕位置设置
                val position = seekBar?.progress ?: 100
                videoUri?.let { uri ->
                    preferencesManager.setSubtitlePosition(uri.toString(), position)
                }
            }
        })
        
        // 重置按钮点击事件
        btnReset.setOnClickListener {
            val defaultSizeProgress = (defaultScale * 10).toInt()
            seekBarSize.progress = defaultSizeProgress
            seekBarPosition.progress = defaultPosition
            tvSizeValue.text = String.format("%.1f", defaultScale)
            tvPositionValue.text = "$defaultPosition"
            engine.setSubtitleScale(defaultScale)
            engine.setSubtitleVerticalPosition(defaultPosition)
            updateResetButton()
            
            // 保存重置后的设置
            videoUri?.let { uri ->
                preferencesManager.setSubtitleScale(uri.toString(), defaultScale)
                preferencesManager.setSubtitlePosition(uri.toString(), defaultPosition)
            }
        }
        
        // 配置对话框
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 获取屏幕宽度，设置对话框宽度
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val dialogWidth = (screenWidth * 0.4).toInt().coerceIn(
            (200 * displayMetrics.density).toInt(),
            (450 * displayMetrics.density).toInt()
        )
        
        // 将 dp 转换为 px
        val marginRightDp = 65
        val marginRightPx = (marginRightDp * displayMetrics.density).toInt()
        
        dialog.window?.setLayout(
            dialogWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // 设置对话框位置在右边界中部（不遮挡顶部和底部控制）
        dialog.window?.setGravity(android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.RIGHT)
        dialog.window?.attributes?.x = marginRightPx  // 右边距离边界100dp（已转换为px）
        dialog.setCanceledOnTouchOutside(true)
        
        dialog.show()
    }
    
    /**
     * 显示字幕样式设置对话框
     */
    private fun showSubtitleStyleDialog(engine: PlaybackEngine) {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_subtitle_style, null)
        
        // 获取当前设置
        val uriString = videoUri?.toString() ?: ""
        var currentTextColor = preferencesManager.getSubtitleTextColor(uriString)
        var currentBorderSize = preferencesManager.getSubtitleBorderSize(uriString).toDouble()
        var currentBorderColor = preferencesManager.getSubtitleBorderColor(uriString)
        var currentBackColor = preferencesManager.getSubtitleBackColor(uriString)
        var currentBorderStyle = preferencesManager.getSubtitleBorderStyle(uriString)
        
        // 默认值
        val defaultTextColor = "#FFFFFF"
        val defaultBorderSize = 3.0
        val defaultBorderColor = "#000000"
        val defaultBackColor = "#00000000"
        val defaultBorderStyle = "outline-and-shadow"
        
        // 获取UI组件
        val tvBorderSizeValue = view.findViewById<TextView>(R.id.tvBorderSizeValue)
        val seekBarBorderSize = view.findViewById<SeekBar>(R.id.seekBarBorderSize)
        val btnReset = view.findViewById<Button>(R.id.btnReset)
        val rgBorderStyle = view.findViewById<android.widget.RadioGroup>(R.id.rgBorderStyle)
        val rbOutlineAndShadow = view.findViewById<android.widget.RadioButton>(R.id.rbOutlineAndShadow)
        val rbOpaqueBox = view.findViewById<android.widget.RadioButton>(R.id.rbOpaqueBox)
        val rbBackgroundBox = view.findViewById<android.widget.RadioButton>(R.id.rbBackgroundBox)
        
        // 设置初始值（直接显示整数 0-100）
        seekBarBorderSize.max = 100
        seekBarBorderSize.progress = currentBorderSize.toInt().coerceIn(0, 100)
        tvBorderSizeValue.text = seekBarBorderSize.progress.toString()
        
        // 设置描边样式初始值
        when (currentBorderStyle) {
            "outline-and-shadow" -> rbOutlineAndShadow.isChecked = true
            "opaque-box" -> rbOpaqueBox.isChecked = true
            "background-box" -> rbBackgroundBox.isChecked = true
        }
        
        // 更新重置按钮状态
        fun updateResetButton() {
            val hasChanged = currentTextColor != defaultTextColor ||
                           currentBorderSize != defaultBorderSize ||
                           currentBorderColor != defaultBorderColor ||
                           currentBackColor != defaultBackColor ||
                           currentBorderStyle != defaultBorderStyle
            btnReset.isEnabled = hasChanged
        }
        updateResetButton()
        
        // 描边粗细SeekBar监听器（直接使用整数值 0-100）
        seekBarBorderSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBorderSizeValue.text = progress.toString()
                currentBorderSize = progress.toDouble()
                engine.setSubtitleBorderSize(progress)
                updateResetButton()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val size = seekBar?.progress ?: 3
                videoUri?.let { uri ->
                    preferencesManager.setSubtitleBorderSize(uri.toString(), size)
                }
            }
        })
        
        // 设置文本颜色
        setupColorPicker(view, "Text", currentTextColor, engine, uriString, 
            { currentTextColor = it; updateResetButton() },
            { engine.setSubtitleTextColor(it) },
            { preferencesManager.setSubtitleTextColor(uriString, it) })
        
        // 设置描边颜色
        setupColorPicker(view, "Border", currentBorderColor, engine, uriString,
            { currentBorderColor = it; updateResetButton() },
            { engine.setSubtitleBorderColor(it) },
            { preferencesManager.setSubtitleBorderColor(uriString, it) })
        
        // 设置背景颜色
        setupColorPicker(view, "Back", currentBackColor, engine, uriString,
            { currentBackColor = it; updateResetButton() },
            { engine.setSubtitleBackColor(it) },
            { preferencesManager.setSubtitleBackColor(uriString, it) })
        
        // 描边样式选择监听
        rgBorderStyle.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.rbOutlineAndShadow -> "outline-and-shadow"
                R.id.rbOpaqueBox -> "opaque-box"
                R.id.rbBackgroundBox -> "background-box"
                else -> "outline-and-shadow"
            }
            currentBorderStyle = style
            engine.setSubtitleBorderStyle(style)
            videoUri?.let { uri ->
                preferencesManager.setSubtitleBorderStyle(uri.toString(), style)
            }
            updateResetButton()
        }
        
        // 重置按钮
        btnReset.setOnClickListener {
            currentTextColor = defaultTextColor
            currentBorderSize = defaultBorderSize
            currentBorderColor = defaultBorderColor
            currentBackColor = defaultBackColor
            currentBorderStyle = defaultBorderStyle
            
            seekBarBorderSize.progress = defaultBorderSize.toInt()
            tvBorderSizeValue.text = seekBarBorderSize.progress.toString()
            
            engine.setSubtitleTextColor(defaultTextColor)
            engine.setSubtitleBorderSize(defaultBorderSize.toInt())
            engine.setSubtitleBorderColor(defaultBorderColor)
            engine.setSubtitleBackColor(defaultBackColor)
            engine.setSubtitleBorderStyle(defaultBorderStyle)
            
            videoUri?.let { uri ->
                preferencesManager.setSubtitleTextColor(uri.toString(), defaultTextColor)
                preferencesManager.setSubtitleBorderSize(uri.toString(), defaultBorderSize.toInt())
                preferencesManager.setSubtitleBorderColor(uri.toString(), defaultBorderColor)
                preferencesManager.setSubtitleBackColor(uri.toString(), defaultBackColor)
                preferencesManager.setSubtitleBorderStyle(uri.toString(), defaultBorderStyle)
            }
            
            // 重置描边样式单选按钮
            rbOutlineAndShadow.isChecked = true
            
            // 重新设置颜色选择器（重置RGBA滑块和预设按钮）
            setupColorPicker(view, "Text", defaultTextColor, engine, uriString,
                { currentTextColor = it; updateResetButton() },
                { engine.setSubtitleTextColor(it) },
                { preferencesManager.setSubtitleTextColor(uriString, it) })
            setupColorPicker(view, "Border", defaultBorderColor, engine, uriString,
                { currentBorderColor = it; updateResetButton() },
                { engine.setSubtitleBorderColor(it) },
                { preferencesManager.setSubtitleBorderColor(uriString, it) })
            setupColorPicker(view, "Back", defaultBackColor, engine, uriString,
                { currentBackColor = it; updateResetButton() },
                { engine.setSubtitleBackColor(it) },
                { preferencesManager.setSubtitleBackColor(uriString, it) })
            
            updateResetButton()
            DialogUtils.showToastShort(this, "已重置为默认样式")
        }
        
        // 配置对话框
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 获取屏幕宽度，设置对话框宽度（与字幕杂项一致）
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val dialogWidth = (screenWidth * 0.4).toInt().coerceIn(
            (200 * displayMetrics.density).toInt(),
            (450 * displayMetrics.density).toInt()
        )
        
        // 将 dp 转换为 px
        val marginRightDp = 65
        val marginRightPx = (marginRightDp * displayMetrics.density).toInt()
        
        dialog.window?.setLayout(
            dialogWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // 设置对话框位置在右边界中部（与字幕杂项一致）
        dialog.window?.setGravity(android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.RIGHT)
        dialog.window?.attributes?.x = marginRightPx
        dialog.setCanceledOnTouchOutside(true)
        
        dialog.show()
    }
    
    /**
     * 设置颜色选择器（预设色块 + RGBA滑块）
     */
    private fun setupColorPicker(
        view: View, 
        colorType: String,  // "Text", "Border", "Back"
        initialColor: String,
        engine: PlaybackEngine,
        uriString: String,
        onColorChange: (String) -> Unit,
        onEngineUpdate: (String) -> Unit,
        onPreferenceSave: (String) -> Unit
    ) {
        val btnAdvanced = view.findViewById<ImageView>(view.resources.getIdentifier("btn${colorType}ColorAdvanced", "id", packageName))
        val layoutRGBA = view.findViewById<LinearLayout>(view.resources.getIdentifier("layout${colorType}ColorRGBA", "id", packageName))
        
        // 预设颜色按钮
        val presetColors = when (colorType) {
            "Text" -> listOf("White", "Black", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta")
            "Border" -> listOf("White", "Black", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta")
            "Back" -> listOf("Transparent", "Black", "White", "Red", "Green", "Blue", "Yellow", "Gray")
            else -> listOf()
        }
        
        val colorValues = when (colorType) {
            "Text" -> listOf("#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF", "#FF00FF")
            "Border" -> listOf("#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF", "#FF00FF")
            "Back" -> listOf("#00000000", "#FF000000", "#FFFFFFFF", "#80FF0000", "#8000FF00", "#800000FF", "#80FFFF00", "#80808080")
            else -> listOf()
        }
        
        presetColors.forEachIndexed { index, colorName ->
            val btnId = view.resources.getIdentifier("btn${colorType}Color$colorName", "id", packageName)
            view.findViewById<View>(btnId)?.setOnClickListener {
                val color = colorValues[index]
                onColorChange(color)
                onEngineUpdate(color)
                onPreferenceSave(color)
                setupRGBASliders(view, colorType, color, onColorChange, onEngineUpdate, onPreferenceSave)
            }
        }
        
        // 高级编辑按钮（切换RGBA滑块显示）
        btnAdvanced.setOnClickListener {
            if (layoutRGBA.visibility == View.GONE) {
                layoutRGBA.visibility = View.VISIBLE
            } else {
                layoutRGBA.visibility = View.GONE
            }
        }
        
        // 初始化RGBA滑块
        setupRGBASliders(view, colorType, initialColor, onColorChange, onEngineUpdate, onPreferenceSave)
    }
    
    /**
     * 设置RGBA滑块
     */
    private fun setupRGBASliders(
        view: View,
        colorType: String,
        currentColor: String,
        onColorChange: (String) -> Unit,
        onEngineUpdate: (String) -> Unit,
        onPreferenceSave: (String) -> Unit
    ) {
        val color = android.graphics.Color.parseColor(currentColor)
        var r = android.graphics.Color.red(color)
        var g = android.graphics.Color.green(color)
        var b = android.graphics.Color.blue(color)
        var a = android.graphics.Color.alpha(color)
        
        val seekBarR = view.findViewById<SeekBar>(view.resources.getIdentifier("seekBar${colorType}R", "id", packageName))
        val seekBarG = view.findViewById<SeekBar>(view.resources.getIdentifier("seekBar${colorType}G", "id", packageName))
        val seekBarB = view.findViewById<SeekBar>(view.resources.getIdentifier("seekBar${colorType}B", "id", packageName))
        val seekBarA = view.findViewById<SeekBar>(view.resources.getIdentifier("seekBar${colorType}A", "id", packageName))
        
        val tvRValue = view.findViewById<TextView>(view.resources.getIdentifier("tv${colorType}RValue", "id", packageName))
        val tvGValue = view.findViewById<TextView>(view.resources.getIdentifier("tv${colorType}GValue", "id", packageName))
        val tvBValue = view.findViewById<TextView>(view.resources.getIdentifier("tv${colorType}BValue", "id", packageName))
        val tvAValue = view.findViewById<TextView>(view.resources.getIdentifier("tv${colorType}AValue", "id", packageName))
        
        seekBarR?.progress = r
        seekBarG?.progress = g
        seekBarB?.progress = b
        seekBarA?.progress = a
        
        tvRValue?.text = r.toString()
        tvGValue?.text = g.toString()
        tvBValue?.text = b.toString()
        tvAValue?.text = a.toString()
        
        fun updateColor() {
            val newColor = String.format("#%02X%02X%02X%02X", a, r, g, b)
            onColorChange(newColor)
            onEngineUpdate(newColor)
            onPreferenceSave(newColor)
        }
        
        seekBarR?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                r = progress
                tvRValue?.text = progress.toString()
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarG?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                g = progress
                tvGValue?.text = progress.toString()
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarB?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                b = progress
                tvBValue?.text = progress.toString()
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarA?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                a = progress
                tvAValue?.text = progress.toString()
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleDecoder() {
        val items = listOf("硬件解码", "软件解码")
        val currentSelection = if (isHardwareDecoding) 0 else 1
        
        // 获取解码按钮（在顶部，对话框显示在下方）
        val btnDecoder = findViewById<ImageView>(R.id.btnDecoder)
        
        showPopupDialog(btnDecoder, items, currentSelection, showAbove = false) { position ->
            isHardwareDecoding = (position == 0)
            playbackEngine?.setHardwareDecoding(isHardwareDecoding)
            DialogUtils.showToastShort(this@VideoPlayerActivity, "已切换到${if (isHardwareDecoding) "硬件" else "软件"}解码")
            Log.d(TAG, "Decoder switched to: ${if (isHardwareDecoding) "auto" else "no"}")
        }
    }

    private fun showSpeedDialog() {
        val speeds = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "2.5x", "3.0x")
        val speedValues = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0)
        val currentSelection = speedValues.indexOf(currentSpeed)
        
        // 获取倍速按钮（在底部，对话框显示在上方）
        val btnSpeed = findViewById<ImageView>(R.id.btnSpeed)
        
        showPopupDialog(btnSpeed, speeds, currentSelection, showAbove = true, useFixedHeight = true, showScrollHint = true) { position ->
            currentSpeed = speedValues[position]
            playbackEngine?.setSpeed(currentSpeed)
            danmakuManager.setSpeed(currentSpeed.toFloat())
            DialogUtils.showToastShort(this@VideoPlayerActivity, "播放速度：${speeds[position]}")
            Log.d(TAG, "Speed changed to: $currentSpeed")
        }
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
        
        // 清除屏幕常亮标志
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 恢复原始系统设置(音量、亮度)
        gestureHandler?.restoreOriginalSettings()
        
        // 清理进度恢复提示框的handler
        resumePromptHandler.removeCallbacksAndMessages(null)
        
        // 释放弹幕资源
        danmakuManager.release()
        
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
        Log.d(TAG, "Activity resumed")
        
        // 重新应用音量增强设置(如果开启)
        // 这样可以确保从后台返回时，音量设置正确
        gestureHandler?.reapplyVolumeBoostSettings()
        
        // 恢复弹幕播放（只有在弹幕应该显示时才恢复）
        if (danmakuManager.isVisible()) {
            danmakuManager.resume()
            Log.d(TAG, "Danmaku resumed")
        }
    }

    override fun onPause() {
        super.onPause()
        // 暂停弹幕
        danmakuManager.pause()
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
        
        Log.d(TAG, "Activity stopped, isFinishing: $isFinishing")
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
                
                // 禁用点击反馈效果
                itemView.isClickable = true
                itemView.isFocusable = true
                itemView.background = null
                innerLayout.background = null
                
                // 如果 selectedPosition 为 -1，表示所有项都显示为未选中状态（如"更多"菜单）
                if (selectedPosition == -1) {
                    itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                } else {
                    // 正常的选中/未选中状态（如字幕轨道）
                    val isSelected = position == selectedPosition
                    if (isSelected) {
                        itemText.setTextColor(ThemeManager.getThemeColor(this@VideoPlayerActivity, com.google.android.material.R.attr.colorPrimary))
                        itemText.setTypeface(null, android.graphics.Typeface.BOLD)
                    } else {
                        itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                        itemText.setTypeface(null, android.graphics.Typeface.NORMAL)
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
    
    // 弹出式菜单适配器（用于在按钮上方显示的弹出菜单）
    inner class PopupMenuAdapter(
        private val items: List<String>,
        private var selectedPosition: Int = -1,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PopupMenuAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val itemText: TextView = view.findViewById(R.id.itemText)
            val itemLayout: android.widget.LinearLayout = view.findViewById(R.id.itemLayout)
            
            fun bind(position: Int) {
                itemText.text = items[position]
                
                // 禁用点击反馈效果
                itemView.isClickable = true
                itemView.isFocusable = true
                itemView.background = null
                itemLayout.background = null
                
                // 设置选中状态
                val isSelected = position == selectedPosition
                
                // 选中项文字使用主题颜色并加粗，未选中用黑色
                if (isSelected) {
                    itemText.setTypeface(null, android.graphics.Typeface.BOLD)
                    itemText.setTextColor(ThemeManager.getThemeColor(this@VideoPlayerActivity, com.google.android.material.R.attr.colorPrimary))
                } else {
                    itemText.setTypeface(null, android.graphics.Typeface.NORMAL)
                    itemText.setTextColor(android.graphics.Color.parseColor("#333333"))
                }
                
                itemView.setOnClickListener {
                    onItemClick(position)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.dialog_popup_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }
        
        override fun getItemCount() = items.size
    }
    
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
        
        // 添加样式覆盖选项，显示当前状态
        val assOverrideState = videoUri?.let { vUri ->
            preferencesManager.isAssOverrideEnabled(vUri.toString())
        } ?: false
        val assOverrideText = if (assOverrideState) "样式覆盖: 开" else "样式覆盖: 关"
        options.add(assOverrideText)
        
        if (options.isEmpty()) {
            DialogUtils.showToastShort(this, "暂无可用选项")
            return
        }
        
        // 获取更多按钮（在顶部，对话框显示在下方）
        val btnMore = findViewById<ImageView>(R.id.btnMore)
        
        showPopupDialog(btnMore, options, -1, showAbove = false) { position ->
            val selectedOption = options[position]
            when {
                selectedOption == "章节" -> showChapterDialog()
                selectedOption.startsWith("样式覆盖") -> toggleAssOverride()
                selectedOption == "截图" -> takeScreenshot()
            }
        }
    }
    
    /**
     * 切换ASS样式覆盖状态
     */
    private fun toggleAssOverride() {
        // 获取当前状态
        val currentState = videoUri?.let { vUri ->
            preferencesManager.isAssOverrideEnabled(vUri.toString())
        } ?: false
        
        // 切换状态
        val newState = !currentState
        
        // 保存设置
        videoUri?.let { vUri ->
            preferencesManager.setAssOverrideEnabled(vUri.toString(), newState)
        }
        
        // 延迟应用新状态，避免在 MPV 未准备好时设置导致字幕卡住
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            playbackEngine?.setAssOverride(newState)
            Log.d(TAG, "ASS override toggled to: $newState")
        }, 100)
        
        // Toast消息已取消
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
                
                // 格式化章节列表为显示文本
                val chapterItems = chapters.map { (title, time) ->
                    "$title (${formatChapterTime(time)})"
                }
                
                // 获取更多按钮（章节是从更多菜单触发的）
                val btnMore = findViewById<ImageView>(R.id.btnMore)
                
                // 使用弹窗显示章节列表
                showPopupDialog(
                    anchorView = btnMore,
                    items = chapterItems,
                    selectedPosition = -1,
                    showAbove = false,
                    useFixedHeight = true,
                    showScrollHint = true
                ) { position ->
                    // 跳转到选中的章节
                    val chapter = chapters[position]
                    playbackEngine?.seekTo(chapter.second.toInt())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing chapter dialog", e)
                DialogUtils.showToastShort(this, "获取章节失败")
            }
        }
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
    
    // 截图功能 - 直接从MPVView抓取当前画面
    private fun takeScreenshot() {
        try {
            Log.d(TAG, "takeScreenshot() called")
            
            // 从MPVView抓取当前画面
            val bitmap = Bitmap.createBitmap(
                mpvView.width,
                mpvView.height,
                Bitmap.Config.ARGB_8888
            )
            
            // 使用PixelCopy API (Android 7.0+) 从MPVView抓取内容
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val location = IntArray(2)
                mpvView.getLocationInWindow(location)
                
                android.view.PixelCopy.request(
                    mpvView.holder.surface,
                    bitmap,
                    { copyResult ->
                        if (copyResult == android.view.PixelCopy.SUCCESS) {
                            Log.d(TAG, "PixelCopy successful")
                            saveBitmapToFile(bitmap)
                        } else {
                            Log.e(TAG, "PixelCopy failed with result: $copyResult")
                            DialogUtils.showToastShort(this, "截图失败: 无法捕获画面")
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                // Android 7.0以下使用备用方法
                DialogUtils.showToastShort(this, "您的Android版本不支持截图")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot failed", e)
            DialogUtils.showToastShort(this, "截图失败: ${e.message}")
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
                    
                    DialogUtils.showToastShort(this, "已保存")
                    Log.d(TAG, "Screenshot saved to MediaStore: $filename")
                } else {
                    Log.e(TAG, "Failed to create MediaStore URI")
                    DialogUtils.showToastShort(this, "截图保存失败")
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
                
                DialogUtils.showToastShort(this, "已保存")
                Log.d(TAG, "Screenshot saved: ${file.absolutePath}")
            }
            
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap", e)
            DialogUtils.showToastShort(this, "保存截图失败: ${e.message}")
        }
    }
    
    /**
     * 显示弹幕对话框
     */
    private fun showDanmakuDialog() {
        val options = mutableListOf<String>()
        
        // 1. 常驻选项：显示/隐藏弹幕（根据当前状态动态显示）
        // 只有加载了弹幕才显示切换选项，否则显示"未加载弹幕"
        if (danmakuView.isPrepared) {
            val visibilityText = if (danmakuView.isShown) "隐藏弹幕" else "显示弹幕"
            options.add(visibilityText)
        } else {
            options.add("未加载弹幕")
        }
        
        // 2. 常驻选项：本地弹幕
        options.add("本地弹幕")
        
        // 3. 常驻选项：下载弹幕
        options.add("下载弹幕")
        
        // 4. 常驻选项：弹幕样式
        options.add("弹幕样式")
        
        // 获取弹幕按钮
        val btnDanmaku = findViewById<ImageView>(R.id.btnDanmaku)
        
        showPopupDialog(btnDanmaku, options, -1, showAbove = false) { position ->
            val selectedOption = options[position]
            when (selectedOption) {
                "本地弹幕" -> openDanmakuPicker()
                "显示弹幕" -> {
                    danmakuView.show()
                    Log.d(TAG, "Danmaku shown")
                }
                "隐藏弹幕" -> {
                    danmakuView.hide()
                    Log.d(TAG, "Danmaku hidden")
                }
                "未加载弹幕" -> {
                    Toast.makeText(this, "请先加载弹幕文件", Toast.LENGTH_SHORT).show()
                }
                "弹幕样式" -> showDanmakuSettingsDialog()
                "下载弹幕" -> {
                    Toast.makeText(this, "下载弹幕功能开发中...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 显示弹幕设置对话框
     */
    private fun showDanmakuSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_danmaku_settings, null)
        
        // 获取所有控件
        val seekBarSize = dialogView.findViewById<SeekBar>(R.id.seekBarDanmakuSize)
        val tvSizeValue = dialogView.findViewById<TextView>(R.id.tvDanmakuSizeValue)
        val seekBarSpeed = dialogView.findViewById<SeekBar>(R.id.seekBarDanmakuSpeed)
        val tvSpeedValue = dialogView.findViewById<TextView>(R.id.tvDanmakuSpeedValue)
        val seekBarAlpha = dialogView.findViewById<SeekBar>(R.id.seekBarDanmakuAlpha)
        val tvAlphaValue = dialogView.findViewById<TextView>(R.id.tvDanmakuAlphaValue)
        val seekBarStroke = dialogView.findViewById<SeekBar>(R.id.seekBarDanmakuStroke)
        val tvStrokeValue = dialogView.findViewById<TextView>(R.id.tvDanmakuStrokeValue)
        
        // 初始化当前值
        seekBarSize.progress = com.fam4k007.videoplayer.danmaku.DanmakuConfig.size
        tvSizeValue.text = "${com.fam4k007.videoplayer.danmaku.DanmakuConfig.size}%"
        seekBarSpeed.progress = com.fam4k007.videoplayer.danmaku.DanmakuConfig.speed
        tvSpeedValue.text = "${com.fam4k007.videoplayer.danmaku.DanmakuConfig.speed}%"
        seekBarAlpha.progress = com.fam4k007.videoplayer.danmaku.DanmakuConfig.alpha
        tvAlphaValue.text = "${com.fam4k007.videoplayer.danmaku.DanmakuConfig.alpha}%"
        seekBarStroke.progress = com.fam4k007.videoplayer.danmaku.DanmakuConfig.stroke
        tvStrokeValue.text = "${com.fam4k007.videoplayer.danmaku.DanmakuConfig.stroke}%"
        
        // 设置监听器
        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSizeValue.text = "$progress%"
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setSize(progress)
                danmakuManager.updateSize()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSpeedValue.text = "$progress%"
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setSpeed(progress)
                danmakuManager.updateSpeed()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAlphaValue.text = "$progress%"
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setAlpha(progress)
                danmakuManager.updateAlpha()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        seekBarStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvStrokeValue.text = "$progress%"
                com.fam4k007.videoplayer.danmaku.DanmakuConfig.setStroke(progress)
                danmakuManager.updateStroke()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        AlertDialog.Builder(this)
            .setTitle("弹幕设置")
            .setView(dialogView)
            .setPositiveButton("确定", null)
            .show()
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

// 扩展函数：dp转px
fun Int.dpToPx(): Int {
    return (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
}
