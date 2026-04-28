package xyz.a202132.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import xyz.a202132.app.ui.components.StartupSplashOverlay
import xyz.a202132.app.ui.components.NodeListScreen
import xyz.a202132.app.ui.dialogs.NetworkToolboxScreen
import xyz.a202132.app.ui.navigation.AppRoute
import xyz.a202132.app.ui.screens.MainScreen
import xyz.a202132.app.ui.screens.LanProxyScreen
import xyz.a202132.app.ui.screens.OtherConfigScreen
import xyz.a202132.app.ui.screens.PerAppProxyScreen
import xyz.a202132.app.ui.screens.UnlockTestScreen
import xyz.a202132.app.ui.theme.FireflyVPNTheme
import xyz.a202132.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private var pendingVpnAction: (() -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingVpnAction?.invoke()
        } else {
            Toast.makeText(this, "VPN 权限被拒绝", Toast.LENGTH_SHORT).show()
        }
        pendingVpnAction = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        requestNotificationPermission()

        setContent {
            FireflyVPNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    val navController = rememberNavController()
                    val nodes by viewModel.nodes.collectAsState()
                    val selectedNodeId by viewModel.selectedNodeId.collectAsState()
                    val isTesting by viewModel.isTesting.collectAsState()
                    val testingLabel by viewModel.testingLabel.collectAsState()
                    val startupUpdateCheckCompleted by viewModel.startupUpdateCheckCompleted.collectAsState()
                    val splashDurationSeconds = AppConfig.STARTUP_SPLASH_DURATION_SECONDS.coerceAtLeast(0)
                    var splashCountdownSeconds by remember(splashDurationSeconds) {
                        mutableIntStateOf(splashDurationSeconds)
                    }
                    var splashTimedOut by remember(splashDurationSeconds) {
                        mutableStateOf(splashDurationSeconds == 0)
                    }
                    var splashSkipped by remember { mutableStateOf(false) }
                    val showStartupSplash =
                        splashDurationSeconds > 0 &&
                            !splashSkipped &&
                            !splashTimedOut &&
                            !startupUpdateCheckCompleted

                    LaunchedEffect(splashDurationSeconds) {
                        if (splashDurationSeconds == 0) return@LaunchedEffect
                        splashCountdownSeconds = splashDurationSeconds
                        splashTimedOut = false
                        repeat(splashDurationSeconds) {
                            delay(1000)
                            splashCountdownSeconds = (splashCountdownSeconds - 1).coerceAtLeast(0)
                        }
                        splashTimedOut = true
                    }

                    if (showStartupSplash) {
                        StartupSplashOverlay(
                            countdownSeconds = splashCountdownSeconds,
                            onSkip = { splashSkipped = true }
                        )
                    } else {
                        val navigateTo: (String) -> Unit = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.MAIN
                        ) {
                            composable(AppRoute.MAIN) {
                                MainScreen(
                                    viewModel = viewModel,
                                    onStartVpn = { action -> requestVpnPermission(action) },
                                    onOpenPerAppProxy = { navigateTo(AppRoute.PER_APP_PROXY) },
                                    onOpenNodeList = { navigateTo(AppRoute.NODE_LIST) },
                                    onOpenNetworkToolbox = { navigateTo(AppRoute.NETWORK_TOOLBOX) },
                                    onOpenUnlockTest = { navigateTo(AppRoute.UNLOCK_TEST) },
                                    onOpenOtherConfig = { navigateTo(AppRoute.OTHER_CONFIG) },
                                    onOpenLanProxy = { navigateTo(AppRoute.LAN_PROXY) }
                                )
                            }

                            composable(AppRoute.PER_APP_PROXY) {
                                var isLeavingPerAppProxy by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxSize()) {
                                    PerAppProxyScreen(
                                        onBack = { hasChanges ->
                                            if (isLeavingPerAppProxy) return@PerAppProxyScreen
                                            isLeavingPerAppProxy = true
                                            navController.popBackStack()
                                            if (hasChanges) {
                                                viewModel.restartVpnIfNeeded()
                                            }
                                        }
                                    )

                                    if (isLeavingPerAppProxy) {
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

                            composable(AppRoute.NODE_LIST) {
                                NodeListScreen(
                                    nodes = nodes,
                                    selectedNodeId = selectedNodeId,
                                    isTesting = isTesting,
                                    testingLabel = testingLabel,
                                    onNodeSelected = { node ->
                                        viewModel.selectNode(node)
                                        navController.popBackStack(AppRoute.MAIN, false)
                                    },
                                    onRefresh = { viewModel.refreshNodesWithDefaultTest() },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(AppRoute.NETWORK_TOOLBOX) {
                                NetworkToolboxScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(AppRoute.UNLOCK_TEST) {
                                UnlockTestScreen(
                                    visibleNodes = nodes,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(AppRoute.OTHER_CONFIG) {
                                OtherConfigScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(AppRoute.LAN_PROXY) {
                                LanProxyScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestVpnPermission(action: () -> Unit) {
        val intent = android.net.VpnService.prepare(this)
        if (intent != null) {
            pendingVpnAction = action
            vpnPermissionLauncher.launch(intent)
        } else {
            action()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun configureEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
    }
}
