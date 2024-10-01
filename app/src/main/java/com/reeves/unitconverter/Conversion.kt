package com.reeves.unitconverter

data class Conversion(val numerator: Quantity, val denominator: Quantity) {

    fun inverse(): Conversion {
        return Conversion(denominator, numerator)
    }

    fun apply(value: RunningAnswer) {
        value.value *= numerator.value
        value.value /= denominator.value
    }

    /**
     * @throws IllegalArgumentException if the unit you supply cannot be converted by this conversion
     */
    fun getOther(unit: SimpleUnit): Quantity {
        if (numerator.units.containsKey(unit) && numerator.units.size == 1) {
            return denominator
        }
        if (denominator.units.containsKey(unit) && denominator.units.size == 1) {
            return numerator
        }
        throw IllegalArgumentException("This conversion does not contain $unit as the only unit in its numerator or denominator")
    }

    fun getLonelies(): Pair<SimpleUnit?, SimpleUnit?> = Pair(
        if (numerator.units.size == 1) numerator.units.keys.first() else null,
        if (denominator.units.size == 1) denominator.units.keys.first() else null,
    )

    override fun toString(): String = buildString {
        append(numerator)
        append(" = ")
        append(denominator)
    }

    fun flippedToConvertInto(unit: SimpleUnit, invert: Boolean): Conversion =
        if (denominator.units.containsKey(unit)) {
            if (invert) this else inverse()
        }
        else if (numerator.units.containsKey(unit)) {
            if (invert) inverse() else this
        }
        else throw IllegalArgumentException("This conversion does not contain $unit as the only unit in its numerator or denominator")
}