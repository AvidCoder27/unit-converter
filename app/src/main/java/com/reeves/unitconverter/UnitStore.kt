package com.reeves.unitconverter

import android.content.Context
import android.util.Log
import org.json.JSONObject
import kotlin.math.pow

private const val PERCENT = "%"

object UnitStore {
    private val unitNames: HashMap<String, SimpleUnit> = HashMap()
    private val aliases: HashMap<String, SimpleUnit> = HashMap()
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
                val conversionString = array.getString(i)
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
        alpha?.addSimpleConversion(conversion)
        beta?.addSimpleConversion(conversion)
        conversions.add(conversion)
        conversion.denominator.forEach {
            it.key.addComplexConversion(conversion)
        }
        conversion.numerator.forEach {
            it.key.addComplexConversion(conversion)
        }
    }

    private fun loadAliases(jsonObject: JSONObject) =
        jsonObject.getJSONArray("aliases").let { array ->
            for (i in 0 until array.length()) {
                val aliasString = array.getString(i)
                val equalParts = splitConversionByEquals(aliasString)
                val quantity = equalParts[1].intoQuantity()
                val names = equalParts[0].extractNames()
                val unit = createUnit(names.first, names.second, listOf(), quantity.dimensionality(), aliases)
                unit.addComplexConversion(Conversion(quantity, Quantity(1.0, mapOf(unit to 1))))
            }
        }

    private fun loadUnits(jsonObject: JSONObject) = jsonObject.getJSONArray("units").let { array ->
        for (i in 0 until array.length()) {
            val alikeUnitsArray = array.getJSONArray(i)
            val dimensionality: Map<DIMENSION, Int> =
                alikeUnitsArray.getString(0).parseUnitsToStringMap()
                    .mapKeys { stringToDimension(it.key) }
            for (j in 1 until alikeUnitsArray.length()) {
                val line = alikeUnitsArray.getString(j)
                if (line.startsWith(PERCENT)) {
                    createPrefixedUnit(line, dimensionality)
                } else {
                    val names = line.extractNames()
                    createUnit(names.first, names.second, names.third, dimensionality, unitNames)
                }
            }
        }
    }

    private fun stringToDimension(s: String) = when (s) {
        "d" -> DIMENSION.LENGTH
        "t" -> DIMENSION.TIME
        "T" -> DIMENSION.TEMPERATURE
        "m" -> DIMENSION.MASS
        "I" -> DIMENSION.ELECTRIC_CURRENT
        "n" -> DIMENSION.NUMBER
        "L" -> DIMENSION.LUMINOUS_INTENSITY
        "r" -> DIMENSION.ROTATION
        "b" -> DIMENSION.DIGITAL_INFORMATION
        else -> throw Exception("Invalid dimension when loading units")
    }

    private fun List<String>.cleanPrefix(): List<String> = map { it.replace(PERCENT, "") }

    private fun createPrefixedUnit(
        line: String,
        dimensionality: Map<DIMENSION, Int>,
    ) {
        val (singulars, plurals, abbreviations) = line.extractNames()
        val allNames = singulars + plurals + abbreviations
        assert(singulars.isNotEmpty() && plurals.isNotEmpty() && abbreviations.isNotEmpty()) { "Prefixed unit `$line` must include a singular, plural, and abbreviated form" }
        allNames.forEach {
            assert(it.contains(PERCENT)) { "Prefixed unit `$allNames` must contain $PERCENT in all names" }
        }
        val baseUnit = createUnit(
            singulars.cleanPrefix(),
            plurals.cleanPrefix(),
            abbreviations.cleanPrefix(),
            dimensionality,
            unitNames
        )
        val baseQuantity = Quantity(1.0, mapOf(baseUnit to 1))
        Prefix.PREFIXES.forEach { prefix ->
            val prefixedSingulars = singulars.map {
                it.replace(PERCENT, prefix.prefix)
            }
            val prefixedPlurals = plurals.map {
                it.replace(PERCENT, prefix.prefix)
            }
            val prefixedAbbreviations = abbreviations.map {
                it.replace(PERCENT, prefix.label)
            }.toMutableList()

            val prefixedUnit = createUnitChecking(
                prefixedSingulars, prefixedPlurals, prefixedAbbreviations, dimensionality
            )
            val conversion = if (prefix.power < 0) {
                Conversion(
                    baseQuantity, Quantity(10.0.pow(-prefix.power), mapOf(prefixedUnit to 1))
                )
            } else {
                Conversion(
                    baseQuantity.multiply(10.0.pow(prefix.power)), Quantity(1.0, mapOf(prefixedUnit to 1))
                )
            }
            addConversion(conversion, baseUnit, prefixedUnit)
        }
    }

    private fun createUnitChecking(
        singulars: List<String>,
        plurals: List<String>,
        abbreviations: List<String>,
        dimensionality: Map<DIMENSION, Int>,
    ): SimpleUnit {
        val names = (singulars + plurals + abbreviations)
        if (names.any { unitNames.containsKey(it) }) {
            var predefinedUnit: SimpleUnit? = null
            val undefinedNames = mutableListOf<String>()
            for (name in names) {
                if (unitNames.containsKey(name)) {
                    predefinedUnit = unitNames[name]!!
                } else {
                    undefinedNames.add(name)
                }
            }
            assignNames(undefinedNames, predefinedUnit!!, unitNames)
            return predefinedUnit
        }
        return createUnit(singulars, plurals, abbreviations, dimensionality, unitNames)
    }

    private fun createUnit(
        singulars: List<String>,
        plurals: List<String>,
        abbreviations: List<String>,
        dimensionality: Map<DIMENSION, Int>,
        map: HashMap<String,SimpleUnit>,
    ): SimpleUnit {
        val unit = SimpleUnit(singulars, plurals, abbreviations, dimensionality)
        assignNames(singulars + plurals + abbreviations, unit, map)
        return unit
    }

    private fun assignNames(names: List<String>, unit: SimpleUnit, map: HashMap<String, SimpleUnit>) {
        names.forEach {
            map[it.lowercaseGreaterThan3()] = unit
        }
    }

    private fun computeComplexities() {
        val distinctUnits = unitNames.values.distinct()
        val unprocessed = distinctUnits.toMutableSet()
        val distance: HashMap<SimpleUnit, Int> = hashMapOf()

        setOf(
            "m", "s", "K", "kg", "A", "mol", "cd", "rotation", "byte"
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
            if (index > distinctUnits.size * 3) {
                Log.e("UnitStore", "unprocessed units: $unprocessed")
                throw Exception("index surpassed ${distinctUnits.size * 3} when processing units; are you sure all your units are connected?")
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
            for (conversion in node.getComplexConversions()) {
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

    private fun splitConversionByEquals(string: String): List<String> =
        string.split("=").map { it.trim() }.let {
            require(it.size == 2) { "Invalid conversion: $string" }
            return it
        }


    private fun String.extractNames(): Triple<List<String>, List<String>, List<String>> =
        split(";").let { triple ->
            assert(triple.size <= 3) { "Invalid list of names: $this" }
            val singulars = triple[0].split(',').map { it.trim() }.filter { it.isNotEmpty() }

            val abbreviations =
                triple.getOrNull(1)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
                    ?: listOf()

            val plurals =
                triple.getOrNull(2)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
                    ?.toMutableList() ?: mutableListOf()

            return Triple(singulars.map { singular ->
                val split = singular.split('|')
                assert(split.size <= 2) { "Invalid name: $singular" }
                if (split.size == 2) {
                    plurals.add(singular.replace("|", ""))
                }
                split[0]
            }, plurals, abbreviations.flatMap { old ->
                mutableListOf<String>().also { news ->
                    old.split(",").forEach { substring ->
                        val builder = StringBuilder()
                        for (chunk in substring.trim().split('|')) {
                            builder.append(chunk)
                            news.add(builder.toString())
                        }
                    }
                }
            })
        }

    /**
     * For a name of a SimpleUnit, this will just return a mapOf(it to 1)
     * So for those you can just take
     * @return the unit or alias with the given name as a map of SimpleUnits to their exponents
     * @throws UndefinedUnitException if the unit is not defined as a unit or an alias
     */
    fun getUnit(name: String): Map<SimpleUnit, Int> {
        val casedName = name.lowercaseGreaterThan3()
        unitNames[casedName]?.let { return mapOf(it to 1) }
        aliases[casedName]?.let { return it.getComplexConversions().first().getOther(it).units }
        unitNames.forEach { (key, value) ->
            Log.e("UnitStore", "key: $key, value: $value")
        }
        throw UndefinedUnitException(casedName)
    }

    fun getDescriptions() = (unitNames + aliases).values.distinct().map { it.describe() }.sorted()
}