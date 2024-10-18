package com.reeves.unitconverter

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.MultiAutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private fun <T> MultiAutoCompleteTextView.setup(adapter: ArrayAdapter<T>): MultiAutoCompleteTextView {
        setAdapter(adapter)
        setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        UnitStore.loadFromJson(this)

        val adapter = DescribedUnitAdapter(
            this, R.layout.list_item, UnitStore.getSuggestedNames()
        )

        val outputMathView = findViewById<MathView>(R.id.output_value)
        val stepsMathView = findViewById<MathView>(R.id.conversion_steps)

        val inputValueField = findViewById<EditText>(R.id.input_value)
        val startingNumerator =
            findViewById<MultiAutoCompleteTextView>(R.id.starting_numerator).setup(adapter)
        val startingDenominator =
            findViewById<MultiAutoCompleteTextView>(R.id.starting_denominator).setup(adapter)
        val endingNumerator =
            findViewById<MultiAutoCompleteTextView>(R.id.ending_numerator).setup(adapter)
        val endingDenominator =
            findViewById<MultiAutoCompleteTextView>(R.id.ending_denominator).setup(adapter)

        val allFields = listOf(
            inputValueField,
            startingNumerator,
            startingDenominator,
            endingNumerator,
            endingDenominator
        )
        allFields.forEach {
            it.clearTextOnDrawableEndClick()
        }

        val converter = Converter(outputMathView, stepsMathView)

        findViewById<Button>(R.id.clear_all).setOnClickListener {
            allFields.forEach { it.fullClear() }
            outputMathView.setDisplayText("")
            stepsMathView.setDisplayText("")
        }

        findViewById<Button>(R.id.convert_button).setOnClickListener {
            @Suppress("DEPRECATION") ViewCompat.getWindowInsetsController(window.decorView)
                ?.hide(WindowInsetsCompat.Type.ime())
            try {
                converter.convert(
                    inputValueField.text.toString(),
                    startingNumerator.text.toString(),
                    startingDenominator.text.toString(),
                    endingNumerator.text.toString(),
                    endingDenominator.text.toString(),
                )
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
}