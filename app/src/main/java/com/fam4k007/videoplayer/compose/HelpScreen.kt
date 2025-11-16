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
                    • 屏幕左侧与右侧上下滑动可调节音量/亮度
                """.trimIndent()
            )

            // 播放设置
            HelpSection(
                icon = Icons.Default.Settings,
                title = "播放设置",
                content = """
                    • 精确进度定位:开启后拖动进度条时会更准确但是响应会稍慢
                    • 音量增强：提升音量上限（需谨慎使用）
                    • 快进/快退时长:自定义前进后退按钮的跳转时间
                    • 长按倍速：设置长按屏幕的播放速度
                """.trimIndent()
            )

            // Anime4K 设置
            HelpSection(
                icon = Icons.Default.AutoAwesome,
                title = "Anime4K 画质增强",
                content = """
                    • Mode A：1080P用这个模式！最常用的模式！
                    
                    • Mode B：720P用这个模式！不怎么用到！
                    
                    • Mode C：480P用这个！不怎么用到！
                    
                    • Mode A+：1080P的增强版！极致超分4K画质，
                      最强版本！但性能消耗巨大！
                    
                    • Mode B+：720P的增强版！不怎么用到！
                    
                    • Mode C+：480P的增强版！不怎么用到！
                    
                    注意：画质增强会增加性能消耗
                """.trimIndent()
            )

            // 字幕设置
            HelpSection(
                icon = Icons.Default.Subtitles,
                title = "字幕功能",
                content = """
                    • 可导入外部字幕文件
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

            // 手势操作
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

            // 常见问题
            HelpSection(
                icon = Icons.Default.Help,
                title = "常见问题",
                content = """
                    Q：为什么视频无法播放？
                    A：请注意视频格式，本播放器可能有些格式不支持
                    
                    Q：为什么开了超分以后手机发热严重？
                    A：超分的工作原理就是利用手机处理器实时运算并且
                    反馈到屏幕上，所以相当于你的处理器在不停的高强度
                    工作，为避免严重发热，请使用超分的模式A！尽量减少
                    模式A+的使用！
                    
                    Q：为什么软件会卡死崩溃？
                    A：由于开发者精力有限，优化层面做的一般，如遇到
                    崩溃请重启软件，并且请确保有足够的运行空间！
                    
                    Q：为什么哔哩哔哩的番剧解析有时候第一集无法播放？
                    A：无法定位到原因！请更换番剧观看！
                    
                    Q：为什么哔哩哔哩番剧解析有时候返回的清晰度不正确？
                    A：随机BUG！根本原因不明！若返回了错误的清晰度，
                    请使用本播放器的超分功能进行超分！
                    
                    Q：软件的使用有什么建议？
                    A：一定不要同时开启弹幕与超分与在线观看视频！这三样
                    都是利用你的手机处理器在实时运算，发热量大大增加！
                    请按需选择！或者自行解决散热问题！
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
