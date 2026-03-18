package xyz.a202132.app.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Masks
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.a202132.app.AppConfig
import xyz.a202132.app.NetworkTool
import xyz.a202132.app.ui.components.AppScreenScaffold
import xyz.a202132.app.ui.theme.Primary

@Composable
fun NetworkToolboxScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    AppScreenScaffold(
        title = "网络工具箱",
        subtitle = "常用网络检测与查询工具",
        onBack = onBack
    ) {
        NetworkToolboxContent(
            onOpenTool = { tool ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tool.url))
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun NetworkToolboxContent(
    onOpenTool: (NetworkTool) -> Unit
) {
    val tools = remember { AppConfig.getNetworkTools() }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools) { tool ->
                NetworkToolCard(
                    tool = tool,
                    onClick = { onOpenTool(tool) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击卡片后会使用系统浏览器打开对应站点",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NetworkToolCard(
    tool: NetworkTool,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getToolIcon(tool.icon),
                contentDescription = tool.name,
                tint = Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = tool.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = Uri.parse(tool.url).host ?: tool.url,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun getToolIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "outbound" -> Icons.Outlined.TravelExplore
        "ip" -> Icons.Outlined.LocationSearching
        "webrtc" -> Icons.Outlined.Videocam
        "dns" -> Icons.Outlined.Dns
        "check" -> Icons.Outlined.VerifiedUser
        "precision" -> Icons.Outlined.GpsFixed
        "disguise" -> Icons.Outlined.Masks
        "bgp" -> Icons.Outlined.AccountTree
        "speed" -> Icons.Outlined.Speed
        else -> Icons.Outlined.Language
    }
}
