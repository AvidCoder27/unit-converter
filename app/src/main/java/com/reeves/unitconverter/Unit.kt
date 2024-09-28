package com.reeves.unitconverter

class Unit(private val names: List<String>) {
    private val conversions: HashMap<Unit, Conversion> = hashMapOf()

    public fun convert(other: Unit, value: RunningAnswer): ConversionStep {
        conversions[other]?.let {
            it.apply(value)
            return ConversionStep(it.numerator, this, it.denominator, other)
        }
        throw IllegalArgumentException("Cannot convert from `$this` to `$other`")
    }

    public fun addConversion(other: Unit, conversion: Conversion) {
        conversions[other] = conversion
    }

    public fun getConnections(): List<Unit> {
        return conversions.keys.toList()
    }

    public fun getName(): String {
        return names[0]
    }

    public fun getNames(): List<String> {
        return names
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Unit) return false
        return this.getName() == other.getName()
    }

    override fun hashCode(): Int {
        return getName().hashCode()
    }

    override fun toString(): String {
        return getName()
    }
}