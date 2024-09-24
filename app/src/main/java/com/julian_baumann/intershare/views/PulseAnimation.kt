package com.julian_baumann.intershare.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min


@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val durationMillis = 2000
    val delayBetweenRings = durationMillis / 4

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = delayBetweenRings),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = delayBetweenRings),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = 2 * delayBetweenRings),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = 2 * delayBetweenRings),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = 3 * delayBetweenRings),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha4 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing, delayMillis = 3 * delayBetweenRings),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val radius = size.minDimension / 2
        drawCircle(
            color = Color.Red.copy(alpha = alpha1),
            radius = radius * scale1,
            style = Stroke(width = 4.dp.toPx())
        )
        drawCircle(
            color = Color.Red.copy(alpha = alpha2),
            radius = radius * scale2,
            style = Stroke(width = 4.dp.toPx())
        )
        drawCircle(
            color = Color.Red.copy(alpha = alpha3),
            radius = radius * scale3,
            style = Stroke(width = 4.dp.toPx())
        )
        drawCircle(
            color = Color.Red.copy(alpha = alpha4),
            radius = radius * scale4,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}
