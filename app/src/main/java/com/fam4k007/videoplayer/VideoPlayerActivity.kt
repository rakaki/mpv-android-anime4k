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
import androidx.lifecycle.lifecycleScope
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
import com.fam4k007.videoplayer.utils.UriUtils.getFolderName
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.utils.getThemeAttrColor
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

/**
 * 视频播放器 Activity (重构版)
 * 使用管理器模式进行职责分离，防止内存泄漏
 */
class VideoPlayerActivity : AppCompatActivity(),
    com.fam4k007.videoplayer.player.SubtitleDialogCallback,
    com.fam4k007.videoplayer.player.DanmakuDialogCallback,
    com.fam4k007.videoplayer.player.MoreOptionsCallback,
    com.fam4k007.videoplayer.player.VideoUriProvider {

    companion object {
        private const val TAG = "VideoPlayerActivity"
        private const val SEEK_DEBUG = "SEEK_DEBUG"  // 快进调试专用日志标签
    }

    private lateinit var playbackEngine: PlaybackEngine
    private lateinit var controlsManager: PlayerControlsManager
    private lateinit var gestureHandler: GestureHandler
    private lateinit var seriesManager: SeriesManager
    private lateinit var anime4KManager: Anime4KManager
    private lateinit var danmakuManager: com.fam4k007.videoplayer.danmaku.DanmakuManager
    private lateinit var dialogManager: com.fam4k007.videoplayer.player.PlayerDialogManager
    private lateinit var filePickerManager: com.fam4k007.videoplayer.player.FilePickerManager
    private lateinit var composeOverlayManager: com.fanchen.fam4k007.manager.compose.ComposeOverlayManager
    private lateinit var screenshotManager: com.fam4k007.videoplayer.manager.ScreenshotManager
    private lateinit var skipIntroOutroManager: com.fanchen.fam4k007.manager.SkipIntroOutroManager

    private lateinit var mpvView: CustomMPVView
    private lateinit var danmakuView: com.fam4k007.videoplayer.danmaku.DanmakuPlayerView
    private lateinit var clickArea: View
    
    private var resumeProgressPrompt: LinearLayout? = null
    private var btnResumePromptConfirm: TextView? = null
    private var btnResumePromptClose: TextView? = null
    private val resumePromptHandler = Handler(Looper.getMainLooper())

    private var videoUri: Uri? = null
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var historyManager: PlaybackHistoryManager
    private val subtitleManager = SubtitleManager()
    private var savedPosition = 0.0
    private var hasRestoredPosition = false
    private var hasShownPrompt = false
    private var lastPlaybackPosition = 0L  // 从列表传入的播放位置
    
    private var currentPosition = 0.0
    private var duration = 0.0
    private var isPlaying = false
    private var currentSpeed = 1.0
    private var isHardwareDecoding = true
    
    private var seekTimeSeconds = 5
    
    private var currentSeries: List<Uri> = emptyList()
    private var currentVideoIndex = -1

    private var anime4KDialog: android.app.Dialog? = null
    private var anime4KEnabled = false
    private var anime4KMode = Anime4KManager.Mode.OFF
    private var anime4KQuality = Anime4KManager.Quality.BALANCED
    
    // 当前视频所在文件夹路径
    private var currentFolderPath: String? = null
    
    // 是否为在线视频
    private var isOnlineVideo = false
    
    private lateinit var seekHint: TextView
    private lateinit var speedHint: LinearLayout
    private lateinit var speedHintText: TextView
    
    private lateinit var subtitlePickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    private lateinit var danmakuPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    
    private var wasPlayingBeforeSubtitlePicker = false
    
    private var wasPlayingBeforeDanmakuPicker = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_video_player)
        
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "Screen keep-on enabled")

        preferencesManager = PreferencesManager.getInstance(this)
        
        historyManager = PlaybackHistoryManager(this)
        
        loadUserSettings()

        // 处理视频URI - 支持本地文件和在线URL
        try {
            videoUri = when {
                intent.action == android.content.Intent.ACTION_VIEW -> {
                    Log.d(TAG, "ACTION_VIEW intent")
                    intent.data
                }
                intent.action == android.content.Intent.ACTION_SEND -> {
                    Log.d(TAG, "ACTION_SEND intent")
                    if (intent.type?.startsWith("video/") == true || intent.type?.startsWith("audio/") == true) {
                        intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                    } else {
                        intent.data
                    }
                }
                // 支持从MainActivity传递的URL
                intent.hasExtra("uri") -> {
                    Log.d(TAG, "Has 'uri' extra")
                    val uriString = intent.getStringExtra("uri")
                    Log.d(TAG, "URI string from extra: $uriString")
                    if (uriString != null) {
                        Uri.parse(uriString)
                    } else {
                        intent.data
                    }
                }
                else -> {
                    Log.d(TAG, "Using intent.data")
                    intent.data
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing video URI", e)
            DialogUtils.showToastShort(this, "解析视频地址失败: ${e.message}")
            finish()
            return
        }
        
        if (videoUri == null) {
            Log.e(TAG, "Video URI is null")
            DialogUtils.showToastShort(this, "无效的视频路径")
            finish()
            return
        }

        Log.d(TAG, "Video URI: $videoUri")
        Log.d(TAG, "URI scheme: ${videoUri?.scheme}")
        
        // 判断是否为在线视频
        isOnlineVideo = intent.getBooleanExtra("is_online", false) ||
            videoUri?.scheme?.let { it == "http" || it == "https" } == true
        
        Log.d(TAG, "Is online video: $isOnlineVideo")
                if (isOnlineVideo) {
            Log.d(TAG, "Playing online video")
            // 在线视频不需要获取文件夹路径
            currentFolderPath = null
        } else {
            // 获取当前视频所在文件夹路径
            videoUri?.let { uri ->
                currentFolderPath = uri.getFolderName()
                Log.d(TAG, "Folder path: $currentFolderPath")
            }
        }

        savedPosition = preferencesManager.getPlaybackPosition(videoUri.toString())
        Log.d(TAG, "Saved position: $savedPosition seconds")

        mpvView = findViewById(R.id.surfaceView)
        danmakuView = findViewById(R.id.danmakuView)
        clickArea = findViewById(R.id.clickArea)
        
        Log.d(TAG, "Initializing MPV in Activity...")
        try {
            // 总是调用 initialize，CustomMPVView 内部会处理重复初始化的保护
            mpvView.initialize(filesDir.path, cacheDir.path)
            Log.d(TAG, "MPV View initialized")
            
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
        
        resumeProgressPrompt = findViewById(R.id.resumeProgressPrompt)
        btnResumePromptConfirm = findViewById(R.id.btnResumePromptConfirm)
        btnResumePromptClose = findViewById(R.id.btnResumePromptClose)
        
        btnResumePromptConfirm?.setOnClickListener {
            onResumePromptConfirm()
        }
        
        btnResumePromptClose?.setOnClickListener {
            hideResumeProgressPrompt()
        }
        
        seekHint = findViewById(R.id.seekHint)
        speedHint = findViewById(R.id.speedHint)
        speedHintText = findViewById(R.id.speedHintText)
        
        danmakuManager = com.fam4k007.videoplayer.danmaku.DanmakuManager(this, danmakuView)
        danmakuManager.initialize()
        
        initializeManagers()
        
        handleVideoListIntent()
        
    }

    /**
     * 初始化所有管理器
     */
    private fun initializeManagers() {
        playbackEngine = PlaybackEngine(
            mpvView,
            WeakReference(this),
            object : PlaybackEngine.PlaybackEventCallback {
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    this@VideoPlayerActivity.isPlaying = isPlaying
                    controlsManager?.updatePlayPauseButton(isPlaying)
                    
                    if (isPlaying) {
                        // 只调用 resume，不要调用 start
                        // start() 会清空弹幕状态，只应该在首次加载时调用
                        danmakuManager.resume()
                    } else {
                        danmakuManager.pause()
                    }
                }
                
                override fun onProgressUpdate(position: Double, duration: Double) {
                    this@VideoPlayerActivity.currentPosition = position
                    this@VideoPlayerActivity.duration = duration
                    controlsManager?.updateProgress(position, duration)
                    
                    // 处理片头片尾跳过
                    skipIntroOutroManager.handleSkipIntroOutro(
                        folderPath = currentFolderPath,
                        position = position,
                        duration = duration,
                        getChapters = { playbackEngine.getChapters() },
                        seekTo = { playbackEngine.seekTo(it) },
                        onOutroReached = {
                            // 使用seriesManager判断是否有下一集
                            val hadNext = seriesManager.hasNext
                            if (hadNext) {
                                playNextVideo()
                            }
                            hadNext
                        }
                    )
                }
                
                override fun onFileLoaded() {
                    isPlaying = true
                    controlsManager?.updatePlayPauseButton(true)
                    
                    // 重置片头片尾跳过标记
                    skipIntroOutroManager.resetFlags()
                    
                    // 延迟标记视频准备好，确保视频真正开始播放
                    Handler(Looper.getMainLooper()).postDelayed({
                        skipIntroOutroManager.markVideoReady()
                        Log.d(TAG, "Video marked as ready for skip detection")
                    }, 500)  // 延迟500ms
                    
                    // 不在这里启动弹幕，弹幕的启动由 onPlaybackStateChanged 统一管理
                    Log.d(TAG, "Video file loaded")
                }
                
                override fun onEndOfFile() {
                    videoUri?.let { uri ->
                        preferencesManager.clearPlaybackPosition(uri.toString())
                        Log.d(TAG, "Video ended, position reset to 0")
                    }
                    
                    Log.d(TAG, "Video playback ended, auto-play disabled")
                }
                
                override fun onError(message: String) {
                    DialogUtils.showToastLong(this@VideoPlayerActivity, message)
                }
                
                override fun onSurfaceReady() {
                    Log.d(TAG, "Surface ready callback received")
                }
            }
        )
        
        if (!playbackEngine.initialize()) {
            DialogUtils.showToastLong(this, "播放器初始化失败")
            finish()
            return
        }
        
        gestureHandler = GestureHandler(
            WeakReference(this),
            WeakReference(window),
            object : GestureHandler.GestureCallback {
                override fun onGestureStart() {
                }
                
                override fun onGestureEnd() {
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
                    if (currentSpeed != 1.0) {
                        currentSpeed = 1.0
                        playbackEngine?.setSpeed(1.0)
                        danmakuManager.setSpeed(1.0f)
                    }
                }
                
                override fun onSingleTap() {
                    controlsManager?.toggleControls()
                }
                
                override fun onDoubleTap() {
                    playbackEngine?.togglePlayPause()
                }
                
                override fun onLongPress() {
                    if (currentSpeed == 1.0) {
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
                    if (duration > 0) {
                        val newPos = (currentPosition + seekSeconds).coerceIn(0.0, duration).toInt()
                        val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                        playbackEngine?.seekTo(newPos, usePrecise)
                        danmakuManager.seekTo((newPos * 1000).toLong())
                        
                        val currentTime = FormatUtils.formatProgressTime(newPos.toDouble())
                        val sign = if (seekSeconds >= 0) "+" else ""
                        val seekTime = FormatUtils.formatProgressTime(seekSeconds.toDouble())
                        seekHint.text = "$currentTime\n[$sign$seekTime]"
                        
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
                    val newPos = (currentPosition - seekTimeSeconds).coerceAtLeast(0.0)
                    danmakuManager.seekTo((newPos * 1000).toLong())
                }
                
                override fun onForwardClick() {
                    Log.d(SEEK_DEBUG, "onForwardClick: seekTimeSeconds = $seekTimeSeconds, currentPosition = $currentPosition, seekBy = $seekTimeSeconds")
                    playbackEngine.seekBy(seekTimeSeconds)
                    val newPos = (currentPosition + seekTimeSeconds).coerceAtMost(duration)
                    danmakuManager.seekTo((newPos * 1000).toLong())
                }
                
                override fun onSubtitleClick() {
                    dialogManager.showSubtitleDialog()
                }
                
                override fun onAudioTrackClick() {
                    dialogManager.showAudioTrackDialog()
                }
                
                override fun onDecoderClick() {
                    dialogManager.showDecoderDialog()
                }
                
                override fun onAnime4KClick() {
                    dialogManager.showAnime4KModeDialog(anime4KMode)
                }
                
                override fun onMoreClick() {
                    dialogManager.showMoreOptionsDialog()
                }
                
                override fun onSpeedClick() {
                    dialogManager.showSpeedDialog(currentSpeed)
                }
                
                override fun onSeekBarChange(position: Double) {
                    val usePrecise = gestureHandler.isPreciseSeekingEnabled()
                    playbackEngine.seekTo(position.toInt(), usePrecise)
                    danmakuManager.seekTo((position * 1000).toLong())
                }
                
                override fun onBackClick() {
                    gestureHandler?.restoreOriginalSettings()
                    finish()
                    // 添加返回动画：播放器向右滑出，列表从左滑入
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            },
            WeakReference(gestureHandler)  // 传入GestureHandler引用
        )
        
        seriesManager = SeriesManager()
        
        // 只在本地视频时处理系列
        if (!isOnlineVideo) {
            val videoListParcelable = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list")
            
            if (videoListParcelable != null && videoListParcelable.isNotEmpty()) {
                val uriList = videoListParcelable.map { Uri.parse(it.uri) }
                videoUri?.let { uri ->
                    seriesManager.setVideoList(uriList, uri)
                    Log.d(TAG, "Video list from intent: ${uriList.size} videos, currentIndex: ${seriesManager.currentIndex}")
                }
            } else {
                videoUri?.let { uri ->
                    seriesManager.identifySeries(this, uri) { videoUri ->
                        getFileNameFromUri(videoUri)
                    }
                }
            }
            
            Log.d(TAG, "Series list size: ${seriesManager.getVideoList().size}, currentIndex: ${seriesManager.currentIndex}")
            Log.d(TAG, "hasPrevious: ${seriesManager.hasPrevious}, hasNext: ${seriesManager.hasNext}")
        } else {
            Log.d(TAG, "Online video - skipping series detection")
        }
        
        anime4KManager = Anime4KManager(this)
        if (anime4KManager.initialize()) {
            Log.d(TAG, "Anime4K initialized successfully")
        } else {
            Log.w(TAG, "Anime4K initialization failed")
        }
        
        // 初始化Compose管理器（必须在dialogManager之前）
        composeOverlayManager = com.fanchen.fam4k007.manager.compose.ComposeOverlayManager(
            context = this,
            lifecycleOwner = this,
            rootView = findViewById(android.R.id.content)
        )
        
        dialogManager = com.fam4k007.videoplayer.player.PlayerDialogManager(
            WeakReference(this),
            playbackEngine,
            danmakuManager,
            anime4KManager,
            preferencesManager,
            composeOverlayManager,
            WeakReference(controlsManager)
        )
        dialogManager.setCallback(object : com.fam4k007.videoplayer.player.PlayerDialogManager.DialogCallback {
            override fun onSpeedChanged(speed: Double) {
                currentSpeed = speed
                playbackEngine.setSpeed(speed)
                danmakuManager.setSpeed(speed.toFloat())
            }
            
            override fun onAnime4KChanged(enabled: Boolean, mode: Anime4KManager.Mode, quality: Anime4KManager.Quality) {
                anime4KEnabled = enabled
                anime4KMode = mode
                anime4KQuality = quality
                applyAnime4K()
            }
        })
        
        // 初始化文件选择器管理器
        filePickerManager = com.fam4k007.videoplayer.player.FilePickerManager(
            WeakReference(this),
            subtitleManager,
            danmakuManager,
            historyManager,
            WeakReference(playbackEngine),
            preferencesManager
        )
        filePickerManager.initialize()
        
        // 初始化截图管理器
        screenshotManager = com.fam4k007.videoplayer.manager.ScreenshotManager(this)
        
        // 初始化片头片尾管理器
        skipIntroOutroManager = com.fanchen.fam4k007.manager.SkipIntroOutroManager(
            this,
            preferencesManager,
            composeOverlayManager
        )
        
        bindViewsToManagers()
    }
    
    /**
     * 绑定所有View到对应的管理器
     */
    private fun bindViewsToManagers() {
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
        
        val btnDanmaku = findViewById<ImageView>(R.id.btnDanmaku)
        btnDanmaku.setOnClickListener {
            dialogManager.showDanmakuDialog()
        }
        
        controlsManager.initialize()
        
        videoUri?.let { uri ->
            val fileName = getFileNameFromUri(uri)
            controlsManager.setFileName(fileName)
        }
        
        gestureHandler.initialize()
        
        gestureHandler.bindIndicatorViews(
            brightnessIndicator = findViewById(R.id.brightnessIndicator),
            volumeIndicator = findViewById(R.id.volumeIndicator),
            brightnessBar = findViewById(R.id.brightnessBar),
            volumeBar = findViewById(R.id.volumeBar),
            brightnessText = findViewById(R.id.brightnessText),
            volumeText = findViewById(R.id.volumeText)
        )
        
        clickArea.setOnTouchListener { _, event ->
            gestureHandler.onTouchEvent(event)
        }
        
        // 设置当前视频 URI 给文件选择器管理器
        videoUri?.let { uri ->
            filePickerManager.setCurrentVideoUri(uri)
        }
        
        updateEpisodeButtons()
    }
    
    /**
     * 加载视频
     */
    private fun loadVideo() {
        videoUri?.let { uri ->
            val position = if (duration > 0 && duration < 30) {
                Log.d(TAG, "Short video detected, starting from 0")
                0.0
            } else {
                savedPosition
            }
            
            if (position > 5.0) {
                Log.d(TAG, "Saved position detected: $position seconds - showing resume prompt")
                showResumeProgressPrompt()
            }
            
            // 对于在线视频,直接使用URI字符串;对于本地文件,使用URI对象
            if (isOnlineVideo) {
                // 在线视频:直接使用原始URL字符串
                val urlString = uri.toString()
                Log.d(TAG, "Loading online video with URL string: $urlString")
                
                // B站视频需要设置Referer头(防盗链)
                if (urlString.contains("bilivideo.com")) {
                    Log.d(TAG, "Detected Bilibili video, setting Referer header")
                    `is`.xyz.mpv.MPVLib.setOptionString(
                        "http-header-fields",
                        "Referer: https://www.bilibili.com"
                    )
                }
                
                playbackEngine?.loadVideoFromUrl(urlString, position)
            } else {
                // 本地视频:使用URI对象
                playbackEngine?.loadVideo(uri, position)
            }
            
            loadDanmakuForVideo(uri)
            
            // 只在position > 0时同步弹幕(延迟更长,等待在线视频加载)
            if (position > 0) {
                lifecycleScope.launch {
                    // 在线视频需要更长延迟
                    val delayTime = if (isOnlineVideo) 3000L else 800L
                    delay(delayTime)
                    danmakuManager.seekTo((position * 1000).toLong())
                    Log.d(TAG, "Synced danmaku to position: $position seconds")
                }
            }
            
            // 使用协程延迟恢复字幕设置
            lifecycleScope.launch {
                delay(500)
                restoreSubtitlePreferences(uri)
            }
        }
    }
    
    /**
     * 加载视频对应的弹幕文件
     * 参考 DanDanPlay 的弹幕加载逻辑：优先使用历史记录，其次自动查找
     */
    private fun loadDanmakuForVideo(videoUri: android.net.Uri) {
        try {
            Log.d(TAG, "Loading danmaku for video: $videoUri")
            
            val history = historyManager.getHistoryForUri(videoUri)
            
            if (history?.danmuPath != null && File(history.danmuPath).exists()) {
                Log.d(TAG, "Restoring danmaku from history: ${history.danmuPath}")
                // 恢复用户上次的弹幕可见性设置
                val autoShow = history.danmuVisible
                val loaded = danmakuManager.loadDanmakuFile(
                    history.danmuPath,
                    autoShow = autoShow
                )
                
                if (loaded) {
                    Log.d(TAG, "Danmaku restored successfully, autoShow=$autoShow")
                    Log.d(TAG, "Current danmaku visibility: ${danmakuManager.isVisible()}")
                    
                    // 如果视频正在播放，启动弹幕
                    if (isPlaying) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            danmakuManager.resume()
                            Log.d(TAG, "Danmaku resumed after restore, isPlaying=$isPlaying")
                        }, 300)
                    } else {
                        Log.d(TAG, "Video not playing yet, danmaku will start when video plays")
                    }
                } else {
                    Log.w(TAG, "Failed to restore danmaku, trying auto-find")
                    autoFindAndLoadDanmaku(videoUri)
                }
            } else {
                Log.d(TAG, "No danmaku history found, auto-finding...")
                autoFindAndLoadDanmaku(videoUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading danmaku", e)
        }
    }
    
    /**
     * 自动查找并加载同名弹幕文件
     */
    private fun autoFindAndLoadDanmaku(videoUri: android.net.Uri) {
        try {
            val videoPath = videoUri.resolveUri(this)
            if (videoPath != null) {
                Log.d(TAG, "Auto-finding danmaku for: $videoPath")
                danmakuManager.loadDanmakuForVideo(videoUri.toString(), videoPath)
                // prepared回调会自动处理弹幕启动，这里不需要手动启动
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-finding danmaku", e)
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
                val assOverride = preferencesManager.isAssOverrideEnabled(uriString)
                if (assOverride) {
                    lifecycleScope.launch {
                        delay(300)
                        engine.setAssOverride(assOverride)
                        Log.d(TAG, "Restored ASS override: $assOverride")
                    }
                }
                
                val savedScale = preferencesManager.getSubtitleScale(uriString)
                if (savedScale != 1.0) {
                    engine.setSubtitleScale(savedScale)
                    Log.d(TAG, "Restored subtitle scale: $savedScale")
                }
                
                val savedPosition = preferencesManager.getSubtitlePosition(uriString)
                if (savedPosition != 100) {
                    engine.setSubtitleVerticalPosition(savedPosition)
                    Log.d(TAG, "Restored subtitle position: $savedPosition")
                }
                
                val savedDelay = preferencesManager.getSubtitleDelay(uriString)
                if (savedDelay != 0.0) {
                    engine.setSubtitleDelay(savedDelay)
                    Log.d(TAG, "Restored subtitle delay: $savedDelay")
                }
                
                val savedSubtitlePath = preferencesManager.getExternalSubtitle(uriString)
                if (savedSubtitlePath != null) {
                    if (File(savedSubtitlePath).exists()) {
                        try {
                            MPVLib.command("sub-add", savedSubtitlePath, "select")
                            Log.d(TAG, "Restored external subtitle from path: $savedSubtitlePath")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to restore external subtitle", e)
                        }
                    } else {
                        Log.w(TAG, "Saved subtitle file not found: $savedSubtitlePath")
                    }
                }
                
                val savedTrackId = preferencesManager.getSubtitleTrackId(uriString)
                if (savedTrackId != -1) {
                    lifecycleScope.launch {
                        delay(600)
                        try {
                            engine.selectSubtitleTrack(savedTrackId)
                            Log.d(TAG, "Restored subtitle track: $savedTrackId")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to restore subtitle track", e)
                        }
                    }
                }
                
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
                
                // 总是应用描边样式，即使是默认值，确保MPV状态正确
                val savedBorderStyle = preferencesManager.getSubtitleBorderStyle(uriString)
                engine.setSubtitleBorderStyle(savedBorderStyle)
                Log.d(TAG, "Restored subtitle border style: $savedBorderStyle")
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
        
        resumePromptHandler.postDelayed({
            hideResumeProgressPrompt()
        }, 5000)
    }
    
    /**
     * 加载用户设置
     */
    private fun loadUserSettings() {
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
        
        playbackEngine?.seekTo(0)
        
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
     * 更新上一集下一集按钮状态
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
        
        val fileName = getFileNameFromUri(uri)
        controlsManager?.setFileName(fileName)
        
        val position = preferencesManager.getPlaybackPosition(uri.toString())
        
        playbackEngine?.loadVideo(uri, position)
        
        updateEpisodeButtons()
    }
    
    override fun onBackPressed() {
        gestureHandler?.restoreOriginalSettings()
        super.onBackPressed()
        // 添加返回动画
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    override fun onPause() {
        super.onPause()
        savePlaybackState()
    }
    
    override fun onStop() {
        super.onStop()
        savePlaybackState()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
        
        savePlaybackState()
        
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 检查gestureHandler是否已初始化
        if (::gestureHandler.isInitialized) {
            gestureHandler.restoreOriginalSettings()
        }
        
        resumePromptHandler.removeCallbacksAndMessages(null)
        
        // 释放弹幕资源
        if (::danmakuManager.isInitialized) {
            danmakuManager.release()
        }
        
        // 清理Dialog（防止内存泄漏）
        anime4KDialog?.dismiss()
        anime4KDialog = null
        
        // 清理对话框管理器
        if (::dialogManager.isInitialized) {
            dialogManager.cleanup()
        }
        
        // 销毁播放引擎（会自动移除MPVLib观察者）
        playbackEngine?.destroy()
        controlsManager?.cleanup()
        gestureHandler?.cleanup()
        filePickerManager?.cleanup()
    }
    
    private fun savePlaybackState() {
        val uri = videoUri ?: return
        
        try {
            // 1. 保存播放进度到 PreferencesManager
            if (duration > 0 && currentPosition > 0) {
                preferencesManager.setPlaybackPosition(uri.toString(), currentPosition)
                Log.d(TAG, "Playback position saved: $currentPosition / $duration")
            }
            
            // 2. 添加到历史记录
            if (duration > 0) {
                val fileName = getFileNameFromUri(uri)
                val folderName = uri.getFolderName()
                
                historyManager.addHistory(
                    uri = uri,
                    fileName = fileName,
                    position = (currentPosition * 1000).toLong(),
                    duration = (duration * 1000).toLong(),
                    folderName = folderName
                )
                Log.d(TAG, "History saved: $fileName")
                
                // 3. 保存弹幕信息到历史记录
                val danmakuPath = danmakuManager.getCurrentDanmakuPath()
                if (danmakuPath != null) {
                    historyManager.updateDanmu(
                        uri = uri,
                        danmuPath = danmakuPath,
                        danmuVisible = danmakuManager.isVisible(),
                        danmuOffsetTime = 0L
                    )
                    Log.d(TAG, "Danmu info saved: path=$danmakuPath, visible=${danmakuManager.isVisible()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save playback state", e)
        }
    }
    
    
    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else uri.lastPathSegment ?: "未知文件"
            } else {
                uri.lastPathSegment ?: "未知文件"
            }
        } ?: uri.lastPathSegment ?: "未知文件"
    }
    
    private fun applyAnime4K() {
        if (anime4KEnabled) {
            val shaderString = anime4KManager.getShaderChain(anime4KMode, anime4KQuality)
            val shaders = if (shaderString.isNotEmpty()) {
                shaderString.split(":")
            } else {
                emptyList()
            }
            playbackEngine.setShaderList(shaders)
            Log.d(TAG, "Anime4K applied: mode=$anime4KMode, quality=$anime4KQuality")
        } else {
            playbackEngine.setShaderList(emptyList())
            Log.d(TAG, "Anime4K disabled")
        }
    }
    
    override fun onImportSubtitle() {
        filePickerManager.importSubtitle(isPlaying)
    }
    
    override fun onImportDanmaku() {
        filePickerManager.importDanmaku(isPlaying)
    }
    
    override fun onDanmakuVisibilityChanged(visible: Boolean) {
        // 更新历史记录中的弹幕可见性状态
        videoUri?.let { uri ->
            historyManager.updateDanmu(
                uri = uri,
                danmuPath = danmakuManager.getCurrentDanmakuPath(),
                danmuVisible = visible,
                danmuOffsetTime = 0L
            )
            Log.d(TAG, "Danmaku visibility updated in history: $visible")
        }
    }
    
    override fun onScreenshot() {
        screenshotManager.takeScreenshot()
    }
    
    override fun onShowSkipSettings() {
        skipIntroOutroManager.showSkipSettingsDrawer(currentFolderPath)
    }
    
    override fun getVideoUri(): Uri? = videoUri
}

fun Int.dpToPx(): Int {
    return (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
}
