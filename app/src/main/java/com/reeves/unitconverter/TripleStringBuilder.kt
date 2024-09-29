package com.reeves.unitconverter

class TripleStringBuilder(private val maxEms: Int) {
    private val squished: StringBuilder = StringBuilder()
    private val top: StringBuilder = StringBuilder()
    private val middle: StringBuilder = StringBuilder()
    private val bottom: StringBuilder = StringBuilder()

    private fun openParen() {
        top.append("⎧ ")
        middle.append("⎪-")
        bottom.append("⎩ ")
    }

    private fun closeParen() {
        top.append(" ⎫")
        middle.append("-⎪")
        bottom.append(" ⎭")
    }

    fun checkForSquish(finalNewLines: Int): StringBuilder {
        if (top.length >= maxEms) {
            squish(finalNewLines)
        }
        return squished
    }

    fun squish(finalNewLines: Int): StringBuilder {
        squished.append(top)
        squished.append('\n')
        squished.append(middle)
        squished.append('\n')
        squished.append(bottom)
        for (i in 0 until finalNewLines) {
            squished.append('\n')
        }
        top.clear()
        middle.clear()
        bottom.clear()
        return squished
    }

    private fun extend(center: Char = ' ') {
        val maxWidth = maxOf(top.length, middle.length, bottom.length)
        top.extend(maxWidth, ' ')
        middle.extend(maxWidth, center)
        bottom.extend(maxWidth, ' ')
    }

    fun appendConversionStep(step: ConversionStep) {
        openParen()
        top.append(step.top())
        bottom.append(step.bottom())
        extend('-')
        closeParen()
    }

    fun appendValue(value: Double, precision: Int) {
        middle.append(value.truncate(precision))
        middle.append(' ')
        extend()
    }

    fun appendUnits(numerator: List<Unit>, denominator: List<Unit>) {
        val numeratorBuilder =
            if (denominator.isEmpty()) middle
            else top

        if (numerator.size == 1) {
            numeratorBuilder.append(numerator[0].plural())
        } else {
            for (i in 0 until numerator.size - 1) {
                numeratorBuilder.append(numerator[i].singular())
                numeratorBuilder.append(" * ")
            }
            numeratorBuilder.append(numerator[numerator.size - 1].plural())
        }

        if (denominator.isEmpty()) {
            extend()
        } else {
            bottom.append(denominator.joinToString(" * ") { it.singular() })
            extend('-')
        }
    }

//    fun appendAll(any: Any?) {
//        top.append(any)
//        middle.append(any)
//        bottom.append(any)
//    }

    fun appendMiddle(any: Any?) {
        middle.append(any)
        extend()
    }

    private fun StringBuilder.extend(length: Int, char: Char) {
        while (this.length < length) {
            this.append(char)
        }
    }
}