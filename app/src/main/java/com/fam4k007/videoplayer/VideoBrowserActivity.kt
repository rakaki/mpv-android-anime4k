package com.fam4k007.videoplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fam4k007.videoplayer.databinding.ActivityVideoBrowserBinding
import com.fam4k007.videoplayer.utils.DialogUtils
import com.fam4k007.videoplayer.utils.NoMediaChecker
import com.fam4k007.videoplayer.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoBrowserActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoBrowserActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityVideoBrowserBinding

    private val videoFolders = mutableListOf<VideoFolder>()
    private lateinit var adapter: VideoFolderAdapter
    
    // 排序设置
    private enum class SortType { NAME, VIDEO_COUNT }
    private enum class SortOrder { ASCENDING, DESCENDING }
    private var currentSortType = SortType.VIDEO_COUNT
    private var currentSortOrder = SortOrder.DESCENDING
    
    // PreferencesManager
    private lateinit var preferencesManager: com.fam4k007.videoplayer.manager.PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 PreferencesManager
        preferencesManager = com.fam4k007.videoplayer.manager.PreferencesManager.getInstance(this)
        
        // 读取保存的排序设置
        loadSortSettings()
        
        initViews()
        setupRecyclerView()
        checkPermissions()
    }
    
    private fun loadSortSettings() {
        val sortType = preferencesManager.getFolderSortType()
        val sortOrder = preferencesManager.getFolderSortOrder()
        
        currentSortType = when (sortType) {
            "VIDEO_COUNT" -> SortType.VIDEO_COUNT
            else -> SortType.NAME
        }
        
        currentSortOrder = when (sortOrder) {
            "DESCENDING" -> SortOrder.DESCENDING
            else -> SortOrder.ASCENDING
        }
        
        Log.d(TAG, "加载文件夹排序设置: $currentSortType $currentSortOrder")
    }
    
    private fun saveSortSettings() {
        val sortTypeStr = when (currentSortType) {
            SortType.NAME -> "NAME"
            SortType.VIDEO_COUNT -> "VIDEO_COUNT"
        }
        
        val sortOrderStr = when (currentSortOrder) {
            SortOrder.ASCENDING -> "ASCENDING"
            SortOrder.DESCENDING -> "DESCENDING"
        }
        
        preferencesManager.setFolderSortType(sortTypeStr)
        preferencesManager.setFolderSortOrder(sortOrderStr)
        
        Log.d(TAG, "保存文件夹排序设置: $sortTypeStr $sortOrderStr")
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSort.setOnClickListener { showSortDialog() }

        binding.btnRequestPermission.setOnClickListener {
            requestStoragePermission()
        }

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
    }

    private fun setupRecyclerView() {
        adapter = VideoFolderAdapter(videoFolders) { folder ->
            openVideoList(folder)
        }
        binding.recyclerViewFolders.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFolders.adapter = adapter
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            binding.permissionPrompt.visibility = View.GONE
            scanVideoFiles()
        } else {
            binding.permissionPrompt.visibility = View.VISIBLE
            binding.recyclerViewFolders.visibility = View.GONE
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // 用户之前拒绝过权限，显示详细说明
            binding.tvPermissionMessage.text = "需要存储权限以浏览您的视频文件。请在设置中授予权限。"
            binding.btnRequestPermission.text = "前往设置"
            binding.btnRequestPermission.setOnClickListener {
                openAppSettings()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.permissionPrompt.visibility = View.GONE
                binding.recyclerViewFolders.visibility = View.VISIBLE
                scanVideoFiles()
            } else {
                DialogUtils.showToastShort(this, "权限被拒绝，无法浏览视频")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从设置返回后重新检查权限
        if (binding.permissionPrompt.visibility == View.VISIBLE) {
            checkPermissions()
        }
    }

    private fun scanVideoFiles() {
        Log.d(TAG, "开始扫描视频文件...")
        
        lifecycleScope.launch {
            try {
                // 在IO线程执行扫描
                val scannedFolders = withContext(Dispatchers.IO) {
                    val projection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.DATE_ADDED
                    )

                    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

                    val cursor = contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )

                    val folderMap = mutableMapOf<String, MutableList<VideoFile>>()

                    cursor?.use {
                        val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                        val pathColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                        val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                        while (it.moveToNext()) {
                            val id = it.getLong(idColumn)
                            val name = it.getString(nameColumn)
                            val path = it.getString(pathColumn)
                            val size = it.getLong(sizeColumn)
                            val duration = it.getLong(durationColumn)
                            val dateAdded = it.getLong(dateColumn)

                            // 检查文件路径是否在包含 .nomedia 的文件夹中
                            if (NoMediaChecker.fileInNoMediaFolder(path)) {
                                Log.d(TAG, "跳过 .nomedia 文件夹中的视频: $path")
                                continue
                            }

                            val uri = Uri.withAppendedPath(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                            // 提取文件夹路径
                            val folderPath = path.substringBeforeLast("/")

                            val videoFile = VideoFile(
                                uri = uri.toString(),
                                name = name,
                                path = path,
                                size = size,
                                duration = duration,
                                dateAdded = dateAdded
                            )

                            folderMap.getOrPut(folderPath) { mutableListOf() }.add(videoFile)
                        }
                    }

                    // 转换为VideoFolder列表
                    val folders = mutableListOf<VideoFolder>()
                    folderMap.forEach { (path, videos) ->
                        val folderName = path.substringAfterLast("/")
                        folders.add(
                            VideoFolder(
                                folderPath = path,
                                folderName = folderName.ifEmpty { "根目录" },
                                videoCount = videos.size,
                                videos = videos
                            )
                        )
                    }

                    // 按视频数量降序排序
                    folders.sortByDescending { it.videoCount }

                    Log.d(TAG, "扫描完成，找到 ${folders.size} 个文件夹，共 ${folderMap.values.sumOf { it.size }} 个视频")
                    
                    folders
                }
                
                // 在主线程更新UI
                videoFolders.clear()
                videoFolders.addAll(scannedFolders)
                
                // 应用保存的排序设置
                applySavedSort()
                
                adapter.notifyDataSetChanged()
                binding.swipeRefreshLayout.isRefreshing = false
                
            } catch (e: Exception) {
                Log.e(TAG, "扫描视频文件失败", e)
                binding.swipeRefreshLayout.isRefreshing = false
                DialogUtils.showToastShort(this@VideoBrowserActivity, "扫描视频失败: ${e.message}")
            }
        }
    }
    
    private fun applySavedSort() {
        val sortedList = when (currentSortType) {
            SortType.NAME -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    videoFolders.sortedBy { it.folderName.lowercase() }
                } else {
                    videoFolders.sortedByDescending { it.folderName.lowercase() }
                }
            }
            SortType.VIDEO_COUNT -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    videoFolders.sortedBy { it.videoCount }
                } else {
                    videoFolders.sortedByDescending { it.videoCount }
                }
            }
        }
        
        videoFolders.clear()
        videoFolders.addAll(sortedList)
    }
    
    private fun showSortDialog() {
        Log.d(TAG, "显示文件夹排序对话框")
        
        val dialog = android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val view = layoutInflater.inflate(R.layout.dialog_folder_sort_popup, null)
        
        val tvSortNameAsc = view.findViewById<TextView>(R.id.tvSortNameAsc)
        val tvSortNameDesc = view.findViewById<TextView>(R.id.tvSortNameDesc)
        val tvSortCountAsc = view.findViewById<TextView>(R.id.tvSortCountAsc)
        val tvSortCountDesc = view.findViewById<TextView>(R.id.tvSortCountDesc)
        
        // 设置主题颜色 - 高亮当前选中的排序方式
        val primaryColor = ThemeManager.getThemeColor(this, com.google.android.material.R.attr.colorPrimary)
        val normalColor = getColor(android.R.color.black)
        
        tvSortNameAsc.setTextColor(if (currentSortType == SortType.NAME && currentSortOrder == SortOrder.ASCENDING) primaryColor else normalColor)
        tvSortNameDesc.setTextColor(if (currentSortType == SortType.NAME && currentSortOrder == SortOrder.DESCENDING) primaryColor else normalColor)
        tvSortCountAsc.setTextColor(if (currentSortType == SortType.VIDEO_COUNT && currentSortOrder == SortOrder.ASCENDING) primaryColor else normalColor)
        tvSortCountDesc.setTextColor(if (currentSortType == SortType.VIDEO_COUNT && currentSortOrder == SortOrder.DESCENDING) primaryColor else normalColor)
        
        // 名称升序
        tvSortNameAsc.setOnClickListener {
            currentSortType = SortType.NAME
            currentSortOrder = SortOrder.ASCENDING
            saveSortSettings()
            sortFolderListWithAnimation()
            dialog.dismiss()
        }
        
        // 名称降序
        tvSortNameDesc.setOnClickListener {
            currentSortType = SortType.NAME
            currentSortOrder = SortOrder.DESCENDING
            saveSortSettings()
            sortFolderListWithAnimation()
            dialog.dismiss()
        }
        
        // 视频数量升序
        tvSortCountAsc.setOnClickListener {
            currentSortType = SortType.VIDEO_COUNT
            currentSortOrder = SortOrder.ASCENDING
            saveSortSettings()
            sortFolderListWithAnimation()
            dialog.dismiss()
        }
        
        // 视频数量降序
        tvSortCountDesc.setOnClickListener {
            currentSortType = SortType.VIDEO_COUNT
            currentSortOrder = SortOrder.DESCENDING
            saveSortSettings()
            sortFolderListWithAnimation()
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
        layoutParams?.x = anchorX + (binding.btnSort.width - dialogWidth) / 2 - 40 // 左移40px，离右边框更远
        layoutParams?.y = anchorY + binding.btnSort.height + 10
        layoutParams?.width = dialogWidth
        layoutParams?.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams
        
        // 设置进场和出场动画
        window?.setWindowAnimations(R.style.PopupAnimation)
        
        dialog.show()
    }
    
    private fun sortFolderListWithAnimation() {
        Log.d(TAG, "开始排序文件夹，当前列表大小: ${videoFolders.size}")
        
        val sortedList = when (currentSortType) {
            SortType.NAME -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    videoFolders.sortedBy { it.folderName.lowercase() }
                } else {
                    videoFolders.sortedByDescending { it.folderName.lowercase() }
                }
            }
            SortType.VIDEO_COUNT -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    videoFolders.sortedBy { it.videoCount }
                } else {
                    videoFolders.sortedByDescending { it.videoCount }
                }
            }
        }
        
        videoFolders.clear()
        videoFolders.addAll(sortedList)
        
        adapter.notifyDataSetChanged()
        
        Log.d(TAG, "排序完成")
    }

    private fun refreshVideoList() {
        scanVideoFiles()
    }

    private fun openVideoList(folder: VideoFolder) {
        val intent = Intent(this, VideoListActivity::class.java)
        intent.putExtra("folder_name", folder.folderName)
        intent.putParcelableArrayListExtra("video_list", ArrayList(folder.videos.map {
            VideoFileParcelable(it.uri, it.name, it.path, it.size, it.duration, it.dateAdded)
        }))
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
