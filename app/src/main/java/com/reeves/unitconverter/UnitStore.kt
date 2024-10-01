package com.reeves.unitconverter

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.math.min

object UnitStore {
    private val unitNames: HashMap<String, SimpleUnit> = HashMap()
    private val aliases: HashMap<String, Quantity> = HashMap()
    private val units: MutableList<SimpleUnit> = mutableListOf()
    private val conversions: MutableList<Conversion> = mutableListOf()

    fun loadFromJson(context: Context) {
        val jsonString = context.assets.open("units.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        jsonObject.getJSONArray("units").let { array ->
            for (i in 0 until array.length()) {
                val alikeUnitsArray = array.getJSONArray(i)
                val dimensionality: Map<DIMENSION, Int> =
                    alikeUnitsArray.getString(0).parseUnitsToStringMap().mapKeys {
                        when (it.key) {
                            "d" -> DIMENSION.LENGTH
                            "t" -> DIMENSION.TIME
                            "T" -> DIMENSION.TEMPERATURE
                            "m" -> DIMENSION.MASS
                            "I" -> DIMENSION.ELECTRIC_CURRENT
                            "n" -> DIMENSION.AMOUNT_OF_SUBSTANCE
                            "L" -> DIMENSION.LUMINOUS_INTENSITY
                            "r" -> DIMENSION.ROTATION
                            else -> throw Exception("Invalid dimension when loading units")
                        }
                    }
                for (j in 1 until alikeUnitsArray.length()) {
                    val names = extractNames(alikeUnitsArray.getString(j))
                    val unit = SimpleUnit(names, dimensionality)
                    for (name in names) {
                        unitNames[name] = unit
                    }
                    units.add(unit)
                }
            }
        }

        jsonObject.getJSONArray("aliases").let { array ->
            for (i in 0 until array.length()) {
                val aliasString = array.getString(i).lowercase()
                val equalParts = splitConversionByEquals(aliasString)
                val quantity = equalParts[1].intoQuantity()
                for (alias in extractNames(equalParts[0])) {
                    aliases[alias] = quantity
                }
            }
        }

        jsonObject.getJSONArray("conversions").let { array ->
            for (i in 0 until array.length()) {
                val conversionString = array.getString(i).lowercase()
                val equalParts = splitConversionByEquals(conversionString)
                val conversion =
                    Conversion(equalParts[0].intoQuantity(), equalParts[1].intoQuantity())
                conversion.getLonelies().let { (numerator, denominator) ->
                    numerator?.addConversion(conversion)
                    denominator?.addConversion(conversion)
                    conversions.add(conversion)
                    conversion.denominator.forEach {
                        it.key.addConnection(conversion.numerator)
                    }
                    conversion.numerator.forEach {
                        it.key.addConnection(conversion.denominator)
                    }
                }
            }
        }

        setOf(
            "m", "s", "K", "kg", "A", "mol", "cd", "rotation"
        ).forEach { name ->
            val fundamental = getUnit(name).keys.first()
            fundamental.complexity = 0
            val (_, distance) = breadthFirstSearch(fundamental) { unit ->
                unit.getConnections().flatMap { it.units.keys }
            }
            distance.forEach { (unit, distance) ->
                unit.complexity = min(unit.complexity, distance)
            }
        }

        units.sortedBy { it.complexity }.reversed().forEach {
            Log.i("UnitStore", "${it.singular()}: ${it.complexity}")
        }
    }

    private fun extractNames(input: String) = mutableListOf<String>().also { names ->
        input.split(",").forEach { name ->
            val builder = StringBuilder()
            for (chunk in name.trim().lowercase().split('|')) {
                builder.append(chunk)
                names.add(builder.toString())
            }
        }
    }

    private fun splitConversionByEquals(string: String): List<String> =
        string.split("=").map { it.trim() }.let {
            require(it.size == 2) { "Invalid conversion: $string" }
            return it
        }

    /**
     * For a name of a SimpleUnit, this will just return a mapOf(it to 1)
     * So for those you can just take
     * @return the unit or alias with the given name as a map of SimpleUnits to their exponents
     * @throws UndefinedUnitException if the unit is not defined as a unit or an alias
     */
    fun getUnit(name: String): Map<SimpleUnit, Int> {
        unitNames[name.lowercase()]?.let { return mapOf(it to 1) }
        aliases[name.lowercase()]?.let { return it.units }
        throw UndefinedUnitException(name)
    }

    /**
     * For any quantity, this function will return a quantity that has units equivalent to the input
     * but all units will be from the fundamental base units (meter for length, second for time, etc.)
     *
     * **HOWEVER,** the value on the returned quantity will not be equivalent; it will just be 1
     */
    fun Quantity.asFundamental() = Quantity(1.0, dimensionality().mapKeys {
        when (it.key) {
            DIMENSION.LENGTH -> getUnit("meter")
            DIMENSION.TIME -> getUnit("second")
            DIMENSION.TEMPERATURE -> getUnit("kelvin")
            DIMENSION.MASS -> getUnit("kilogram")
            DIMENSION.ELECTRIC_CURRENT -> getUnit("ampere")
            DIMENSION.AMOUNT_OF_SUBSTANCE -> getUnit("mole")
            DIMENSION.LUMINOUS_INTENSITY -> getUnit("candela")
            DIMENSION.ROTATION -> getUnit("rotation")
        }.keys.first()
    })
}