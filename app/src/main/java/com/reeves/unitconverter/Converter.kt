package com.reeves.unitconverter

import android.util.Log
import kotlin.math.absoluteValue
import kotlin.math.sign

private const val TAG = "Converter"
private const val ADD_TO_CELSIUS = 273.15
private const val ADD_TO_FAHRENHEIT = 459.67

class Converter(private val outputValue: MathView, private val conversionSteps: MathView) {
    private val fahrenheit = UnitStore.getUnit("fahrenheit").keys.first()
    private val celsius = UnitStore.getUnit("celsius").keys.first()
    private val kelvin = UnitStore.getUnit("kelvin").keys.first()
    private val rankine = UnitStore.getUnit("rankine").keys.first()

    private var finalValue: Quantity? = null

    fun getFinalValue() = finalValue

    fun convert(
        inputValueString: String,
        startingNumeratorString: String,
        startingDenominatorString: String,
        endingNumeratorString: String,
        endingDenominatorString: String,
    ) {
        finalValue = null
        val unvalidatedLeft =
            startingNumeratorString.intoQuantity().divide(startingDenominatorString.intoQuantity())
                .removeValue().clean()
        val right =
            endingNumeratorString.intoQuantity().divide(endingDenominatorString.intoQuantity())
                .removeValue().clean()
        Log.d(TAG, "convert: left = `$unvalidatedLeft`")
        Log.d(TAG, "convert: right = `$right`")
        val flip = validateConversion(unvalidatedLeft, right)
        val left = if (flip) unvalidatedLeft.inverse() else unvalidatedLeft

        val inputValue = try {
            inputValueString.toDouble()
        } catch (_: NumberFormatException) {
            1.0
        }.let {
            if (flip) 1.0 / it
            else it
        }

        val runningAnswer = RunningAnswer(inputValue)
        var doingSimpleTempConversion = false
        val steps = mutableListOf<Conversion>().runCatching {
            if (left.dimensionality() == mapOf(DIMENSION.TEMPERATURE to 1) && left.units.size == 1 && right.units.size == 1) {
                Log.d(TAG, "convert: simple temp conversion")
                val path = findPathsBetween(left, right, true).let {
                    if (it.size != 1) {
                        // we expect exactly one path for simple temperature conversion
                        throw PromotionRequiredException()
                    }
                    it.first()
                }
                addAll(traversePath(path, runningAnswer, false))
                doingSimpleTempConversion = true
            } else {
                Log.d(TAG, "convert: simple conversion")
                for (path in findPathsBetween(left, right, true)) {
                    addAll(traversePath(path, runningAnswer, false))
                }
                for (path in findPathsBetween(left, right, false)) {
                    addAll(traversePath(path, runningAnswer, true))
                }
            }
            this
        }.getOrElse { failure ->
            if (failure !is PromotionRequiredException) throw failure
            Log.d(TAG, "convert: promoted conversion")
            mutableListOf<Conversion>().apply {
                for (conversion in pathToFundamentals(left, true)) {
                    add(conversion)
                    conversion.apply(runningAnswer)
                }
                for (conversion in pathToFundamentals(right, false).reversed()) {
                    add(conversion)
                    conversion.apply(runningAnswer)
                }
            }
        }

        if (steps.isEmpty()) {
            throw MeaninglessConversionException("the input and output units either cancel out or are the same")
        }

        if (doingSimpleTempConversion) {
            displayWeirdOutput(steps, left.withValue(inputValue), right)
        } else {
            displayOutput(
                dedupAndCancelOut(steps),
                left.withValue(inputValue),
                right.withValue(runningAnswer.value),
                flip
            )
        }
    }

    private fun dedupAndCancelOut(conversions: List<Conversion>): Map<Conversion, Int> {
        val conversionMap = conversions.groupingBy { it }.eachCountTo(mutableMapOf())
        val processedConversions = mutableSetOf<Conversion>()

        for (conversion in conversions) {
            if (conversion in processedConversions) continue
            val inverse = conversions.find { it.isInverseOf(conversion) }
            if (inverse != null && inverse !in processedConversions) {
                val count = conversionMap[conversion] ?: 0
                val inverseCount = conversionMap[inverse] ?: 0

                if (count > inverseCount) {
                    conversionMap[conversion] = count - inverseCount
                } else if (count < inverseCount) {
                    conversionMap[inverse] = inverseCount - count
                } else {
                    conversionMap.remove(conversion)
                    conversionMap.remove(inverse)
                }
                processedConversions.add(conversion)
                processedConversions.add(inverse)
            }
        }
        return conversionMap
    }

