package com.reeves.unitconverter

class InvalidChemicalException(message: String) : Exception("Chemical formula has $message")

data class ChemicalElement(
    val name: String,
    val symbol: String,
    val atomicNumber: Int,
    val atomicMass: Double,
) {
    override fun toString(): String = name
}

data class ChemicalCompound(
    val formula: String,
    val elements: Map<ChemicalElement, Int>,
) {
    private fun getMass(): Double {
        var totalMass = 0.0
        for ((element, count) in elements) {
            totalMass += element.atomicMass * count
        }
        return totalMass
    }

    fun convertToGrams(): Conversion {
        val list = listOf("mol [$formula]")
        val molesOfThis = SimpleUnit(list, list, list, Dimensionality(mapOf(DIMENSION.NUMBER to 1)))
        return Conversion(
            Quantity(getMass(), mapOf(UnitStore.getUnit("grams").keys.first() to 1)),
            Quantity(1.0, mapOf(molesOfThis to 1))
        )
    }

    override fun equals(other: Any?): Boolean {
        return elements == (other as? ChemicalCompound)?.elements
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }
}

object ChemicalParser {

    fun parseFormula(input: String): ChemicalCompound {
        if (input.isBlank() || !input.matches(Regex("[A-Za-z0-9()]+"))) {
            throw InvalidChemicalException("malformed input: $input")
        }

        val elementCounts = mutableMapOf<ChemicalElement, Int>()
        parseRecursive(input, elementCounts, 1)
        val formula = buildString {
            elementCounts.forEach { (element, count) ->
                append(element.symbol)
                if (count > 1) {
                    append(count.toSubscript())
                }
            }
        }
        return ChemicalCompound(formula, elementCounts)
    }

    // Helper function for parsing formulas recursively, including handling parentheses.
    private fun parseRecursive(
        input: String,
        elementCounts: MutableMap<ChemicalElement, Int>,
        multiplier: Int,
    ) {
        var i = 0
        while (i < input.length) {
            when {
                input[i] == '(' -> {
                    // Find the matching closing parenthesis.
                    val closingIndex = findClosingParenthesis(input, i)
                        ?: throw InvalidChemicalException("unmatched parenthesis in: $input")

                    // Parse the content inside the parentheses.
                    val subFormula = input.substring(i + 1, closingIndex)
                    var quantity = 1
                    var j = closingIndex + 1

                    // Check for multiplier after the parentheses (e.g., (NO3)2).
                    if (j < input.length && input[j].isDigit()) {
                        val quantityBuilder = StringBuilder()
                        while (j < input.length && input[j].isDigit()) {
                            quantityBuilder.append(input[j])
                            j++
                        }
                        quantity = quantityBuilder.toString().toInt()
                    }

                    // Recursively parse the sub-formula with the correct multiplier.
                    parseRecursive(subFormula, elementCounts, multiplier * quantity)
                    i = j - 1  // Update the index to continue after the parenthesis block.
                }

                else -> {
                    // Extract the element symbol: 1 or 2 letters.
                    val symbol = if (i + 1 < input.length && input[i + 1].isLowerCase()) {
                        input.substring(i, i + 2)
                    } else {
                        input.substring(i, i + 1)
                    }

                    // Check if the symbol is valid.
                    val element = elements[symbol]
                        ?: throw InvalidChemicalException("an unknown element: $symbol")
                    i += symbol.length

                    // Extract the quantity (optional). Default to 1 if not provided.
                    val quantityBuilder = StringBuilder()
                    while (i < input.length && input[i].isDigit()) {
                        quantityBuilder.append(input[i])
                        i++
                    }
                    val quantity =
                        if (quantityBuilder.isEmpty()) 1 else quantityBuilder.toString().toInt()

                    // Update the element count with the correct multiplier.
                    elementCounts[element] =
                        elementCounts.getOrDefault(element, 0) + (quantity * multiplier)
                    i--
                }
            }
            i++
        }
    }

    // Helper function to find the matching closing parenthesis.
    private fun findClosingParenthesis(input: String, openIndex: Int): Int? {
        var balance = 1
        for (i in openIndex + 1 until input.length) {
            when (input[i]) {
                '(' -> balance++
                ')' -> balance--
            }
            if (balance == 0) return i
        }
        return null  // Return null if no matching closing parenthesis is found.
    }

