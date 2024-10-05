package com.reeves.unitconverter

data class RunningAnswer(var value: Double) {
    override fun toString(): String = value.toString()
}

class InvalidUnitsException(culprit: String) :
    Exception("`$culprit` is not a valid sequence of units")

class UndefinedUnitException(culprit: String) : Exception("`$culprit` is not a defined unit")
class ImpossibleConversionException : Exception("This conversion is impossible!")

class MeaninglessConversionException(cause: String) :
    Exception("This conversion is meaningless because $cause")

class PromotionRequiredException :
    Exception("This conversion cannot be completed until promoted to a more rigorous method")

fun Double.truncate(precision: Int) = "%.${precision}f".format(this)

/**
 * @throws UndefinedUnitException
 * @throws InvalidUnitsException
 */
fun String.intoQuantity(): Quantity {
    if (this.isBlank()) return Quantity(1.0, mapOf())
    return extractValue().let { (value, text) ->
        Quantity(value, mutableMapOf<SimpleUnit, Int>().also { map ->
            text.parseUnitsToStringMap().forEach { (name, unitExponent) ->
                val subMap = UnitStore.getUnit(name)
                subMap.forEach { (unit, aliasExponent) ->
                    map[unit] = (map[unit] ?: 0) + unitExponent * aliasExponent
                }
            }
        }.clean())
    }
}

fun <T> Map<T, Int>.clean() = this.filterValues { it != 0 }

fun Int.toSuperscript(ignore1: Boolean = true, throwZero: Boolean = true): String {
    if (this == 0 && throwZero) throw IllegalArgumentException("Cannot convert just 0 to superscript")
    if (this == 1 && ignore1) return ""
    val superscriptChars = mapOf(
        '0' to '\u2070',
        '1' to '\u00B9',
        '2' to '\u00B2',
        '3' to '\u00B3',
        '4' to '\u2074',
        '5' to '\u2075',
        '6' to '\u2076',
        '7' to '\u2077',
        '8' to '\u2078',
        '9' to '\u2079',
        '-' to '\u207B'
    )
    return this.toString().map { superscriptChars[it] ?: it }.joinToString("")
}

@Throws(InvalidUnitsException::class)
fun String.parseUnitsToStringMap(): Map<String, Int> = mutableMapOf<String, Int>().also {
    Regex("([,*/]?\\s*(?:[\\w-]+\\s*)+\\^?\\d*)").findAll(this).forEach { matchResult ->
        val term = matchResult.value.trim()
        val parts = term.split("^")
        val unit = parts[0].replace("*", "").replace("/", "").replace(",", "").trim()
        val exponent = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val sign = if (term.startsWith("/")) -1 else 1
        it[unit] = (it[unit] ?: 0) + sign * exponent
    }
}.also {
    if (it.isEmpty()) throw InvalidUnitsException(this)
}

fun String.extractValue(): Pair<Double, String> {
    val regex = Regex("^\\s*([-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)")
    return regex.find(this).let {
        if (it == null) Pair(1.0, this.trim())
        else Pair(it.value.trim().toDouble(), substring(it.range.last + 1).trim())
    }
}