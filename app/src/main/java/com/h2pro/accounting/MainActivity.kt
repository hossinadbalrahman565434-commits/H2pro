package com.h2pro.accounting

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val dp: (Int) -> Int = { value ->
        (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20, 42, 58)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(28), dp(20), dp(20))
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(247, 249, 251))
        }

        root.addView(text("H2pro المحاسبي", 30, true, Color.rgb(20, 42, 58)))
        root.addView(text("نظام محاسبي عربي متكامل", 17, false, Color.DKGRAY), margin(0, 4, 0, 22))

        addCard(root, "رصيد الصندوق", "0.00", "ريال")
        addCard(root, "إجمالي المبيعات", "0.00", "ريال")
        addCard(root, "إجمالي المصروفات", "0.00", "ريال")
        addCard(root, "صافي الحركة", "0.00", "ريال")

        root.addView(text("الوحدات الأساسية", 21, true, Color.rgb(20, 42, 58)), margin(0, 20, 0, 10))
        root.addView(text("الحسابات  •  القيود اليومية  •  العملاء  •  الموردون  •  التقارير", 16, false, Color.DKGRAY))

        setContentView(root)
    }

    private fun addCard(root: LinearLayout, title: String, value: String, unit: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(Color.WHITE)
        }
        card.addView(text(title, 16, false, Color.DKGRAY))
        card.addView(text("$value $unit", 25, true, Color.rgb(20, 42, 58)), margin(0, 6, 0, 0))
        root.addView(card, margin(0, 0, 0, 10))
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.RIGHT
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun margin(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(l), dp(t), dp(r), dp(b))
        }
}
