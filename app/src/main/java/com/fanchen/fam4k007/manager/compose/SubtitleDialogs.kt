package com.fanchen.fam4k007.manager.compose

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 右侧抽屉式字幕设置面板
 */
@Composable
fun SubtitleSettingsDrawer(
    currentDelay: Double,
    currentScale: Float,
    currentPosition: Int,
    currentBorderSize: Int,
    onDelayChange: (Double) -> Unit,
    onScaleChange: (Float) -> Unit,
    onPositionChange: (Int) -> Unit,
    onBorderSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // 启动时触发动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 点击背景关闭
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { 
                isVisible = false
                coroutineScope.launch {
                    kotlinx.coroutines.delay(300)
                    onDismiss() 
                }
            }
    ) {
        // 右侧抽屉
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .drawBehind {
                        // 综合左右和上下的渐变效果
                        val horizontalBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x59121212), // 左边缘 35% 不透明 (0x59 = 89/255 ≈ 35%)
                                Color(0x99121212)  // 右边缘 60% 不透明 (0x99 = 153/255 ≈ 60%)
                            )
                        )
                        val verticalBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x59121212), // 上边缘 35% 不透明
                                Color(0x99121212)  // 下边缘 60% 不透明
                            )
                        )
                        // 绘制水平渐变
                        drawRect(brush = horizontalBrush)
                        // 叠加垂直渐变（使用较低的透明度避免过黑）
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x00000000), // 上边缘透明
                                    Color(0x40121212)  // 下边缘轻微加深
                                )
                            )
                        )
                    }
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止点击穿透 */ }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 标题
                    Text(
                        text = "更多设置",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Divider(
                        color = Color(0x33FFFFFF),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // 使用LazyColumn实现可滚动列表，最多显示3个，支持未来扩展
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp) // 增大间距
                    ) {
                        // 字幕延迟设置
                        item {
                            ExpandableSection(
                                title = "字幕延迟设置",
                                isExpanded = expandedSection == "delay",
                                onToggle = { expandedSection = if (expandedSection == "delay") null else "delay" }
                            ) {
                                SubtitleDelayContent(
                                    currentDelay = currentDelay,
                                    onDelayChange = onDelayChange
                                )
                            }
                        }
                        
                        // 字幕样式设置
                        item {
                            ExpandableSection(
                                title = "字幕样式设置",
                                isExpanded = expandedSection == "style",
                                onToggle = { expandedSection = if (expandedSection == "style") null else "style" }
                            ) {
                                SubtitleStyleContent(
                                    currentBorderSize = currentBorderSize,
                                    onBorderSizeChange = onBorderSizeChange
                                )
                            }
                        }
                        
                        // 字幕大小位置设置
                        item {
                            ExpandableSection(
                                title = "字幕大小位置设置",
                                isExpanded = expandedSection == "misc",
                                onToggle = { expandedSection = if (expandedSection == "misc") null else "misc" }
                            ) {
                                SubtitleMiscContent(
                                    currentScale = currentScale,
                                    currentPosition = currentPosition,
                                    onScaleChange = onScaleChange,
                                    onPositionChange = onPositionChange
                                )
                            }
                        }
                        
                        // 字体设置
                        item {
                            ExpandableSection(
                                title = "字体设置",
                                isExpanded = expandedSection == "font",
                                onToggle = { expandedSection = if (expandedSection == "font") null else "font" }
                            ) {
                                SubtitleFontContent()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 可展开折叠的分组
 */
@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x1AFFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▶",
                fontSize = 14.sp,
                color = Color(0xFF64B5F6),
                modifier = Modifier.rotate(rotationAngle)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        // 内容区域 - 优化动画性能
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 字幕延迟内容
 */
@Composable
fun SubtitleDelayContent(
    currentDelay: Double,
    onDelayChange: (Double) -> Unit
) {
    var delay by remember { mutableStateOf(currentDelay) }
    var inputText by remember { mutableStateOf(String.format("%.2f", currentDelay)) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 当前值显示（可编辑）
        androidx.compose.foundation.text.BasicTextField(
            value = inputText,
            onValueChange = { newValue ->
                inputText = newValue
                newValue.toDoubleOrNull()?.let { newDelay ->
                    if (newDelay in -60.0..60.0) { // 限制范围
                        delay = newDelay
                        onDelayChange(newDelay)
                    }
                }
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64B5F6),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                .padding(12.dp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        )
        
        Text(
            text = "范围: -60.0 ~ +60.0 秒",
            fontSize = 11.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 对称布局：上下两行
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 第一行：-0.5s 和 +0.5s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickButton("-0.5s") {
                    delay -= 0.5
                    delay = delay.coerceIn(-60.0, 60.0)
                    inputText = String.format("%.2f", delay)
                    onDelayChange(delay)
                }
                QuickButton("+0.5s") {
                    delay += 0.5
                    delay = delay.coerceIn(-60.0, 60.0)
                    inputText = String.format("%.2f", delay)
                    onDelayChange(delay)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：-0.1s 和 +0.1s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickButton("-0.1s") {
                    delay -= 0.1
                    delay = delay.coerceIn(-60.0, 60.0)
                    inputText = String.format("%.2f", delay)
                    onDelayChange(delay)
                }
                QuickButton("+0.1s") {
                    delay += 0.1
                    delay = delay.coerceIn(-60.0, 60.0)
                    inputText = String.format("%.2f", delay)
                    onDelayChange(delay)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 重置按钮
        TextButton(
            onClick = {
                delay = 0.0
                inputText = "0.00"
                onDelayChange(0.0)
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF64B5F6)
            )
        ) {
            Text("重置为 0")
        }
    }
}

/**
 * 字幕样式内容
 */
@Composable
fun SubtitleStyleContent(
    currentBorderSize: Int,
    onBorderSizeChange: (Int) -> Unit
) {
    var borderSize by remember { mutableStateOf(currentBorderSize.toFloat()) }

    Column {
        Text(
            text = "边框大小：${borderSize.toInt()}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = borderSize,
            onValueChange = {
                borderSize = it
                onBorderSizeChange(it.toInt())
            },
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                borderSize = 3f
                onBorderSizeChange(3)
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF64B5F6)
            )
        ) {
            Text("重置为 3")
        }
    }
}

/**
 * 字幕大小位置内容
 */
@Composable
fun SubtitleMiscContent(
    currentScale: Float,
    currentPosition: Int,
    onScaleChange: (Float) -> Unit,
    onPositionChange: (Int) -> Unit
) {
    var scale by remember { mutableStateOf(currentScale) }
    var position by remember { mutableStateOf(currentPosition.toFloat()) }
    
    // 使用LaunchedEffect监听外部状态变化
    LaunchedEffect(currentScale) {
        scale = currentScale
    }
    
    LaunchedEffect(currentPosition) {
        position = currentPosition.toFloat()
    }
    
    // 添加动画 - 优化性能
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Column {
        // 字幕大小
        Text(
            text = "字幕大小：${(animatedScale * 100).toInt()}%",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "范围: 50% ~ 300%",
            fontSize = 11.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = scale,
            onValueChange = {
                scale = it
                onScaleChange(it)
            },
            valueRange = 0.5f..3.0f,
            steps = 24,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 字幕垂直位置
        Text(
            text = "字幕垂直位置：${position.toInt()}",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "范围: -100 (上移) ~ +100 (下移)",
            fontSize = 11.sp,
            color = Color(0x99FFFFFF),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = position,
            onValueChange = {
                position = it
                onPositionChange(it.toInt())
            },
            valueRange = -100f..100f,
            steps = 199,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6),
                inactiveTrackColor = Color(0xFF555555)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                scale = 1.0f
                position = 100f
                onScaleChange(1.0f)
                onPositionChange(100)
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF64B5F6)
            )
        ) {
            Text("重置默认值 (100%, 位置100)")
        }
    }
}

/**
 * 快捷按钮
 */
@Composable
fun QuickButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64B5F6))
    ) {
        Text(text, fontSize = 14.sp)
    }
}

/**
 * 字体设置内容
 */
@Composable
fun SubtitleFontContent() {
    val context = LocalContext.current
    
    Column {
        Button(
            onClick = {
                Toast.makeText(context, "正在开发中", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF64B5F6),
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("更改字体")
        }
    }
}

// 保留旧的Dialog组件（兼容性）
@Composable
fun SubtitleDelayDialog(
    currentDelay: Double,
    onDelayChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    // 已废弃，保留以防其他地方调用
}

@Composable
fun SubtitleMiscDialog(
    currentScale: Float,
    currentPosition: Int,
    onScaleChange: (Float) -> Unit,
    onPositionChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 已废弃
}

@Composable
fun SubtitleStyleDialog(
    currentBorderSize: Int,
    onBorderSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 已废弃
}
