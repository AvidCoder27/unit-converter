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
            val result = convert()
            result?.let { Log.e(TAG, "attemptConversion: $it") }
        }
    }

    private fun getAndValidateInputValue(): Result<Double> {
        val inputValueText = inputValue.text.toString()
        if (inputValueText.isEmpty()) {
            return Result.failure(Throwable("Input value cannot be empty"))
        }
        return Result.success(inputValueText.toDouble())
    }

    private fun validateUnitCounts(
        left: List<Unit>,
        right: List<Unit>,
        field: String
    ): Throwable? {
        if (left.size > right.size) {
            return Throwable("Starting $field has more units than ending $field")
        } else if (left.size < right.size) {
            return Throwable("Ending $field has more units than starting $field")
        }
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
            startingNumeratorText,
            "Starting Numerator",
            false
        ).getOrElse { return it }
        val endNumerator = UnitStore.extractUnits(
            endingNumeratorText,
            "Ending Numerator",
            false
        ).getOrElse { return it }
        val startDenominator = UnitStore.extractUnits(
            startingDenominatorText,
            "Starting Denominator",
            true
        ).getOrElse { return it }
        val endDenominator = UnitStore.extractUnits(
            endingDenominatorText,
            "Ending Denominator",
            true
        ).getOrElse { return it }
        validateUnitCounts(startNumerator, endNumerator, "numerator")?.let { return it }
        validateUnitCounts(startDenominator, endDenominator, "denominator")?.let { return it }
        val steps = mutableListOf<ConversionStep>()
        val runningAnswer = RunningAnswer(inputValue)
        for (path in findPathsBetween(startNumerator, endNumerator.toMutableList())) {
            steps.addPath(path, runningAnswer, false)
        }
        for (path in findPathsBetween(startDenominator, endDenominator.toMutableList())) {
            steps.addPath(path, runningAnswer, true)
        }

        displayOutput(
            inputValue,
            runningAnswer.value,
            steps,
            startNumerator,
            startDenominator,
            endNumerator,
            endDenominator
        )
        return null
    }

    private fun displayOutput(
        inputValue: Double,
        answer: Double,
        steps: MutableList<ConversionStep>,
        startNumerator: MutableList<Unit>,
        startDenominator: MutableList<Unit>,
        endNumerator: MutableList<Unit>,
        endDenominator: MutableList<Unit>
    ) {
        outputValue.text = TripleStringBuilder(outputValue.maxEms).let {
            it.appendValue(answer, 3, 1)
            it.appendUnits(endNumerator, endDenominator, 0)
            it.squishSquash(0)
        }

        conversionSteps.text = TripleStringBuilder(conversionSteps.maxEms).let {
            it.appendValue(inputValue, 2, 2)
            it.appendUnits(startNumerator, startDenominator, 2)
            for (step in steps) {
                it.appendMiddle(" × ", 2)
                it.appendConversionStep(step, 2)
            }
            it.appendMiddle(" = ", 2)
            it.appendValue(answer, 3, 2)
            it.appendUnits(endNumerator, endDenominator, 2)
            it.squishSquash(0)
        }

        Log.i(
            TAG, "outputValue.text: \n${outputValue.text}"
        )
        Log.i(
            TAG, "conversionSteps.text:\n${conversionSteps.text}"
        )
    }

    private fun MutableList<ConversionStep>.addPath(
        path: List<Unit>,
        runningAnswer: RunningAnswer,
        inverse: Boolean
    ) {
        val iterator = path.iterator().peeking()
        for (unit in iterator) {
            val next = try {
                iterator.peek()
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
        starts: MutableList<Unit>,
        ends: MutableList<Unit>
    ): List<List<Unit>> {
        val paths: MutableList<List<Unit>> = mutableListOf()
        for (start in starts) {
            val shortestPath = findFirstShortestPath(start, ends)
            if (shortestPath.isNotEmpty()) {
                paths.add(shortestPath)
            }
        }
        return paths
    }

    private fun findFirstShortestPath(start: Unit, ends: MutableList<Unit>): List<Unit> {
        val (parent, distance) = breadthFirstSearch(start)
        for (destination in ends) {
            if (distance.containsKey(destination)) {
                val path = mutableListOf<Unit>()
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

    private fun breadthFirstSearch(start: Unit): Pair<HashMap<Unit, Unit>, HashMap<Unit, Int>> {
        val parent: HashMap<Unit, Unit> = hashMapOf()
        val distance: HashMap<Unit, Int> = hashMapOf()
        distance[start] = 0
        val queue: ArrayDeque<Unit> = ArrayDeque()
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