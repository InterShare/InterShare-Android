package com.julian_baumann.intershare.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.julian_baumann.data_rct.SendProgressState
import com.julian_baumann.intershare.SendProgress


@Composable
fun AnimatedCircularProgressIndicator(
    progress: SendProgress,
    progressIndicatorColor: Color,
    completedColor: Color,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val maxValue = 1.0f
    val errorColor = Color(0xFFFF3333)
    var currentValue by remember { mutableFloatStateOf(0.0f) }
    var successful by remember { mutableStateOf(true) }

    when (val state = progress.state) {
        is SendProgressState.Transferring -> currentValue = state.progress.toFloat()
        SendProgressState.Finished -> {
            currentValue = 1.0f
            successful = true
        }
        SendProgressState.Cancelled -> {
            currentValue = 1.0f
            successful = false
        }
        SendProgressState.Declined -> {
            currentValue = 1.0f
            successful = false
        }
        SendProgressState.Connecting -> {

        }
        SendProgressState.Requesting -> {

        }
        else -> {
            // Do nothing
        }
    }

    val stroke = with(LocalDensity.current) {
        Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()

        val animateFloat = currentValue / maxValue

        Canvas(Modifier
            .progressSemantics(currentValue / maxValue)
            .size(CircularIndicatorDiameter)
        ) {
            val startAngle = 270f
            val sweep: Float = animateFloat * 360f
            val diameterOffset = stroke.width / 2

            drawCircularProgressIndicator(startAngle, sweep, if (showProgress) progressIndicatorColor else Color.Transparent, stroke)

            if (currentValue == maxValue) {
                drawCircle(
                    color =  if (!showProgress) Color.Transparent else (if (successful) completedColor else errorColor),
                    style = stroke,
                    radius = size.minDimension / 2.0f - diameterOffset
                )
            }
        }
    }
}

private fun DrawScope.drawCircularProgressIndicator(
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameterOffset = stroke.width / 2
    val arcDimen = size.width - 2 * diameterOffset

    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset, diameterOffset),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

// Diameter of the indicator circle
private val CircularIndicatorDiameter = 70.dp
