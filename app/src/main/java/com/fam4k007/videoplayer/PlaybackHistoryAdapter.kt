package com.fam4k007.videoplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.signature.ObjectKey
import java.io.File

class PlaybackHistoryAdapter(
    private val historyList: MutableList<PlaybackHistoryManager.HistoryItem>,
    private val onItemClick: (PlaybackHistoryManager.HistoryItem) -> Unit,
    private val onDeleteClick: (PlaybackHistoryManager.HistoryItem) -> Unit
) : RecyclerView.Adapter<PlaybackHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivHistoryThumbnail)
        val tvFileName: TextView = view.findViewById(R.id.tvHistoryFileName)
        val tvPlayTime: TextView = view.findViewById(R.id.tvHistoryPlayTime)
        val tvProgress: TextView = view.findViewById(R.id.tvHistoryProgress)
        val progressBar: ProgressBar = view.findViewById(R.id.progressHistory)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playback_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        val context = holder.itemView.context

        // 设置标题
        holder.tvFileName.text = item.fileName
        
        // 设置播放时间标签
        holder.tvPlayTime.text = item.getFormattedDate()
        
        // 设置进度标签和进度条
        val progress = item.getProgressPercentage()
        holder.progressBar.progress = progress
        holder.tvProgress.text = "进度 $progress%"

        // 加载缩略图 - 直接从视频URI提取指定位置的帧（优化缓存）
        try {
            val videoUri = android.net.Uri.parse(item.uri)
            Glide.with(context)
                .load(videoUri)
                .frame(item.position * 1000) // 转换为微秒
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // 缓存提取的帧，避免重复解码
                .signature(ObjectKey("${item.uri}_${item.position}")) // 位置变化时更新缓存
                .transform(CenterCrop())
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .into(holder.ivThumbnail)
        } catch (e: Exception) {
            // 加载失败时显示默认图标
            Glide.with(context)
                .load(R.drawable.ic_video_placeholder)
                .into(holder.ivThumbnail)
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = historyList.size

    fun removeItem(item: PlaybackHistoryManager.HistoryItem) {
        val position = historyList.indexOf(item)
        if (position != -1) {
            historyList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clearAll() {
        val size = historyList.size
        historyList.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun updateData(newList: List<PlaybackHistoryManager.HistoryItem>) {
        historyList.clear()
        historyList.addAll(newList)
        notifyDataSetChanged()
    }
}
