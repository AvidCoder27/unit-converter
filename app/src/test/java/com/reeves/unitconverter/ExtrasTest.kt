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
}