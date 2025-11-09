package com.fam4k007.videoplayer.danmaku

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * B站弹幕下载Compose对话框
 * 包含文件夹选择、链接输入和下载状态显示
 */
@Composable
fun BiliBiliDanmakuDownloadDialog(
    onDismiss: () -> Unit,
    onDownloadComplete: (String) -> Unit,
    savedDirectoryUri: Uri?,
    onRequestFolderPicker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var currentDirectoryUri by remember { mutableStateOf(savedDirectoryUri) }
    var videoUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFolderSelector by remember { mutableStateOf(savedDirectoryUri == null) }
    
    val downloadManager = remember { BiliBiliDanmakuDownloadManager(context) }
    
    // 监听savedDirectoryUri的变化并更新状态
    LaunchedEffect(savedDirectoryUri) {
        currentDirectoryUri = savedDirectoryUri
        showFolderSelector = savedDirectoryUri == null
    }
    
    // 开始下载
    fun startDownload() {
        if (videoUrl.isBlank()) {
            errorMessage = "请输入视频链接"
            return
        }
        
        if (!downloadManager.isValidBilibiliUrl(videoUrl)) {
            errorMessage = "请输入有效的B站视频链接"
            return
        }
        
        val dirUri = currentDirectoryUri
        if (dirUri == null) {
            errorMessage = "请先选择保存文件夹"
            return
        }
        
        isDownloading = true
        errorMessage = null
        
        scope.launch {
            when (val result = downloadManager.downloadDanmaku(videoUrl, dirUri)) {
                is BiliBiliDanmakuDownloadManager.DownloadResult.Success -> {
                    isDownloading = false
                    onDownloadComplete(result.fileName)
                    onDismiss()
                }
                is BiliBiliDanmakuDownloadManager.DownloadResult.Error -> {
                    isDownloading = false
                    errorMessage = result.message
                }
            }
        }
    }
    
    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "哔哩哔哩弹幕下载",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                when {
                    // 需要选择文件夹
                    showFolderSelector -> {
                        FolderSelectorContent(
                            onSelectFolder = { onRequestFolderPicker() },
                            onCancel = onDismiss
                        )
                    }
                    
                    // 正在下载
                    isDownloading -> {
                        DownloadingContent()
                    }
                    
                    // 输入链接
                    else -> {
                        InputUrlContent(
                            videoUrl = videoUrl,
                            onUrlChange = { 
                                videoUrl = it
                                errorMessage = null
                            },
                            errorMessage = errorMessage,
                            onDownload = { startDownload() },
                            onChangeFolder = { showFolderSelector = true },
                            onCancel = onDismiss
                        )
                    }
                }
            }
        }
    }
}

/**
 * 文件夹选择界面
 */
@Composable
private fun FolderSelectorContent(
    onSelectFolder: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = "📁",
            fontSize = 48.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "请选择弹幕保存文件夹",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "弹幕将以视频标题命名保存为 .xml 文件",
            fontSize = 13.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text("取消")
            }
            
            Button(
                onClick = onSelectFolder,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A1D6)
                )
            ) {
                Text("选择文件夹")
            }
        }
    }
}

/**
 * 输入链接界面
 */
@Composable
private fun InputUrlContent(
    videoUrl: String,
    onUrlChange: (String) -> Unit,
    errorMessage: String?,
    onDownload: () -> Unit,
    onChangeFolder: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Text(
            text = "请输入视频链接",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = videoUrl,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "https://www.bilibili.com/video/BV...",
                    fontSize = 14.sp,
                    color = Color(0xFFCCCCCC)
                )
            },
            singleLine = true,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDownload() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00A1D6),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )
        
        // 错误提示
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = Color(0xFFE53935)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 更改文件夹按钮
        TextButton(
            onClick = onChangeFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📁 更改保存文件夹",
                fontSize = 13.sp,
                color = Color(0xFF00A1D6)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text("取消")
            }
            
            Button(
                onClick = onDownload,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00A1D6)
                )
            ) {
                Text("开始下载")
            }
        }
    }
}

/**
 * 下载中界面（带旋转动画）
 */
@Composable
private fun DownloadingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        // 旋转的圆圈
        Box(
            modifier = Modifier
                .size(64.dp)
                .rotate(rotation)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF00A1D6),
                strokeWidth = 4.dp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "正在下载弹幕...",
            fontSize = 16.sp,
            color = Color(0xFF666666)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请稍候",
            fontSize = 13.sp,
            color = Color(0xFF999999)
        )
    }
}
