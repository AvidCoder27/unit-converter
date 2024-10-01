package com.reeves.unitconverter

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitNumberFromStringTest {

    @Test
    fun extractValue_isCorrect() {
        assertEquals(Pair(5.1e-9, "meters / second"), "5.1e-9 meters / second".extractValue())
        assertEquals(Pair(1.0, "blobs * biGCS"), "blobs * biGCS".extractValue())
    }

    @Test
    fun testWithScientificNotation() {
        assertEquals(
            Pair(5.1e-9, "meters / second"), "5.1e-9 meters / second".extractValue()
        )
    }

    @Test
    fun testWithDecimalNumber() {
        assertEquals(Pair(123.45, "abc"), "123.45abc".extractValue())
    }

    @Test
    fun testWithInteger() {
        assertEquals(Pair(123.0, "xyz"), "123xyz".extractValue())
    }

    @Test
    fun testWithNoNumber() {
        assertEquals(Pair(1.0, "abc"), "abc".extractValue())
    }

    @Test
    fun testWithNegativeNumber() {
        assertEquals(Pair(-12.34, "meters"), "-12.34 meters".extractValue())
    }

    @Test
    fun testWithPositiveSign() {
        assertEquals(Pair(12.34, "meters"), "+12.34 meters".extractValue())
    }

    @Test
    fun testWithWhitespace() {
        assertEquals(Pair(12.34, "meters"), " 12.34 meters".extractValue())
    }
}