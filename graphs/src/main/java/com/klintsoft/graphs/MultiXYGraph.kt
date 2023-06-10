package com.klintsoft.graphs

import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalTextApi::class)
@Composable
fun MultiXYGraph(
    graphs: List<XYGraph>, //for multi-graphs on same canvas
    modifier: Modifier = Modifier,
    title: String = "",
    unitsX: String = "",
    unitsY: String = "",
    titleStyle: TextStyle = MaterialTheme.typography.caption,
    labelX: String = "",
    labelY: String = "",
    showGrid: Boolean = true,
    scaleXAxis: List<Float> = listOf(),
    scaleYAxis: List<Float> = listOf(),
    backgroundColor: Color = MaterialTheme.colors.background,
    axisColor: Color = MaterialTheme.colors.onBackground,
    scaleColor: Color = MaterialTheme.colors.onBackground,
    scaleTextStyle: TextStyle = MaterialTheme.typography.caption,
    labelTextStyle: TextStyle = MaterialTheme.typography.caption,
    showLegend: Boolean = true,
    legendTextStyle: TextStyle = MaterialTheme.typography.caption,
) {
    val textMeasurer = rememberTextMeasurer()

    val allPoints = graphs.map { it.points }.flatten()

    val maxScaleTextHeight = allPoints.maxOfOrNull {
        textMeasurer.measure(
            AnnotatedString("${it.x}"),
            style = labelTextStyle
        ).size.height
    } ?: 0
    val maxXScaleTextWidth = allPoints.maxOfOrNull {
        textMeasurer.measure(
            AnnotatedString("${it.x}"),
            style = labelTextStyle
        ).size.width
    } ?: 0

    val fontWidth = scaleTextStyle.fontSize.value

    Column(
        modifier = modifier.background(color = backgroundColor)
            .border(1.dp, color = Color.Black)

    ) {
        if(title.isNotEmpty()) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textAlign = TextAlign.Center,
                style = titleStyle,
                color = scaleColor
            )
        }

        /** Canvas **/
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(
                    top = with(LocalDensity.current) { maxScaleTextHeight.toDp() / 2 },
                    end = with(LocalDensity.current) { maxXScaleTextWidth.toDp() / 2 }
                )

            )
        {
            val rect = Rect(Offset.Zero, size)

            val graphHeight =
                size.height - maxScaleTextHeight - fontWidth * 2 - if (labelX.isNotEmpty()) maxScaleTextHeight else 0

            val scaleY = scaleYAxis.map { it.toString() }.ifEmpty {
                autoScale(
                    allPoints.map { it.y },
                    graphHeight.toInt(),
                    fontWidth
                )
            }
            Log.i("MyInfo", "MultiXYGraph: scaleY: $scaleY")

            val maxYScaleTextWidth = scaleY.maxOfOrNull {
                textMeasurer.measure(
                    AnnotatedString(it),
                    style = labelTextStyle
                ).size.width
            } ?: 0

            val graphWidth =
                size.width - maxYScaleTextWidth - fontWidth * 2 - if (labelY.isNotEmpty()) maxScaleTextHeight else 0

            val graphSize = Size(graphWidth, graphHeight)

            val scaleX = scaleXAxis.map { it.toString() }
                .ifEmpty { //if scale for X axis is empty then autoScale
                    autoScale(allPoints.map { it.x }, graphSize.width.toInt(), fontWidth)
                }

            val scaleXF = scaleXAxis.ifEmpty { scaleX.map { it.toFloat() } }
            val scaleYF = scaleYAxis.ifEmpty { scaleY.map { it.toFloat() } }

            val maxX = scaleXF.maxOrNull() ?: 0f
            val minX = scaleXF.minOrNull() ?: 0f
            val maxY = scaleYF.maxOrNull() ?: 0f
            val minY = scaleYF.minOrNull() ?: 0f

            val xMultiplier = graphSize.width / (maxX - minX)
            val yMultiplier = graphSize.height / (maxY - minY)

            val xGraphStart = size.width - graphSize.width

            //Draw axes
            drawLine(
                axisColor,
                Offset(xGraphStart, 0f),
                Offset(xGraphStart, graphSize.height),
                5f
            )
            drawLine(
                axisColor,
                Offset(xGraphStart, graphSize.height),
                Offset(size.width, graphSize.height),
                5f
            )

            /** Create each graph one by one on the canvas **/
            graphs.forEach { graph ->
                val coordinates = mutableListOf<PointF>()
                val controlPoints1 = mutableListOf<PointF>()
                val controlPoints2 = mutableListOf<PointF>()

                /** Draw points **/
                for (point in graph.points) {
                    val x1 = xGraphStart + (point.x - minX) * xMultiplier
                    val y1 = (maxY - point.y) * yMultiplier
                    coordinates.add(PointF(x1, y1))
                    if (graph.showPoints) {
                        drawCircle(
                            color = graph.pointColor,
                            center = Offset(x1, y1),
                            radius = graph.pointSize.toPx()
                        )
                    }
                }
                if (graph.showLine) {
                    if (graph.curveStyle == CurveStyle.BICUBIC) {
                        /** calculating the Bicubic Bezier control points */
                        for (i in 1 until coordinates.size) {
                            controlPoints1.add(
                                PointF(
                                    (coordinates[i].x + coordinates[i - 1].x) / 2,
                                    coordinates[i - 1].y
                                )
                            )
                            controlPoints2.add(
                                PointF(
                                    (coordinates[i].x + coordinates[i - 1].x) / 2,
                                    coordinates[i].y
                                )
                            )
                        }
                        /** Drawing the path */
                        val stroke = Path().apply {
                            reset()
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 0 until coordinates.size - 1) {
                                cubicTo(
                                    controlPoints1[i].x, controlPoints1[i].y,
                                    controlPoints2[i].x, controlPoints2[i].y,
                                    coordinates[i + 1].x, coordinates[i + 1].y
                                )
                            }
                        }
                        drawPath(
                            stroke,
                            color = graph.lineColor,
                            style = Stroke(
                                width = graph.lineWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    } else if (graph.curveStyle == CurveStyle.LINEAR) {
                        /** Drawing the path */
                        val stroke = Path().apply {
                            reset()
                            moveTo(coordinates.first().x, coordinates.first().y)
                            for (i in 1 until coordinates.size) {
                                lineTo(coordinates[i].x, coordinates[i].y,)
                            }
                        }
                        drawPath(
                            stroke,
                            color = graph.lineColor,
                            style = Stroke(width = graph.lineWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            } //end of individual graph

            val xScaleMultiplier = graphSize.width / (scaleX.size - 1)
            val yScaleMultiplier = graphSize.height / (scaleY.size - 1)

            /** Draw grid **/
            if(showGrid) {
                scaleXF.forEach {
                    val x = xGraphStart + (it - minX) * xMultiplier
                    drawLine(
                        axisColor,
                        Offset(x, 0f),
                        Offset(x, graphSize.height),
                        1f
                    )
                }
                scaleYF.forEach {
                    val y = (maxY - it) * yMultiplier
                    drawLine(
                        axisColor,
                        Offset(xGraphStart, y),
                        Offset(size.width, y),
                        1f
                    )
                }
            }

            /** Draw the scales **/
            scaleX.forEachIndexed { index, it ->
                val scalePointX = xGraphStart + index * xScaleMultiplier
                val measuredText =
                    textMeasurer.measure(
                        AnnotatedString(it),
                        style = scaleTextStyle
                    )
                drawLine(
                    scaleColor,
                    Offset(scalePointX, graphSize.height),
                    Offset(scalePointX, graphSize.height + scaleTextStyle.fontSize.value)
                )
                drawText(
                    measuredText,
                    color = scaleColor,
                    topLeft = Offset(
                        scalePointX - measuredText.size.width / 2,
                        graphSize.height + scaleTextStyle.fontSize.value * 2
                    )
                )
            }
            scaleY.forEachIndexed { index, it ->
                val scalePointY = graphSize.height - index * yScaleMultiplier
                val measuredText =
                    textMeasurer.measure(
                        AnnotatedString(it),
                        style = scaleTextStyle
                    )
                drawLine(
                    scaleColor,
                    Offset(xGraphStart - scaleTextStyle.fontSize.value, scalePointY),
                    Offset(xGraphStart, scalePointY)
                )
                drawText(
                    measuredText,
                    color = scaleColor,
                    topLeft = Offset(
                        xGraphStart - scaleTextStyle.fontSize.value * 2 - measuredText.size.width,
                        scalePointY - measuredText.size.height / 2f
                    )
                )
            }
            /** Draw labels **/
            if (labelX.isNotEmpty()) {
                val measuredXLabel = textMeasurer.measure(
                    AnnotatedString(labelX + if(unitsX.isNotEmpty()) " ($unitsX)" else ""),
                    style = labelTextStyle
                )
                drawText(
                    measuredXLabel,
                    color = scaleColor,
                    topLeft = Offset(
                        xGraphStart + graphWidth / 2 - measuredXLabel.size.width / 2,
                        size.height - measuredXLabel.size.height
                    )
                )
            }
            if (labelY.isNotEmpty()) {
                val measuredYLabel = textMeasurer.measure(
                    AnnotatedString(labelY + if(unitsY.isNotEmpty()) " ($unitsY)" else ""),
                    style = labelTextStyle
                )
                val newX = rect.center.x - measuredYLabel.size.width / 2
                val newY = (size.height - size.width) / 2
                rotate(-90f, pivot = rect.center) {
                    drawText(
                        measuredYLabel,
                        color = scaleColor,
                        topLeft = Offset(newX, newY)
                    )
                }
            }
        }
        /** Graph Legend **/
        if(showLegend) {
            graphs.forEach { graph ->
                Row(
                    modifier = Modifier.padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier
                            .weight(0.1f)
                            .padding(horizontal = 8.dp),
                        color = graph.lineColor,
                        thickness = 4.dp
                    )
                    Text(
                        text = graph.description.ifEmpty { "N/A" },
                        modifier = Modifier
                            .weight(0.9f),
                        style = legendTextStyle,
                        color = scaleColor
                    )
                }
            }
        }
    }
}
