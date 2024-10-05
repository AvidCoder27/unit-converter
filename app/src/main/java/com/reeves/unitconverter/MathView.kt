@file:Suppress("SpellCheckingInspection")

package com.reeves.unitconverter

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView
import androidx.core.content.ContextCompat

/**
 * Copied from https://github.com/lingarajsankaravelu/Katex
 * Created by lingaraj on 3/15/17.
 * Translated to kotlin by avidcoder on 10/4/24
 */
private const val TAG = "KhanAcademyKatexView"
private const val defaultTextSize = 18f

class MathView : WebView {
    private var displayText: String = ""
    private var textColor = 0
    private var textSize = 0
    private var enableZoomInControls = false

    constructor(context: Context) : super(context) {
        configurationSettingWebView(enableZoomInControls)
        setDefaultTextColor(context)
        setDefaultTextSize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        configurationSettingWebView(enableZoomInControls)
        val mTypeArray = context.theme.obtainStyledAttributes(attrs, R.styleable.MathView, 0, 0)
        try {
            setBackgroundColor(
                mTypeArray.getInteger(
                    R.styleable.MathView_setViewBackgroundColor,
                    ContextCompat.getColor(context, android.R.color.transparent)
                )
            )
            setTextColor(
                mTypeArray.getColor(
                    R.styleable.MathView_setTextColor,
                    ContextCompat.getColor(context, android.R.color.black)
                )
            )
            pixelSizeConversion(
                mTypeArray.getDimension(
                    R.styleable.MathView_setTextSize, defaultTextSize
                )
            )
            setDisplayText(mTypeArray.getString(R.styleable.MathView_setText) ?: "")
        } catch (e: Exception) {
            Log.d(TAG, "Exception:$e")
        }
    }

    fun setViewBackgroundColor(color: Int) {
        setBackgroundColor(color)
        invalidate()
    }

    private fun pixelSizeConversion(dimension: Float) {
        if (dimension == defaultTextSize) {
            setTextSize(defaultTextSize.toInt())
        } else {
            setTextSize((dimension.toDouble() / 1.6).toInt())
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "NewApi")
    private fun configurationSettingWebView(enableZoomInControls1: Boolean) {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.displayZoomControls = enableZoomInControls1
        settings.builtInZoomControls = enableZoomInControls1
        settings.setSupportZoom(enableZoomInControls1)
        isVerticalScrollBarEnabled = enableZoomInControls1
        isHorizontalScrollBarEnabled = enableZoomInControls1
        Log.d(TAG, "Zoom in controls:$enableZoomInControls1")
    }

    fun setDisplayText(formulaText: String) {
        displayText = formulaText
        loadData()
    }

    private fun offlineKatexConfig(): String = """
<!DOCTYPE html><html><head><meta charset="UTF-8">
<title>MathView</title>
        <link rel="stylesheet" type="text/css" href="file:///android_asset/katex/katex.min.css">
        <link rel="stylesheet" type="text/css" href="file:///android_asset/themes/style.css" >
        <script type="text/javascript" src="file:///android_asset/katex/katex.min.js" ></script>
        <script type="text/javascript" src="file:///android_asset/katex/contrib/auto-render.min.js" ></script>
        <script type="text/javascript" src="file:///android_asset/katex/contrib/auto-render.js" ></script>
        <script type="text/javascript" src="file:///android_asset/jquery.min.js" ></script>
        <script type="text/javascript" src="file:///android_asset/latex_parser.js" ></script>
        <meta name="viewport" content="width=device-width"/>
<link rel="stylesheet" href="file:///android_asset/webviewstyle.css"/>
<style type='text/css'>body {
margin: 0px;
padding: 0px;
line-height: 2;
font-size:${textSize}px;
color:${
        getHexColor(
            textColor
        )
    }; } </style>
        </head>
    <body>
        $displayText
    </body>
</html>
"""

    private fun setTextSize(size: Int) {
        textSize = size
        loadData()
    }

    private fun setTextColor(color: Int) {
        textColor = color
        loadData()
    }

    private fun getHexColor(intColor: Int): String {
        //Android and javascript color format differ javascript support Hex color, so the android color which user sets is converted to hex color to replicate the same in javascript.
        val hexColor = String.format("#%06X", (0xFFFFFF and intColor))
        Log.d(TAG, "Hex Color:$hexColor")
        return hexColor
    }


    private fun setDefaultTextColor(context: Context) {
        //sets default text color to black
        textColor = ContextCompat.getColor(context, android.R.color.black)
    }

    private fun setDefaultTextSize() {
        //sets view default text size to 18
        textSize = defaultTextSize.toInt()
    }

    private fun loadData() {
        loadDataWithBaseURL(
            "null", offlineKatexConfig(), "text/html", "UTF-8", "about:blank"
        )
    }

    fun getText(): String {
        return displayText
    }
}
