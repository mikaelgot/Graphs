package com.klintsoft.graphs

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.text.substring

fun autoScale(axisValues: List<Float>, dimensionPx: Int, fontWidth: Float): List<String> {
    //Log.i("MyInfo", "yPoints: ${axisValues.sorted()}")
    val minY = axisValues.minOrNull() ?: 0f
    //Log.i("MyInfo", "minY: $minY")
    val maxY = axisValues.maxOrNull() ?: 0f
    //Log.i("MyInfo", "maxY: $maxY")
    val range = maxY - minY
    //Log.i("MyInfo", "range: $range")
    //Log.i("MyInfo", "fontWidth: $fontWidth")
    val orderOfRange = orderOfNumber(range.toDouble())
    //Log.i("MyInfo", "orderOfRange: $orderOfRange")
    val xTextWidth =
        if (orderOfRange > 0) (orderOfRange + 2) * fontWidth
    else (abs(orderOfRange) + 3) * fontWidth


    val minNumberOfTicks = ceil(dimensionPx.toDouble() / (xTextWidth * 2).toDouble()).toInt()
    //Log.i("MyInfo", "minNumberOfTicks: $minNumberOfTicks")
    val earlyTickSpacing = (range / minNumberOfTicks.toDouble())
    //Log.i("MyInfo", "earlyTickSpacing: $earlyTickSpacing")
    val tickSpacing = fixSpacing(earlyTickSpacing)
    //Log.i("MyInfo", "tickSpacing: $tickSpacing")

    val a = floor(minY / tickSpacing).toInt()
    val b = ceil(maxY / tickSpacing).toInt()
    val allTicks = (a..b).toList().map { it * tickSpacing }
    //Log.i("MyInfo", "allTicks: $allTicks")

    return if (allTicks.all { it.isInteger() }) {
        //Log.i("MyInfo", "allTicks are integers")
        allTicks.map { it.toInt().toString() }
    } else {
        //Log.i("MyInfo", "allTicks have decimal parts")
        val decimalDigits = - orderOfNumber(tickSpacing)
        allTicks.map { String.format("%2.${decimalDigits}f", it) }
    }
}

fun Double.isInteger(): Boolean{
    val rem = abs(this.rem(1))
    return rem.equals(0.0)
}

fun fixSpacing(tickSpacing : Double): Double{
    //Log.i("MyInfo", "tickSpacing: $tickSpacing")
    val order = orderOfNumber(tickSpacing)
    val tickOptions = listOf(1.0, 2.0, 5.0, 10.0).map { it * 10.0.pow(order) }
    //Log.i("MyInfo", "tickOptions: $tickOptions")
    val closestOption = tickOptions.firstOrNull { it - tickSpacing > 0 }?.toDouble() ?: 0.0
    //Log.i("MyInfo", "closestOption: $closestOption")
    return closestOption
}

fun orderOfNumber(input: Double): Int {
    val string = String.format("%f", input)
    val order: Int =
        if (input < 1 && string.contains('.')) {
            val decimalPart = string.substring(string.indexOf('.') + 1)
            val indexOfLastNonZeroDigit = decimalPart.indexOfFirst { it != '0' }
            //Log.i("MyInfo", "indexOfLastNonZeroDigit: $indexOfLastNonZeroDigit")
            -indexOfLastNonZeroDigit - 1
        } else {
            val intPart = string.split('.').first()
            intPart.length - 1
        }
    //Log.i("MyInfo", "order: $order")
    return order
}

