package com.reeves.unitconverter

import kotlin.math.absoluteValue

data class Quantity(val value: Double, val units: Map<SimpleUnit, Int>) :
    Iterable<Map.Entry<SimpleUnit, Int>> {

    fun dimensionality(): Map<DIMENSION, Int> = mutableMapOf<DIMENSION, Int>().also { map ->
        units.forEach { (unit, unitCount) ->
            unit.dimensionality.forEach { (dimension, dimensionCount) ->
                map[dimension] = (map[dimension] ?: 0) + unitCount * dimensionCount
            }
        }
    }

    fun complexity(): Int = 0// make it combined complexity of all units minus total unit count //dimensionality().values.sumOf { it.absoluteValue } * 2 - units.size

    fun multiply(other: Quantity) = Quantity(value * other.value, HashMap(units).apply {
        other.units.forEach { (unit, count) ->
            this[unit] = (this[unit] ?: 0) + count
        }
    }).clean()

    fun multiply(conversion: Conversion) =
        multiply(conversion.numerator).divide(conversion.denominator)

    fun divide(conversion: Conversion) =
        divide(conversion.numerator).multiply(conversion.denominator)

    fun divide(other: Quantity) = multiply(other.inverse())

    fun inverse() = Quantity(1.0 / value, units.mapValues { -it.value })

    fun clean() = Quantity(value, units.clean())

    fun top() = units.filterValues { it > 0 }

    fun bottom() = units.filterValues { it < 0 }.mapValues { -it.value }

    fun multiply(value: Double) = Quantity(this.value * value, units)

    fun divide(value: Double) = Quantity(this.value / value, units)

    fun removeValue() = Quantity(1.0, units)

    fun expand(positives: Boolean) = units.flatMap { (unit, count) ->
        if (positives && count > 0) List(count) { unit }
        else if (!positives && count < 0) List(-count) { unit }
        else listOf()
    }

    override fun iterator(): Iterator<Map.Entry<SimpleUnit, Int>> = units.iterator()

    override fun toString(): String = buildString {
        append(value)
        append(" ")
        append(units.map { it.key.abbreviation() + it.value.toSuperscript() }.joinToString("×"))
    }
}