package com.reeves.unitconverter

private const val CDOT = "\\cdot"

class KatexStringBuilder {
    private val builder: StringBuilder = StringBuilder()

    init {
        builder.append("\t\$\\displaystyle ")
    }

    override fun toString(): String {
        builder.append('$')
        return builder.toString()
    }

    fun appendInverseQuantity(inverse: Quantity) {
        " \\left( ".append()
        appendValue(inverse.value)
        appendUnits(inverse)
        " \\right)^{-1}".append()
        appendEqualsSign()
    }

    fun appendConversion(step: Conversion, exponent: Int) {
        " \\left( \\frac{".append()
        step.numerator.appendQuantity(action = SimpleUnit::abbreviation)
        "}{".append()
        step.denominator.appendQuantity(action = SimpleUnit::abbreviation)
        "}".append()
        " \\right)".append()
        if (exponent > 1) "^{${exponent}}".append()
    }

    fun appendValue(value: Double) {
        value.beatify().append()
        "\\ ".append()
    }

    fun appendUnits(quantity: Quantity) {
        val numerator = quantity.top()
        val denominator = quantity.bottom()
        if (denominator.isNotEmpty()) " \\frac{".append()

        numerator.onEachIndexed { index, entry ->
            if (index < numerator.size - 1) {
                entry.toPair().katex(SimpleUnit::singular).append()
                CDOT.append()
            } else {
                entry.toPair().katex(SimpleUnit::plural).append()
            }
        }
        if (denominator.isNotEmpty()) {
            "}{".append()
            denominator.toList().joinToString(CDOT) { it.katex(SimpleUnit::singular) }.append()
            "}".append()
        }
    }

    fun appendEqualsSign() {
        " = ".append()
    }

    fun appendMultiplicationSign() {
        CDOT.append()
    }

    private fun String.append() {
        builder.append(this)
    }

    private fun Quantity.appendQuantity(
        ignoreOne: Boolean = false,
        action: (SimpleUnit) -> String,
    ) {
        if (value != 1.0 || !ignoreOne) {
            appendValue(value)
        }
        units.toList().joinToString(CDOT) { it.katex(action) }.append()
    }

    private fun Pair<SimpleUnit, Int>.katex(action: (SimpleUnit) -> String) =
        " \\text{${action(first)}}" + if (second != 1) "^$second" else ""

    private fun Double.beatify() = truncate(2)
}
