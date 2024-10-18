package com.reeves.unitconverter

import kotlin.math.min

data class DescribedUnit(val name: String, val visibleAlts: List<String>, val alts: List<String>) :
    Comparable<DescribedUnit> {

    override fun toString(): String =
        if (name.length < 13) name else visibleAlts.filter { it.length < 13 }
            .maxByOrNull { s -> s.length } ?: name

    override fun compareTo(other: DescribedUnit): Int {
        val len1 = this.name.length
        val len2 = other.name.length

        // If lengths are equal, compare characters
        for (k in 0 until min(len1, len2)) {
            val c1 = this.name[k]
            val c2 = other.name[k]
            if (c1 != c2) {
                val order1 = when (c1) {
                    in 'a'..'z' -> 0
                    in 'A'..'Z' -> 1
                    ' ' -> 2
                    else -> 3
                }
                val order2 = when (c2) {
                    in 'a'..'z' -> 0
                    in 'A'..'Z' -> 1
                    ' ' -> 2
                    else -> 3
                }
                return if (order1 != order2) {
                    order1.compareTo(order2)
                } else {
                    c1.compareTo(c2)
                }
            }
        }
        // If strings are equal, return 0
        return len1.compareTo(len2)
    }
}
