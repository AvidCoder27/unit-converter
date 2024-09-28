package com.reeves.unitconverter

data class Conversion(val numerator: Double, val denominator: Double) {
    public fun inverse(): Conversion {
        return Conversion(denominator, numerator)
    }

    public fun apply(value: RunningAnswer) {
        value.value *= numerator
        value.value /= denominator
    }
}