    private val elements = mutableMapOf(
        "H" to ChemicalElement("Hydrogen", "H", 1, 1.008),
        "He" to ChemicalElement("Helium", "He", 2, 4.0026),
        "Li" to ChemicalElement("Lithium", "Li", 3, 6.94),
        "Be" to ChemicalElement("Beryllium", "Be", 4, 9.0122),
        "B" to ChemicalElement("Boron", "B", 5, 10.81),
        "C" to ChemicalElement("Carbon", "C", 6, 12.011),
        "N" to ChemicalElement("Nitrogen", "N", 7, 14.007),
        "O" to ChemicalElement("Oxygen", "O", 8, 15.999),
        "F" to ChemicalElement("Fluorine", "F", 9, 18.998),
        "Ne" to ChemicalElement("Neon", "Ne", 10, 20.180),
        "Na" to ChemicalElement("Sodium", "Na", 11, 22.990),
        "Mg" to ChemicalElement("Magnesium", "Mg", 12, 24.305),
        "Al" to ChemicalElement("Aluminum", "Al", 13, 26.982),
        "Si" to ChemicalElement("Silicon", "Si", 14, 28.085),
        "P" to ChemicalElement("Phosphorus", "P", 15, 30.974),
        "S" to ChemicalElement("Sulfur", "S", 16, 32.06),
        "Cl" to ChemicalElement("Chlorine", "Cl", 17, 35.45),
        "Ar" to ChemicalElement("Argon", "Ar", 18, 39.948),
        "K" to ChemicalElement("Potassium", "K", 19, 39.098),
        "Ca" to ChemicalElement("Calcium", "Ca", 20, 40.078),
        "Sc" to ChemicalElement("Scandium", "Sc", 21, 44.956),
        "Ti" to ChemicalElement("Titanium", "Ti", 22, 47.867),
        "V" to ChemicalElement("Vanadium", "V", 23, 50.942),
        "Cr" to ChemicalElement("Chromium", "Cr", 24, 51.996),
        "Mn" to ChemicalElement("Manganese", "Mn", 25, 54.938),
        "Fe" to ChemicalElement("Iron", "Fe", 26, 55.845),
        "Co" to ChemicalElement("Cobalt", "Co", 27, 58.933),
        "Ni" to ChemicalElement("Nickel", "Ni", 28, 58.693),
        "Cu" to ChemicalElement("Copper", "Cu", 29, 63.546),
        "Zn" to ChemicalElement("Zinc", "Zn", 30, 65.38),
        "Ga" to ChemicalElement("Gallium", "Ga", 31, 69.723),
        "Ge" to ChemicalElement("Germanium", "Ge", 32, 72.63),
        "As" to ChemicalElement("Arsenic", "As", 33, 74.922),
        "Se" to ChemicalElement("Selenium", "Se", 34, 78.971),
        "Br" to ChemicalElement("Bromine", "Br", 35, 79.904),
        "Kr" to ChemicalElement("Krypton", "Kr", 36, 83.798),
        "Rb" to ChemicalElement("Rubidium", "Rb", 37, 85.468),
        "Sr" to ChemicalElement("Strontium", "Sr", 38, 87.62),
        "Y" to ChemicalElement("Yttrium", "Y", 39, 88.906),
        "Zr" to ChemicalElement("Zirconium", "Zr", 40, 91.224),
        "Nb" to ChemicalElement("Niobium", "Nb", 41, 92.906),
        "Mo" to ChemicalElement("Molybdenum", "Mo", 42, 95.95),
        "Tc" to ChemicalElement("Technetium", "Tc", 43, 98.0),
        "Ru" to ChemicalElement("Ruthenium", "Ru", 44, 101.07),
        "Rh" to ChemicalElement("Rhodium", "Rh", 45, 102.91),
        "Pd" to ChemicalElement("Palladium", "Pd", 46, 106.42),
        "Ag" to ChemicalElement("Silver", "Ag", 47, 107.87),
        "Cd" to ChemicalElement("Cadmium", "Cd", 48, 112.41),
        "In" to ChemicalElement("Indium", "In", 49, 114.82),
        "Sn" to ChemicalElement("Tin", "Sn", 50, 118.71),
        "Sb" to ChemicalElement("Antimony", "Sb", 51, 121.76),
        "Te" to ChemicalElement("Tellurium", "Te", 52, 127.60),
        "I" to ChemicalElement("Iodine", "I", 53, 126.90),
        "Xe" to ChemicalElement("Xenon", "Xe", 54, 131.29),
        "Cs" to ChemicalElement("Cesium", "Cs", 55, 132.91),
        "Ba" to ChemicalElement("Barium", "Ba", 56, 137.33),
        "La" to ChemicalElement("Lanthanum", "La", 57, 138.91),
        "Ce" to ChemicalElement("Cerium", "Ce", 58, 140.12),
        "Pr" to ChemicalElement("Praseodymium", "Pr", 59, 140.91),
        "Nd" to ChemicalElement("Neodymium", "Nd", 60, 144.24),
        "Pm" to ChemicalElement("Promethium", "Pm", 61, 145.0),
        "Sm" to ChemicalElement("Samarium", "Sm", 62, 150.36),
        "Eu" to ChemicalElement("Europium", "Eu", 63, 151.96),
        "Gd" to ChemicalElement("Gadolinium", "Gd", 64, 157.25),
        "Tb" to ChemicalElement("Terbium", "Tb", 65, 158.93),
        "Dy" to ChemicalElement("Dysprosium", "Dy", 66, 162.50),
        "Ho" to ChemicalElement("Holmium", "Ho", 67, 164.93),
        "Er" to ChemicalElement("Erbium", "Er", 68, 167.26),
        "Tm" to ChemicalElement("Thulium", "Tm", 69, 168.93),
        "Yb" to ChemicalElement("Ytterbium", "Yb", 70, 173.05),
        "Lu" to ChemicalElement("Lutetium", "Lu", 71, 174.97),
        "Hf" to ChemicalElement("Hafnium", "Hf", 72, 178.49),
        "Ta" to ChemicalElement("Tantalum", "Ta", 73, 180.95),
        "W" to ChemicalElement("Tungsten", "W", 74, 183.84),
        "Re" to ChemicalElement("Rhenium", "Re", 75, 186.21),
        "Os" to ChemicalElement("Osmium", "Os", 76, 190.23),
        "Ir" to ChemicalElement("Iridium", "Ir", 77, 192.22),
        "Pt" to ChemicalElement("Platinum", "Pt", 78, 195.08),
        "Au" to ChemicalElement("Gold", "Au", 79, 196.97),
        "Hg" to ChemicalElement("Mercury", "Hg", 80, 200.59),
        "Tl" to ChemicalElement("Thallium", "Tl", 81, 204.38),
        "Pb" to ChemicalElement("Lead", "Pb", 82, 207.2),
        "Bi" to ChemicalElement("Bismuth", "Bi", 83, 208.98),
        "Po" to ChemicalElement("Polonium", "Po", 84, 209.0),
        "At" to ChemicalElement("Astatine", "At", 85, 210.0),
        "Rn" to ChemicalElement("Radon", "Rn", 86, 222.0),
        "Fr" to ChemicalElement("Francium", "Fr", 87, 223.0),
        "Ra" to ChemicalElement("Radium", "Ra", 88, 226.0),
        "Ac" to ChemicalElement("Actinium", "Ac", 89, 227.0),
        "Th" to ChemicalElement("Thorium", "Th", 90, 232.04),
        "Pa" to ChemicalElement("Protactinium", "Pa", 91, 231.04),
        "U" to ChemicalElement("Uranium", "U", 92, 238.03),
        "Np" to ChemicalElement("Neptunium", "Np", 93, 237.0),
        "Pu" to ChemicalElement("Plutonium", "Pu", 94, 244.0),
        "Am" to ChemicalElement("Americium", "Am", 95, 243.0),
        "Cm" to ChemicalElement("Curium", "Cm", 96, 247.0),
        "Bk" to ChemicalElement("Berkelium", "Bk", 97, 247.0),
        "Cf" to ChemicalElement("Californium", "Cf", 98, 251.0),
        "Es" to ChemicalElement("Einsteinium", "Es", 99, 252.0),
        "Fm" to ChemicalElement("Fermium", "Fm", 100, 257.0),
        "Md" to ChemicalElement("Mendelevium", "Md", 101, 258.0),
        "No" to ChemicalElement("Nobelium", "No", 102, 259.0),
        "Lr" to ChemicalElement("Lawrencium", "Lr", 103, 262.0),
        "Rf" to ChemicalElement("Rutherfordium", "Rf", 104, 267.0),
        "Db" to ChemicalElement("Dubnium", "Db", 105, 270.0),
        "Sg" to ChemicalElement("Seaborgium", "Sg", 106, 271.0),
        "Bh" to ChemicalElement("Bohrium", "Bh", 107, 270.0),
        "Hs" to ChemicalElement("Hassium", "Hs", 108, 277.0),
        "Mt" to ChemicalElement("Meitnerium", "Mt", 109, 278.0),
        "Ds" to ChemicalElement("Darmstadtium", "Ds", 110, 281.0),
        "Rg" to ChemicalElement("Roentgenium", "Rg", 111, 282.0),
        "Cn" to ChemicalElement("Copernicium", "Cn", 112, 285.0),
        "Nh" to ChemicalElement("Nihonium", "Nh", 113, 286.0),
        "Fl" to ChemicalElement("Flerovium", "Fl", 114, 289.0),
        "Mc" to ChemicalElement("Moscovium", "Mc", 115, 290.0),
        "Lv" to ChemicalElement("Livermorium", "Lv", 116, 293.0),
        "Ts" to ChemicalElement("Tennessine", "Ts", 117, 294.0),
        "Og" to ChemicalElement("Oganesson", "Og", 118, 294.0)
    )
}