package com.reeves.unitconverter

data class SimpleUnit(private val names: List<String>, val dimensionality: Map<DIMENSION, Int>) {
    init {
        require(names.size >= 2) { "Unit must have at least two names, only has `${names[0]}`" }
    }

    var complexity: Int = Int.MAX_VALUE
    private val conversions: MutableSet<Conversion> = mutableSetOf()
    private val connections: MutableSet<Quantity> = mutableSetOf()

    fun addConversion(conversion: Conversion) = conversions.add(conversion)

    fun addConnection(connection: Quantity) = connections.add(connection)

    fun getConversions(): List<Conversion> = conversions.toList()

    fun getConnections(): List<Quantity> = connections.toList()

    fun getOneToOneConnections(): List<SimpleUnit> =
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

    fun plural(): String = names[1]
    fun singular(): String = names[0]
    fun abbreviation(): String = if (names.size > 2) names[2] else singular()
    override fun toString(): String = abbreviation()
}