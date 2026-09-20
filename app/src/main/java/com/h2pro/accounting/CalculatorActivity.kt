package com.h2pro.accounting

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class CalculatorActivity : AppCompatActivity() {
    private lateinit var display: TextView
    private var current = "0"
    private var stored = 0.0
    private var pending: String? = null
    private var fresh = true
    private val fmt = DecimalFormat("0.##########")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "H2pro - الآلة الحاسبة"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 28, 20, 20)
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }

        display = TextView(this).apply {
            text = "0"
            textSize = 38f
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(16, 18, 16, 18)
            minHeight = 100
        }
        root.addView(display, LinearLayout.LayoutParams(-1, 0, 1f))

        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 5
        }
        val keys = arrayOf(
            "AC", "⌫", "%", "÷",
            "7", "8", "9", "×",
            "4", "5", "6", "−",
            "1", "2", "3", "+",
            "0", ".", "=", "±"
        )
        keys.forEach { key ->
            val b = Button(this).apply {
                text = key
                textSize = 22f
                setOnClickListener { press(key) }
            }
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(5, 5, 5, 5)
            })
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 2.4f))
        setContentView(root)
    }

    private fun refresh() { display.text = current }

    private fun press(k: String) {
        when {
            k == "AC" -> { current = "0"; stored = 0.0; pending = null; fresh = true }
            k == "⌫" -> if (!fresh) { current = if (current.length > 1) current.dropLast(1) else "0" }
            k == "±" -> if (current != "0") current = if (current.startsWith("-")) current.drop(1) else "-$current"
            k == "." -> if (fresh) { current = "0."; fresh = false } else if (!current.contains(".")) current += "."
            k == "%" -> current = fmt.format(current.toDoubleOrNull()?.div(100.0) ?: 0.0)
            k in setOf("+","−","×","÷") -> {
                stored = current.toDoubleOrNull() ?: 0.0
                pending = k
                fresh = true
            }
            k == "=" -> {
                val right = current.toDoubleOrNull() ?: 0.0
                val result = when (pending) {
                    "+" -> stored + right
                    "−" -> stored - right
                    "×" -> stored * right
                    "÷" -> if (right == 0.0) Double.NaN else stored / right
                    else -> right
                }
                current = if (result.isNaN()) "خطأ" else fmt.format(result)
                pending = null
                fresh = true
            }
            k[0].isDigit() -> {
                if (fresh || current == "خطأ") { current = k; fresh = false }
                else if (current.length < 18) current += k
            }
        }
        refresh()
    }
}
