package com.fam4k007.videoplayer.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fam4k007.videoplayer.compose.SettingsColors as SettingsPalette

/**
 * Compose 许可证书列表界面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    licenses: List<LicenseItem>,
    onBack: () -> Unit
) {
    var selectedLicense by remember { mutableStateOf<LicenseItem?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("许可证书", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsPalette.ScreenBackground)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(licenses) { license ->
                LicenseCard(license = license) { selectedLicense = license }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    selectedLicense?.let { license ->
        LicenseDetailDialog(
            license = license,
            onDismiss = { selectedLicense = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseCard(
    license: LicenseItem,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsPalette.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = license.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPalette.PrimaryText,
                    modifier = Modifier.weight(1f)
                )

                AssistChip(
                    onClick = onClick,
                    label = { Text(license.licenseTag) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SettingsPalette.Highlight,
                        labelColor = SettingsPalette.AccentText
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = license.summary,
                fontSize = 14.sp,
                color = SettingsPalette.SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = license.website,
                    fontSize = 13.sp,
                    color = SettingsPalette.AccentText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LicenseDetailDialog(
    license: LicenseItem,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = SettingsPalette.AccentText)
            }
        },
        title = {
            Column {
                Text(license.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SettingsPalette.PrimaryText)
                Text(license.licenseTag, fontSize = 13.sp, color = SettingsPalette.SecondaryText)
            }
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SettingsPalette.Highlight)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = license.website,
                                color = SettingsPalette.AccentText,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { uriHandler.openUri(license.website) }) {
                                Icon(
                                    imageVector = Icons.Default.TextSnippet,
                                    contentDescription = "打开官网",
                                    tint = primaryColor
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "许可证全文",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsPalette.PrimaryText
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SettingsPalette.DialogSurface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = license.licenseText,
                        fontSize = 13.sp,
                        color = SettingsPalette.SecondaryText,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = SettingsPalette.DialogBackground
    )
}

/**
 * 许可证展示数据。
 */
data class LicenseItem(
    val name: String,
    val licenseTag: String,
    val summary: String,
    val website: String,
    val licenseText: String
)
