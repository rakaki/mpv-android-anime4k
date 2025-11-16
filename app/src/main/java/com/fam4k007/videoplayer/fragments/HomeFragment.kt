package com.fam4k007.videoplayer.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fam4k007.videoplayer.VideoBrowserActivity
import com.fam4k007.videoplayer.BiliBiliPlayActivity
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.DownloadActivity
import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.databinding.FragmentHomeBinding
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity
import com.fam4k007.videoplayer.webdav.WebDavConfig
import com.fam4k007.videoplayer.webdav.WebDavConfigDialog
import com.fam4k007.videoplayer.webdav.WebDavBrowserActivity
import androidx.lifecycle.lifecycleScope
import android.util.Log

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyManager: PlaybackHistoryManager
    
    companion object {
        private const val TAG = "HomeFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化历史记录管理器
        historyManager = PlaybackHistoryManager(requireContext())
        
        // 检查是否有上次播放记录
        updateContinuePlayState()
        
        // 右上角设置按钮点击事件
        binding.ivSettings.setOnClickListener {
            startActivity(Intent(requireContext(), com.fam4k007.videoplayer.SettingsComposeActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.scale_in,
                com.fam4k007.videoplayer.R.anim.scale_out
            )
        }
        
        // 下载按钮点击事件
        binding.ivDownload.setOnClickListener {
            startActivity(Intent(requireContext(), DownloadActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.slide_in_right,
                com.fam4k007.videoplayer.R.anim.slide_out_left
            )
        }
        
        // 设置继续播放图标点击事件
        binding.ivLogo.setOnClickListener {
            continueLastPlay()
        }
        
        binding.btnSelectVideo.setOnClickListener {
            startActivity(Intent(requireContext(), VideoBrowserActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.slide_in_right,
                com.fam4k007.videoplayer.R.anim.slide_out_left
            )
        }
        
        binding.btnBiliBili.setOnClickListener {
            startActivity(Intent(requireContext(), BiliBiliPlayActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.scale_in,
                com.fam4k007.videoplayer.R.anim.scale_out
            )
        }
        
        // WebDAV 按钮点击事件
        binding.ivWebDav.setOnClickListener {
            handleWebDavClick()
        }
    }
    
    /**
     * 处理 WebDAV 按钮点击
     */
    private fun handleWebDavClick() {
        if (WebDavConfig.isConfigured(requireContext())) {
            // 已配置，直接进入文件浏览
            startActivity(Intent(requireContext(), WebDavBrowserActivity::class.java))
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.slide_in_right,
                com.fam4k007.videoplayer.R.anim.slide_out_left
            )
        } else {
            // 未配置，显示配置对话框
            WebDavConfigDialog(requireContext(), lifecycleScope) { config ->
                // 配置保存后，进入文件浏览
                startActivity(Intent(requireContext(), WebDavBrowserActivity::class.java))
                requireActivity().overridePendingTransition(
                    com.fam4k007.videoplayer.R.anim.slide_in_right,
                    com.fam4k007.videoplayer.R.anim.slide_out_left
                )
            }.show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 每次返回主页时更新继续播放状态
        updateContinuePlayState()
    }
    
    /**
     * 更新继续播放状态
     */
    private fun updateContinuePlayState() {
        val lastVideo = historyManager.getLastPlayedLocalVideo()
        if (lastVideo != null) {
            // 有播放记录，显示提示文本
            binding.tvContinueHint.visibility = View.VISIBLE
            binding.tvContinueHint.text = "继续播放: ${lastVideo.fileName}"
            Log.d(TAG, "Last played video found: ${lastVideo.fileName}")
        } else {
            // 没有播放记录，隐藏提示
            binding.tvContinueHint.visibility = View.GONE
            Log.d(TAG, "No last played video found")
        }
    }
    
    /**
     * 继续上次播放
     */
    private fun continueLastPlay() {
        val lastVideo = historyManager.getLastPlayedLocalVideo()
        if (lastVideo == null) {
            // 没有播放记录，提示用户
            android.widget.Toast.makeText(
                requireContext(), 
                "暂无播放记录", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "No history to continue")
            return
        }
        
        try {
            val videoUri = Uri.parse(lastVideo.uri)
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                data = videoUri
                action = Intent.ACTION_VIEW
                // 传递文件夹路径，以便返回时能定位到正确的列表
                putExtra("folder_path", lastVideo.folderName)
                putExtra("last_position", lastVideo.position)
            }
            
            Log.d(TAG, "Continue playing: ${lastVideo.fileName}, position: ${lastVideo.position}ms, folder: ${lastVideo.folderName}")
            
            startActivity(intent)
            requireActivity().overridePendingTransition(
                com.fam4k007.videoplayer.R.anim.slide_in_right,
                com.fam4k007.videoplayer.R.anim.slide_out_left
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to continue play", e)
            android.widget.Toast.makeText(
                requireContext(), 
                "无法播放该视频: ${e.message}", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
