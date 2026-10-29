package com.pedro.eam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()

    val accessibleDescription = remember(items, title) {
        if (items.isEmpty()) {
            "$title. Nenhum dado disponível."
        } else {
            val details = items.joinToString(", ") { "${it.label}: ${it.value.toInt()} de ${it.maxValue.toInt()}" }
            "$title: $details"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            }
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = primaryColor)
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = onSurfaceVariantColor)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Nenhum dado disponível para análise", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariantColor)
            }
        } else {
            val scrollState = rememberScrollState()
            val minChartWidth = (items.size * 64).dp.coerceAtLeast(280.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(minChartWidth)
                        .height(220.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 40.dp.toPx()
                    val count = items.size
                    val spacing = 16.dp.toPx()
                    val totalSpacing = spacing * (count + 1)
                    val barWidth = ((canvasWidth - totalSpacing) / count).coerceIn(24.dp.toPx(), 48.dp.toPx())

                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = (canvasHeight / gridSteps) * i
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val labelStyle = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    val valueStyle = TextStyle(
                        color = onSurfaceColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    items.forEachIndexed { index, item ->
                        val maxVal = if (item.maxValue > 0f) item.maxValue else 100f
                        val safeValue = item.value.coerceAtLeast(0f)
                        val barHeight = ((safeValue / maxVal) * canvasHeight).coerceAtMost(canvasHeight)
                        val x = spacing + index * (barWidth + spacing)

                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(x, canvasHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        if (item.secondaryValue != null && item.secondaryColor != null) {
                            val secVal = item.secondaryValue.coerceAtLeast(0f)
                            val secHeight = ((secVal / maxVal) * canvasHeight).coerceAtMost(canvasHeight)
                            drawRoundRect(
                                color = item.secondaryColor.copy(alpha = 0.6f),
                                topLeft = Offset(x + barWidth * 0.7f, canvasHeight - secHeight),
                                size = Size(barWidth * 0.3f, secHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        val valLayout = textMeasurer.measure(item.value.toInt().toString(), valueStyle)
                        drawText(
                            textLayoutResult = valLayout,
                            topLeft = Offset(
                                x + (barWidth - valLayout.size.width) / 2,
                                (canvasHeight - barHeight - valLayout.size.height - 4f).coerceAtLeast(0f)
                            )
                        )

                        val displayLabel = if (item.label.length > 7) item.label.take(6) + "…" else item.label
                        val lblLayout = textMeasurer.measure(displayLabel, labelStyle)
                        drawText(
                            textLayoutResult = lblLayout,
                            topLeft = Offset(
                                x + (barWidth - lblLayout.size.width) / 2,
                                canvasHeight + 8.dp.toPx()
                            )
                        )
                    }
                }
            }
        }
    }
}
