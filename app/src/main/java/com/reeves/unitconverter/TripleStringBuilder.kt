package com.reeves.unitconverter

class TripleStringBuilder(private val maxEms: Int) {
    private val squashed: StringBuilder = StringBuilder()
    private val squishedTop: StringBuilder = StringBuilder()
    private val squishedMiddle: StringBuilder = StringBuilder()
    private val squishedBottom: StringBuilder = StringBuilder()
    private val top: StringBuilder = StringBuilder()
    private val middle: StringBuilder = StringBuilder()
    private val bottom: StringBuilder = StringBuilder()

    /**
     * Finalizes everything and returns it as a string
     */
    fun squishSquash(finalNewLines: Int): String {
        squish()
        squash(finalNewLines)
        return squashed.toString()
    }

    fun appendConversionStep(step: ConversionStep, finalNewLines: Int) {
        openParen()
        top.append(step.top())
        bottom.append(step.bottom())
        extend('-')
        closeParen()
        check(finalNewLines)
    }

    fun appendValue(value: Double, precision: Int, finalNewLines: Int) {
        middle.append(value.truncate(precision))
        middle.append(' ')
        extend()
        check(finalNewLines)
    }

    fun appendUnits(numerator: Map<SimpleUnit, Int>, denominator: Map<SimpleUnit, Int>, finalNewLines: Int) {
        val numeratorBuilder =
            if (denominator.isEmpty()) middle
            else top

        if (numerator.size == 1) {
            numeratorBuilder.append(numerator.keys.first().plural())
            numeratorBuilder.append(numerator.values.first().toSuperscript())
        } else {
            numerator.onEachIndexed { index, entry ->
                if (index < numerator.size - 1) {
                    numeratorBuilder.append(entry.key.singular())
                    numeratorBuilder.append(entry.value.toSuperscript())
                    numeratorBuilder.append(" * ")
                }
                else {
                    numeratorBuilder.append(entry.key.plural())
                    numeratorBuilder.append(entry.value.toSuperscript())
                }
            }
        }

        if (denominator.isEmpty()) {
            extend()
        } else {
            bottom.append(denominator.map {
                "${it.key.singular()}${it.value.toSuperscript()}"
            }.joinToString(" * "))
            extend('-')
        }
        check(finalNewLines)
    }

    fun appendMiddle(any: Any?, finalNewLines: Int) {
        middle.append(any)
        extend()
        check(finalNewLines)
    }

    fun appendExponent(exponent: Int, finalNewLines: Int) {
        if (exponent < 2) return
        top.append(exponent.toString())
        extend()
        check(finalNewLines)
    }

    /**
     * This will squash if necessary and always squish the un-squished
     * @param finalNewLines the number of lines to append if it has to squash
     */
    private fun check(finalNewLines: Int) {
        // if combining the squished stuff with the un-squished would be too long...
        if (top.length + squishedTop.length >= maxEms * 0.75) {
            // then we squash the squished
            squash(finalNewLines)
        }
        // then, we squish the un-squished
        squish()
    }

    /**
     * Move the squished text into the squashed text
     * This creates a triple line break
     */
    private fun squash(finalNewLines: Int) {
        squashed.append(squishedTop)
        squashed.append('\n')
        squashed.append(squishedMiddle)
        squashed.append('\n')
        squashed.append(squishedBottom)
        for (i in 0 until finalNewLines) {
            squashed.append('\n')
        }
        squishedTop.clear()
        squishedMiddle.clear()
        squishedBottom.clear()
    }

    /**
     * Move the current normal text into the squished rows
     * The squished text can be squashed safely without disrupting continuity
     */
    private fun squish() {
        squishedTop.append(top)
        squishedMiddle.append(middle)
        squishedBottom.append(bottom)
        top.clear()
        middle.clear()
        bottom.clear()
    }

    private fun extend(center: Char = ' ') {
        val maxWidth = maxOf(top.length, middle.length, bottom.length)
        top.extend(maxWidth, ' ')
        middle.extend(maxWidth, center)
        bottom.extend(maxWidth, ' ')
    }

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

    private fun StringBuilder.extend(length: Int, char: Char) {
        while (this.length < length) {
            this.append(char)
        }
    }
}