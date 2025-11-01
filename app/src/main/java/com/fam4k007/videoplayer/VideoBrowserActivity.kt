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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.utils.DialogUtils
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.fam4k007.videoplayer.utils.ThemeManager

class VideoBrowserActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoBrowserActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var rvVideoFolders: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var permissionPrompt: View
    private lateinit var btnGrantPermission: Button
    private lateinit var tvPermissionMessage: TextView
    private lateinit var btnBack: View

    private val videoFolders = mutableListOf<VideoFolder>()
    private lateinit var adapter: VideoFolderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_browser)

        initViews()
        setupRecyclerView()
        checkPermissions()
    }

    private fun initViews() {
        rvVideoFolders = findViewById(R.id.recyclerViewFolders)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        permissionPrompt = findViewById(R.id.permissionPrompt)
        btnGrantPermission = findViewById(R.id.btnRequestPermission)
        tvPermissionMessage = findViewById(R.id.tvPermissionMessage)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnGrantPermission.setOnClickListener {
            requestStoragePermission()
        }

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
    }

    private fun setupRecyclerView() {
        adapter = VideoFolderAdapter(videoFolders) { folder ->
            openVideoList(folder)
        }
        rvVideoFolders.layoutManager = LinearLayoutManager(this)
        rvVideoFolders.adapter = adapter
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            permissionPrompt.visibility = View.GONE
            scanVideoFiles()
        } else {
            permissionPrompt.visibility = View.VISIBLE
            rvVideoFolders.visibility = View.GONE
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
            tvPermissionMessage.text = "需要存储权限以浏览您的视频文件。请在设置中授予权限。"
            btnGrantPermission.text = "前往设置"
            btnGrantPermission.setOnClickListener {
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
                permissionPrompt.visibility = View.GONE
                rvVideoFolders.visibility = View.VISIBLE
                scanVideoFiles()
            } else {
                DialogUtils.showToastShort(this, "权限被拒绝，无法浏览视频")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从设置返回后重新检查权�?
        if (permissionPrompt.visibility == View.VISIBLE) {
            checkPermissions()
        }
    }

    private fun scanVideoFiles() {
        Log.d(TAG, "开始扫描视频文�?..")
        videoFolders.clear()

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

                // 提取文件夹路�?
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
        folderMap.forEach { (path, videos) ->
            val folderName = path.substringAfterLast("/")
            videoFolders.add(
                VideoFolder(
                    folderPath = path,
                    folderName = folderName.ifEmpty { "根目录" },
                    videoCount = videos.size,
                    videos = videos
                )
            )
        }

        // 按视频数量降序排�?
        videoFolders.sortByDescending { it.videoCount }

        Log.d(TAG, "扫描完成，找到 ${videoFolders.size} 个文件夹，共 ${folderMap.values.sumOf { it.size }} 个视频")

        adapter.notifyDataSetChanged()
        swipeRefreshLayout.setRefreshing(false)
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
