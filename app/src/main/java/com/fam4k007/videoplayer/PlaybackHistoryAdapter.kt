package com.fam4k007.videoplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlaybackHistoryAdapter(
    private val historyList: MutableList<PlaybackHistoryManager.HistoryItem>,
    private val onItemClick: (PlaybackHistoryManager.HistoryItem) -> Unit,
    private val onDeleteClick: (PlaybackHistoryManager.HistoryItem) -> Unit
) : RecyclerView.Adapter<PlaybackHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName: TextView = view.findViewById(R.id.tvHistoryFileName)
        val tvFolderName: TextView = view.findViewById(R.id.tvHistoryFolderName)
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

        holder.tvFileName.text = item.fileName
        holder.tvFolderName.text = item.folderName
        holder.tvPlayTime.text = item.getFormattedDate()
        
        val progress = item.getProgressPercentage()
        holder.progressBar.progress = progress
        holder.tvProgress.text = "$progress%"

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
