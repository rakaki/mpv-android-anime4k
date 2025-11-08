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

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupRecyclerView()
        checkPermissions()
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener { finish() }

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
                adapter.notifyDataSetChanged()
                binding.swipeRefreshLayout.isRefreshing = false
                
            } catch (e: Exception) {
                Log.e(TAG, "扫描视频文件失败", e)
                binding.swipeRefreshLayout.isRefreshing = false
                DialogUtils.showToastShort(this@VideoBrowserActivity, "扫描视频失败: ${e.message}")
            }
        }
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