    private fun pathToFundamentals(input: Quantity, goingDown: Boolean): List<Conversion> =
        mutableListOf<Conversion>().also { path ->
            var quantity = input
            while (true) {
                quantity = stepTowardsFundamentals(quantity, path, goingDown) ?: break
            }
        }

    private fun stepTowardsFundamentals(
        inputQuantity: Quantity, path: MutableList<Conversion>, goingDown: Boolean,
    ): Quantity? {
        val x = inputQuantity.units.map { (unit, exponent) ->
            Triple(unit, exponent, getLowestComplexityConvertibleQuantity(unit))
        }.filter { it.third != null }
        if (x.isEmpty()) return null
        val (chosen, exponent, leastComplex) = x.first()
        repeat(exponent.absoluteValue) {
            path.add(
                chosen.getConversionToQuantity(leastComplex!!)
                    .flippedToConvertInto(chosen, goingDown xor (exponent.sign < 0))
            )
        }
        return inputQuantity.multiply(leastComplex!!.divide(chosen).pow(exponent))
    }

    private fun getLowestComplexityConvertibleQuantity(unit: SimpleUnit): Quantity? {
        if (unit.complexity == 0) return null
        var lowestComplexity = unit.complexity
        val leastComplex: MutableList<Quantity> = mutableListOf()
        for (conversion in unit.getSimpleConversions()) {
            val quantity = conversion.getOther(unit)
            if (quantity.complexity() < lowestComplexity) {
                leastComplex.clear()
                leastComplex.add(quantity)
                lowestComplexity = quantity.complexity()
            } else if (quantity.complexity() == lowestComplexity) {
                leastComplex.add(quantity)
            }
        }
        leastComplex.sortBy { it.hashCode() }
        return leastComplex.first()
    }

    private fun traversePath(
        path: List<SimpleUnit>, answer: RunningAnswer, invert: Boolean,
    ): List<Conversion> = mutableListOf<Conversion>().also { list ->
        for (index in 1 until path.size) {
            val (conversion, exponent) = path[index - 1].getConversionToUnit(path[index])
            if (exponent.absoluteValue > 1) {
                throw PromotionRequiredException()
            }
            conversion.flippedToConvertInto(path[index], invert).let { flipped ->
                list.add(flipped)
                flipped.apply(answer)
            }
        }
    }

    private fun validateConversion(left: Quantity, right: Quantity): Boolean {
        val leftDimensionality = left.dimensionality().clean()
        val rightDimensionality = right.dimensionality().clean()
        Log.d(TAG, "validateConversion: left: $leftDimensionality, right: $rightDimensionality")
        if (left === right) {
            throw MeaninglessConversionException("the input and output are the same!")
        }
        if (leftDimensionality.isEmpty() || rightDimensionality.isEmpty()) {
            throw MeaninglessConversionException("all the units cancel out!")
        }
        if (leftDimensionality == rightDimensionality.mapValues { -it.value }) {
            return true
            //throw RequiresFlippingException()
        }
        if (leftDimensionality == rightDimensionality) return false
        throw ImpossibleConversionException()
    }

