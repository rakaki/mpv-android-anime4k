package com.fam4k007.videoplayer.webdav

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.databinding.ItemWebdavFileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WebDAV 文件列表适配器
 */
class WebDavFileAdapter(
    private val onItemClick: (WebDavClient.WebDavFile) -> Unit
) : RecyclerView.Adapter<WebDavFileAdapter.FileViewHolder>() {

    private val files = mutableListOf<WebDavClient.WebDavFile>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun updateFiles(newFiles: List<WebDavClient.WebDavFile>) {
        files.clear()
        android.util.Log.d("WebDavAdapter", "收到 ${newFiles.size} 个文件")
        
        // 只显示文件夹和视频文件，过滤其他文件
        val filteredFiles = newFiles.filter { file ->
            val isVideo = WebDavClient.isVideoFile(file.name)
            android.util.Log.d("WebDavAdapter", "文件: ${file.name}, 是文件夹: ${file.isDirectory}, 是视频: $isVideo")
            file.isDirectory || isVideo
        }
        
        android.util.Log.d("WebDavAdapter", "过滤后剩余 ${filteredFiles.size} 个文件")
        
        // 文件夹在前，文件在后
        files.addAll(filteredFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name })))
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemWebdavFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(
        private val binding: ItemWebdavFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: WebDavClient.WebDavFile) {
            binding.tvFileName.text = file.name
            
            if (file.isDirectory) {
                binding.ivIcon.setImageResource(R.drawable.ic_folder)
                binding.ivArrow.visibility = View.VISIBLE
                binding.tvFileInfo.text = "文件夹"
            } else {
                binding.ivIcon.setImageResource(R.drawable.ic_video_file)
                binding.ivArrow.visibility = View.GONE
                
                val sizeStr = formatFileSize(file.size)
                val dateStr = if (file.modifiedTime > 0) {
                    dateFormat.format(Date(file.modifiedTime))
                } else {
                    ""
                }
                binding.tvFileInfo.text = if (dateStr.isNotEmpty()) {
                    "$sizeStr · $dateStr"
                } else {
                    sizeStr
                }
            }
            
            binding.root.setOnClickListener {
                onItemClick(file)
            }
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
                else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
            }
        }
    }
}
