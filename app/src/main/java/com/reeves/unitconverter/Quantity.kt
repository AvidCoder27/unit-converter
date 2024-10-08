package com.reeves.unitconverter

import kotlin.math.pow

data class Quantity(val value: Double, val units: Map<SimpleUnit, Int>) :
    Iterable<Map.Entry<SimpleUnit, Int>> {

    fun dimensionality(): Map<DIMENSION, Int> = mutableMapOf<DIMENSION, Int>().also { map ->
        units.forEach { (unit, unitCount) ->
            unit.dimensionality.forEach { (dimension, dimensionCount) ->
                map[dimension] = (map[dimension] ?: 0) + unitCount * dimensionCount
            }
        }
    }

    fun complexity(): Int = units.keys.sumOf { it.complexity } - units.size

    fun multiply(other: Quantity) = Quantity(value * other.value, HashMap(units).apply {
        other.units.forEach { (unit, count) ->
            this[unit] = (this[unit] ?: 0) + count
        }
    }).clean()

    fun divide(other: Quantity) = multiply(other.inverse())

    fun divide(unit: SimpleUnit) = divide(Quantity(1.0, mapOf(unit to 1)))

    fun pow(exponent: Int) = Quantity(value.pow(exponent), units.mapValues { (_, power) -> power * exponent })

    fun inverse() = Quantity(1.0 / value, units.mapValues { -it.value })

    fun clean() = Quantity(value, units.clean())

    fun top() = units.filterValues { it > 0 }

    fun bottom() = units.filterValues { it < 0 }.mapValues { -it.value }

    fun removeValue() = Quantity(1.0, units)

    fun withValue(value: Double) = Quantity(value, units)

    fun expand(positives: Boolean) = units.flatMap { (unit, count) ->
        if (positives && count > 0) List(count) { unit }
        else if (!positives && count < 0) List(-count) { unit }
        else listOf()
    }

    override fun iterator(): Iterator<Map.Entry<SimpleUnit, Int>> = units.iterator()

    override fun toString(): String = buildString {
        append(value)
        append(" ")
        append(units.map { it.key.abbreviation() + it.value.toSuperscript() }.joinToString(" * "))
    }
}