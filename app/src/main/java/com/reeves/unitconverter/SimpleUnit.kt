package com.reeves.unitconverter

open class SimpleUnit(private val names: List<String>) {
    init {
        require(names.size >= 2) { "Unit must have at least two names, only has `${names[0]}`" }
    }

    private val conversions: HashMap<SimpleUnit, Conversion> = hashMapOf()

    fun convert(other: SimpleUnit, value: RunningAnswer): ConversionStep {
        conversions[other]?.let {
            it.apply(value)
            return ConversionStep(it.numerator, other, it.denominator, this)
        }
        throw IllegalArgumentException("Cannot convert from `$this` to `$other`")
    }

    open fun getSize(): Pair<Int, Int> {
        return Pair(1, 0)
    }

    open fun getConstituents(): Map<SimpleUnit, Int> = mapOf(this to 1)

    fun addConversion(other: SimpleUnit, conversion: Conversion) {
        conversions[other] = conversion
    }

    fun getConnections(): List<SimpleUnit> {
        return conversions.keys.toList()
    }

    fun plural(): String {
        return names[1]
    }

    fun singular(): String {
        return names[0]
    }

    fun abbreviation(): String {
        return if (names.size > 2) names[2] else names[0]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleUnit) return false
        return this.singular() == other.singular()
    }

    override fun hashCode(): Int {
        return plural().hashCode()
    }
}