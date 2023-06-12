package com.klintsoft.graphs

import android.graphics.PointF

class BezierSpline {
    fun getCurveControlPoints(points: List<PointF>): ControlPoints {
        // Calculate first Bezier control points
        // Right hand side vector
        //double[] rhs = new double[n];
        val n = points.size - 1

        val firstControlPoints = Array<PointF>(n) { PointF(0f, 0f) }
        val secondControlPoints = Array<PointF>(n) { PointF(0f, 0f) };

        if (n < 1) throw Exception("At least two knot points required")
        if (n == 1) { // Special case: Bezier curve should be a straight line.
            // 3P1 = 2P0 + P3
            firstControlPoints[0].x = (2 * points[0].x + points[1].x) / 3
            firstControlPoints[0].x = (2 * points[0].y + points[1].y) / 3
            // P2 = 2P1 – P0
            secondControlPoints[0].x = 2 *
                    firstControlPoints[0].x - points[0].x
            secondControlPoints[0].y = 2 *
                    firstControlPoints[0].y - points[0].y
            return ControlPoints(
                first = firstControlPoints.toList(),
                second = secondControlPoints.toList()
            )
        }

        val rhs = DoubleArray(n)

        // Set right hand side X values
        //for (int i = 1; i < n - 1; ++i)
        for (i in 1 until n) {
            rhs[i] = 4.0 * points[i].x + 2.0 * points[i + 1].x
        }
        rhs[0] = points[0].x + 2.0 * points[1].x
        rhs[n - 1] = (8.0 * points[n - 1].x + points[n].x) / 2.0;
        // Get first control points X-values
        val x = getFirstControlPoints(rhs) ?: DoubleArray(n) { 0.0 }

        // Set right hand side Y values
        for (i in 1 until n) {
            rhs[i] = 4.0 * points[i].y + 2.0 * points[i + 1].y;
        }
        rhs[0] = points[0].y + 2.0 * points[1].y;
        rhs[n - 1] = (8.0 * points[n - 1].y + points[n].y) / 2.0;
        // Get first control points Y-values
        val y = getFirstControlPoints(rhs) ?: DoubleArray(n) { 0.0 }

        // Fill output arrays.
        for (i in 0 until n) {
            // First control point
            firstControlPoints[i] = PointF(x[i].toFloat(), y[i].toFloat())
            // Second control point
            if (i < n - 1)
                secondControlPoints[i] = PointF(
                    (2.0 * points[i + 1].x - x[i + 1]).toFloat(),
                    (2.0 * points[i + 1].y - y[i + 1]).toFloat()
                )
            else
                secondControlPoints[i] = PointF(
                    ((points[n].x + x[n - 1]) / 2).toFloat(),
                    ((points[n].y + y[n - 1]) / 2).toFloat()
                )
        }
        return ControlPoints(
            first = firstControlPoints.toList(),
            second = secondControlPoints.toList()
        )
    }

    /// <summary>
/// Solves a tridiagonal system for one of coordinates (x or y)
/// of first Bezier control points.
/// </summary>
/// <param name="rhs">Right hand side vector.</param>
/// <returns>Solution vector.</returns>
    private fun getFirstControlPoints(rhs: DoubleArray): DoubleArray? {
        val n: Int = rhs.size
        val x = DoubleArray(n) // Solution vector.
        val tmp = DoubleArray(n) // Temp workspace.
        var b = 2.0
        x[0] = rhs[0] / b
        for (i in 1 until n)  // Decomposition and forward substitution.
        {
            tmp[i] = 1 / b
            b = (if (i < n - 1) 4.0 else 3.5) - tmp[i]
            x[i] = (rhs[i] - x[i - 1]) / b
        }
        for (i in 1 until n) x[n - i - 1] -= tmp[n - i] * x[n - i] // BackSubstitution.
        return x
    }

    data class ControlPoints(
        val first: List<PointF> = listOf(),
        val second: List<PointF> = listOf(),
    )
}