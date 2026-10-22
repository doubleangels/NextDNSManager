package com.doubleangels.nextdnsmanagement.ui.components

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

fun formatCount(value: Long): String =
    CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(value)

fun formatPercent(fraction: Double): String =
    NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }.format(fraction)

data class ChartLine(val label: String, val color: Color, val values: List<Long>)

data class Slice(val label: String, val value: Long, val color: Color)

/** Rounds up to 1, 2 or 5 times a power of ten, so axis labels stay readable. */
private fun niceCeiling(value: Long): Long {
    if (value <= 0) return 1
    val magnitude = 10.0.pow(ceil(log10(value.toDouble())) - 1)
    val step = listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= value }
    return (step * magnitude).toLong().coerceAtLeast(1)
}

/**
 * Area line chart. Tap or drag across it to inspect a point; [pointLabel] describes
 * the inspected index (usually its time).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineChart(
    lines: List<ChartLine>,
    pointLabel: (Int) -> String,
    axisLabels: Triple<String, String, String>,
    modifier: Modifier = Modifier,
) {
    val points = lines.maxOfOrNull { it.values.size } ?: 0
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = niceCeiling(lines.flatMap { it.values }.maxOrNull() ?: 0L)
    val leftPad = with(LocalDensity.current) { 36.dp.toPx() }
    val bottomPad = with(LocalDensity.current) { 18.dp.toPx() }

    fun indexAt(x: Float, width: Float): Int? {
        if (points < 2) return null
        val plotWidth = width - leftPad
        return (((x - leftPad) / plotWidth) * (points - 1)).toInt().coerceIn(0, points - 1)
    }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .semantics { contentDescription = lines.joinToString { "${it.label}: ${formatCount(it.values.sum())}" } }
                .pointerInput(points) {
                    detectTapGestures { selected = indexAt(it.x, size.width.toFloat()) }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        selected = indexAt(change.position.x, size.width.toFloat())
                    }
                },
        ) {
            val plotWidth = size.width - leftPad
            val plotHeight = size.height - bottomPad
            // Grid lines and y-axis labels at 0, 50% and 100% of the scale.
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = plotHeight * (1 - fraction)
                drawLine(gridColor, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1f)
                val label = formatCount((maxValue * fraction).toLong())
                val measured = textMeasurer.measure(label, labelStyle)
                drawText(
                    textMeasurer = textMeasurer, text = label, style = labelStyle,
                    topLeft = Offset(leftPad - measured.size.width - 6f, (y - measured.size.height / 2f).coerceAtLeast(0f)),
                )
            }
            // X-axis labels: start, middle, end.
            listOf(axisLabels.first to 0f, axisLabels.second to 0.5f, axisLabels.third to 1f).forEach { (label, fraction) ->
                val measured = textMeasurer.measure(label, labelStyle)
                val x = (leftPad + plotWidth * fraction - measured.size.width * fraction)
                drawText(textMeasurer = textMeasurer, text = label, style = labelStyle, topLeft = Offset(x, plotHeight + 4f))
            }
            if (points < 2) return@Canvas
            fun xOf(i: Int) = leftPad + plotWidth * i / (points - 1)
            fun yOf(v: Long) = plotHeight * (1 - v.toFloat() / maxValue)
            lines.forEach { line ->
                if (line.values.size < 2) return@forEach
                val stroke = Path()
                line.values.forEachIndexed { i, v ->
                    if (i == 0) stroke.moveTo(xOf(i), yOf(v)) else stroke.lineTo(xOf(i), yOf(v))
                }
                val area = Path().apply {
                    addPath(stroke)
                    lineTo(xOf(line.values.lastIndex), plotHeight)
                    lineTo(xOf(0), plotHeight)
                    close()
                }
                drawPath(
                    area,
                    Brush.verticalGradient(listOf(line.color.copy(alpha = 0.28f), line.color.copy(alpha = 0f)), endY = plotHeight),
                )
                drawPath(stroke, line.color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            selected?.let { index ->
                drawLine(gridColor, Offset(xOf(index), 0f), Offset(xOf(index), plotHeight), strokeWidth = 2f)
                lines.forEach { line ->
                    line.values.getOrNull(index)?.let { drawCircle(line.color, 4.dp.toPx(), Offset(xOf(index), yOf(it))) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val index = selected
            if (index != null) {
                Text(pointLabel(index), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            lines.forEach { line ->
                val value = if (index != null) line.values.getOrNull(index) ?: 0L else line.values.sum()
                LegendItem(line.color, "${line.label}: ${formatCount(value)}")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Donut chart with a legend beside it. */
@Composable
fun DonutChart(
    slices: List<Slice>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(132.dp).semantics { contentDescription = "$centerValue $centerLabel" }) {
                val strokeWidth = 16.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
                val visible = slices.filter { it.value > 0 }
                val gap = if (visible.size > 1) 2f else 0f
                var start = -90f
                visible.forEach { slice ->
                    val sweep = 360f * slice.value / total
                    drawArc(
                        slice.color, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, topLeft, arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerValue, style = MaterialTheme.typography.titleMedium)
                Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { slice ->
                LegendItem(slice.color, "${slice.label} · ${formatPercent(slice.value.toDouble() / total)}")
            }
        }
    }
}

/** Ranked horizontal bars; each row's bar is relative to the largest value. */
@Composable
fun BarList(
    rows: List<BarRow>,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: ((BarRow) -> Unit)? = null,
) {
    val max = rows.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (onClick != null) Modifier.clickable { onClick(row) } else Modifier)
                    .padding(vertical = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        row.detail?.let {
                            Text(
                                it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(formatCount(row.value), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color.copy(alpha = 0.15f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(row.value.toFloat() / max)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

data class BarRow(val key: String, val label: String, val value: Long, val detail: String? = null)
