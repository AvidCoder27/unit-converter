package com.reeves.unitconverter

data class Conversion(val numerator: Double, val denominator: Double) {
    fun inverse(): Conversion {
        return Conversion(denominator, numerator)
    }

    fun apply(value: RunningAnswer) {
        value.value *= numerator
        value.value /= denominator
    }
}