    private fun displayWeirdOutput(
        steps: MutableList<Conversion>,
        left: Quantity,
        right: Quantity,
    ) {
        val runningAnswer = RunningAnswer(left.value)
        val katexSteps = KatexStringBuilder()
        var putAnswerAgainAtEnd = true
        for ((index, step) in steps.withIndex()) {
            putAnswerAgainAtEnd = true
            val (numerator, denominator) = step.getLonelies().let {
                try {
                    Pair(it.first!!, it.second!!)
                } catch (e: NullPointerException) {
                    throw IllegalArgumentException("Can only displayWeirdOutput on steps with one-to-one conversions!")
                }
            }
            if (index > 0) {
                katexSteps.appendNewLine()
            }
            if (denominator == celsius && numerator == kelvin) {
                katexSteps.appendAddingConversion(runningAnswer, celsius, ADD_TO_CELSIUS, kelvin)
                putAnswerAgainAtEnd = false
            } else if (denominator == kelvin && numerator == celsius) {
                katexSteps.appendAddingConversion(runningAnswer, kelvin, -ADD_TO_CELSIUS, celsius)
                putAnswerAgainAtEnd = false
            } else if (denominator == fahrenheit && numerator == rankine) {
                katexSteps.appendAddingConversion(runningAnswer, fahrenheit, ADD_TO_FAHRENHEIT, rankine)
                putAnswerAgainAtEnd = false
            } else if (denominator == rankine && numerator == fahrenheit) {
                katexSteps.appendAddingConversion(runningAnswer, rankine, -ADD_TO_FAHRENHEIT, fahrenheit)
                putAnswerAgainAtEnd = false
            } else if (denominator == celsius && numerator == fahrenheit) {
                katexSteps.appendCelsiusToFahrenheit(runningAnswer)
            } else if (denominator == fahrenheit && numerator == celsius) {
                katexSteps.appendFahrenheitToCelsius(runningAnswer)
            } else {
                katexSteps.appendValueAndUnits(Quantity(runningAnswer.value, mapOf(denominator to 1)))
                katexSteps.appendConversion(step, 1)
                step.apply(runningAnswer)
            }
        }
        val realRight = right.withValue(runningAnswer.value)
        finalValue = realRight
        if (putAnswerAgainAtEnd) {
            katexSteps.appendEqualsSign()
            katexSteps.appendValueAndUnits(realRight)
        }
        outputValue.setDisplayText(KatexStringBuilder().let {
            it.appendValueAndUnits(realRight)
            it.toString()
        })
        conversionSteps.setDisplayText(katexSteps.toString())
    }

    private fun displayOutput(
        steps: Map<Conversion, Int>, left: Quantity, right: Quantity, flip: Boolean,
    ) {
        finalValue = right
        outputValue.setDisplayText(KatexStringBuilder().let {
            it.appendValueAndUnits(right)
            it.toString()
        })

        conversionSteps.setDisplayText(KatexStringBuilder().let {
            if (flip) it.appendInverseQuantity(left.inverse())
            it.appendValueAndUnits(left)
            for ((step, exponent) in steps) {
                it.appendMultiplicationSign()
                it.appendConversion(step, exponent)
            }
            it.appendEqualsSign()
            it.appendValueAndUnits(right)
            it.toString()
        })

        Log.d(TAG, "outputValue: \n${outputValue.getText()}")
        Log.v(TAG, "conversionSteps:\n${conversionSteps.getText()}")
    }

    private fun findPathsBetween(
        starts: Quantity, ends: Quantity, top: Boolean,
    ): List<List<SimpleUnit>> = mutableListOf<List<SimpleUnit>>().also {
        val expandedEnds = ends.expand(top).toMutableList()
        for (start in starts.expand(top)) {
            val shortestPath = findFirstShortestPath(start, expandedEnds)
            if (shortestPath.isEmpty()) {
                throw PromotionRequiredException()
            }
            it.add(shortestPath)
        }
        // if we were unable to convert all the starts to ends,
        // then this method fails and we promote
        if (expandedEnds.isNotEmpty()) throw PromotionRequiredException()
    }.toList()

    private fun findFirstShortestPath(
        start: SimpleUnit, ends: MutableList<SimpleUnit>,
    ): List<SimpleUnit> {
        val (parent, distance) = breadthFirstSearch(start)
        for (destination in ends) {
            if (distance.containsKey(destination)) {
                val path = mutableListOf<SimpleUnit>()
                var current = destination
                path.add(destination)
                while (parent.containsKey(current)) {
                    current = parent[current]!!
                    path.add(current)
                }
                assert(current === start) { "Path thru parents did not properly lead back to start" }
                path.reverse()
                ends.remove(destination) // ensure that this destination is not used again
                return path
            }
        }
        return listOf()
    }

    private fun breadthFirstSearch(start: SimpleUnit): Pair<HashMap<SimpleUnit, SimpleUnit>, HashMap<SimpleUnit, Int>> {
        val parent: HashMap<SimpleUnit, SimpleUnit> = hashMapOf()
        val distance: HashMap<SimpleUnit, Int> = hashMapOf()
        distance[start] = 0
        val queue: ArrayDeque<SimpleUnit> = ArrayDeque()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (neighbor in node.getOneToOneConversions()) {
                if (!distance.containsKey(neighbor)) {
                    distance[neighbor] = distance[node]!! + 1
                    parent[neighbor] = node
                    queue.add(neighbor)
                }
            }
        }
        return Pair(parent, distance)
    }
}