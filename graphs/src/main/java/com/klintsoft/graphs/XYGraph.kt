package com.klintsoft.graphs

import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalTextApi::class)
@Composable
fun XYGraph(
    points: List<PointF>,
    modifier: Modifier = Modifier,
    title: String = "",
    unitsX: String = "",
    unitsY: String = "",
    titleStyle: TextStyle = MaterialTheme.typography.caption,
    showLine: Boolean = false,
    showPoints: Boolean = true,
    showGrid: Boolean = true,
    pointSize: Dp = 5.dp,
    lineWidth: Float = 5f,
    curveStyle: CurveStyle = CurveStyle.LINEAR,
    labelX: String = "",
    labelY: String = "",
    scaleXAxis: List<Float> = listOf(),
    scaleYAxis: List<Float> = listOf(),
    backgroundColor: Color = MaterialTheme.colors.background,
    axisColor: Color = MaterialTheme.colors.onBackground,
    pointColor: Color = MaterialTheme.colors.onBackground,
    lineColor: Color = MaterialTheme.colors.onBackground,
    scaleColor: Color = MaterialTheme.colors.onBackground,
    scaleTextStyle: TextStyle = MaterialTheme.typography.caption,
    labelTextStyle: TextStyle = MaterialTheme.typography.caption,
) {
    val textMeasurer = rememberTextMeasurer()

    val maxScaleTextHeight = points.maxOfOrNull {
        textMeasurer.measure(
            AnnotatedString("${it.x}"),
            style = labelTextStyle
        ).size.height
    } ?: 0
    val maxXScaleTextWidth = points.maxOfOrNull {
        textMeasurer.measure(
            AnnotatedString("${it.x}"),
            style = labelTextStyle
        ).size.width
    } ?: 0

    val fontWidth = scaleTextStyle.fontSize.value

    Column(
        modifier = modifier.background(color = backgroundColor)
    ) {
        if (title.isNotEmpty()) Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            textAlign = TextAlign.Center,
            style = titleStyle
        )

        var tapOffset by remember { mutableStateOf(Offset(0f, 0f)) }

        /** Canvas **/
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = with(LocalDensity.current) { maxScaleTextHeight.toDp() / 2 },
                    end = with(LocalDensity.current) { maxXScaleTextWidth.toDp() / 2 }
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        tapOffset = offset
                        Log.i("MyInfo", "TAP @ offset: $offset")
                    }
                }
        )
        {
            val rect = Rect(Offset.Zero, size)

            val graphHeight =
                size.height - maxScaleTextHeight - fontWidth * 2 - if (labelX.isNotEmpty()) maxScaleTextHeight else 0

            val scaleY = scaleYAxis.map { it.toString() }
                .ifEmpty { autoScale( axisValues =  points.map { it.y }, dimensionPx = graphHeight.toInt(), fontWidth = fontWidth) }

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
                    autoScale(points.map { it.x }, graphSize.width.toInt(), fontWidth = fontWidth)
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

            /** Draw axes **/
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

            val coordinates = mutableListOf<PointF>()
            val controlPoints1 = mutableListOf<PointF>()
            val controlPoints2 = mutableListOf<PointF>()


            //Adjust point values to Canvas Offset values
            val pointsInCanvas: List<Offset> = points.map { Offset(xGraphStart + (it.x - minX) * xMultiplier,  (maxY - it.y) * yMultiplier)}
            /** Find tapped point **/
            //Find closest point to tap
            val pointTapped: Offset = pointsInCanvas.minBy { distance(it, tapOffset) }
            //range in which a point is considered tapped
            val safeTapDistance = graphWidth / (points.size * 2f)
            //if the closest point is within range the this is the one, otherwise null
            val indexOfTapped = with (pointsInCanvas.indexOf(pointTapped)) {if(distance( pointsInCanvas[this], tapOffset) < safeTapDistance) this else null}

            /** Draw points **/
            pointsInCanvas.forEachIndexed {index, point ->
                coordinates.add(PointF(point.x, point.y))
                val radius = if (index == indexOfTapped) pointSize.toPx() * 2 else pointSize.toPx()
                if (showPoints) {
                    drawCircle(
                        color = pointColor,
                        center = point,
                        radius = radius
                    )
                }
            }
            if (showLine) {
                if (curveStyle == CurveStyle.BICUBIC) {
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
                        color = lineColor,
                        style = Stroke(
                            width = lineWidth,
                            cap = StrokeCap.Round
                        )
                    )
                } else if (curveStyle == CurveStyle.LINEAR) {
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
                        color = lineColor,
                        style = Stroke(width = lineWidth, cap = StrokeCap.Round)
                    )
                }
            }

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
                    axisColor,
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
                    axisColor,
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
                Log.i("MyInfo", "size: width ${size.width}, height ${size.height}")
                rotate(-90f, pivot = rect.center) {
                    drawText(
                        measuredYLabel,
                        color = scaleColor,
                        topLeft = Offset(
                            newX,
                            newY
                        )
                    )
                }
            }
            /** Show tapped point info **/
            if (indexOfTapped != null) {
                val pointInfo = "x: ${points[indexOfTapped].x} $unitsX\ny:${points[indexOfTapped].y} $unitsY"
                val measuredInfoText =
                    textMeasurer.measure(
                        AnnotatedString(pointInfo),
                        style = scaleTextStyle
                    )
                //Fill
                drawRoundRect(
                    color =backgroundColor,
                    topLeft = Offset(graphWidth / 2f, graphHeight / 2f),
                    style = Fill,
                    cornerRadius = CornerRadius(x = 6.dp.toPx(), 6.dp.toPx()),
                    size = Size(measuredInfoText.size.width.toFloat() * 1.2f, measuredInfoText.size.height.toFloat() * 1.2f)
                )
                //Border
                drawRoundRect(
                    color = scaleColor,
                    topLeft = Offset(graphWidth / 2f, graphHeight / 2f),
                    style = Stroke(width = 1.dp.toPx()),
                    cornerRadius = CornerRadius(x = 6.dp.toPx(), 6.dp.toPx()),
                    size = Size(measuredInfoText.size.width.toFloat() * 1.2f, measuredInfoText.size.height.toFloat() * 1.2f)
                )
                drawText(
                    measuredInfoText,
                    color = scaleColor,
                    topLeft = Offset(
                        graphWidth / 2f + measuredInfoText.size.width * 0.1f,
                        graphHeight / 2f + measuredInfoText.size.height * 0.1f
                    )
                )
                Log.i("MyInfo", "indexOfTapped: $indexOfTapped, distance: ${distance(pointsInCanvas[indexOfTapped], tapOffset)}")
            }
            else Log.i("MyInfo", "indexOfTapped: NULL")
        }
    }
}

fun distance(point1: Offset, point2: Offset): Float {
    return sqrt(abs(point1.x - point2.x).pow(2) + abs(point1.y - point2.y).pow(2))
}

enum class CurveStyle{
    LINEAR,
    BICUBIC
}
