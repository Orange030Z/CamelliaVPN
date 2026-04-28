package xyz.a202132.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backEnabled: Boolean = true,
    onBackBlocked: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable () -> Unit
) {
    var isLeaving by remember { mutableStateOf(false) }

    val handleBack = {
        if (!isLeaving && backEnabled) {
            isLeaving = true
            onBack()
        }
    }

    BackHandler(enabled = !isLeaving) {
        if (backEnabled) {
            handleBack()
        } else {
            onBackBlocked?.invoke()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = handleBack,
                            enabled = backEnabled && !isLeaving
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(contentPadding)
            ) {
                content()
            }
        }

        if (isLeaving) {
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
