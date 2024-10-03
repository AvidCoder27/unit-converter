package com.reeves.unitconverter

import android.content.Context
import android.util.Log
import org.json.JSONObject

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
                        it.key.addConnection(conversion)
                    }
                    conversion.numerator.forEach {
                        it.key.addConnection(conversion)
                    }
                }
            }
        }

        val unprocessed = units.toMutableSet()
        val distance: HashMap<SimpleUnit, Int> = hashMapOf()

        setOf(
            "m", "s", "K", "kg", "A", "mol", "cd", "rotation"
        ).forEach { name ->
            val fundamental = getUnit(name).keys.first()
            fundamental.complexity = 0
            distance[fundamental] = 0
        }

        var index = unprocessed.size
        while (unprocessed.isNotEmpty()) {
            val processed = bfsForComplexity(unprocessed.elementAt(index % unprocessed.size), distance)
            var countRemoved = 0
            unprocessed.removeAll {
                if (it in processed) {
                    countRemoved++
                    true
                } else {
                    false
                }
            }
            index = 1 + index - countRemoved
            if (index > units.size * 3) {
                Log.e("UnitStore", "unprocessed units: $unprocessed")
                throw Exception("index surpassed ${units.size * 3} when processing units; are you sure all your units are connected?")
            }
        }
        distance.forEach { (unit, distance) ->
            unit.complexity = distance
        }

        units.sortedBy { it.complexity }.forEach {
            Log.i("UnitStore", "Complexity of ${it.singular()} is ${it.complexity}")
        }
    }

    private fun bfsForComplexity(start: SimpleUnit, distance: HashMap<SimpleUnit, Int>): List<SimpleUnit> {
        val processed = mutableListOf<SimpleUnit>()
        if (!distance.containsKey(start)) return processed
        val queue: ArrayDeque<SimpleUnit> = ArrayDeque()
        queue.addLast(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            processed.add(node)
            for (neighbor in node.getOneToOneConversions()) {
                if (!distance.containsKey(neighbor)) {
                    distance[neighbor] = distance[node]!! + 1
                    queue.addLast(neighbor)
                }
            }
            for (conversion in node.getConnections()) {
                val lonelies = conversion.getLonelies().toList().filterNotNull()
                if (lonelies.size == 1) {
                    val lonely = lonelies.first()
                    val neighbor = conversion.getOther(lonely)
                    if (!distance.containsKey(lonely)) {
                        var complexity = 0
                        var missingDistance = false
                        neighbor.forEach { (unit, _) ->
                            if (distance.containsKey(unit)) {
                                complexity += distance[unit]!! + 1
                            } else {
                                missingDistance = true
                            }
                        }
                        if (missingDistance) {
                            continue
                        }
                        distance[lonely] = distance[node]!! + complexity
                        processed.add(lonely)
                        queue.addLast(lonely)
                    }
                }
            }
        }
        return processed
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
}