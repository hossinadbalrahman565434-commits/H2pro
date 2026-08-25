package com.h2pro.accounting

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    private val navy = Color.rgb(15, 34, 48)
    private val surface = Color.rgb(24, 45, 60)
    private val gold = Color.rgb(198, 161, 91)
    private val goldLight = Color.rgb(231, 201, 130)
    private val bg = Color.rgb(12, 28, 40)
    private val text = Color.rgb(245, 241, 232)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String, size: Float = 16f, color: Int = text, bold: Boolean = false) = TextView(this).apply {
        this.text = s; textSize = size; setTextColor(color); gravity = Gravity.RIGHT
        if (bold) setTypeface(typeface, 1)
    }
    private fun card(title: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(12), dp(14), dp(12), dp(14))
        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(surface); setStroke(dp(1), gold) }
        addView(label(title, 14f, goldLight, true)); addView(label(value, 22f, text, true))
    }
    private fun button(title: String, action: () -> Unit) = Button(this).apply {
        text = title; isAllCaps = false; textSize = 16f; setTextColor(goldLight); minHeight = dp(54)
        background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(surface); setStroke(dp(1), gold) }
        setOnClickListener { action() }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = navy; window.navigationBarColor = navy
        val db = AccountingDb(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL; setBackgroundColor(bg); setPadding(dp(16), dp(18), dp(16), dp(18)) }
        root.addView(label("H2pro", 30f, goldLight, true))
        root.addView(label("لوحة التحكم المحاسبية", 19f, text, true), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(14)) })
        val stats = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun addStat(title: String, value: String) { stats.addView(card(title, value), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) }) }
        addStat("قيمة المخزون", db.inventoryValue().toString())
        addStat("الفواتير / المستندات", db.sumDocuments("بيع").toString())
        addStat("عدد القيود", db.journalCount().toString())
        addStat("أصناف منخفضة", db.lowStock().toString())
        root.addView(stats)
        root.addView(label("الوصول السريع", 18f, goldLight, true), LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(8), 0, dp(8)) })
        root.addView(button("فتح النظام المحاسبي") { startActivity(Intent(this, MainActivity::class.java)) }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(button("تحديث المؤشرات") { recreate() }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })
        root.addView(button("خروج") { finishAffinity() })
        setContentView(ScrollView(this).apply { addView(root, ViewGroup.LayoutParams(-1, -1)) })
    }
}
