package xyz.a202132.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.a202132.app.data.model.LatencyLevel
import xyz.a202132.app.data.model.Node
import xyz.a202132.app.ui.theme.LatencyBad
import xyz.a202132.app.ui.theme.LatencyGood
import xyz.a202132.app.ui.theme.LatencyMedium
import xyz.a202132.app.ui.theme.Primary

@Composable
fun NodeListScreen(
    nodes: List<Node>,
    selectedNodeId: String?,
    isTesting: Boolean,
    testingLabel: String? = null,
    onNodeSelected: (Node) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var isClosing by remember { mutableStateOf(false) }

    val handleBack = {
        if (!isClosing) {
            isClosing = true
            onBack()
        }
    }

    val handleNodeSelected: (Node) -> Unit = { node ->
        if (!isClosing) {
            isClosing = true
            onNodeSelected(node)
        }
    }

    AppScreenScaffold(
        title = "节点列表",
        subtitle = testingLabel,
        onBack = handleBack,
        backEnabled = !isClosing,
        actions = {
            NodeListTopActions(
                showSearch = showSearch,
                enabled = !isClosing,
                onToggleSearch = {
                    if (isClosing) return@NodeListTopActions
                    showSearch = !showSearch
                    if (!showSearch) {
                        keyword = ""
                    }
                },
                onRefresh = onRefresh
            )
        }
    ) {
        NodeListContent(
            nodes = nodes,
            selectedNodeId = selectedNodeId,
            isTesting = isTesting,
            showSearch = showSearch,
            keyword = keyword,
            onKeywordChange = { keyword = it },
            onNodeSelected = handleNodeSelected,
            interactionEnabled = !isClosing,
        )
    }
}

@Composable
private fun NodeListTopActions(
    showSearch: Boolean,
    enabled: Boolean,
    onToggleSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    IconButton(
        onClick = onToggleSearch,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "搜索",
            tint = if (showSearch) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
    IconButton(
        onClick = onRefresh,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "刷新",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NodeListContent(
    nodes: List<Node>,
    selectedNodeId: String?,
    isTesting: Boolean,
    showSearch: Boolean,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onNodeSelected: (Node) -> Unit,
    interactionEnabled: Boolean,
) {
    var frozenNodeOrderIds by remember { mutableStateOf<List<String>?>(null) }
    var wasTesting by remember { mutableStateOf(isTesting) }
    var pendingScrollToTop by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(isTesting) {
        if (isTesting) {
            if (frozenNodeOrderIds == null) {
                frozenNodeOrderIds = nodes.map { it.id }
            }
        } else {
            if (wasTesting) {
                pendingScrollToTop = true
            }
            frozenNodeOrderIds = null
        }
        wasTesting = isTesting
    }

    val displayNodes = remember(nodes, isTesting, frozenNodeOrderIds) {
        if (!isTesting || frozenNodeOrderIds == null) {
            nodes
        } else {
            val nodeMap = nodes.associateBy { it.id }
            val frozenNodes = frozenNodeOrderIds.orEmpty().mapNotNull(nodeMap::get)
            val newNodes = nodes.filterNot { it.id in frozenNodeOrderIds.orEmpty() }
            frozenNodes + newNodes
        }
    }

    val filteredNodes = remember(displayNodes, keyword) {
        val query = keyword.trim()
        if (query.isBlank()) {
            displayNodes
        } else {
            displayNodes.filter { node ->
                node.getDisplayName().contains(query, ignoreCase = true) ||
                    node.name.contains(query, ignoreCase = true) ||
                    node.country?.contains(query, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(pendingScrollToTop, isTesting, frozenNodeOrderIds, filteredNodes.size) {
        if (pendingScrollToTop && !isTesting && frozenNodeOrderIds == null && filteredNodes.isNotEmpty()) {
            withFrameNanos { }
            listState.scrollToItem(0)
            pendingScrollToTop = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isTesting) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (showSearch) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("输入关键词搜索节点") },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(
                            onClick = { onKeywordChange("") },
                            enabled = interactionEnabled
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "清空")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            nodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTesting) "正在获取节点..." else "暂无可用节点",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }

            filteredNodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到匹配节点",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredNodes,
                        key = { it.id }
                    ) { node ->
                        NodeListItem(
                            node = node,
                            isSelected = node.id == selectedNodeId,
                            isTesting = isTesting,
                            enabled = interactionEnabled,
                            onClick = { onNodeSelected(node) }
                        )
                    }
                }
            }
        }

        if (!interactionEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            )
        }
    }
}

@Composable
private fun NodeListItem(
    node: Node,
    isSelected: Boolean,
    isTesting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor =
        if (isSelected) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) Primary else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = node.getFlagEmoji(),
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Text(
                        text = node.getDisplayName(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (node.isAvailable) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = node.type.protocol.uppercase(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LatencyBadge(node = node, isTesting = isTesting)

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LatencyBadge(node: Node, isTesting: Boolean = false) {
    val latencyColor = when (node.getLatencyLevel()) {
        LatencyLevel.GOOD -> LatencyGood
        LatencyLevel.MEDIUM -> LatencyMedium
        LatencyLevel.BAD -> LatencyBad
    }

    Surface(
        color = latencyColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = if (isTesting && node.latency == -1) "测试中" else node.getLatencyText(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = latencyColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
