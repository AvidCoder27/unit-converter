package com.reeves.unitconverter

import android.content.Context
import android.util.Log
import org.json.JSONObject

object UnitStore {
    private val unitAliases: HashMap<String, SimpleUnit> = HashMap()

    fun loadUnitsFromJson(context: Context) {
        val jsonString = context.assets.open("units.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        jsonObject.getJSONArray("units").let { array ->
            for (i in 0 until array.length()) {
                val unitString = array.getString(i)
                createUnit(unitString)
            }
        }

        jsonObject.getJSONArray("compound_units").let { array ->
            for (i in 0 until array.length()) {
                val unitString = array.getString(i)
                createUnit(unitString, true)
            }
        }

        jsonObject.getJSONArray("conversions").let { array ->
            for (i in 0 until array.length()) {
                val conversionString = array.getString(i).lowercase()
                val equalParts = splitConversionByEquals(conversionString)
                val unit1Parts = equalParts[0].split(" ", limit = 2).map { it.trim() }
                val unit2Parts = equalParts[1].split(" ", limit = 2).map { it.trim() }
                require(unit1Parts.size == 2 && unit2Parts.size == 2) { "Invalid conversion: `$conversionString` has unit1Parts.size = ${unit1Parts.size} and unit2Parts.size = ${unit2Parts.size}" }
                try {
                    createConversion(
                        unitAliases[unit1Parts[1]]!!,
                        unitAliases[unit2Parts[1]]!!,
                        unit1Parts[0].toDouble(),
                        unit2Parts[0].toDouble()
                    )
                } catch (_: NullPointerException) {
                    throw Exception("Invalid unit in conversion: $conversionString")
                } catch (_: NumberFormatException) {
                    throw NumberFormatException("Invalid number in conversion: $conversionString")
                }
            }
        }

        jsonObject.getJSONArray("compound_definitions").let { array ->
            for (i in 0 until array.length()) {
                val definitionString = array.getString(i).lowercase()
                val equalParts = splitConversionByEquals(definitionString)
                val (compoundUnit, constituentUnits) = try {
                    Pair(
                        unitAliases[equalParts[0]]!!,
                        equalParts[1].parseUnitsToMap().mapKeys { unitAliases[it.key.trim()]!! }
                    )
                } catch (_: NullPointerException) {
                    throw Exception("Invalid unit in compound definition: $definitionString")
                }
                require(compoundUnit is CompoundUnit) { "Non-compound unit used in left side compound definition: ${compoundUnit.singular()}" }
                compoundUnit.addConstituents(constituentUnits)
                Log.i("UnitStore", "Loaded compound definition: $definitionString, assigning $compoundUnit size of ${compoundUnit.getSize()}")
            }
        }
    }

    private fun splitConversionByEquals(string: String): List<String> =
        string.split("=").map { it.trim() }.let {
            require(it.size == 2) { "Invalid conversion: $string" }
            return it
        }

    fun extractUnits(
        text: String,
        field: String,
        allowEmpty: Boolean
    ): Result<MutableList<SimpleUnit>> {
        if (text.isEmpty()) {
            if (allowEmpty) {
                return Result.success(mutableListOf())
            }
            return Result.failure(Throwable("$field cannot be empty"))
        }
        val substrings = text.split('*', ',').map { it.lowercase().trim() }
        val units = mutableListOf<SimpleUnit>()
        for (str in substrings) {
            units.add(
                unitAliases[str]
                    ?: return Result.failure(Throwable("$field contains invalid units"))
            )
        }
        return Result.success(units)
    }

    private fun createUnit(names: String, compound: Boolean = false): SimpleUnit {
        val namesList = mutableListOf<String>()
        for (name in names.split(",")) {
            val chunks = name.trim().lowercase().split('|')
            val builder = StringBuilder()
            for (chunk in chunks) {
                builder.append(chunk)
                namesList.add(builder.toString())
            }
        }

        val unit = if (compound) CompoundUnit(namesList) else SimpleUnit(namesList)
        namesList.forEach { unitAliases[it] = unit }
        return unit
    }

    private fun createConversion(from: SimpleUnit, to: SimpleUnit, fromValue: Double, toValue: Double) {
        val conversion = Conversion(toValue, fromValue)
        from.addConversion(to, conversion)
        to.addConversion(from, conversion.inverse())
    }
}