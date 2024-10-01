package com.reeves.unitconverter

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExtrasTest {
    @Test
    fun parseUnitsToMap_isCorrect() {
        assertEquals(
            mapOf("meters" to 2, "kg" to 1, "second" to -2),
            "meters^2 * kg / second^2".parseUnitsToStringMap()
        )
        assertEquals(
            mapOf("meters" to 2, "kg" to 1, "metric ton" to 1, "second" to -2),
            "meters^2 * kg * metric ton/ second^2".parseUnitsToStringMap()
        )
    }

    @Test
    fun quantityDimensionality_isCorrect() {
        val meter = SimpleUnit(listOf("meter", "meters", "m"), mapOf(DIMENSION.LENGTH to 1))
        val foot = SimpleUnit(listOf("foot", "feet", "ft"), mapOf(DIMENSION.LENGTH to 1))
        val mile = SimpleUnit(listOf("mile", "miles", "mi"), mapOf(DIMENSION.LENGTH to 1))
        val second = SimpleUnit(listOf("second", "seconds", "s"), mapOf(DIMENSION.TIME to 1))
        val quantity = Quantity(1.0, mapOf(meter to 2, second to -2))
        val quantity2 = Quantity(1.0, mapOf(meter to 3, second to -1, foot to 1, mile to 1))
        assertEquals(mapOf(DIMENSION.LENGTH to 2, DIMENSION.TIME to -2), quantity.dimensionality())
        assertEquals(mapOf(DIMENSION.LENGTH to 5, DIMENSION.TIME to -1), quantity2.dimensionality())
    }

    @Test
    fun complexity_isCorrect() {
        val meter = SimpleUnit(listOf("meter", "meters", "m"), mapOf(DIMENSION.LENGTH to 1))
        val quantity = Quantity(1.0, mapOf(meter to 1))
        assertEquals(0, quantity.complexity())
    }
}