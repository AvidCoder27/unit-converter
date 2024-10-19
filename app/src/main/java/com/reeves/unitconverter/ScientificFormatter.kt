package com.reeves.unitconverter

import java.text.DecimalFormat

class ScientificFormatter(
    normalFormat: String = "###,###,###.####",
    scientificFormat: String = "#.####E0",
    private val scientificLowerBound: Double = 0.1,
    private val scientificUpperBound: Double = 1e+9,
) {
    private val normalFormatter: DecimalFormat = DecimalFormat(normalFormat)
    private val scientificFormatter: DecimalFormat = DecimalFormat(scientificFormat)

    fun format(value: Double): String =
        if (value < scientificLowerBound || value > scientificUpperBound) {
            scientificFormatter.format(value)
        } else {
            normalFormatter.format(value)
        }
}