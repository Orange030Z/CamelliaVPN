package xyz.a202132.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import xyz.a202132.app.R

@Composable
fun StartupSplashOverlay(
    countdownSeconds: Int,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val skipButtonStyle = remember(context) {
        resolveSkipButtonStyle(context)
    }

    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_firefly),
            contentDescription = "\u542F\u52A8\u56FE",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.03f),
                            Color.Black.copy(alpha = 0.07f)
                        )
                    )
                )
        )

        Button(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = skipButtonStyle.containerColor,
                contentColor = skipButtonStyle.contentColor
            ),
            border = BorderStroke(1.dp, skipButtonStyle.borderColor)
        ) {
            Text(
                text = "\u8DF3\u8FC7 ${countdownSeconds}s",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class SkipButtonStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

private fun resolveSkipButtonStyle(context: Context): SkipButtonStyle {
    val luminance = sampleSplashTopEndLuminance(context)
    return if (luminance >= 0.62f) {
        SkipButtonStyle(
            containerColor = Color.Black.copy(alpha = 0.12f),
            contentColor = Color.Black,
            borderColor = Color.Black.copy(alpha = 0.20f)
        )
    } else {
        SkipButtonStyle(
            containerColor = Color.White.copy(alpha = 0.24f),
            contentColor = Color.White,
            borderColor = Color.White.copy(alpha = 0.34f)
        )
    }
}

private fun sampleSplashTopEndLuminance(context: Context): Float {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.splash_firefly)
        ?: return 1f

    return try {
        val startX = (bitmap.width * 0.68f).toInt().coerceIn(0, bitmap.width - 1)
        val endX = (bitmap.width * 0.96f).toInt().coerceIn(startX + 1, bitmap.width)
        val startY = (bitmap.height * 0.03f).toInt().coerceIn(0, bitmap.height - 1)
        val endY = (bitmap.height * 0.15f).toInt().coerceIn(startY + 1, bitmap.height)

        var total = 0.0
        var count = 0
        val stepX = ((endX - startX) / 24).coerceAtLeast(1)
        val stepY = ((endY - startY) / 12).coerceAtLeast(1)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val pixel = bitmap.getPixel(x, y)
                val red = android.graphics.Color.red(pixel) / 255.0
                val green = android.graphics.Color.green(pixel) / 255.0
                val blue = android.graphics.Color.blue(pixel) / 255.0
                total += 0.2126 * red + 0.7152 * green + 0.0722 * blue
                count++
                x += stepX
            }
            y += stepY
        }

        if (count == 0) 1f else (total / count).toFloat()
    } finally {
        bitmap.recycle()
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
