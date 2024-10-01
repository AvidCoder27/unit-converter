package com.reeves.unitconverter

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.reeves.unitconverter.UnitStore.asFundamental

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var inputValue: EditText
    private lateinit var startingNumerator: EditText
    private lateinit var startingDenominator: EditText
    private lateinit var endingNumerator: EditText
    private lateinit var endingDenominator: EditText
    private lateinit var convertButton: Button
    private lateinit var conversionSteps: TextView
    private lateinit var outputValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inputValue = findViewById(R.id.input_value)
        startingNumerator = findViewById(R.id.starting_numerator)
        startingDenominator = findViewById(R.id.starting_denominator)
        endingNumerator = findViewById(R.id.ending_numerator)
        endingDenominator = findViewById(R.id.ending_denominator)
        convertButton = findViewById(R.id.convert_button)
        conversionSteps = findViewById(R.id.conversion_steps)
        outputValue = findViewById(R.id.output_value)

        UnitStore.loadFromJson(this)

        convertButton.setOnClickListener {
            @Suppress("DEPRECATION") ViewCompat.getWindowInsetsController(window.decorView)
                ?.hide(WindowInsetsCompat.Type.ime())
            outputValue.text = ""
            conversionSteps.text = ""
            try {
                convert()
            } catch (e: Exception) {
                when (e) {
                    is InvalidUnitsException, is UndefinedUnitException, is ImpossibleConversionException, is MeaninglessConversionException, is PromotionRequiredException -> {
                        Snackbar.make(window.decorView, "Error: ${e.message}", Snackbar.LENGTH_LONG)
                            .show()
                        Log.e(TAG, "attemptConversion: $e")
                    }

                    else -> throw e
                }
            }
        }
    }

    private fun convert() {
        val inputValue = try {
            inputValue.text.toString().toDouble()
        } catch (_: NumberFormatException) {
            1.0
        }

        val left = startingNumerator.text.toString().intoQuantity()
            .divide(startingDenominator.text.toString().intoQuantity()).removeValue().clean()
        val right = endingNumerator.text.toString().intoQuantity()
            .divide(endingDenominator.text.toString().intoQuantity()).removeValue().clean()
        Log.d(TAG, "convert: left = `$left` ")
        Log.d(TAG, "convert: right = `$right` ")
        validateConversion(left, right)

        val runningAnswer = RunningAnswer(inputValue)

        val steps = mutableListOf<Conversion>().runCatching {
            for (path in findPathsBetween(left, right, true)) {
                addAll(traversePath(path, runningAnswer, false))
            }
            for (path in findPathsBetween(left, right, false)) {
                addAll(traversePath(path, runningAnswer, true))
            }
            toList()
        }.getOrElse { failure ->
            if (failure !is PromotionRequiredException) throw failure
            mutableListOf<Conversion>().run {
                Log.d(TAG, "convert: fundamental = `${left.asFundamental()}` ")
                for (conversion in findPathToFundamental(left)) {
                    add(conversion)
                }
                for (conversion in findPathToFundamental(right).reversed().drop(1)) {
                    add(conversion)
                }
                toList()
            }
        }

        if (steps.isEmpty()) {
            throw ReallyBadException("No paths found with conversion input:`$inputValue` `${startingNumerator.text}` / `${startingDenominator.text}` --> `${endingNumerator.text}` / `${endingDenominator.text}`")
        }

        displayOutput(
            inputValue, runningAnswer.value, steps.dedupCount(), left, right
        )
    }

    private fun findPathToFundamental(
        quantity: Quantity, path: MutableList<Conversion> = mutableListOf(), upperExponent: Int = 1
    ): MutableList<Conversion> {
        if (quantity.complexity() == 0) {
            Log.d(TAG, "findPathToFundamental: returning $path on quantity $quantity")
            return path
        }
        quantity.units.forEach { (unit, exponent) ->
            findPathToFundamental(getLowestComplexityConvertibleQuantity(unit), path, upperExponent * exponent)
        }
        throw IllegalStateException("While finding path to fundamental")
    }

    private fun getLowestComplexityConvertibleQuantity(unit: SimpleUnit): Quantity {
        var lowestComplexity = Int.MAX_VALUE
        var leastComplex: Quantity? = null
        for (conversion in unit.getConversions()) {
            val quantity = conversion.getOther(unit)
            if (quantity.complexity() < lowestComplexity) {
                leastComplex = quantity
                lowestComplexity = quantity.complexity()
            }
        }
        Log.d(TAG, "getLowestComplexityConvertibleQuantity: returning $leastComplex")
        return leastComplex!!
    }

    private fun traversePath(
        path: List<SimpleUnit>, answer: RunningAnswer, invert: Boolean
    ): List<Conversion> = mutableListOf<Conversion>().also { list ->
        for (index in 1 until path.size) {
            Log.d(TAG, "traversePath: ${path[index - 1]} -> ${path[index]}")
            val conversion = path[index - 1].getConversionTo(path[index])
            conversion.flippedToConvertInto(path[index], invert).let {
                list.add(it)
                it.apply(answer)
            }
        }
    }

    private fun validateConversion(left: Quantity, right: Quantity) {
        if (left === right) {
            throw MeaninglessConversionException("the input and output units are the same")
        }
        val leftDimensionality = left.dimensionality().clean()
        val rightDimensionality = right.dimensionality().clean()
        Log.d(TAG, "validateConversion: left: $leftDimensionality, right: $rightDimensionality")
        if (leftDimensionality != rightDimensionality) {
            throw ImpossibleConversionException()
        }
        if (leftDimensionality.isEmpty() || rightDimensionality.isEmpty()) {
            throw MeaninglessConversionException("all the units cancel out on both sides")
        }
    }

    private fun displayOutput(
        inputValue: Double,
        answer: Double,
        steps: Map<Conversion, Int>,
        left: Quantity,
        right: Quantity
    ) {
        outputValue.text = TripleStringBuilder(outputValue.maxEms).let {
            it.appendValue(answer, 3, 1)
            it.appendUnits(right, 0)
            it.squishSquash(0)
        }

        conversionSteps.text = TripleStringBuilder(conversionSteps.maxEms).let {
            it.appendValue(inputValue, 2, 2)
            it.appendUnits(left, 2)
            for ((step, exponent) in steps) {
                it.appendMiddle(" × ", 2)
                it.appendConversion(step, exponent, 2)
            }
            it.appendMiddle(" = ", 2)
            it.appendValue(answer, 3, 2)
            it.appendUnits(right, 2)
            it.squishSquash(0)
        }

        Log.d(
            TAG, "outputValue.text: \n${outputValue.text}"
        )
        Log.v(
            TAG, "conversionSteps.text:\n${conversionSteps.text}"
        )
    }

    private fun findPathsBetween(
        starts: Quantity, ends: Quantity, top: Boolean
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
        start: SimpleUnit, ends: MutableList<SimpleUnit>
    ): List<SimpleUnit> {
        val (parent, distance) = breadthFirstSearch(start) { it.getOneToOneConnections() }
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
                Log.d(
                    TAG,
                    "findFirstShortestPath: removing $destination from $ends and returning path: $path"
                )
                ends.remove(destination) // ensure that this destination is not used again
                return path
            }
        }
        return listOf()
    }
}