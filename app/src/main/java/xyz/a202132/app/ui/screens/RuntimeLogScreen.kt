package xyz.a202132.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import xyz.a202132.app.ui.components.AppScreenScaffold
import xyz.a202132.app.ui.theme.Primary
import xyz.a202132.app.util.RuntimeLog
import xyz.a202132.app.util.RuntimeLogEntry
import xyz.a202132.app.util.RuntimeLogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RuntimeLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entries by RuntimeLog.entries.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    AppScreenScaffold(
        title = "运行日志",
        onBack = onBack,
        actions = {
            IconButton(
                onClick = {
                    RuntimeLog.refresh()
                    Toast.makeText(context, "运行日志已刷新", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "刷新",
                    tint = Primary
                )
            }

            IconButton(
                onClick = {
                    RuntimeLog.info("RuntimeLog", "User requested runtime log share")
                    val file = RuntimeLog.exportToCacheFile(context)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, file.name)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "分享运行日志"))
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "分享",
                    tint = Primary
                )
            }

            IconButton(
                onClick = { showClearConfirm = true }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "清空",
                    tint = Primary
                )
            }
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "暂无运行日志",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries.asReversed(), key = { it.id }) { entry ->
                        RuntimeLogEntryCard(entry = entry)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空运行日志") },
            text = { Text("清空后当前设备上的运行日志记录会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        RuntimeLog.clear()
                        showClearConfirm = false
                        Toast.makeText(context, "运行日志已清空", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun RuntimeLogEntryCard(entry: RuntimeLogEntry) {
    val levelColor = when (entry.level) {
        RuntimeLogLevel.INFO -> MaterialTheme.colorScheme.primary
        RuntimeLogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        RuntimeLogLevel.ERROR -> MaterialTheme.colorScheme.error
    }
    val timeText = remember(entry.timestampMillis) {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(entry.timestampMillis))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.level.name} · ${entry.component}",
                    color = levelColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Text(
                text = entry.message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
