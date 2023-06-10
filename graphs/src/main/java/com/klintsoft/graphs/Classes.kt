package com.klintsoft.graphs

import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class XYGraph(
    val points: List<PointF>,
    val description: String = "",
    val showLine: Boolean = true,
    val showPoints: Boolean = true,
    val pointColor: Color = Color.Black,
    val lineColor: Color = Color.Black,
    val pointSize: Dp = 5.dp,
    val lineWidth: Float = 5f,
    val curveStyle: CurveStyle = CurveStyle.LINEAR,
)