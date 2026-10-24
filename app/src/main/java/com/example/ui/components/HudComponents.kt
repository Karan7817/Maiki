package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudAmberOrange
import com.example.ui.theme.HudBorderSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanDim
import com.example.ui.theme.HudCyanGlow
import com.example.ui.theme.HudDarkBg
import com.example.ui.theme.HudPanelBg
import com.example.ui.theme.HudPanelBorder
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Iron Man Arc Reactor / Energy Level HUD Widget.
 * Directly reproduces the iconic circular gauge from the reference image:
 * Outer segmented cyan arc, amber inner arc, target reticle, and dynamic rotation.
 */
@Composable
fun HudArcReactorCore(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    label: String = "ENERGY LEVEL",
    coreText: String = "MAIKI AI",
    energyPercent: Int = 100
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(210.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.height / 2f) * 0.92f

            // 1. Outer Dark Halo
            drawCircle(
                color = Color(0x3300D2FF),
                radius = outerRadius * 1.02f,
                center = center,
                style = Stroke(width = 2f)
            )

            // 2. Cyan Segmented Outer Arc (matches the reference: segments between 180 and 360 deg)
            rotate(rotationAngle, pivot = center) {
                // Segmented cyan blocks
                for (i in 0 until 24) {
                    val startAngle = i * 15f
                    drawArc(
                        color = if (i % 2 == 0) HudCyan else HudCyanDim,
                        startAngle = startAngle,
                        sweepAngle = 10f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 16f, cap = StrokeCap.Butt)
                    )
                }
            }

            // 3. Middle Amber Energy Warning Arc
            val amberRadius = outerRadius * 0.82f
            rotate(reverseRotation, pivot = center) {
                drawArc(
                    color = HudAmber,
                    startAngle = 45f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(center.x - amberRadius, center.y - amberRadius),
                    size = Size(amberRadius * 2, amberRadius * 2),
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )

                // Amber ticks
                for (j in 0 until 18) {
                    val angleRad = Math.toRadians((j * 20.0))
                    val p1 = Offset(
                        center.x + (amberRadius - 6f) * cos(angleRad).toFloat(),
                        center.y + (amberRadius - 6f) * sin(angleRad).toFloat()
                    )
                    val p2 = Offset(
                        center.x + (amberRadius + 6f) * cos(angleRad).toFloat(),
                        center.y + (amberRadius + 6f) * sin(angleRad).toFloat()
                    )
                    drawLine(
                        color = HudAmberOrange,
                        start = p1,
                        end = p2,
                        strokeWidth = 2.5f
                    )
                }
            }

            // 4. Inner Cyan Target Compass Reticle
            val innerRadius = outerRadius * 0.65f
            drawCircle(
                color = HudCyanGlow,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            // 5. Crosshair targeting lines
            val crosshairLen = innerRadius * 0.85f
            drawLine(
                color = HudCyanGlow.copy(alpha = 0.6f),
                start = Offset(center.x - crosshairLen, center.y),
                end = Offset(center.x + crosshairLen, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = HudCyanGlow.copy(alpha = 0.6f),
                start = Offset(center.x, center.y - crosshairLen),
                end = Offset(center.x, center.y + crosshairLen),
                strokeWidth = 1.5f
            )

            // Central Core Glowing Disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HudCyan.copy(alpha = if (isActive) 0.6f else 0.25f), Color.Transparent),
                    center = center,
                    radius = innerRadius * 0.7f
                ),
                radius = innerRadius * 0.7f,
                center = center
            )
        }

        // Center HUD Telemetry Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = coreText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = HudCyan
            )
            Text(
                text = "$energyPercent%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = HudTextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                ),
                color = HudAmber
            )
        }
    }
}

/**
 * Top Telemetry Scanner Reticle matching top-left corner of the reference image.
 */
@Composable
fun HudTopScannerReticle(
    modifier: Modifier = Modifier,
    statusText: String = "TEMPERATURE: OPTIMAL",
    metricText: String = "NEURAL LINK: 98.4%"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_anim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing)),
        label = "scan_angle"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HudPanelBg, RoundedCornerShape(8.dp))
            .border(1.dp, HudBorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.width / 2f - 2f
            drawCircle(color = HudCyanDim, radius = r, style = Stroke(width = 2f))
            rotate(angle, pivot = center) {
                drawArc(
                    color = HudCyan,
                    startAngle = 0f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(2f, 2f),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
            drawCircle(color = HudAmber, radius = 3f, center = center)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = HudCyan
            )
            Text(
                text = metricText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = HudTextSecondary
            )
        }
    }
}

/**
 * Angular Sci-Fi HUD Bracket Card
 */
@Composable
fun HudPanelFrame(
    modifier: Modifier = Modifier,
    headerTitle: String? = null,
    cornerCut: Float = 14f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = HudPanelBg,
                shape = CutCornerShape(
                    topStart = cornerCut.dp,
                    bottomEnd = cornerCut.dp
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(HudPanelBorder, HudBorderSubtle, HudCyanDim)
                ),
                shape = CutCornerShape(
                    topStart = cornerCut.dp,
                    bottomEnd = cornerCut.dp
                )
            )
            .padding(14.dp)
    ) {
        Column {
            if (!headerTitle.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .background(HudCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = headerTitle.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = HudCyan
                    )
                }
            }
            content()
        }
    }
}
