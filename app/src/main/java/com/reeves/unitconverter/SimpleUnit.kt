package com.reeves.unitconverter

data class SimpleUnit(
    private val singulars: List<String>,
    private val plurals: List<String>,
    private val abbreviations: List<String>,
    val dimensionality: Dimensionality,
) {
    var complexity: Int = Int.MAX_VALUE
    private val simpleConversions: MutableSet<Conversion> = mutableSetOf()
    private val complexConversions: MutableSet<Conversion> = mutableSetOf()

    init {
        assert(singulars.isNotEmpty()) { "Cannot create unit with empty Singulars" }
    }

    fun addSimpleConversion(conversion: Conversion) = simpleConversions.add(conversion)

    fun addComplexConversion(connection: Conversion) = complexConversions.add(connection)

    fun getSimpleConversions(): List<Conversion> = simpleConversions.toList()

    fun getComplexConversions(): List<Conversion> = complexConversions.toList()

    fun getOneToOneConversions(): List<SimpleUnit> =
        simpleConversions.toList().map { it.getOther(this) }.filter { it.units.size == 1 }
            .map { it.units.keys.first() }

    fun getConversionToUnit(other: SimpleUnit): Pair<Conversion,Int> {
        simpleConversions.forEach {
            if (it.numerator.units.size == 1 && it.numerator.units.containsKey(other)) {
                return Pair(it, it.numerator.units[other]!!)
            }
            if (it.denominator.units.size == 1 && it.denominator.units.containsKey(other)) {
                return Pair(it, it.denominator.units[other]!!)
            }
        }
        throw IllegalStateException("The other unit `$other` cannot be converted to from this unit `$this`")
    }

    fun getConversionToQuantity(other: Quantity): Conversion {
        complexConversions.forEach {
            if (it.numerator == other) {
                return it
            }
            if (it.denominator == other) {
                return it
            }
        }
        throw IllegalStateException("The other unit `$other` cannot be converted to from this unit `$this`")
    }

    fun describe() = DescribedUnit(
        plural(), abbreviations, singulars + plurals + abbreviations
    )

    fun singular(): String = singulars.first()
    fun plural(): String = plurals.getOrNull(0) ?: singular()
    fun abbreviation(): String = abbreviations.getOrNull(0) ?: plural()
    override fun toString(): String = abbreviation()
}