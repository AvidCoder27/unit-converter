package com.reeves.unitconverter

data class ConversionStep(
    val topValue: Double,
    val topUnit: Unit,
    val bottomValue: Double,
    val bottomUnit: Unit
)