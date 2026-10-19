package com.pedro.eam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

data class BarChartData(
    val label: String,
    val value: Float,
    val maxValue: Float = 100f,
    val color: Color,
    val secondaryValue: Float? = null,
    val secondaryColor: Color? = null
)

@Composable
fun BarChart(
    title: String,
    subtitle: String,
    items: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = primaryColor)
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Nenhum dado disponível para análise", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 40.dp.toPx()
                val barWidth = (canvasWidth / (items.size * 2 + 1)).coerceAtMost(50f)
                val spacing = barWidth

                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = (canvasHeight / gridSteps) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                items.forEachIndexed { index, item ->
                    val maxVal = if (item.maxValue > 0f) item.maxValue else 100f
                    val barHeight = (item.value / maxVal) * canvasHeight
                    val x = spacing + index * (barWidth * 2)

                    drawRoundRect(
                        color = item.color,
                        topLeft = Offset(x, canvasHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    if (item.secondaryValue != null && item.secondaryColor != null) {
                        val secHeight = (item.secondaryValue / maxVal) * canvasHeight
                        drawRoundRect(
                            color = item.secondaryColor.copy(alpha = 0.5f),
                            topLeft = Offset(x + barWidth + 4f, canvasHeight - secHeight),
                            size = Size(barWidth / 2, secHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val displayLabel = if (item.label.length > 8) item.label.take(7) + ".." else item.label
                        drawText(
                            item.value.toInt().toString(),
                            x + barWidth / 2,
                            (canvasHeight - barHeight - 8f).coerceAtLeast(20f),
                            paint
                        )

                        val labelPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            displayLabel,
                            x + barWidth / 2,
                            canvasHeight + 35f,
                            labelPaint
                        )
                    }
                }
            }
        }
    }
}
