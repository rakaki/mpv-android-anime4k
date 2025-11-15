package com.fam4k007.videoplayer.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
            // App 信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App 图标
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x15667eea)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFF667eea),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "FAM4K007",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Version $versionName",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "基于 MPV 的 Anime4K 视频播放器",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // 功能列表
            AboutItem(
                icon = Icons.Default.Info,
                title = "关于应用",
                subtitle = "查看详细信息",
                onClick = { showAboutDialog = true }
            )

            AboutItem(
                icon = Icons.Default.Code,
                title = "开源主页",
                subtitle = "访问 GitHub 仓库",
                onClick = {
                    openUrl(context, "https://github.com/azxcvn/mpv-android-anime4k")
                }
            )

            AboutItem(
                icon = Icons.Default.Description,
                title = "许可证书",
                subtitle = "查看开源许可",
                onClick = onNavigateToLicense
            )

            AboutItem(
                icon = Icons.Default.Email,
                title = "联系作者",
                subtitle = "发送反馈和建议",
                onClick = {
                    openUrl(context, "mailto:your-email@example.com")
                }
            )

            AboutItem(
                icon = Icons.Default.BugReport,
                title = "意见反馈",
                subtitle = "报告问题或建议",
                onClick = onNavigateToFeedback
            )

            // 技术栈
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
                        text = "技术栈",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF667eea)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = """
                            • MPV - 强大的媒体播放引擎
                            • Anime4K - 实时画质增强
                            • Jetpack Compose - 现代化 UI
                            • Kotlin Coroutines - 异步处理
                            • Material Design 3 - 设计规范
                        """.trimIndent(),
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    // 关于详情对话框
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { 
                Text(
                    "FAM4K007",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                ) 
            },
            text = {
                Column {
                    Text(
                        text = "Version $versionName",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = """
                            这是一款基于 MPV 和 Anime4K 的高品质视频播放器，专为动漫爱好者打造。
                            
                            主要特性：
                            • 实时画质增强（Anime4K）
                            • 支持多种视频格式
                            • B站弹幕支持
                            • 字幕自动加载
                            • 播放历史记忆
                            • Material Design 3 设计
                            
                            感谢所有开源项目的贡献！
                        """.trimIndent(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF444444)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("关闭", color = Color(0xFF667eea))
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun AboutItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
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
                    color = Color(0xFF222222)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
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

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}
