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

        UnitStore.loadUnitsFromJson(this)

        convertButton.setOnClickListener {
            ViewCompat.getWindowInsetsController(window.decorView)
                ?.hide(WindowInsetsCompat.Type.ime())
            outputValue.text = ""
            conversionSteps.text = ""
            val result = convert()
            result?.let {
                Snackbar.make(window.decorView, "Error: ${it.message}", Snackbar.LENGTH_LONG).show()
                Log.e(TAG, "attemptConversion: $it")
            }
        }
    }

    private fun getAndValidateInputValue(): Result<Double> = inputValue.text.toString().let {
        if (it.isBlank()) Result.failure(Throwable("Input value cannot be empty"))
        else Result.success(it.toDouble())
    }

    private fun validateUnitCounts(
        leftTop: List<SimpleUnit>,
        leftBottom: List<SimpleUnit>,
        rightTop: List<SimpleUnit>,
        rightBottom: List<SimpleUnit>
    ): Throwable? {
        val left = foldTopAndBottom(leftTop, leftBottom)
        val right = foldTopAndBottom(rightTop, rightBottom)
        Log.i(TAG, "validateUnitCounts: $left, $right")
        require(left == right) { "Starting and ending units have different sizes" }
        return null
    }

    private fun convert(): Throwable? {
        val inputValue = getAndValidateInputValue().getOrElse { return it }

        val startingNumeratorText = startingNumerator.text.toString()
        val endingNumeratorText = endingNumerator.text.toString()
        val startingDenominatorText = startingDenominator.text.toString()
        val endingDenominatorText = endingDenominator.text.toString()

        Log.i(
            TAG,
            "convert: `$inputValue` `$startingNumeratorText` / `$startingDenominatorText` --> `$endingNumeratorText` / `$endingDenominatorText`"
        )

        if (startingNumeratorText.isEmpty() || endingNumeratorText.isEmpty()) {
            return Throwable("Numerator cannot be empty")
        }
        val startNumerator = UnitStore.extractUnits(
            startingNumeratorText, "Starting Numerator", false
        ).getOrElse { return it }
        val endNumerator = UnitStore.extractUnits(
            endingNumeratorText, "Ending Numerator", false
        ).getOrElse { return it }
        val startDenominator = UnitStore.extractUnits(
            startingDenominatorText, "Starting Denominator", true
        ).getOrElse { return it }
        val endDenominator = UnitStore.extractUnits(
            endingDenominatorText, "Ending Denominator", true
        ).getOrElse { return it }
        try {
            validateUnitCounts(startNumerator, startDenominator, endNumerator, endDenominator)
        } catch (e: IllegalArgumentException) {
            return e
        }
        val steps = mutableListOf<ConversionStep>()
        val runningAnswer = RunningAnswer(inputValue)
        val (startNumeratorCopy, startDenominatorCopy) = recombineNumeratorAndDenominator(
            startNumerator,
            startDenominator
        )
        val (endNumeratorCopy, endDenominatorCopy) = recombineNumeratorAndDenominator(
            endNumerator,
            endDenominator
        )
        for (path in findPathsBetween(startNumeratorCopy, endNumeratorCopy)) {
            steps.addPath(path, runningAnswer, false)
        }
        for (path in findPathsBetween(startDenominatorCopy, endDenominatorCopy)) {
            steps.addPath(path, runningAnswer, true)
        }

        if (steps.isEmpty()) {
            return Throwable("No conversion path found")
        }

        displayOutput(
            inputValue,
            runningAnswer.value,
            steps.dedupCount(),
            startNumerator.dedupCount(),
            startDenominator.dedupCount(),
            endNumerator.dedupCount(),
            endDenominator.dedupCount()
        )
        return null
    }

    private fun recombineNumeratorAndDenominator(
        n1: List<SimpleUnit>,
        d1: List<SimpleUnit>
    ): Pair<MutableList<SimpleUnit>, MutableList<SimpleUnit>> {
        val n2 = mutableListOf<SimpleUnit>()
        val d2 = mutableListOf<SimpleUnit>()
        recombine(n1, n2, d2)
        recombine(d1, d2, n2)
        return Pair(n2, d2)
    }

    private fun recombine(
        supplier: List<SimpleUnit>,
        alikeReceiver: MutableList<SimpleUnit>,
        oppositeReceiver: MutableList<SimpleUnit>
    ) {
        supplier.forEach {
            it.getConstituents().forEach { (unit, count) ->
                if (count > 0) alikeReceiver.addAll(List(count) { unit })
                else if (count < 0) oppositeReceiver.addAll(List(-count) { unit })
            }
        }
    }

    private fun displayOutput(
        inputValue: Double,
        answer: Double,
        steps: Map<ConversionStep, Int>,
        startNumerator: Map<SimpleUnit, Int>,
        startDenominator: Map<SimpleUnit, Int>,
        endNumerator: Map<SimpleUnit, Int>,
        endDenominator: Map<SimpleUnit, Int>
    ) {
        outputValue.text = TripleStringBuilder(outputValue.maxEms).let {
            it.appendValue(answer, 3, 1)
            it.appendUnits(endNumerator, endDenominator, 0)
            it.squishSquash(0)
        }

        conversionSteps.text = TripleStringBuilder(conversionSteps.maxEms).let {
            it.appendValue(inputValue, 2, 2)
            it.appendUnits(startNumerator, startDenominator, 2)
            for ((step, exponent) in steps) {
                it.appendMiddle(" × ", 2)
                it.appendConversionStep(step, exponent, 2)
            }
            it.appendMiddle(" = ", 2)
            it.appendValue(answer, 3, 2)
            it.appendUnits(endNumerator, endDenominator, 2)
            it.squishSquash(0)
        }

        Log.d(
            TAG, "outputValue.text: \n${outputValue.text}"
        )
        Log.v(
            TAG, "conversionSteps.text:\n${conversionSteps.text}"
        )
    }

    private fun MutableList<ConversionStep>.addPath(
        path: List<SimpleUnit>, runningAnswer: RunningAnswer, inverse: Boolean
    ) = path.iterator().peeking().let {
        for (unit in it) {
            val next = try {
                it.peek()
            } catch (_: NoSuchElementException) {
                break
            }
            add(
                if (inverse) {
                    next.convert(unit, runningAnswer)
                } else {
                    unit.convert(next, runningAnswer)
                }
            )
        }
    }

    private fun findPathsBetween(
        starts: List<SimpleUnit>, ends: MutableList<SimpleUnit>
    ) = mutableListOf<List<SimpleUnit>>().also {
        for (start in starts) {
            val shortestPath = findFirstShortestPath(start, ends)
            if (shortestPath.isNotEmpty()) {
                it.add(shortestPath)
            }
        }
    }.toList()

    private fun findFirstShortestPath(
        start: SimpleUnit,
        ends: MutableList<SimpleUnit>
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
        queue.addLast(start)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (neighbor in node.getConnections()) {
                if (!distance.containsKey(neighbor)) {
                    distance[neighbor] = distance[node]!! + 1
                    parent[neighbor] = node
                    queue.addLast(neighbor)
                }
            }
        }
        return Pair(parent, distance)
    }
}