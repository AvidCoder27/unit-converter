package com.reeves.unitconverter

data class SimpleUnit(private val names: List<String>, val dimensionality: Map<DIMENSION, Int>) {
    var complexity: Int = Int.MAX_VALUE
    private val conversions: MutableSet<Conversion> = mutableSetOf()
    private val connections: MutableSet<Conversion> = mutableSetOf()

    fun addConversion(conversion: Conversion) = conversions.add(conversion)

    fun addConnection(connection: Conversion) = connections.add(connection)

    fun getConversions(): List<Conversion> = conversions.toList()

    fun getConnections(): List<Conversion> = connections.toList()

    fun getOneToOneConversions(): List<SimpleUnit> =
        conversions.toList().map { it.getOther(this) }.filter { it.units.size == 1 }.map { it.units.keys.first() }

    fun getConversionTo(other: SimpleUnit): Conversion {
        conversions.forEach {
            if (it.numerator.units.size == 1 && it.numerator.units.containsKey(other)) {
                return it
            }
            if (it.denominator.units.size == 1 && it.denominator.units.containsKey(other)) {
                return it
            }
        }
        throw IllegalStateException("The other unit `$other` cannot be converted to from this unit `$this`")
    }

    fun getConnectionTo(other: Quantity): Conversion {
        connections.forEach {
            if (it.numerator == other) {
                return it
            }
            if (it.denominator == other) {
                return it
            }
        }
        throw IllegalStateException("The other unit `$other` cannot be converted to from this unit `$this`")
    }

    fun singular(): String = names[0]
    fun plural(): String = if (names.size > 1) names[1] else singular()
    fun abbreviation(): String = if (names.size > 2) names[2] else plural()
    override fun toString(): String = abbreviation()
}