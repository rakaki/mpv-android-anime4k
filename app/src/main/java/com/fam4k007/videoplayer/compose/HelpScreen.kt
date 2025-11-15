package com.fam4k007.videoplayer.compose

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帮助说明", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本使用
            HelpSection(
                icon = Icons.Default.PlayArrow,
                title = "基本使用",
                content = """
                    • 从主页选择视频文件夹
                    • 点击视频即可播放
                    • 支持记忆播放进度
                    • 左右滑动快进/快退
                    • 上下滑动调节音量/亮度
                """.trimIndent()
            )

            // 播放设置
            HelpSection(
                icon = Icons.Default.Settings,
                title = "播放设置",
                content = """
                    • 精准搜索：启用后可更准确地跳转到指定位置
                    • 音量增强：提升音量上限（需谨慎使用）
                    • 快进时间：自定义双击快进/快退的时长
                    • 长按倍速：设置长按屏幕的播放速度
                """.trimIndent()
            )

            // Anime4K 设置
            HelpSection(
                icon = Icons.Default.AutoAwesome,
                title = "Anime4K 画质增强",
                content = """
                    • Mode A：适合1080P原盘降噪
                    • Mode B：适合720P提升至1080P
                    • Mode C：适合480P提升至1080P
                    • Mode A+A：极致降噪模式
                    • Mode B+B：极致提升模式
                    
                    注意：画质增强会增加性能消耗
                """.trimIndent()
            )

            // 字幕设置
            HelpSection(
                icon = Icons.Default.Subtitles,
                title = "字幕功能",
                content = """
                    • 自动加载同名字幕文件
                    • 支持 .ass .srt .vtt 格式
                    • 可在播放界面选择字幕轨道
                    • 支持字幕延迟调整
                """.trimIndent()
            )

            // 弹幕功能
            HelpSection(
                icon = Icons.Default.Comment,
                title = "弹幕功能",
                content = """
                    • 支持B站弹幕下载
                    • 自动匹配同名弹幕文件
                    • 可调整弹幕样式和透明度
                    • 支持弹幕屏蔽关键词
                """.trimIndent()
            )

            // 常见问题
            HelpSection(
                icon = Icons.Default.Help,
                title = "常见问题",
                content = """
                    Q: 为什么视频无法播放？
                    A: 请确保视频格式受支持（mp4/mkv/avi等）
                    
                    Q: 如何加载字幕？
                    A: 将字幕文件与视频放在同一文件夹，且文件名相同
                    
                    Q: Anime4K 没有效果？
                    A: 请确保设备性能足够，并尝试降低画质增强等级
                    
                    Q: 弹幕显示异常？
                    A: 尝试重新下载弹幕文件或调整弹幕设置
                """.trimIndent()
            )

            // 快捷键说明
            HelpSection(
                icon = Icons.Default.Keyboard,
                title = "手势操作",
                content = """
                    • 单击：显示/隐藏控制栏
                    • 双击：播放/暂停
                    • 左右滑动：快进/快退
                    • 上下滑动（左侧）：调节亮度
                    • 上下滑动（右侧）：调节音量
                    • 长按：快速播放（2x-3.5x可调）
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun HelpSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsPalette.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SettingsPalette.IconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsPalette.PrimaryText
                    )
            }

            Text(
                text = content,
                fontSize = 14.sp,
                color = SettingsPalette.SecondaryText,
                lineHeight = 20.sp
            )
        }
    }
}
