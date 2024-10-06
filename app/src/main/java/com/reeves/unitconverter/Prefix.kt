package com.reeves.unitconverter

data class Prefix(val prefix: String, val label: String, val power: Int) {
    companion object {
        val PREFIXES: List<Prefix> = listOf(
            Prefix("yocto", "y", -24),
            Prefix("zepto", "z", -21),
            Prefix("atto", "a", -18),
            Prefix("femto", "f", -15),
            Prefix("pico", "p", -12),
            Prefix("nano", "n", -9),
            Prefix("micro", "µ", -6),
            Prefix("milli", "m", -3),
            Prefix("centi", "c", -2),
            Prefix("deci", "d", -1),
            Prefix("deca", "da", 1),
            Prefix("hecto", "h", 2),
            Prefix("kilo", "k", 3),
            Prefix("mega", "M", 6),
            Prefix("giga", "G", 9),
            Prefix("tera", "T", 12),
            Prefix("peta", "P", 15),
            Prefix("exa", "E", 18),
            Prefix("zetta", "Z", 21),
            Prefix("yotta", "Y", 24)
        )
    }
}