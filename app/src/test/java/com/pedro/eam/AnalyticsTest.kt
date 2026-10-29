package com.pedro.eam

import com.pedro.eam.ui.components.BarChartData
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class AnalyticsTest {

    @Test
    fun testBarChartDataDynamicScaling() {
        val data = listOf(
            BarChartData("Cálculo", 10f, 15f, Color.Red),
            BarChartData("Física", 0f, 15f, Color.Blue)
        )

        assertEquals(2, data.size)
        assertEquals(10f, data[0].value, 0.01f)
        assertEquals(15f, data[0].maxValue, 0.01f)
        assertEquals(0f, data[1].value, 0.01f)
    }

    @Test
    fun testEmptyChartList() {
        val data = emptyList<BarChartData>()
        assertTrue(data.isEmpty())
    }
}
