package com.reeves.unitconverter

import android.annotation.SuppressLint
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

    private val units: MutableList<Unit> = mutableListOf()
    private val unitAliases: HashMap<String, Unit> = HashMap()

    init {
        val meter = createUnit("meter|s, m")
        val kilometer = createUnit("kilometer|s, km")
        val foot = createUnit("foot, feet, ft")
        val inch = createUnit("inch|es, in")
        val second = createUnit("second|s, s, sec")
        val minute = createUnit("minute|s, min|s")
        val hour = createUnit("hour|s, hr, h")
        createConversion(foot, inch, 1.0, 12.0)
        createConversion(foot, meter, 3.28084, 1.0)
        createConversion(meter, kilometer, 1000.0, 1.0)
        createConversion(second, minute, 60.0, 1.0)
        createConversion(minute, hour, 60.0, 1.0)
    }

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
        val startNumerator = extractUnits(
            startingNumeratorText,
            "Starting Numerator",
            false
        ).getOrElse { return it }
        val endNumerator = extractUnits(
            endingNumeratorText,
            "Ending Numerator",
            false
        ).getOrElse { return it }
        val startDenominator = extractUnits(
            startingDenominatorText,
            "Starting Denominator",
            true
        ).getOrElse { return it }
        val endDenominator = extractUnits(
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

    @SuppressLint("SetTextI18n")
    private fun displayOutput(
        inputValue: Double,
        answer: Double,
        steps: MutableList<ConversionStep>,
        startNumerator: MutableList<Unit>,
        startDenominator: MutableList<Unit>,
        endNumerator: MutableList<Unit>,
        endDenominator: MutableList<Unit>
    ) {
        outputValue.text = TripleStringBuilder(outputValue.maxEms / 2).let {
            it.appendValue(answer, 3)
            it.checkForSquish(1)
            it.appendUnits(endNumerator, endDenominator)
            it.squish(0)
        }

        conversionSteps.text = TripleStringBuilder(conversionSteps.maxEms / 2).let {
            it.appendValue(inputValue, 2)
            it.checkForSquish(2)
            it.appendUnits(startNumerator, startDenominator)
            it.checkForSquish(2)
            for (step in steps) {
                it.appendMiddle(" × ")
                it.appendConversionStep(step)
                it.checkForSquish(2)
            }
            it.appendMiddle(" = ")
            it.appendValue(answer, 3)
            it.checkForSquish(2)
            it.appendUnits(endNumerator, endDenominator)
            it.squish(0)
        }
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

    private fun extractUnits(
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
        units.add(unit)
        namesList.forEach { unitAliases[it] = unit }
        return unit
    }

    private fun createConversion(from: Unit, to: Unit, fromValue: Double, toValue: Double) {
        val conversion = Conversion(toValue, fromValue)
        from.addConversion(to, conversion)
        to.addConversion(from, conversion.inverse())
    }
}