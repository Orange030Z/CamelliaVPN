package xyz.a202132.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.a202132.app.data.model.Node
import xyz.a202132.app.ui.components.AppScreenScaffold
import xyz.a202132.app.ui.dialogs.UnlockNodeSelectionPane
import xyz.a202132.app.ui.dialogs.UnlockResultDetailDialog
import xyz.a202132.app.ui.dialogs.UnlockResultPane
import xyz.a202132.app.viewmodel.UnlockTestViewModel

@Composable
fun UnlockTestScreen(
    visibleNodes: List<Node>,
    onBack: () -> Unit,
    viewModel: UnlockTestViewModel = viewModel()
) {
    val context = LocalContext.current
    val nodes by viewModel.nodes.collectAsState()
    val selected by viewModel.selectedNodeIds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val progressText by viewModel.progressText.collectAsState()
    val results by viewModel.results.collectAsState()
    val error by viewModel.error.collectAsState()
    var detailDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var randomModeEnabled by remember { mutableStateOf(false) }
    var randomNodeCountInput by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    val allNodesSelected = !randomModeEnabled && nodes.isNotEmpty() && selected.size == nodes.size
    val displayNodes = remember(nodes, selected) {
        nodes.sortedByDescending { selected.contains(it.id) }
    }
    val filteredDisplayNodes = remember(displayNodes, keyword) {
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
    val nodeListState = rememberLazyListState()

    LaunchedEffect(visibleNodes) {
        viewModel.updateVisibleNodes(visibleNodes)
    }

    LaunchedEffect(nodes) {
        if (nodes.isNotEmpty() && selected.isEmpty() && !randomModeEnabled) {
            viewModel.setAllSelected(true)
        }
        val maxCount = nodes.size
        val parsed = randomNodeCountInput.toIntOrNull()
        if (parsed != null && parsed > maxCount) {
            randomNodeCountInput = maxCount.toString()
            if (randomModeEnabled) {
                viewModel.selectRandomNodes(maxCount)
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(filteredDisplayNodes) {
        if (filteredDisplayNodes.isNotEmpty()) {
            nodeListState.scrollToItem(0)
        }
    }

    AppScreenScaffold(
        title = "流媒体解锁测试",
        subtitle = "通过临时本地代理逐个测试所选节点",
        onBack = onBack,
        backEnabled = !isRunning,
        onBackBlocked = {
            Toast.makeText(context, "请先停止测试再退出", Toast.LENGTH_SHORT).show()
        },
        actions = {
            IconButton(
                onClick = {
                    showSearch = !showSearch
                    if (!showSearch) {
                        keyword = ""
                    }
                }
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "筛选节点")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showSearch) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("过滤节点关键词") },
                    placeholder = { Text("名称 / 地区等") },
                    enabled = !isRunning
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = allNodesSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                randomModeEnabled = false
                                randomNodeCountInput = ""
                            }
                            viewModel.setAllSelected(checked)
                        },
                        enabled = !isRunning
                    )
                    Text("全选节点 (${selected.size}/${nodes.size})")
                }
                Button(
                    onClick = {
                        if (isRunning) {
                            viewModel.stopTests()
                        } else {
                            viewModel.startTests()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "停止" else "开始")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = randomModeEnabled,
                        onCheckedChange = { enabled ->
                            randomModeEnabled = enabled
                            if (enabled) {
                                viewModel.setAllSelected(false)
                            } else {
                                randomNodeCountInput = ""
                            }
                        },
                        enabled = !isRunning
                    )
                }

                OutlinedTextField(
                    value = randomNodeCountInput,
                    onValueChange = { value ->
                        val digits = value.filter { it.isDigit() }
                        if (digits.isEmpty()) {
                            randomNodeCountInput = ""
                            if (randomModeEnabled) {
                                viewModel.selectRandomNodes(0)
                            }
                            return@OutlinedTextField
                        }
                        val parsed = digits.toIntOrNull() ?: return@OutlinedTextField
                        val clamped = parsed.coerceAtMost(nodes.size)
                        randomNodeCountInput = clamped.toString()
                        if (randomModeEnabled) {
                            viewModel.selectRandomNodes(clamped)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("随机测试节点数") },
                    singleLine = true,
                    enabled = !isRunning && randomModeEnabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedButton(
                    onClick = { viewModel.selectCurrentNodeOnly() },
                    enabled = !isRunning
                ) {
                    Text("当前节点")
                }
            }

            if (isRunning) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            progressText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideLayout = maxWidth >= 900.dp
                if (isWideLayout) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UnlockNodeSelectionPane(
                            modifier = Modifier.weight(1f),
                            filteredDisplayNodes = filteredDisplayNodes,
                            selected = selected,
                            isRunning = isRunning,
                            nodeListState = nodeListState,
                            onToggleNode = viewModel::toggleNode
                        )
                        UnlockResultPane(
                            modifier = Modifier.weight(1.15f),
                            results = results,
                            onShowDetail = { title, content ->
                                detailDialog = title to content
                            }
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UnlockNodeSelectionPane(
                            modifier = Modifier.weight(0.85f),
                            filteredDisplayNodes = filteredDisplayNodes,
                            selected = selected,
                            isRunning = isRunning,
                            nodeListState = nodeListState,
                            onToggleNode = viewModel::toggleNode
                        )
                        UnlockResultPane(
                            modifier = Modifier.weight(1.15f),
                            results = results,
                            onShowDetail = { title, content ->
                                detailDialog = title to content
                            }
                        )
                    }
                }
            }
        }
    }

    detailDialog?.let { (title, content) ->
        UnlockResultDetailDialog(
            title = title,
            content = content,
            onDismiss = { detailDialog = null }
        )
    }
}
