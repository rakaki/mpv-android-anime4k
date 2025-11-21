package com.fam4k007.videoplayer.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.PlaybackHistoryManager
import com.fam4k007.videoplayer.VideoBrowserActivity
import com.fam4k007.videoplayer.VideoPlayerActivity
import com.fam4k007.videoplayer.BiliBiliPlayActivity
import com.fam4k007.videoplayer.webdav.WebDavBrowserActivity
import com.fam4k007.videoplayer.webdav.WebDavConfig
import com.fam4k007.videoplayer.webdav.WebDavConfigDialog
import com.fanchen.fam4k007.manager.compose.BiliBiliLoginActivity

/**
 * Compose 版本的主页
 */
@Composable
fun HomeScreen(
    historyManager: PlaybackHistoryManager,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isExpanded by remember { mutableStateOf(false) }
    
    // 监听生命周期，返回时自动收起
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isExpanded = false // 返回时自动收起
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFE8EAF6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部区域
            TopBar(
                onLoginClick = {
                    context.startActivity(Intent(context, BiliBiliLoginActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_bottom,
                        R.anim.no_anim
                    )
                },
                onSettingsClick = onNavigateToSettings
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo 区域（顶部偏下）
            LogoSection(
                historyManager = historyManager,
                onContinuePlay = { lastVideo ->
                    continueLastPlay(context, lastVideo)
                }
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // 播放本地视频按钮（给文本留出空间）
            GradientButton(
                text = "播放本地视频",
                onClick = {
                    context.startActivity(Intent(context, VideoBrowserActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // 右下角展开/收起按钮和功能区
        ExpandableActionButton(
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
            onBiliBiliClick = {
                context.startActivity(Intent(context, BiliBiliPlayActivity::class.java))
                (context as? android.app.Activity)?.overridePendingTransition(
                    R.anim.scale_in,
                    R.anim.scale_out
                )
            },
            onWebDavClick = {
                if (WebDavConfig.isConfigured(context)) {
                    context.startActivity(Intent(context, WebDavBrowserActivity::class.java))
                    (context as? android.app.Activity)?.overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                } else {
                    WebDavConfigDialog(context, lifecycleOwner.lifecycleScope) { config ->
                        context.startActivity(Intent(context, WebDavBrowserActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }.show()
                }
            }
        )
    }
}

/**
 * 顶部栏
 */
@Composable
fun TopBar(
    onLoginClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左上角登录按钮
        IconWithBackground(
            icon = Icons.Default.Person,
            contentDescription = "登录",
            onClick = onLoginClick
        )
        
        // 中间标题
        Text(
            text = "小喵Player",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
        )
        
        // 右上角设置按钮
        IconWithBackground(
            icon = Icons.Default.Settings,
            contentDescription = "设置",
            onClick = onSettingsClick
        )
    }
}

/**
 * Logo 区域（可点击继续播放）
 */
@Composable
fun LogoSection(
    historyManager: PlaybackHistoryManager,
    onContinuePlay: (PlaybackHistoryManager.HistoryItem) -> Unit
) {
    val context = LocalContext.current
    val lastVideo = historyManager.getLastPlayedLocalVideo()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo 图片（直接显示图标，不带背景框）
        Icon(
            painter = painterResource(id = R.drawable.ic_continue_play),
            contentDescription = "继续播放",
            modifier = Modifier
                .size(120.dp)
                .clickable {
                    val video = historyManager.getLastPlayedLocalVideo()
                    if (video != null) {
                        onContinuePlay(video)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "暂无播放记录",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
            tint = Color.Unspecified
        )
        
        // 提示文字（仅在有播放记录时显示）
        if (lastVideo != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "继续播放: ${lastVideo.fileName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 16.dp),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 渐变按钮
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * 可展开的操作按钮
 */
@Composable
fun ExpandableActionButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onBiliBiliClick: () -> Unit,
    onWebDavClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // 展开的功能区
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(300)) + 
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + 
                       shrinkVertically(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .wrapContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 哔哩哔哩番剧
                        ActionItem(
                            icon = Icons.Default.Tv,
                            label = "哔哩哔哩番剧",
                            onClick = onBiliBiliClick
                        )
                        
                        // WebDAV
                        ActionItem(
                            icon = Icons.Default.Cloud,
                            label = "WebDAV",
                            onClick = onWebDavClick
                        )
                    }
                }
            }
            
            // 展开/收起按钮
            FloatingActionButton(
                onClick = onToggle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(60.dp)
            ) {
                // 旋转动画
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f,
                    animationSpec = tween(300),
                    label = "rotation"
                )
                
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

/**
 * 功能项（图标 + 文字）
 */
@Composable
fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // 图标背景（参考设置页样式）
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

/**
 * 带背景的图标按钮（参考设置页样式）
 */
@Composable
fun IconWithBackground(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFBBDEFB)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 继续播放功能
 */
private fun continueLastPlay(
    context: android.content.Context,
    lastVideo: PlaybackHistoryManager.HistoryItem
) {
    try {
        val videoUri = Uri.parse(lastVideo.uri)
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            data = videoUri
            action = Intent.ACTION_VIEW
            putExtra("folder_path", lastVideo.folderName)
            putExtra("last_position", lastVideo.position)
        }
        
        context.startActivity(intent)
        (context as? android.app.Activity)?.overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "无法播放该视频: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
