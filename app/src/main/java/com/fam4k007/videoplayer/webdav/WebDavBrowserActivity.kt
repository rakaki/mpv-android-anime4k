package com.fam4k007.videoplayer.webdav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.databinding.ActivityWebdavBrowserBinding
import com.fam4k007.videoplayer.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Stack

/**
 * WebDAV 文件浏览器
 */
class WebDavBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebdavBrowserBinding
    private lateinit var adapter: WebDavFileAdapter
    private lateinit var client: WebDavClient
    private val pathStack = Stack<String>()
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityWebdavBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 加载配置
        val config = WebDavConfig.load(this)
        if (config.serverUrl.isEmpty()) {
            Toast.makeText(this, "请先配置 WebDAV 服务器", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化 WebDAV 客户端
        client = WebDavClient(config)

        // 设置标题
        supportActionBar?.title = config.displayName

        // 初始化适配器
        adapter = WebDavFileAdapter { file ->
            onFileClick(file)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // 加载根目录
        loadFiles("")
    }

    private fun loadFiles(path: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                try {
                    client.listFiles(path)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            binding.progressBar.visibility = View.GONE

            if (files == null) {
                Toast.makeText(this@WebDavBrowserActivity, "加载失败", Toast.LENGTH_SHORT).show()
                if (pathStack.isEmpty()) {
                    finish()
                } else {
                    // 返回上一级
                    pathStack.pop()
                    currentPath = if (pathStack.isEmpty()) "" else pathStack.peek()
                    updatePathDisplay()
                }
                return@launch
            }

            if (files.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.updateFiles(files)
            }

            currentPath = path
            updatePathDisplay()
        }
    }

    private fun onFileClick(file: WebDavClient.WebDavFile) {
        if (file.isDirectory) {
            // 进入文件夹
            pathStack.push(currentPath)
            loadFiles(file.path)
        } else if (WebDavClient.isVideoFile(file.name)) {
            // 播放视频
            playVideo(file)
        } else {
            Toast.makeText(this, "不支持的文件类型", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo(file: WebDavClient.WebDavFile) {
        try {
            // 构建包含认证信息的 URL
            val fileUrl = if (client.config.isAnonymous || client.config.account.isEmpty()) {
                // 匿名访问
                client.getFileUrl(file.path)
            } else {
                // 账号认证：在 URL 中嵌入用户名和密码
                val uri = android.net.Uri.parse(client.config.serverUrl)
                val scheme = uri.scheme // http 或 https
                val host = uri.host
                val port = if (uri.port != -1) ":${uri.port}" else ""
                val username = android.net.Uri.encode(client.config.account)
                val password = android.net.Uri.encode(client.config.password)
                
                // 获取基础路径（如 /dav/）
                val basePath = uri.path ?: "/"
                // 对文件路径的每个部分进行编码
                val encodedPath = file.path.split("/").joinToString("/") { android.net.Uri.encode(it) }
                val fullPath = "$basePath${if (encodedPath.startsWith("/")) encodedPath.substring(1) else encodedPath}"
                
                "$scheme://$username:$password@$host$port$fullPath"
            }
            
            android.util.Log.d("WebDavBrowser", "播放 URL: $fileUrl")
            
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                data = Uri.parse(fileUrl)
                action = Intent.ACTION_VIEW
                putExtra("is_webdav", true)
                putExtra("file_name", file.name)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePathDisplay() {
        binding.tvCurrentPath.text = if (currentPath.isEmpty()) {
            "/"
        } else {
            "/$currentPath"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pathStack.isNotEmpty()) {
            // 返回上一级目录
            pathStack.pop()
            val previousPath = if (pathStack.isEmpty()) "" else pathStack.peek()
            loadFiles(previousPath)
        } else {
            // 退出Activity
            super.onBackPressed()
        }
    }
}
