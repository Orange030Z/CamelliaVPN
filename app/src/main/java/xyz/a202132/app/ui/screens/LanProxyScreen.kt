package xyz.a202132.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import xyz.a202132.app.AppConfig
import xyz.a202132.app.data.model.VpnState
import xyz.a202132.app.ui.components.AppScreenScaffold
import xyz.a202132.app.ui.theme.Primary
import xyz.a202132.app.viewmodel.MainViewModel
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

@Composable
fun LanProxyScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabled by viewModel.lanProxyEnabled.collectAsState()
    val autoPort by viewModel.lanProxyAutoPort.collectAsState()
    val port by viewModel.lanProxyPort.collectAsState()
    val authEnabled by viewModel.lanProxyAuthEnabled.collectAsState()
    val username by viewModel.lanProxyUsername.collectAsState()
    val password by viewModel.lanProxyPassword.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()

    var enabledDraft by remember { mutableStateOf(enabled) }
    var autoPortDraft by remember { mutableStateOf(autoPort) }
    var portInput by remember { mutableStateOf(port.toString()) }
    var authEnabledDraft by remember { mutableStateOf(authEnabled) }
    var usernameInput by remember { mutableStateOf(username) }
    var passwordInput by remember { mutableStateOf(password) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var ignoringBatteryOptimizations by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }

    LaunchedEffect(enabled, autoPort, port, authEnabled, username, password) {
        enabledDraft = enabled
        autoPortDraft = autoPort
        portInput = port.toString()
        authEnabledDraft = authEnabled
        usernameInput = username
        passwordInput = password
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val savedPort = portInput.toIntOrNull()
        ?.coerceIn(AppConfig.LAN_PROXY_MIN_PORT, AppConfig.LAN_PROXY_MAX_PORT)
        ?: AppConfig.LAN_PROXY_DEFAULT_PORT
    val savedSocksPort = remember(savedPort) {
        lanProxySocksPortFor(savedPort)
    }
    val lanEndpoints = remember(savedPort, savedSocksPort, refreshTick) {
        findLanProxyEndpoints(savedPort, savedSocksPort)
    }

    AppScreenScaffold(
        title = "局域网代理",
        subtitle = if (enabled) "HTTP $port / SOCKS5 ${lanProxySocksPortFor(port)}" else "未开启",
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    viewModel.saveLanProxySettings(
                        enabled = enabledDraft,
                        autoPort = autoPortDraft,
                        port = savedPort,
                        authEnabled = authEnabledDraft,
                        username = usernameInput,
                        password = passwordInput
                    )
                    Toast.makeText(
                        context,
                        if (vpnState == VpnState.CONNECTED) "已保存，VPN 正在应用新配置" else "已保存",
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
            LanProxySection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "允许局域网连接",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (enabledDraft) "在同一局域网的其他设备可通过本机代理（需选择节点并开启VPN）访问当前节点" else "开启后在手机端启动 HTTP 与 SOCKS5 代理端口，局域网内的设备可连接使用",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabledDraft,
                        onCheckedChange = { enabledDraft = it },
                        colors = visibleSwitchColors()
                    )
                }
            }

            LanProxySection(
                title = "连接地址",
                description = "优先使用 HTTP；不支持 HTTP 的应用可选 SOCKS5。"
            ) {
                if (lanEndpoints.isEmpty()) {
                    Text(
                        text = "未发现可用局域网 IPv4 地址，请确认手机已连接 Wi-Fi 或热点已开启。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    lanEndpoints.forEach { endpoint ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${endpoint.protocol}  ${endpoint.address}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    copyText(context, endpoint.address)
                                    Toast.makeText(context, "代理地址已复制", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "复制")
                            }
                        }
                    }
                }
                TextButton(onClick = { refreshTick++ }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text("刷新地址")
                }
            }

            LanProxySection(
                title = "使用建议",
                description = "Windows 设置里的代理服务器通常按 HTTP 代理使用；SOCKS5 更适合 Firefox 手动代理、curl 或专业代理工具。curl 测试 SOCKS5 建议使用 socks5h://，让 APP 侧解析域名。"
            ) {
                
            }

            LanProxySection(
                title = "后台稳定性",
                description = if (ignoringBatteryOptimizations) {
                    "已允许忽略电池优化。长时间开启局域网代理时，系统更不容易在息屏或待机后限制 VPN 服务。"
                } else {
                    "建议允许忽略电池优化，以提升局域网代理长时间运行稳定性。部分系统仍可能需要在系统管家里额外允许自启动/后台运行。"
                }
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ignoringBatteryOptimizations) "电池优化已忽略" else "电池优化可能限制后台",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (enabledDraft) "当前已开启局域网代理，建议保持允许。" else "开启局域网代理后建议允许此项。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                openBatteryOptimizationSettings(context)
                                ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
                            }
                        ) {
                            Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (ignoringBatteryOptimizations) "管理" else "去允许")
                        }
                    }
                } else {
                    Text(
                        text = "当前 Android 版本无需单独设置电池优化。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LanProxySection(
                title = "端口",
                description = "HTTP 使用当前端口，SOCKS5 使用下一端口；自动选择会寻找一组可用端口。"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("自动选择未占用端口", color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = autoPortDraft,
                        onCheckedChange = { autoPortDraft = it },
                        colors = visibleSwitchColors()
                    )
                }
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it.filter(Char::isDigit) },
                    label = { Text("端口") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextButton(
                    onClick = {
                        portInput = findAvailableHttpPortNear(savedPort).toString()
                        refreshTick++
                    }
                ) {
                    Text("查找当前空闲端口")
                }
            }

            LanProxySection(
                title = "认证",
                description = "⚠️ 开启认证后，浏览器（Firefox/Edge/Chrome）不会弹出认证框，将直接无法使用代理。这是代理内核的限制（未认证连接会被直接关闭而不是返回认证提示）。\n\n如需认证，请使用 curl 命令行（在代理地址前加 user:pass@）验证或使用支持预设凭证的代理工具（如 SwitchyOmega 扩展）。一般场景建议关闭认证。"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用用户名和密码", color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = authEnabledDraft,
                        onCheckedChange = { authEnabledDraft = it },
                        colors = visibleSwitchColors()
                    )
                }
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.trim() },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = authEnabledDraft
                )
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it.trim() },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = authEnabledDraft,
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = {
                        val httpAddr = lanEndpoints.firstOrNull { it.protocol == "HTTP" }?.address
                            ?: "手机局域网IP:$savedPort"
                        val socksAddr = lanEndpoints.firstOrNull { it.protocol == "SOCKS5" }?.address
                            ?: "手机局域网IP:$savedSocksPort"
                        val user = usernameInput.ifBlank { "firefly" }
                        val pass = passwordInput.ifBlank { "firefly" }

                        val text = if (authEnabledDraft) {
                            "用户名: $user\n密码: $pass\nHTTP代理: http://$user:$pass@$httpAddr\nSOCKS5代理: socks5://$user:$pass@$socksAddr 或 socks5h://$user:$pass@$socksAddr"
                        } else {
                            "HTTP代理: http://$httpAddr\nSOCKS5代理: socks5://$socksAddr 或 socks5h://$socksAddr"
                        }
                        copyText(context, text)
                        Toast.makeText(context, "连接信息已复制", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabledDraft
                ) {
                    Text("复制连接信息")
                }
            }

            if (vpnState == VpnState.CONNECTED) {
                Text(
                    text = "保存后会自动重载当前 VPN 连接。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LanProxySection(
    title: String? = null,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
            content()
        }
    }
}

