package com.reeves.unitconverter

class Unit(private val names: List<String>) {
    init {
        require(names.size >= 2)
    }

    private val conversions: HashMap<Unit, Conversion> = hashMapOf()

    fun convert(other: Unit, value: RunningAnswer): ConversionStep {
        conversions[other]?.let {
            it.apply(value)
            return ConversionStep(it.numerator, this, it.denominator, other)
        }
        throw IllegalArgumentException("Cannot convert from `$this` to `$other`")
    }

    fun addConversion(other: Unit, conversion: Conversion) {
        conversions[other] = conversion
    }

    fun getConnections(): List<Unit> {
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
        if (other !is Unit) return false
        return this.singular() == other.singular()
    }

    override fun hashCode(): Int {
        return plural().hashCode()
    }
}