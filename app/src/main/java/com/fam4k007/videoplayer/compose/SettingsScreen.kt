package com.fam4k007.videoplayer.compose

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.*
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.utils.ThemeManager
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette

/**
 * Compose 版本的设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme = remember { mutableStateOf(ThemeManager.getCurrentTheme(context)) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            SettingsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 历史记录分组
            item {
                SettingsSectionHeader(title = "历史记录")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.History,
                    title = "播放历史记录",
                    subtitle = "查看最近播放的视频",
                    onClick = {
                        context.startActivity(Intent(context, PlaybackHistoryComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_bottom,
                            R.anim.no_anim
                        )
                    }
                )
            }
            
            // 外观设置分组
            item {
                SettingsSectionHeader(title = "外观")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Palette,
                    title = "主题设置",
                    subtitle = "当前: ${currentTheme.value.themeName}",
                    onClick = { showThemeDialog = true }
                )
            }
            
            // 播放设置分组
            item {
                SettingsSectionHeader(title = "播放")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Settings,
                    title = "播放设置",
                    subtitle = "调整播放相关参数",
                    onClick = {
                        context.startActivity(Intent(context, PlaybackSettingsComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_bottom,
                            R.anim.no_anim
                        )
                    }
                )
            }
            
            // 弹幕设置分组
            item {
                SettingsSectionHeader(title = "弹幕")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Comment,
                    title = "哔哩哔哩弹幕下载",
                    subtitle = "下载B站视频弹幕",
                    onClick = {
                        context.startActivity(Intent(context, BiliBiliDanmakuComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_bottom,
                            R.anim.no_anim
                        )
                    }
                )
            }
            
            // 下载分组
            item {
                SettingsSectionHeader(title = "下载")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Download,
                    title = "哔哩哔哩视频下载",
                    subtitle = "下载B站视频/番剧",
                    onClick = {
                        context.startActivity(Intent(context, DownloadActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    }
                )
            }
            
            // 其他设置分组
            item {
                SettingsSectionHeader(title = "其他")
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Help,
                    title = "使用说明",
                    subtitle = "查看功能介绍和使用帮助",
                    onClick = {
                        context.startActivity(Intent(context, HelpComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.fade_in,
                            R.anim.fade_out
                        )
                    }
                )
            }
            
            item {
                SettingsCard(
                    icon = Icons.Default.Info,
                    title = "关于",
                    subtitle = "应用信息与许可",
                    onClick = {
                        context.startActivity(Intent(context, AboutComposeActivity::class.java))
                        (context as? android.app.Activity)?.overridePendingTransition(
                            R.anim.slide_in_bottom,
                            R.anim.no_anim
                        )
                    }
                )
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme.value,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                ThemeManager.setTheme(context, theme)
                currentTheme.value = theme
                showThemeDialog = false
                // 重启 Activity 应用主题
                (context as? android.app.Activity)?.recreate()
            }
        )
    }
}

/**
 * 顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * 设置分组标题
 */
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = SettingsPalette.SectionHeaderText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        letterSpacing = 0.05.sp
    )
}

/**
 * 设置卡片项
 */
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SettingsPalette.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))  // 大圆角方形
                    .background(SettingsPalette.IconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SettingsPalette.PrimaryText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = SettingsPalette.SecondaryText
                )
            }
            
            // 箭头
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 主题选择对话框
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeManager.Theme,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeManager.Theme) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
                Text(
                    text = "选择主题",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsPalette.PrimaryText
                )
        },
        text = {
            Column {
                ThemeManager.Theme.values().forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onSelect = { selectedTheme = theme }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onThemeSelected(selectedTheme) }
            ) {
                Text("确定", color = SettingsPalette.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SettingsPalette.SecondaryText)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = SettingsPalette.DialogSurface
    )
}

/**
 * 主题选项
 */
@Composable
fun ThemeOption(
    theme: ThemeManager.Theme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .background(
                if (isSelected) SettingsPalette.Highlight 
                else Color.Transparent
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
                 text = theme.themeName,
                 fontSize = 16.sp,
                 color = if (isSelected) SettingsPalette.AccentText 
                     else SettingsPalette.PrimaryText
        )
    }
}
