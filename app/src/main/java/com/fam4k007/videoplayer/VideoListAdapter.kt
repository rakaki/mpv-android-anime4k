package com.fam4k007.videoplayer

import android.net.Uri
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fam4k007.videoplayer.utils.ThumbnailCacheManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class VideoListAdapter(
    private val videos: List<VideoFileParcelable>,
    private val lifecycleOwner: LifecycleOwner,
    private val onVideoClick: (VideoFileParcelable, Int) -> Unit,
    private val onMoreInfoClick: (VideoFileParcelable) -> Unit  // 新增：更多信息点击回调
) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {

    // 使用缩略图缓存管理器
    private lateinit var thumbnailCacheManager: ThumbnailCacheManager
    
    // 存储每个ViewHolder的Job，用于取消未完成的任务
    private val thumbnailJobs = mutableMapOf<Int, Job>()

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVideoName: TextView = itemView.findViewById(R.id.tvVideoName)
        val tvVideoDuration: TextView = itemView.findViewById(R.id.tvVideoDuration)
        val tvVideoSize: TextView = itemView.findViewById(R.id.tvVideoSize)
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        val btnMoreInfo: ImageView = itemView.findViewById(R.id.btnMoreInfo)  // 新增
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_file, parent, false)
        // 初始化缓存管理器
        if (!::thumbnailCacheManager.isInitialized) {
            thumbnailCacheManager = ThumbnailCacheManager.getInstance(parent.context)
        }
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.tvVideoName.text = video.name
        holder.tvVideoDuration.text = formatDuration(video.duration)
        holder.tvVideoSize.text = formatFileSize(video.size)

        // 加载视频缩略图
        try {
            val context = holder.itemView.context
            val uri = Uri.parse(video.uri)

            // 先设置占位图
            holder.ivThumb.setImageResource(android.R.drawable.ic_media_play)

            // 使用缓存管理器加载缩略图（带缓存）
            val job = lifecycleOwner.lifecycleScope.launch {
                try {
                    val bitmap = thumbnailCacheManager.getThumbnail(context, uri, video.duration)
                    if (bitmap != null) {
                        holder.ivThumb.setImageBitmap(bitmap)
                        holder.ivThumb.scaleType = ImageView.ScaleType.CENTER_CROP
                    } else {
                        throw Exception("Failed to get thumbnail")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoListAdapter", "缩略图加载失败: ${e.message}")
                    // 降级到 Glide
                    loadThumbnailWithGlide(context, uri, holder.ivThumb)
                }
            }
            thumbnailJobs[holder.adapterPosition] = job
        } catch (e: Exception) {
            android.util.Log.e("VideoListAdapter", "缩略图加载异常: ${e.message}")
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener {
            onVideoClick(video, position)
        }
        
        // 新增：更多信息按钮点击事件
        holder.btnMoreInfo.setOnClickListener {
            onMoreInfoClick(video)
        }
    }

    /**
     * 使用 Glide 加载视频缩略图（从视频时长的中间位置提取）
     */
    private fun loadThumbnailWithGlide(context: android.content.Context, uri: Uri, imageView: ImageView, duration: Long = 0) {
        try {
            // 获取视频时长并计算中间帧的时间位置
            val video = videos.find { it.uri == uri.toString() }
            val frameTime = if (video != null && video.duration > 0) {
                (video.duration * 1000L) / 2 // 取中间位置（微秒）
            } else {
                5000000L // 如果无法获取时长，默认提取5秒位置
            }
            
            Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(
                    RequestOptions()
                        .frame(frameTime) // 提取中间位置的帧
                        .override(384, 216) // 16:9 比例，适配 96dp x 54dp
                        .centerCrop()
                )
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        } catch (e: Exception) {
            android.util.Log.e("VideoListAdapter", "Glide 加载失败: ${e.message}")
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    /**
     * 当ViewHolder被回收时，取消未完成的缩略图加载任务
     */
    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        thumbnailJobs[holder.adapterPosition]?.cancel()
        thumbnailJobs.remove(holder.adapterPosition)
    }

    override fun getItemCount(): Int = videos.size

    private fun formatDuration(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            else -> String.format("%.1f KB", kb)
        }
    }
}
