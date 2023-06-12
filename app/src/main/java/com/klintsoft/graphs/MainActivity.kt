package com.klintsoft.graphs

import android.graphics.PointF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.klintsoft.graphs.ui.theme.GraphsTheme
import kotlin.math.pow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GraphsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    GraphsExample()
                }
            }
        }
    }
}

//val xis2 = listOf(1f, 3f, 8f, 4f, 13f, 18f, 9f)


val xis2 = listOf(0f, 3f, 4f, 7f, 8f, 9f, 12f, 19f)
val yis2 = xis2.map { it.pow(2) }
val points2 = List(xis2.size) { i -> PointF(xis2[i], yis2[i]) }

@Composable
fun GraphsExample() {
    Text(text = "Hello")
    XYGraph(
        points = points2.sortedBy { it.x },
        modifier = Modifier.fillMaxSize(),
        showLine = true,
        curveStyle = CurveStyle.BICUBIC
    )
}