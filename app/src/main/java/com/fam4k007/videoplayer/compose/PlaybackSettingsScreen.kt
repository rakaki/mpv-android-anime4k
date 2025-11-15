package com.fam4k007.videoplayer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.manager.PreferencesManager

/**
 * Compose 版本的播放设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager.getInstance(context) }
    
    // 状态
    var preciseSeeking by remember { mutableStateOf(preferencesManager.isPreciseSeekingEnabled()) }
    var volumeBoost by remember { mutableStateOf(preferencesManager.isVolumeBoostEnabled()) }
    var seekTime by remember { mutableIntStateOf(preferencesManager.getSeekTime()) }
    var longPressSpeed by remember { mutableFloatStateOf(preferencesManager.getLongPressSpeed()) }
    var showSeekTimeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("播放设置", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6FA))
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 进度控制
            item {
                SectionHeader("进度控制")
            }
            
            item {
                SwitchSettingCard(
                    title = "精确进度定位",
                    description = if (preciseSeeking) "定位更准确但可能较慢" else "定位更快但使用关键帧",
                    checked = preciseSeeking,
                    onCheckedChange = {
                        preciseSeeking = it
                        preferencesManager.setPreciseSeekingEnabled(it)
                    }
                )
            }
            
            item {
                ClickableSettingCard(
                    title = "快进/快退时长",
                    value = "${seekTime}秒",
                    onClick = { showSeekTimeDialog = true }
                )
            }
            
            // 音量控制
            item {
                SectionHeader("音量控制")
            }
            
            item {
                SwitchSettingCard(
                    title = "音量增强",
                    description = if (volumeBoost) "音量可超过100%,最高300%" else "音量范围限制在1-100%",
                    checked = volumeBoost,
                    onCheckedChange = {
                        volumeBoost = it
                        preferencesManager.setVolumeBoostEnabled(it)
                    }
                )
            }
            
            // 倍速控制
            item {
                SectionHeader("倍速控制")
            }
            
            item {
                SliderSettingCard(
                    title = "长按倍速",
                    value = String.format("%.1fx", longPressSpeed),
                    sliderValue = when (longPressSpeed) {
                        1.5f -> 0f
                        2.0f -> 1f
                        2.5f -> 2f
                        3.0f -> 3f
                        3.5f -> 4f
                        else -> 1f
                    },
                    onValueChange = { progress ->
                        longPressSpeed = when (progress.toInt()) {
                            0 -> 1.5f
                            1 -> 2.0f
                            2 -> 2.5f
                            3 -> 3.0f
                            4 -> 3.5f
                            else -> 2.0f
                        }
                    },
                    onValueChangeFinished = {
                        preferencesManager.setLongPressSpeed(longPressSpeed)
                    }
                )
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    
    // 快进时长选择对话框
    if (showSeekTimeDialog) {
        SeekTimeDialog(
            currentValue = seekTime,
            onDismiss = { showSeekTimeDialog = false },
            onConfirm = { newValue ->
                seekTime = newValue
                preferencesManager.setSeekTime(newValue)
                showSeekTimeDialog = false
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color(0xFF888888),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        letterSpacing = 0.05.sp
    )
}

@Composable
fun SwitchSettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, 
                     color = Color(0xFF222222))
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, 
                     color = Color(0xFF666666))
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun ClickableSettingCard(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, 
                 color = Color(0xFF222222), modifier = Modifier.weight(1f))
            
            Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            
            Spacer(Modifier.width(8.dp))
            
            Icon(Icons.Default.KeyboardArrowRight, null, 
                 tint = Color(0xFFCCCCCC), 
                 modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun SliderSettingCard(
    title: String,
    value: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, 
                     color = Color(0xFF222222), 
                     modifier = Modifier.weight(1f))
                Text(value, fontSize = 15.sp, 
                     color = MaterialTheme.colorScheme.primary, 
                     fontWeight = FontWeight.Medium)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0f..4f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0xFFE0E0E0)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1.5x", fontSize = 12.sp, 
                     color = Color(0xFF999999))
                Text("2.0x", fontSize = 12.sp, 
                     color = Color(0xFF999999))
                Text("2.5x", fontSize = 12.sp, 
                     color = Color(0xFF999999))
                Text("3.0x", fontSize = 12.sp, 
                     color = Color(0xFF999999))
                Text("3.5x", fontSize = 12.sp, 
                     color = Color(0xFF999999))
            }
        }
    }
}

@Composable
fun SeekTimeDialog(
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(currentValue) }
    val options = listOf(3, 5, 10, 15, 20, 25, 30)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快进/快退时长", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = seconds }
                            .background(if (selected == seconds) Color(0xFFE8ECFE) else Color.Transparent)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == seconds,
                            onClick = { selected = seconds },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF667eea))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${seconds}秒",
                            fontSize = 16.sp,
                            color = if (selected == seconds) Color(0xFF667eea) else Color(0xFF333333),
                            fontWeight = if (selected == seconds) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("确定", color = Color(0xFF667eea))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF999999))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
