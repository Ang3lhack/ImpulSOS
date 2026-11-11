package com.impulsos.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

private const val DEMO_LENGTH_MS = 1320
private val StrokeEasing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

@Composable
internal fun GestureDemonstration() {
    val canAnimate = ValueAnimator.areAnimatorsEnabled()
    val timeline = remember { Animatable(0f) }
    var playback by remember { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(playback, canAnimate) {
        timeline.snapTo(0f)
        if (playback > 0 && canAnimate) {
            timeline.animateTo(DEMO_LENGTH_MS.toFloat(), tween(DEMO_LENGTH_MS, easing = LinearEasing))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StrokePanel("1. Primer tajo", Modifier.weight(1f)) { strokePosition(timeline.value, 120f) }
                StrokePanel("2. Repite", Modifier.weight(1f)) { strokePosition(timeline.value, 620f) }
            }
            if (canAnimate) {
                OutlinedButton(
                    onClick = { playback++ },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(if (playback == 0) "Ver demostración" else "Repetir demostración")
                }
            }
        }
    }
}

private fun strokePosition(time: Float, start: Float): Float {
    val elapsed = time - start
    val position = when {
        elapsed in 0f..160f -> elapsed / 160f
        elapsed > 160f && elapsed < 400f -> 1f - (elapsed - 160f) / 240f
        else -> 0f
    }
    return StrokeEasing.transform(position)
}

@Composable
private fun StrokePanel(label: String, modifier: Modifier, progress: () -> Float) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Text(label, color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
        Canvas(Modifier.fillMaxWidth().height(104.dp)) {
            val width = size.width
            val height = size.height
            val phone = Size(width * 0.24f, height * 0.58f)
            val start = Offset(width * 0.13f, height * 0.12f)
            val travel = width * 0.47f
            val radius = CornerRadius(5.dp.toPx())
            val step = progress()

            drawRoundRect(
                color = colors.onPrimaryContainer.copy(alpha = 0.36f),
                topLeft = start.copy(x = start.x + travel),
                size = phone,
                cornerRadius = radius,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            val arrowStart = Offset(width * 0.19f, height * 0.88f)
            val arrowEnd = Offset(width * 0.85f, arrowStart.y)
            val stroke = 2.2.dp.toPx()
            val wing = width * 0.07f
            drawLine(colors.onPrimaryContainer, arrowStart, arrowEnd, stroke, StrokeCap.Round)
            drawLine(colors.onPrimaryContainer, arrowEnd - Offset(wing, wing), arrowEnd, stroke, StrokeCap.Round)
            drawLine(colors.onPrimaryContainer, arrowEnd - Offset(wing, -wing), arrowEnd, stroke, StrokeCap.Round)

            translate(start.x + travel * step, start.y) {
                rotate(-8f + 18f * step, Offset(phone.width / 2f, phone.height * 0.8f)) {
                    drawRoundRect(colors.onPrimaryContainer, size = phone, cornerRadius = radius)
                    drawRoundRect(
                        colors.surface,
                        topLeft = Offset(phone.width * 0.10f, phone.height * 0.11f),
                        size = Size(phone.width * 0.80f, phone.height * 0.74f),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    // A rounded grip wraps the lower edge of the phone.
                    drawRoundRect(
                        colors.secondary,
                        topLeft = Offset(-phone.width * 0.17f, phone.height * 0.57f),
                        size = Size(phone.width * 0.43f, phone.height * 0.48f),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}
