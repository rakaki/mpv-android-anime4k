package com.fam4k007.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.utils.DialogUtils

/**
 * 播放历史记录页面
 */
class PlaybackHistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var tvEmptyHint: TextView
    private lateinit var historyManager: PlaybackHistoryManager
    private lateinit var adapter: PlaybackHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback_history)

        // 设置返回按钮
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        recyclerView = findViewById(R.id.rvHistory)
        emptyView = findViewById(R.id.emptyView)
        tvEmptyHint = findViewById(R.id.tvEmptyHint)
        val btnClearAll = findViewById<View>(R.id.btnClearAll)

        historyManager = PlaybackHistoryManager(this)

        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PlaybackHistoryAdapter(
            historyList = historyManager.getHistory().toMutableList(),
            onItemClick = { item ->
                // 点击播放
                playVideo(Uri.parse(item.uri), item.position)
            },
            onDeleteClick = { item ->
                // 删除单条
                showDeleteConfirmDialog(item)
            }
        )
        recyclerView.adapter = adapter

        // 清空全部按钮
        btnClearAll.setOnClickListener {
            showClearAllConfirmDialog()
        }

        updateEmptyView()
    }

    private fun playVideo(uri: Uri, startPosition: Long) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            data = uri
            putExtra("lastPosition", startPosition)
        }
        startActivity(intent)
        startActivityWithDefaultTransition()
    }

    private fun showDeleteConfirmDialog(item: PlaybackHistoryManager.HistoryItem) {
        DialogUtils.showConfirmDialog(
            this,
            "删除记录",
            "确定要删除《${item.fileName}》的播放记录吗？",
            "删除",
            "取消",
            onPositive = {
                historyManager.removeHistory(item.uri)
                adapter.removeItem(item)
                updateEmptyView()
                DialogUtils.showToastShort(this, "已删除")
            }
        )
    }

    private fun showClearAllConfirmDialog() {
        if (adapter.itemCount == 0) {
            DialogUtils.showToastShort(this, "暂无播放历史")
            return
        }

        DialogUtils.showConfirmDialog(
            this,
            "清空历史",
            "确定要清空所有播放历史吗？此操作不可恢复。",
            "清空",
            "取消",
            onPositive = {
                historyManager.clearHistory()
                adapter.clearAll()
                updateEmptyView()
                DialogUtils.showToastShort(this, "已清空播放历史")
            }
        )
    }

    private fun updateEmptyView() {
        if (adapter.itemCount == 0) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // 刷新列表
        adapter.updateData(historyManager.getHistory())
        updateEmptyView()
    }
}
