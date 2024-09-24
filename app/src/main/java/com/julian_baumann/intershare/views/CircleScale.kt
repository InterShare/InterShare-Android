package com.julian_baumann.intershare.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun CircleScale(rotation: Float) {
    val isDarkTheme = isSystemInDarkTheme()
    val scaleColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .graphicsLayer(rotationZ = rotation)
    ) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2 - 16.dp.toPx()
        val center = center

        drawCircleScale(center.x, center.y, radius, scaleColor)
    }
}

fun DrawScope.drawCircleScale(centerX: Float, centerY: Float, radius: Float, scaleColor: Color) {
    val totalTicks = 60
    val majorTickLength = 20.dp.toPx()
    val minorTickLength = 10.dp.toPx()
    val tickWidth = 2.dp.toPx()

    for (i in 0 until totalTicks) {
        val angle = Math.toRadians((360.0 / totalTicks * i).toDouble())
        val startX = centerX + radius * cos(angle).toFloat()
        val startY = centerY + radius * sin(angle).toFloat()
        val endX = centerX + (radius - if (i % 5 == 0) majorTickLength else minorTickLength) * Math.cos(angle).toFloat()
        val endY = centerY + (radius - if (i % 5 == 0) majorTickLength else minorTickLength) * Math.sin(angle).toFloat()

        drawLine(
            color = scaleColor,
            start = androidx.compose.ui.geometry.Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(endX, endY),
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )
    }

    // Draw center point
//    drawCircle(
//        color = scaleColor,
//        radius = 5.dp.toPx(),
//        center = androidx.compose.ui.geometry.Offset(centerX, centerY),
//    )
}
