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
import com.fam4k007.videoplayer.databinding.ActivityVideoListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.ThemeManager

class VideoListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoListActivity"
    }

    private lateinit var binding: ActivityVideoListBinding

    private val videoList = mutableListOf<VideoFileParcelable>()
    private val filteredList = mutableListOf<VideoFileParcelable>()
    private lateinit var adapter: VideoListAdapter
    
    // 排序设置
    private enum class SortType { NAME, DATE }
    private enum class SortOrder { ASCENDING, DESCENDING }
    private var currentSortType = SortType.NAME
    private var currentSortOrder = SortOrder.ASCENDING
    
    // 搜索状态
    private var isSearchMode = false
    
    // PreferencesManager
    private lateinit var preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 PreferencesManager
        preferencesManager = com.fam4k007.videoplayer.manager.PreferencesManager.getInstance(this)
        
        // 读取保存的排序设置
        loadSortSettings()
        
        initViews()
        loadVideoList()
        setupRecyclerView()
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener { handleBackPressed() }
        binding.btnSort.setOnClickListener { showSortDialog() }
        binding.btnSearch.setOnClickListener { showSearchOverlay() }
        
        // 设置下拉刷新动画（无实际刷新，仅动画，延长时间）
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 800)
        }
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.accent,
            R.color.primary
        )
        
        // 搜索框监听
        binding.etSearchOverlay.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                binding.btnClearSearchOverlay.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                filterVideos(query)
            }
        })
        
        // 搜索框焦点监听 - 检测是否处于输入状态
        binding.etSearchOverlay.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etSearchOverlay.text.isEmpty()) {
                // 失去焦点且内容为空时，自动隐藏输入法
                hideKeyboard()
            }
        }
        
        binding.btnClearSearchOverlay.setOnClickListener {
            binding.etSearchOverlay.text.clear()
        }
    }
    
    private fun showSearchOverlay() {
        isSearchMode = true
        binding.searchOverlay.visibility = View.VISIBLE
        binding.etSearchOverlay.requestFocus()
        
        // 显示输入法
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etSearchOverlay, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun hideSearchOverlay() {
        isSearchMode = false
        binding.searchOverlay.visibility = View.GONE
        binding.etSearchOverlay.text.clear()
        binding.etSearchOverlay.clearFocus()
        hideKeyboard()
        
        // 恢复显示所有视频
        filterVideos("")
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearchOverlay.windowToken, 0)
    }
    
    private fun handleBackPressed() {
        if (isSearchMode) {
            hideSearchOverlay()
        } else {
            finish()
        }
    }
    
    private fun loadSortSettings() {
        val sortType = preferencesManager.getVideoSortType()
        val sortOrder = preferencesManager.getVideoSortOrder()
        
        currentSortType = when (sortType) {
            "DATE" -> SortType.DATE
            else -> SortType.NAME
        }
        
        currentSortOrder = when (sortOrder) {
            "DESCENDING" -> SortOrder.DESCENDING
            else -> SortOrder.ASCENDING
        }
        
        Log.d(TAG, "加载排序设置: $currentSortType $currentSortOrder")
    }
    
    private fun saveSortSettings() {
        val sortTypeStr = when (currentSortType) {
            SortType.NAME -> "NAME"
            SortType.DATE -> "DATE"
        }
        
        val sortOrderStr = when (currentSortOrder) {
            SortOrder.ASCENDING -> "ASCENDING"
            SortOrder.DESCENDING -> "DESCENDING"
        }
        
        preferencesManager.setVideoSortType(sortTypeStr)
        preferencesManager.setVideoSortOrder(sortOrderStr)
        
        Log.d(TAG, "保存排序设置: $sortTypeStr $sortOrderStr")
    }

    private fun loadVideoList() {
        val folderName = intent.getStringExtra("folder_name") ?: "视频列表"
        val videos = intent.getParcelableArrayListExtra<VideoFileParcelable>("video_list") ?: arrayListOf()

        binding.tvFolderTitle.text = folderName
        videoList.addAll(videos)
        
        Log.d(TAG, "加载文件夹: $folderName, 包含 ${videos.size} 个视频")
        
        // 应用保存的排序设置（在添加到videoList之后，在初始化filteredList之前）
        applySavedSort()
        
        filteredList.addAll(videoList) // 使用排序后的videoList初始化过滤列表
    }
    
    private fun applySavedSort() {
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
        binding.rvVideoList.layoutManager = LinearLayoutManager(this)
        binding.rvVideoList.adapter = adapter
    }

    private fun openVideoPlayer(video: VideoFileParcelable, currentIndex: Int) {
        Log.d(TAG, "播放视频: ${video.name}, 索引: $currentIndex")

        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.data = Uri.parse(video.uri)
        intent.putExtra("video_name", video.name)
        intent.putExtra("current_index", currentIndex)
        intent.putExtra("folderName", binding.tvFolderTitle.text.toString())  // 传递文件夹名称
        intent.putParcelableArrayListExtra("video_list", ArrayList(videoList))
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    private fun showSortDialog() {
        Log.d(TAG, "显示排序对话框")
        
        val dialog = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val view = layoutInflater.inflate(R.layout.dialog_sort_popup, null)
        
        val tvSortNameAsc = view.findViewById<TextView>(R.id.tvSortNameAsc)
        val tvSortNameDesc = view.findViewById<TextView>(R.id.tvSortNameDesc)
        val tvSortDateAsc = view.findViewById<TextView>(R.id.tvSortDateAsc)
        val tvSortDateDesc = view.findViewById<TextView>(R.id.tvSortDateDesc)
        
        // 设置主题颜色 - 高亮当前选中的排序方式
        val primaryColor = ThemeManager.getThemeColor(this, com.google.android.material.R.attr.colorPrimary)
        val normalColor = getColor(android.R.color.black)
        
        tvSortNameAsc.setTextColor(if (currentSortType == SortType.NAME && currentSortOrder == SortOrder.ASCENDING) primaryColor else normalColor)
        tvSortNameDesc.setTextColor(if (currentSortType == SortType.NAME && currentSortOrder == SortOrder.DESCENDING) primaryColor else normalColor)
        tvSortDateAsc.setTextColor(if (currentSortType == SortType.DATE && currentSortOrder == SortOrder.ASCENDING) primaryColor else normalColor)
        tvSortDateDesc.setTextColor(if (currentSortType == SortType.DATE && currentSortOrder == SortOrder.DESCENDING) primaryColor else normalColor)
        
        // 名称升序
        tvSortNameAsc.setOnClickListener {
            currentSortType = SortType.NAME
            currentSortOrder = SortOrder.ASCENDING
            saveSortSettings()
            sortVideoListWithAnimation()
            dialog.dismiss()
        }
        
        // 名称降序
        tvSortNameDesc.setOnClickListener {
            currentSortType = SortType.NAME
            currentSortOrder = SortOrder.DESCENDING
            saveSortSettings()
            sortVideoListWithAnimation()
            dialog.dismiss()
        }
        
        // 日期升序
        tvSortDateAsc.setOnClickListener {
            currentSortType = SortType.DATE
            currentSortOrder = SortOrder.ASCENDING
            saveSortSettings()
            sortVideoListWithAnimation()
            dialog.dismiss()
        }
        
        // 日期降序
        tvSortDateDesc.setOnClickListener {
            currentSortType = SortType.DATE
            currentSortOrder = SortOrder.DESCENDING
            saveSortSettings()
            sortVideoListWithAnimation()
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)
        
        // 获取排序按钮在屏幕上的位置
        val location = IntArray(2)
        binding.btnSort.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        
        // 测量对话框大小
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val dialogWidth = view.measuredWidth.coerceAtLeast(binding.btnSort.width)
        val dialogHeight = view.measuredHeight
        
        // 设置对话框位置在排序按钮下方
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        layoutParams?.x = anchorX + (binding.btnSort.width - dialogWidth) / 2
        layoutParams?.y = anchorY + binding.btnSort.height + 10 // 显示在按钮下方，留10px间距
        layoutParams?.width = dialogWidth
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams
        
        // 设置进场和出场动画
        window?.setWindowAnimations(R.style.PopupAnimation)
        
        dialog.show()
    }
    
    private fun sortVideoListWithAnimation() {
        Log.d(TAG, "开始排序动画，当前列表大小: ${videoList.size}")
        
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
        val query = binding.etSearchOverlay.text.toString()
        if (query.isEmpty()) {
            filteredList.clear()
            filteredList.addAll(videoList)
        } else {
            filterVideos(query)
        }
        
        // 使用 notifyDataSetChanged 配合 RecyclerView 的默认动画
        adapter.notifyDataSetChanged()
        
        val sortTypeText = if (currentSortType == SortType.NAME) "名称" else "日期"
        val sortOrderText = if (currentSortOrder == SortOrder.ASCENDING) "升序" else "降序"
        Log.d(TAG, "排序完成: $sortTypeText $sortOrderText")
    }
    
    private fun sortVideoList() {
        Log.d(TAG, "开始排序（无动画），当前列表大小: ${videoList.size}")
        
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
        val query = binding.etSearchOverlay.text.toString()
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
                            
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新视频列表失败", e)
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing = false
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
        if (isSearchMode) {
            hideSearchOverlay()
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
