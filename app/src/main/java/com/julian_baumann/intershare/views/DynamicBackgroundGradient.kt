package com.julian_baumann.intershare.views

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

public enum class DynamicBackgroundGradientColors {
    Start,
    Send,
    Receive,
    Error
}

@Composable
fun DynamicBackgroundGradient(height: Dp, color: DynamicBackgroundGradientColors) {
    val gradientColors = if (isSystemInDarkTheme()) {
        when (color) {
            DynamicBackgroundGradientColors.Error -> {
                listOf(
                    Color(0xE53935).copy(alpha = 0.2f),
                    Color(0xF44336).copy(alpha = 0.1f),
                    Color.Transparent
                )
            }
            else -> {
                listOf(
                    Color(0xFF6200EE).copy(alpha = 0.2f),
                    Color(0xFF9347ff).copy(alpha = 0.1f),
                    Color.Transparent
                )
            }
        }

    } else {
        when (color) {
            DynamicBackgroundGradientColors.Error -> {
                listOf(
                    Color(0xE53935).copy(alpha = 0.2f),
                    Color(0xF44336).copy(alpha = 0.1f),
                    Color.Transparent
                )
            }
            DynamicBackgroundGradientColors.Receive -> {
                listOf(
                    Color(0xFF3700B3).copy(alpha = 0.3f),
                    Color(0xFF03DAC5).copy(alpha = 0.15f),
                    Color.Transparent
                )
            }
            DynamicBackgroundGradientColors.Start -> {
                listOf(
                    Color(0xFFF492F0).copy(alpha = 0.3f),
                    Color(0xFFF9C58D).copy(alpha = 0.15f),
                    Color.Transparent
                )
            }
            else -> {
                listOf(
                    Color(0xFF3700B3).copy(alpha = 0.3f),
                    Color(0xFF03DAC5).copy(alpha = 0.15f),
                    Color.Transparent
                )
            }
        }
    }

    val heightInPx = with(LocalDensity.current) { height.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(0f, heightInPx)
                )
            )
    )
}
