package com.reeves.unitconverter

data class RunningAnswer(var value: Double) {
    override fun toString(): String = value.toString()
}

fun Double.truncate(precision: Int) = "%.${precision}f".format(this)

fun <T> List<T>.dedupCount(): Map<T, Int> = this.groupingBy { it }.eachCount()

fun Int.toSuperscript(ignore1: Boolean = true): String {
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

fun String.parseUnitsToMap(): Map<String, Int> = mutableMapOf<String, Int>().let {
    Regex("([*/]?\\s*(?:\\w+\\s*)+\\^?\\d*)").findAll(this).forEach { matchResult ->
        val term = matchResult.value.trim()
        val parts = term.split("^")
        val unit = parts[0].replace("*", "").replace("/", "").trim()
        val exponent = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val sign = if (term.startsWith("/")) -1 else 1
        it[unit] = (it[unit] ?: 0) + sign * exponent
    }
    it
}

fun Map<SimpleUnit, Int>.foldSize(): Pair<Int, Int> =
    toList().fold(Pair(0, 0)) { acc, pair -> acc.add(pair.first.getSize().multiply(pair.second)) }

fun foldTopAndBottom(top: List<SimpleUnit>, bottom: List<SimpleUnit>): Pair<Int, Int> =
    top.foldSize().add(bottom.foldSize().reciprocate())

private fun List<SimpleUnit>.foldSize(): Pair<Int, Int> =
    fold(Pair(0, 0)) { acc, unit -> acc.add(unit.getSize()) }

fun Pair<Int, Int>.add(other: Pair<Int, Int>) =
    Pair(first + other.first, second + other.second)

fun Pair<Int, Int>.multiply(scalar: Int) = Pair(first * scalar, second * scalar)

fun Pair<Int, Int>.reciprocate() = Pair(second, first)

interface PeekingIterator<T> : Iterator<T> {
    fun peek(): T
}

fun <T> Iterator<T>.peeking(): PeekingIterator<T> =
    if (this is PeekingIterator)
        this
    else
        object : PeekingIterator<T> {
            private var cached = false

            @Suppress("UNCHECKED_CAST")
            private var element: T = null as T
                get() {
                    if (!cached)
                        field = this@peeking.next()
                    return field
                }

            override fun hasNext(): Boolean = cached || this@peeking.hasNext()

            override fun next(): T = element.also { cached = false }

            override fun peek(): T = element.also { cached = true }
        }