@Composable
private fun visibleSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surface,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)

private data class LanProxyEndpoint(
    val protocol: String,
    val address: String
)

private fun findLanProxyEndpoints(httpPort: Int, socksPort: Int): List<LanProxyEndpoint> {
    val result = mutableListOf<LanProxyEndpoint>()
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull() ?: return emptyList()
    while (interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement()
        val isUsable = runCatching {
            networkInterface.isUp &&
                !networkInterface.isLoopback &&
                !networkInterface.isVirtual &&
                !networkInterface.isPointToPoint &&
                isLanProxyNetworkInterface(networkInterface.name)
        }.getOrDefault(false)
        if (!isUsable) continue

        val addresses = networkInterface.inetAddresses
        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (address !is Inet4Address) continue
            val host = address.hostAddress ?: continue
            if (
                !address.isLoopbackAddress &&
                address.isSiteLocalAddress &&
                isLanProxyAddress(host)
            ) {
                result.add(LanProxyEndpoint("HTTP", "$host:$httpPort"))
                result.add(LanProxyEndpoint("SOCKS5", "$host:$socksPort"))
            }
        }
    }
    return result.distinctBy { "${it.protocol}:${it.address}" }
}

private fun findAvailableHttpPortNear(startPort: Int): Int {
    val start = startPort.coerceIn(AppConfig.LAN_PROXY_MIN_PORT, AppConfig.LAN_PROXY_MAX_PORT)
    for (port in start..AppConfig.LAN_PROXY_MAX_PORT) {
        if (canBindLanProxyPorts(port)) return port
    }
    for (port in AppConfig.LAN_PROXY_MIN_PORT until start) {
        if (canBindLanProxyPorts(port)) return port
    }
    return start
}

private fun canBindLanProxyPorts(httpPort: Int): Boolean {
    val socksPort = lanProxySocksPortFor(httpPort)
    return httpPort != socksPort && canBindPort(httpPort) && canBindPort(socksPort)
}

private fun lanProxySocksPortFor(httpPort: Int): Int {
    return if (httpPort < AppConfig.LAN_PROXY_MAX_PORT) {
        httpPort + 1
    } else {
        AppConfig.LAN_PROXY_MIN_PORT
    }
}

private fun isLanProxyNetworkInterface(name: String?): Boolean {
    val lowerName = name?.lowercase() ?: return false
    return !lowerName.startsWith("tun") &&
        !lowerName.startsWith("utun") &&
        !lowerName.startsWith("wg") &&
        !lowerName.startsWith("clat") &&
        !lowerName.contains("vpn")
}

private fun isLanProxyAddress(host: String): Boolean {
    return host != "172.19.0.1" && host != "172.19.0.2"
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
}

private fun openBatteryOptimizationSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val packageUri = Uri.parse("package:${context.packageName}")
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = packageUri
    }
    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = packageUri
    }
    runCatching {
        context.startActivity(requestIntent)
    }.recoverCatching {
        context.startActivity(fallbackIntent)
    }.onFailure {
        Toast.makeText(context, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
    }
}

private fun canBindPort(port: Int): Boolean {
    return runCatching {
        ServerSocket().use { socket ->
            socket.reuseAddress = false
            socket.bind(InetSocketAddress("0.0.0.0", port))
        }
    }.isSuccess
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("lan_proxy", text))
}
