package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager

class VideoListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoListActivity"
    }

    private lateinit var rvVideoList: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvFolderTitle: TextView
    private lateinit var tvVideoCount: TextView
    private lateinit var btnBack: View
    private lateinit var btnSort: Button
    private lateinit var etSearch: android.widget.EditText
    private lateinit var btnClearSearch: android.widget.ImageView

    private val videoList = mutableListOf<VideoFileParcelable>()
    private val filteredList = mutableListOf<VideoFileParcelable>()
    private lateinit var adapter: VideoListAdapter
    
    // 排序设置
    private enum class SortType { NAME, DATE }
    private enum class SortOrder { ASCENDING, DESCENDING }
    private var currentSortType = SortType.NAME
    private var currentSortOrder = SortOrder.ASCENDING

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)

        initViews()
        loadVideoList()
        setupRecyclerView()
    }

    private fun initViews() {
        rvVideoList = findViewById(R.id.rvVideoList)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tvFolderTitle = findViewById(R.id.tvFolderTitle)
        tvVideoCount = findViewById(R.id.tvVideoCount)
        btnBack = findViewById(R.id.btnBack)
        btnSort = findViewById(R.id.btnSort)
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)

        btnBack.setOnClickListener { finish() }
        btnSort.setOnClickListener { showSortDialog() }
        
        // 设置下拉刷新动画（无实际刷新，仅动画，延长时间）
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 800)
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.accent,
            R.color.primary
        )
        
        // 搜索框监听
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                filterVideos(query)
            }
        })
        
        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }
    }

    private fun loadVideoList() {
        val folderName = intent.getStringExtra("folder_name") ?: "视频列表"
        val videos = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list") ?: arrayListOf()

        tvFolderTitle.text = folderName
        tvVideoCount.text = "${videos.size} 个视频"
        videoList.addAll(videos)
        filteredList.addAll(videos) // 初始化过滤列表

        Log.d(TAG, "加载文件夹: $folderName, 包含 ${videos.size} 个视频")
    }

    private fun setupRecyclerView() {
        adapter = VideoListAdapter(
            filteredList, // 使用过滤列表
            this,
            onVideoClick = { video, position ->
                openVideoPlayer(video, position)
            },
            onMoreInfoClick = { video ->
                showVideoInfoDialog(video)
            }
        )
        rvVideoList.layoutManager = LinearLayoutManager(this)
        rvVideoList.adapter = adapter
    }

    private fun openVideoPlayer(video: VideoFileParcelable, currentIndex: Int) {
        Log.d(TAG, "播放视频: ${video.name}, 索引: $currentIndex")

        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.data = Uri.parse(video.uri)
        intent.putExtra("video_name", video.name)
        intent.putExtra("current_index", currentIndex)
        intent.putExtra("folderName", tvFolderTitle.text.toString())  // 传递文件夹名称
        intent.putParcelableArrayListExtra("video_list", ArrayList(videoList))
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun showSortDialog() {
        Log.d(TAG, "显示排序对话框")
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_sort_options)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val rgSortType = dialog.findViewById<RadioGroup>(R.id.rgSortType)
        val rgSortOrder = dialog.findViewById<RadioGroup>(R.id.rgSortOrder)
        val rbSortByName = dialog.findViewById<RadioButton>(R.id.rbSortByName)
        val rbSortByDate = dialog.findViewById<RadioButton>(R.id.rbSortByDate)
        val rbAscending = dialog.findViewById<RadioButton>(R.id.rbAscending)
        val rbDescending = dialog.findViewById<RadioButton>(R.id.rbDescending)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        
        // 设置当前选中状�?
        when (currentSortType) {
            SortType.NAME -> rbSortByName.isChecked = true
            SortType.DATE -> rbSortByDate.isChecked = true
        }
        when (currentSortOrder) {
            SortOrder.ASCENDING -> rbAscending.isChecked = true
            SortOrder.DESCENDING -> rbDescending.isChecked = true
        }
        
        btnCancel.setOnClickListener {
            Log.d(TAG, "取消排序")
            dialog.dismiss()
        }
        
        btnConfirm.setOnClickListener {
            Log.d(TAG, "确认排序")
            // 获取选中的排序类�?
            currentSortType = when (rgSortType.checkedRadioButtonId) {
                R.id.rbSortByName -> SortType.NAME
                R.id.rbSortByDate -> SortType.DATE
                else -> SortType.NAME
            }
            
            // 获取选中的排序顺�?
            currentSortOrder = when (rgSortOrder.checkedRadioButtonId) {
                R.id.rbAscending -> SortOrder.ASCENDING
                R.id.rbDescending -> SortOrder.DESCENDING
                else -> SortOrder.ASCENDING
            }
            
            Log.d(TAG, "排序类型: $currentSortType, 排序顺序: $currentSortOrder")
            
            // 执行排序
            sortVideoList()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun sortVideoList() {
        Log.d(TAG, "开始排序，当前列表大小: ${videoList.size}")
        
        val sortedList = when (currentSortType) {
            SortType.NAME -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    videoList.sortedBy { it.name.lowercase() }
                } else {
                    videoList.sortedByDescending { it.name.lowercase() }
                }
            }
            SortType.DATE -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    videoList.sortedBy { it.dateAdded }
                } else {
                    videoList.sortedByDescending { it.dateAdded }
                }
            }
        }
        
        videoList.clear()
        videoList.addAll(sortedList)
        
        // 同步更新过滤列表
        val query = etSearch.text.toString()
        if (query.isEmpty()) {
            filteredList.clear()
            filteredList.addAll(videoList)
        } else {
            filterVideos(query)
        }
        
        adapter.notifyDataSetChanged()
        
        val sortTypeText = if (currentSortType == SortType.NAME) "名称" else "日期"
        val sortOrderText = if (currentSortOrder == SortOrder.ASCENDING) "升序" else "降序"
        Log.d(TAG, "排序完成: $sortTypeText $sortOrderText")
        
        // 显示提示
        DialogUtils.showToastShort(this, "已按$sortTypeText $sortOrderText 排序")
    }
    
    private fun showVideoInfoDialog(video: VideoFileParcelable) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_video_info, null)
        
        val tvFileName = view.findViewById<TextView>(R.id.tvInfoFileName)
        val tvInfoResolution = view.findViewById<TextView>(R.id.tvInfoResolution)
        val tvInfoCodec = view.findViewById<TextView>(R.id.tvInfoCodec)
        val tvInfoBitrate = view.findViewById<TextView>(R.id.tvInfoBitrate)
        val tvInfoFrameRate = view.findViewById<TextView>(R.id.tvInfoFrameRate)
        val btnClose = view.findViewById<Button>(R.id.btnCloseInfo)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressLoadingInfo)
        val infoLayout = view.findViewById<android.widget.LinearLayout>(R.id.layoutVideoInfo)
        
        tvFileName.text = video.name
        
        // 显示加载状态
        progressBar.visibility = View.VISIBLE
        infoLayout.visibility = View.GONE
        
        val alertDialog = dialog.setView(view).create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // 显示对话框
        alertDialog.show()
        
        // 设置对话框宽度（在show之后设置）
        alertDialog.window?.setLayout(
            (300 * resources.displayMetrics.density).toInt(), // 300dp转换为像素
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // 异步加载元数据
        lifecycleScope.launch {
            val metadata = withContext(Dispatchers.IO) {
                com.fam4k007.videoplayer.utils.VideoMetadataHelper.getVideoMetadata(
                    this@VideoListActivity,
                    Uri.parse(video.uri)
                )
            }
            
            progressBar.visibility = View.GONE
            infoLayout.visibility = View.VISIBLE
            
            if (metadata != null) {
                tvInfoResolution.text = metadata.getResolution()
                tvInfoCodec.text = "${metadata.videoCodec} / ${metadata.audioCodec}"
                tvInfoBitrate.text = metadata.getFormattedBitrate()
                tvInfoFrameRate.text = metadata.getFormattedFrameRate()
            } else {
                tvInfoResolution.text = "无法获取"
                tvInfoCodec.text = "无法获取"
                tvInfoBitrate.text = "无法获取"
                tvInfoFrameRate.text = "无法获取"
            }
        }
        
        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
    
    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    private fun filterVideos(query: String) {
        filteredList.clear()
        
        if (query.isEmpty()) {
            filteredList.addAll(videoList)
        } else {
            val lowerQuery = query.lowercase()
            filteredList.addAll(videoList.filter { video ->
                video.name.lowercase().contains(lowerQuery)
            })
        }
        
        // 更新显示的视频数量
        tvVideoCount.text = if (query.isEmpty()) {
            "${videoList.size} 个视频"
        } else {
            "${filteredList.size} / ${videoList.size} 个视频"
        }
        
        adapter.notifyDataSetChanged()
        Log.d(TAG, "搜索: $query, 找到 ${filteredList.size} 个结果")
    }
    
    private fun refreshVideoList() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 重新扫描视频文件
                    val folderPath = videoList.firstOrNull()?.path?.substringBeforeLast("/")
                    if (folderPath != null) {
                        val projection = arrayOf(
                            android.provider.MediaStore.Video.Media._ID,
                            android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                            android.provider.MediaStore.Video.Media.DATA,
                            android.provider.MediaStore.Video.Media.SIZE,
                            android.provider.MediaStore.Video.Media.DURATION,
                            android.provider.MediaStore.Video.Media.DATE_ADDED
                        )
                        
                        val selection = "${android.provider.MediaStore.Video.Media.DATA} LIKE ?"
                        val selectionArgs = arrayOf("$folderPath/%")
                        val sortOrder = "${android.provider.MediaStore.Video.Media.DATE_ADDED} DESC"
                        
                        val cursor = contentResolver.query(
                            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder
                        )
                        
                        val updatedList = mutableListOf<VideoFileParcelable>()
                        cursor?.use {
                            val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
                            val nameColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                            val pathColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                            val sizeColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
                            val durationColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)
                            val dateColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_ADDED)
                            
                            while (it.moveToNext()) {
                                val id = it.getLong(idColumn)
                                val name = it.getString(nameColumn)
                                val path = it.getString(pathColumn)
                                val size = it.getLong(sizeColumn)
                                val duration = it.getLong(durationColumn)
                                val dateAdded = it.getLong(dateColumn)
                                
                                val uri = Uri.withAppendedPath(
                                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    id.toString()
                                )
                                
                                updatedList.add(VideoFileParcelable(
                                    uri.toString(), name, path, size, duration / 1000, dateAdded
                                ))
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            videoList.clear()
                            videoList.addAll(updatedList)
                            
                            // 重新应用当前排序
                            sortVideoList()
                            
                            tvVideoCount.text = "${videoList.size} 个视频"
                            swipeRefreshLayout.setRefreshing(false)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            swipeRefreshLayout.setRefreshing(false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新视频列表失败", e)
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.setRefreshing(false)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        return true
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
