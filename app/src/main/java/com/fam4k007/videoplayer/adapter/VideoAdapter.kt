package com.fam4k007.videoplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.utils.VideoItem

class VideoAdapter(private val videos: List<VideoItem>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.videoName)
        val folder: TextView = view.findViewById(R.id.videoFolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.name.text = video.name
        holder.folder.text = video.folder
    }

    override fun getItemCount() = videos.size
}
