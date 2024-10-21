package com.reeves.unitconverter

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.MultiAutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private fun MultiAutoCompleteTextView.setup(descriptions: List<DescribedUnit>): MultiAutoCompleteTextView {
        setAdapter(DescribedUnitAdapter(this@MainActivity, descriptions))
        setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        return this@setup
    }

    private fun EditText.fullClear() {
        text.clear()
        clearFocus()
    }

    private fun switchText(text1: EditText, text2: EditText) {
        val temp = text1.text
        text1.text = text2.text
        text2.text = temp
        text1.clearFocus()
        text2.clearFocus()
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

        val descriptions = UnitStore.getDescriptions()

        val outputMathView = findViewById<MathView>(R.id.output_value)
        val stepsMathView = findViewById<MathView>(R.id.conversion_steps)
        val copyOutputButton = findViewById<ImageButton>(R.id.copy_output_button)

        val inputValueField = findViewById<EditText>(R.id.input_value)
        val startingNumerator =
            findViewById<MultiAutoCompleteTextView>(R.id.starting_numerator).setup(descriptions)
        val startingDenominator =
            findViewById<MultiAutoCompleteTextView>(R.id.starting_denominator).setup(descriptions)
        val endingNumerator =
            findViewById<MultiAutoCompleteTextView>(R.id.ending_numerator).setup(descriptions)
        val endingDenominator =
            findViewById<MultiAutoCompleteTextView>(R.id.ending_denominator).setup(descriptions)

        val allFields = listOf(
            inputValueField,
            startingNumerator,
            startingDenominator,
            endingNumerator,
            endingDenominator
        )

        val clearOutput = {
            outputMathView.setDisplayText("")
            stepsMathView.setDisplayText("")
            copyOutputButton.visibility = View.INVISIBLE
        }

        val converter = Converter(outputMathView, stepsMathView)
        var snackBar: Snackbar? = null
        val convert = {
            clearOutput()
            allFields.forEach { it.clearFocus() }
            @Suppress("DEPRECATION") ViewCompat.getWindowInsetsController(window.decorView)
                ?.hide(WindowInsetsCompat.Type.ime())
            try {
                if (snackBar != null) {
                    snackBar!!.dismiss()
                }
                converter.convert(
                    inputValueField.text.toString(),
                    startingNumerator.text.toString(),
                    startingDenominator.text.toString(),
                    endingNumerator.text.toString(),
                    endingDenominator.text.toString(),
                )
                copyOutputButton.visibility = View.VISIBLE
            } catch (e: Exception) {
                when (e) {
                    is InvalidUnitsException, is UndefinedUnitException, is ImpossibleConversionException, is MeaninglessConversionException, is RequiresFlippingException -> {
                        snackBar = Snackbar.make(
                            window.decorView, "ERROR: ${e.message}", Snackbar.LENGTH_INDEFINITE
                        )
                        val bar = snackBar!!
                        if (e is RequiresFlippingException) {
                            bar.setAction("Flip End Units") {
                                switchText(endingNumerator, endingDenominator)
                                bar.dismiss()
                            }
                        } else {
                            bar.setAction("Dismiss") {
                                bar.dismiss()
                            }
                        }
                        bar.show()
                        Log.e(TAG, "attemptConversion: $e")
                    }

                    else -> throw e
                }
            }
        }

        allFields.forEach { field ->
            field.onDrawableEndClick { it.fullClear() }
            field.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus && view is MultiAutoCompleteTextView) {
                    view.showDropDown()
                }
            }
            field.setOnClickListener { view ->
                if (view is MultiAutoCompleteTextView) {
                    view.showDropDown()
                }
            }
        }

        findViewById<Button>(R.id.clear_all).setOnClickListener {
            allFields.forEach { it.fullClear() }
            clearOutput()
        }

        findViewById<ImageButton>(R.id.help_button).setOnClickListener {
            allFields.forEach { it.clearFocus() }
        }

        findViewById<ImageButton>(R.id.switch_start_end_button).setOnClickListener {
            switchText(startingNumerator, endingNumerator)
            switchText(startingDenominator, endingDenominator)
            convert()
        }

        findViewById<Button>(R.id.flip_start_button).setOnClickListener {
            switchText(startingNumerator, startingDenominator)
            convert()
        }

        findViewById<Button>(R.id.flip_end_button).setOnClickListener {
            switchText(endingNumerator, endingDenominator)
            convert()
        }

        findViewById<Button>(R.id.convert_button).setOnClickListener {
            convert()
        }

        copyOutputButton.setOnClickListener {
            copyToClipboard(converter.getFinalValue()?.formatToString())
        }
    }
}