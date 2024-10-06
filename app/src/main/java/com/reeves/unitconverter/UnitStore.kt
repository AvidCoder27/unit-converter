package com.reeves.unitconverter

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.math.pow

private const val PERCENT = "%"

object UnitStore {
    private val unitNames: HashMap<String, SimpleUnit> = HashMap()
    private val aliases: HashMap<String, Quantity> = HashMap()
    private val units: MutableList<SimpleUnit> = mutableListOf()
    private val conversions: MutableList<Conversion> = mutableListOf()

    fun loadFromJson(context: Context) {
        val jsonString = context.assets.open("units.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        loadUnits(jsonObject)
        loadAliases(jsonObject)
        loadConversions(jsonObject)
        computeComplexities()
    }

    private fun loadConversions(jsonObject: JSONObject) =
        jsonObject.getJSONArray("conversions").let { array ->
            for (i in 0 until array.length()) {
                val conversionString = array.getString(i).lowercase()
                val equalParts = splitConversionByEquals(conversionString)
                val conversion =
                    Conversion(equalParts[0].intoQuantity(), equalParts[1].intoQuantity())
                conversion.getLonelies().let { (numerator, denominator) ->
                    addConversion(conversion, numerator, denominator)
                }
            }
        }

    private fun addConversion(
        conversion: Conversion,
        alpha: SimpleUnit?,
        beta: SimpleUnit?,
    ) {
        alpha?.addConversion(conversion)
        beta?.addConversion(conversion)
        conversions.add(conversion)
        conversion.denominator.forEach {
            it.key.addConnection(conversion)
        }
        conversion.numerator.forEach {
            it.key.addConnection(conversion)
        }
    }

    private fun loadAliases(jsonObject: JSONObject) =
        jsonObject.getJSONArray("aliases").let { array ->
            for (i in 0 until array.length()) {
                val aliasString = array.getString(i).lowercase()
                val equalParts = splitConversionByEquals(aliasString)
                val quantity = equalParts[1].intoQuantity()
                for (alias in equalParts[0].extractNames()) {
                    aliases[alias] = quantity
                }
            }
        }

    private fun loadUnits(jsonObject: JSONObject) = jsonObject.getJSONArray("units").let { array ->
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
                val line = alikeUnitsArray.getString(j)
                if (line.startsWith(PERCENT)) {
                    val names = line.extractNames()
                    assert(names.size >= 3) { "Prefixed unit `${names[0]}` must include a singular, plural, and abbreviated form" }
                    names.forEach {
                        assert(it.contains(PERCENT)) { "Prefixed unit `${names[0]}` must contain $PERCENT in all names" }
                    }
                    val baseUnit = createUnit(names.map { it.replace(PERCENT, "") }, dimensionality)
                    val baseQuantity = Quantity(1.0, mapOf(baseUnit to 1))
                    Prefix.PREFIXES.forEach {
                        val prefixedNames = mutableListOf(
                            names[0].replace(PERCENT, it.prefix),
                            names[1].replace(PERCENT, it.prefix),
                            names[2].replace(PERCENT, it.label)
                        )
                        if (it.prefix == "micro") {
                            prefixedNames.add(names[2].replace(PERCENT, "u"))
                        }
                        for (name in names.drop(3)) {
                            prefixedNames.add(it.prefix + name)
                        }
                        val prefixedUnit = createUnitChecking(prefixedNames, dimensionality)
                        val conversion = Conversion(
                            baseQuantity, Quantity(10.0.pow(-it.power), mapOf(prefixedUnit to 1))
                        )
                        addConversion(conversion, baseUnit, prefixedUnit)
                    }
                } else {
                    val names = line.extractNames()
                    createUnit(names, dimensionality)
                }
            }
        }
    }

    private fun createUnitChecking(
        names: List<String>,
        dimensionality: Map<DIMENSION, Int>,
    ): SimpleUnit {
        if (names.any { unitNames.containsKey(it) }) {
            var unit: SimpleUnit? = null
            val undefinedNames = mutableListOf<String>()
            for (name in names) {
                if (unitNames.containsKey(name)) {
                    unit = unitNames[name]!!
                } else {
                    undefinedNames.add(name)
                }
            }
            undefinedNames.forEach {
                unitNames[it] = unit!!
            }
            return unit!!
        }
        val unit = SimpleUnit(names, dimensionality)
        for (name in names) {
            unitNames[name] = unit
        }
        units.add(unit)
        return unit
    }

    private fun createUnit(names: List<String>, dimensionality: Map<DIMENSION, Int>): SimpleUnit {
        val unit = SimpleUnit(names, dimensionality)
        for (name in names) {
            unitNames[name] = unit
        }
        units.add(unit)
        return unit
    }

    private fun computeComplexities() {
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
            val processed =
                bfsForComplexity(unprocessed.elementAt(index % unprocessed.size), distance)
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
    }

    private fun bfsForComplexity(
        start: SimpleUnit,
        distance: HashMap<SimpleUnit, Int>,
    ): List<SimpleUnit> {
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

    private fun String.extractNames() = mutableListOf<String>().also { names ->
        split(",").forEach { name ->
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