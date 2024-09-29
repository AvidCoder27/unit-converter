package com.reeves.unitconverter

data class ConversionStep(
    val topValue: Double,
    val topUnit: Unit,
    val bottomValue: Double,
    val bottomUnit: Unit
) {
    override fun toString(): String {
        return "(${topValue.truncate(2)} $topUnit / ${bottomValue.truncate(2)} $bottomUnit)"
    }

    fun top(): String {
        return "${topValue.truncate(2)} ${topUnit.abbreviation()}"
    }

    fun bottom(): String {
        return "${bottomValue.truncate(2)} ${bottomUnit.abbreviation()}"
    }
}