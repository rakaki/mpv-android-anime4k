package com.fam4k007.videoplayer.compose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliBiliDanmakuScreen(
    savedFolderUri: Uri?,
    onBack: () -> Unit,
    onFolderSelected: (Uri) -> Unit,
    onDownloadDanmaku: (String, Boolean, (String) -> Unit) -> Unit
) {
    var currentFolderUri by remember { mutableStateOf(savedFolderUri) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onFolderSelected(it)
            currentFolderUri = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("B站弹幕下载", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF667eea),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 设置保存文件夹
            SettingCard(
                icon = Icons.Default.Folder,
                title = "设置保存文件夹",
                subtitle = currentFolderUri?.let { 
                    it.lastPathSegment?.substringAfter(':') ?: "已设置" 
                } ?: "点击选择文件夹",
                onClick = { folderPickerLauncher.launch(currentFolderUri) }
            )

            // 下载弹幕
            SettingCard(
                icon = Icons.Default.CloudDownload,
                title = "下载弹幕",
                subtitle = if (currentFolderUri != null) "点击输入视频链接" else "请先设置保存文件夹",
                onClick = {
                    if (currentFolderUri != null) {
                        showDownloadDialog = true
                    }
                },
                enabled = currentFolderUri != null
            )

            // 使用说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF667eea)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = """
                            1. 先设置弹幕保存文件夹
                            2. 输入B站视频或番剧链接
                            3. 选择下载模式（单集或整季）
                            4. 点击下载，等待完成
                            
                            支持的链接格式：
                            • https://www.bilibili.com/video/BVxxx
                            • https://www.bilibili.com/bangumi/play/epxxx
                        """.trimIndent(),
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    // 下载对话框
    if (showDownloadDialog) {
        DownloadDialog(
            onDismiss = { showDownloadDialog = false },
            onDownload = { url, downloadWholeSeason ->
                showDownloadDialog = false
                onDownloadDanmaku(url, downloadWholeSeason) { }
            }
        )
    }
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x15667eea)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF667eea),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color(0xFF222222)
                           else Color(0xFFBBBBBB)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = if (enabled) Color(0xFF666666) 
                           else Color(0xFFBBBBBB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadDialog(
    onDismiss: () -> Unit,
    onDownload: (String, Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var downloadWholeSeason by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载B站弹幕", color = Color(0xFF222222)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("视频链接") },
                    placeholder = { Text("输入B站视频或番剧链接") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("下载模式", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF222222))
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = downloadWholeSeason,
                        onClick = { downloadWholeSeason = true }
                    )
                    Text("整季下载", modifier = Modifier.padding(start = 8.dp), color = Color(0xFF444444))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !downloadWholeSeason,
                        onClick = { downloadWholeSeason = false }
                    )
                    Text("单集下载", modifier = Modifier.padding(start = 8.dp), color = Color(0xFF444444))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onDownload(url.trim(), downloadWholeSeason)
                    }
                },
                enabled = url.isNotBlank()
            ) {
                Text("下载", color = Color(0xFF667eea))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF888888))
            }
        },
        containerColor = Color.White
    )
}
