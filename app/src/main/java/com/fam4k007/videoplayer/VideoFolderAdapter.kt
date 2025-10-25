package com.fam4k007.videoplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoFolderAdapter(
    private val folders: List<VideoFolder>,
    private val onFolderClick: (VideoFolder) -> Unit
) : RecyclerView.Adapter<VideoFolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        val tvFolderPath: TextView = itemView.findViewById(R.id.tvFolderPath)
        val tvVideoCount: TextView = itemView.findViewById(R.id.tvVideoCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.tvFolderName.text = folder.folderName
        holder.tvFolderPath.text = folder.folderPath
        holder.tvVideoCount.text = "${folder.videoCount} 个视频"

        holder.itemView.setOnClickListener {
            onFolderClick(folder)
        }
    }

    override fun getItemCount(): Int = folders.size
}
