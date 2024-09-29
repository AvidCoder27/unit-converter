package com.reeves.unitconverter

import android.content.Context
import org.json.JSONObject
import kotlin.text.split
import kotlin.text.toDouble
import kotlin.text.trim

object UnitStore {
    private val unitAliases: HashMap<String, Unit> = HashMap()

    fun loadUnitsFromJson(context: Context) {
        val jsonString = context.assets.open("units.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        val unitsArray = jsonObject.getJSONArray("units")
        for (i in 0 until unitsArray.length()) {
            val unitString = unitsArray.getString(i)
            createUnit(unitString)
        }

        val conversionsArray = jsonObject.getJSONArray("conversions")
        for (i in 0 until conversionsArray.length()) {
            val conversionString = conversionsArray.getString(i)
            val parts = conversionString.split("=").map { it.trim() }
            require(parts.size >= 2) { "Invalid conversion: $conversionString" }
            val unit1Parts = parts[0].split(" ", limit = 2).map { it.lowercase().trim() }
            val unit2Parts = parts[1].split(" ", limit = 2).map { it.lowercase().trim() }
            require(unit1Parts.size == 2 && unit2Parts.size == 2) { "Invalid conversion: `$conversionString` has unit1Parts.size = ${unit1Parts.size} and unit2Parts.size = ${unit2Parts.size}" }
            val unit1 = unitAliases[unit1Parts[1]]
            val unit2 = unitAliases[unit2Parts[1]]
            try {
                createConversion(unit1!!, unit2!!, unit1Parts[0].toDouble(), unit2Parts[0].toDouble())
            } catch (_: NullPointerException) {
                throw Exception("Invalid unit in conversion: $conversionString")
            } catch (_: NumberFormatException) {
                throw NumberFormatException("Invalid number in conversion: $conversionString")
            }
        }
    }

    fun extractUnits(
        text: String,
        field: String,
        allowEmpty: Boolean
    ): Result<MutableList<Unit>> {
        if (text.isEmpty()) {
            if (allowEmpty) {
                return Result.success(mutableListOf())
            }
            return Result.failure(Throwable("$field cannot be empty"))
        }
        val substrings = text.split('*', ',').map { it.lowercase().trim() }
        val units = mutableListOf<Unit>()
        for (str in substrings) {
            units.add(
                unitAliases[str]
                    ?: return Result.failure(Throwable("$field contains invalid units"))
            )
        }
        return Result.success(units)
    }

    private fun createUnit(names: String): Unit {
        val namesList = mutableListOf<String>()
        for (name in names.split(",")) {
            val chunks = name.trim().lowercase().split('|')
            val builder = StringBuilder()
            for (chunk in chunks) {
                builder.append(chunk)
                namesList.add(builder.toString())
            }
        }
        val unit = Unit(namesList)
        namesList.forEach { unitAliases[it] = unit }
        return unit
    }

    private fun createConversion(from: Unit, to: Unit, fromValue: Double, toValue: Double) {
        val conversion = Conversion(toValue, fromValue)
        from.addConversion(to, conversion)
        to.addConversion(from, conversion.inverse())
    }
}