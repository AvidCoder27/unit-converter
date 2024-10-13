package com.reeves.unitconverter

data class SimpleUnit(
    private val singulars: List<String>,
    private val plurals: List<String>,
    private val abbreviations: List<String>,
    val dimensionality: Map<DIMENSION, Int>,
) {
    var complexity: Int = Int.MAX_VALUE
    private val conversions: MutableSet<Conversion> = mutableSetOf()
    private val connections: MutableSet<Conversion> = mutableSetOf()

    init {
        assert(singulars.isNotEmpty()) { "Cannot create unit with empty Singulars" }
    }

    fun addConversion(conversion: Conversion) = conversions.add(conversion)

    fun addConnection(connection: Conversion) = connections.add(connection)

    fun getConversions(): List<Conversion> = conversions.toList()

    fun getConnections(): List<Conversion> = connections.toList()

    fun getOneToOneConversions(): List<SimpleUnit> =
        conversions.toList().map { it.getOther(this) }.filter { it.units.size == 1 }
            .map { it.units.keys.first() }

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

    fun singular(): String = singulars[0]
    fun plural(): String = plurals.getOrNull(0) ?: singular()
    fun abbreviation(): String = abbreviations.getOrNull(0) ?: plural()
    override fun toString(): String = abbreviation()
}