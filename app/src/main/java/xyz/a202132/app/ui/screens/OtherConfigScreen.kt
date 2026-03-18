package xyz.a202132.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.a202132.app.data.model.VpnState
import xyz.a202132.app.ui.components.AppScreenScaffold
import xyz.a202132.app.viewmodel.MainViewModel
import xyz.a202132.app.viewmodel.StartupDefaultTestMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherConfigScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val startupDefaultTestMode by viewModel.startupDefaultTestMode.collectAsState()
    val nodeIpInfoTestOnVpnStart by viewModel.nodeIpInfoTestOnVpnStart.collectAsState()
    val tcpingTestTimeoutMs by viewModel.tcpingTestTimeoutMs.collectAsState()
    val urlTestTimeoutMs by viewModel.urlTestTimeoutMs.collectAsState()
    val nodeIpInfoTimeoutMs by viewModel.nodeIpInfoTimeoutMs.collectAsState()
    val speedTestDownloadTimeoutMs by viewModel.speedTestDownloadTimeoutMs.collectAsState()
    val tcpingConcurrency by viewModel.tcpingConcurrency.collectAsState()
    val urlTestConcurrency by viewModel.urlTestConcurrency.collectAsState()
    val unlockTestConcurrency by viewModel.unlockTestConcurrency.collectAsState()
    val vpnMtu by viewModel.vpnMtu.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()

    var startupModeDraft by remember(startupDefaultTestMode) { mutableStateOf(startupDefaultTestMode) }
    var nodeIpInfoAutoRunDraft by remember(nodeIpInfoTestOnVpnStart) { mutableStateOf(nodeIpInfoTestOnVpnStart) }
    var tcpingTimeoutInput by remember(tcpingTestTimeoutMs) { mutableStateOf(tcpingTestTimeoutMs.toString()) }
    var urlTestTimeoutInput by remember(urlTestTimeoutMs) { mutableStateOf(urlTestTimeoutMs.toString()) }
    var nodeIpInfoTimeoutInput by remember(nodeIpInfoTimeoutMs) { mutableStateOf(nodeIpInfoTimeoutMs.toString()) }
    var speedTestDownloadTimeoutInput by remember(speedTestDownloadTimeoutMs) { mutableStateOf(speedTestDownloadTimeoutMs.toString()) }
    var tcpingConcurrencyInput by remember(tcpingConcurrency) { mutableStateOf(tcpingConcurrency.toString()) }
    var urlTestConcurrencyInput by remember(urlTestConcurrency) { mutableStateOf(urlTestConcurrency.toString()) }
    var unlockTestConcurrencyInput by remember(unlockTestConcurrency) { mutableStateOf(unlockTestConcurrency.toString()) }
    var mtuInput by remember(vpnMtu) { mutableStateOf(vpnMtu.toString()) }

    LaunchedEffect(
        startupDefaultTestMode,
        nodeIpInfoTestOnVpnStart,
        tcpingTestTimeoutMs,
        urlTestTimeoutMs,
        nodeIpInfoTimeoutMs,
        speedTestDownloadTimeoutMs,
        tcpingConcurrency,
        urlTestConcurrency,
        unlockTestConcurrency,
        vpnMtu
    ) {
        startupModeDraft = startupDefaultTestMode
        nodeIpInfoAutoRunDraft = nodeIpInfoTestOnVpnStart
        tcpingTimeoutInput = tcpingTestTimeoutMs.toString()
        urlTestTimeoutInput = urlTestTimeoutMs.toString()
        nodeIpInfoTimeoutInput = nodeIpInfoTimeoutMs.toString()
        speedTestDownloadTimeoutInput = speedTestDownloadTimeoutMs.toString()
        tcpingConcurrencyInput = tcpingConcurrency.toString()
        urlTestConcurrencyInput = urlTestConcurrency.toString()
        unlockTestConcurrencyInput = unlockTestConcurrency.toString()
        mtuInput = vpnMtu.toString()
    }

    AppScreenScaffold(
        title = "其他配置",
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    val savedMtu = mtuInput.toIntOrNull()?.coerceIn(576, 9000) ?: vpnMtu
                    viewModel.setStartupDefaultTestMode(startupModeDraft)
                    viewModel.setNodeIpInfoTestOnVpnStart(nodeIpInfoAutoRunDraft)
                    viewModel.setTcpingTestTimeoutMs(tcpingTimeoutInput.toLongOrNull()?.coerceAtLeast(500L) ?: tcpingTestTimeoutMs)
                    viewModel.setUrlTestTimeoutMs(urlTestTimeoutInput.toLongOrNull()?.coerceAtLeast(500L) ?: urlTestTimeoutMs)
                    viewModel.setNodeIpInfoTimeoutMs(nodeIpInfoTimeoutInput.toLongOrNull()?.coerceAtLeast(1000L) ?: nodeIpInfoTimeoutMs)
                    viewModel.setSpeedTestDownloadTimeoutMs(speedTestDownloadTimeoutInput.toLongOrNull()?.coerceAtLeast(0L) ?: speedTestDownloadTimeoutMs)
                    viewModel.setTcpingConcurrency(tcpingConcurrencyInput.toIntOrNull()?.coerceIn(1, 128) ?: tcpingConcurrency)
                    viewModel.setUrlTestConcurrency(urlTestConcurrencyInput.toIntOrNull()?.coerceIn(1, 128) ?: urlTestConcurrency)
                    viewModel.setUnlockTestConcurrency(unlockTestConcurrencyInput.toIntOrNull()?.coerceIn(1, 32) ?: unlockTestConcurrency)
                    viewModel.setVpnMtu(savedMtu)
                    Toast.makeText(
                        context,
                        if (savedMtu != vpnMtu && vpnState == VpnState.CONNECTED) {
                            "设置已保存，MTU需断开并重连VPN后生效"
                        } else {
                            "设置已保存"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("保存")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OtherConfigSection(
                title = "自动执行",
                description = "修改配置后会立即生效。"
            ) {
                Text(
                    text = "APP启动后默认执行哪项延迟测试",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = startupModeDraft == StartupDefaultTestMode.NONE,
                        onClick = { startupModeDraft = StartupDefaultTestMode.NONE },
                        label = { Text("不执行") }
                    )
                    FilterChip(
                        selected = startupModeDraft == StartupDefaultTestMode.TCPING,
                        onClick = { startupModeDraft = StartupDefaultTestMode.TCPING },
                        label = { Text("TCPing") }
                    )
                    FilterChip(
                        selected = startupModeDraft == StartupDefaultTestMode.URL_TEST,
                        onClick = { startupModeDraft = StartupDefaultTestMode.URL_TEST },
                        label = { Text("URL Test") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VPN连接后自动获取节点IP信息",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "仅影响后续新的连接动作",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = nodeIpInfoAutoRunDraft,
                        onCheckedChange = { nodeIpInfoAutoRunDraft = it }
                    )
                }
            }

            OtherConfigSection(
                title = "测试超时",
                description = "单位均为毫秒。下载测速超时设为 0 表示不限制。"
            ) {
                OtherConfigNumberField(
                    label = "TCPing 超时",
                    value = tcpingTimeoutInput,
                    onValueChange = { tcpingTimeoutInput = it.filter(Char::isDigit) }
                )
                OtherConfigNumberField(
                    label = "URL Test 超时",
                    value = urlTestTimeoutInput,
                    onValueChange = { urlTestTimeoutInput = it.filter(Char::isDigit) }
                )
                OtherConfigNumberField(
                    label = "节点IP信息超时",
                    value = nodeIpInfoTimeoutInput,
                    onValueChange = { nodeIpInfoTimeoutInput = it.filter(Char::isDigit) }
                )
                OtherConfigNumberField(
                    label = "单次下载测速超时",
                    value = speedTestDownloadTimeoutInput,
                    onValueChange = { speedTestDownloadTimeoutInput = it.filter(Char::isDigit) }
                )
            }

            OtherConfigSection(
                title = "并发与网络",
                description = "并发配置对下一次测试生效；MTU需断开并重连VPN生效。"
            ) {
                OtherConfigNumberField(
                    label = "TCPing 并发数",
                    value = tcpingConcurrencyInput,
                    onValueChange = { tcpingConcurrencyInput = it.filter(Char::isDigit) }
                )
                OtherConfigNumberField(
                    label = "URL Test 并发数",
                    value = urlTestConcurrencyInput,
                    onValueChange = { urlTestConcurrencyInput = it.filter(Char::isDigit) }
                )
                OtherConfigNumberField(
                    label = "流媒体测试并发数",
                    value = unlockTestConcurrencyInput,
                    onValueChange = { unlockTestConcurrencyInput = it.filter(Char::isDigit) }
                )
                OtherConfigNumberField(
                    label = "VPN MTU",
                    value = mtuInput,
                    onValueChange = { mtuInput = it.filter(Char::isDigit) }
                )
                if (vpnState == VpnState.CONNECTED) {
                    Text(
                        text = "当前VPN已连接，修改MTU后请断开并重新连接。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherConfigSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun OtherConfigNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
