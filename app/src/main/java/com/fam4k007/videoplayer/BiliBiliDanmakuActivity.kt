package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.documentfile.provider.DocumentFile
import com.fam4k007.videoplayer.danmaku.BiliBiliDanmakuDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * B站弹幕下载页面
 * 简单直接的两个功能：设置文件夹 + 下载弹幕
 */
class BiliBiliDanmakuActivity : BaseActivity() {

    private lateinit var cardSetFolder: CardView
    private lateinit var cardDownloadDanmaku: CardView
    private lateinit var tvFolderStatus: TextView
    private lateinit var tvFolderPath: TextView
    
    private var savedFolderUri: Uri? = null
    private lateinit var downloadManager: BiliBiliDanmakuDownloadManager

    // 文件夹选择器
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            handleFolderSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bilibili_danmaku)

        // 设置Toolbar
        setupToolbar(R.id.toolbar, "哔哩哔哩弹幕下载", showBackButton = true)

        // 初始化
        downloadManager = BiliBiliDanmakuDownloadManager(this)
        initViews()
        loadSavedFolder()
        setupListeners()
    }

    private fun initViews() {
        cardSetFolder = findViewById(R.id.cardSetFolder)
        cardDownloadDanmaku = findViewById(R.id.cardDownloadDanmaku)
        tvFolderStatus = findViewById(R.id.tvFolderStatus)
        tvFolderPath = findViewById(R.id.tvFolderPath)
    }

    /**
     * 加载已保存的文件夹
     */
    private fun loadSavedFolder() {
        val prefs = getSharedPreferences("bilibili_danmaku", MODE_PRIVATE)
        val savedUriString = prefs.getString("save_directory_uri", null)
        
        if (savedUriString != null) {
            savedFolderUri = Uri.parse(savedUriString)
            updateFolderDisplay(savedFolderUri!!)
        } else {
            tvFolderStatus.text = "未设置"
            tvFolderPath.text = "点击选择弹幕保存位置"
        }
    }

    /**
     * 更新文件夹显示
     */
    private fun updateFolderDisplay(uri: Uri) {
        tvFolderStatus.text = "已设置"
        
        // 获取文件夹名称
        val documentFile = DocumentFile.fromTreeUri(this, uri)
        val folderName = documentFile?.name ?: "未知文件夹"
        tvFolderPath.text = "保存至: $folderName"
    }

    private fun setupListeners() {
        // 设置保存文件夹
        cardSetFolder.setOnClickListener {
            folderPickerLauncher.launch(savedFolderUri)
        }

        // 下载弹幕
        cardDownloadDanmaku.setOnClickListener {
            if (savedFolderUri == null) {
                Toast.makeText(this, "请先设置保存文件夹", Toast.LENGTH_SHORT).show()
            } else {
                showDownloadDialog()
            }
        }
    }

    /**
     * 处理文件夹选择结果
     */
    private fun handleFolderSelected(uri: Uri) {
        try {
            // 获取持久化权限
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)

            // 保存到SharedPreferences
            getSharedPreferences("bilibili_danmaku", MODE_PRIVATE)
                .edit()
                .putString("save_directory_uri", uri.toString())
                .apply()

            savedFolderUri = uri
            updateFolderDisplay(uri)
            
            Toast.makeText(this, "文件夹设置成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示下载对话框
     */
    private fun showDownloadDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_input_url, null)
        val editTextUrl = dialogView.findViewById<EditText>(R.id.editTextUrl)

        val dialog = AlertDialog.Builder(this)
            .setTitle("下载B站弹幕")
            .setView(dialogView)
            .setPositiveButton("下载", null) // 先设置为null，后面手动设置点击事件
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val url = editTextUrl.text.toString().trim()
                
                if (url.isEmpty()) {
                    Toast.makeText(this, "请输入视频链接", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!downloadManager.isValidBilibiliUrl(url)) {
                    Toast.makeText(this, "请输入有效的B站视频链接", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 关闭输入对话框
                dialog.dismiss()
                
                // 开始下载
                startDownload(url)
            }
        }

        dialog.show()
    }

    /**
     * 开始下载弹幕
     */
    private fun startDownload(url: String) {
        // 显示加载对话框
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("下载中")
            .setMessage("正在获取弹幕，请稍候...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        // 在协程中执行下载
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    downloadManager.downloadDanmaku(url, savedFolderUri!!)
                }

                progressDialog.dismiss()

                when (result) {
                    is BiliBiliDanmakuDownloadManager.DownloadResult.Success -> {
                        Toast.makeText(
                            this@BiliBiliDanmakuActivity,
                            "下载成功: ${result.fileName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is BiliBiliDanmakuDownloadManager.DownloadResult.Error -> {
                        Toast.makeText(
                            this@BiliBiliDanmakuActivity,
                            "下载失败: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(
                    this@BiliBiliDanmakuActivity,
                    "下载失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
