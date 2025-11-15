package com.fam4k007.videoplayer.compose

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.PlaybackHistoryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackHistoryScreen(
    historyManager: PlaybackHistoryManager,
    onBack: () -> Unit,
    onPlayVideo: (Uri, Long) -> Unit
) {
    var historyList by remember { mutableStateOf(historyManager.getHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PlaybackHistoryManager.HistoryItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放历史", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空全部")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF667eea),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(paddingValues)
        ) {
            if (historyList.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF667eea).copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无播放历史",
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                }
            } else {
                // 历史列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = historyList,
                        key = { it.uri }
                    ) { item ->
                        HistoryCard(
                            item = item,
                            onClick = { onPlayVideo(Uri.parse(item.uri), item.position) },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // 清空全部对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空历史", color = Color(0xFF222222)) },
            text = { Text("确定要清空所有播放历史吗？此操作不可恢复。", 
                         color = Color(0xFF444444)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyManager.clearHistory()
                        historyList = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = Color(0xFFe94560))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = Color(0xFF888888))
                }
            },
            containerColor = Color.White
        )
    }

    // 删除单条对话框
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("删除记录", color = Color(0xFF222222)) },
            text = { Text("确定要删除《${item.fileName}》的播放记录吗？", 
                         color = Color(0xFF444444)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyManager.removeHistory(item.uri)
                        historyList = historyManager.getHistory()
                        itemToDelete = null
                    }
                ) {
                    Text("删除", color = Color(0xFFe94560))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("取消", color = Color(0xFF888888))
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun HistoryCard(
    item: PlaybackHistoryManager.HistoryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x15667eea)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中间信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.fileName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(item.position),
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimestamp(item.lastPlayed),
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            // 右侧删除按钮
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFe94560).copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 格式化播放时长
 */
private fun formatDuration(positionMs: Long): String {
    val seconds = positionMs / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("已播放 %02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("已播放 %02d:%02d", minutes, secs)
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestampMs: Long): String {
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestampMs))
}
