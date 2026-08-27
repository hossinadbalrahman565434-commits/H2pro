package com.h2pro.accounting

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class FinancialReportsActivity : androidx.appcompat.app.AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg = Color.rgb(12, 28, 40)
    private val fg = Color.rgb(245, 241, 232)
    private val gold = Color.rgb(198, 161, 91)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun money(v: Double) = String.format(Locale.US, "%,.2f", v)

    private fun tv(s: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(fg)
        gravity = Gravity.RIGHT
        if (bold) setTypeface(typeface, 1)
        setPadding(dp(4), dp(6), dp(4), dp(6))
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(fg)
        setOnClickListener { action() }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        home()
    }

    private fun home() {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        r.addView(tv("التقارير المالية والمساعدة", 23f, true))
        r.addView(tv("ملخص الفترة الحالية", 18f, true))
        db.financialSummary().forEach { r.addView(tv(it, 15f)) }

        val totals = db.trialBalanceTotals()
        r.addView(tv("ميزان المراجعة", 19f, true))
        r.addView(tv("إجمالي المدين: ${money(totals.debit)}", 15f))
        r.addView(tv("إجمالي الدائن: ${money(totals.credit)}", 15f))
        r.addView(tv(if (totals.balanced) "✓ الميزان متوازن" else "⚠ الميزان غير متوازن", 16f, true))
        db.trialBalance().ifEmpty { listOf("لا توجد أرصدة حتى الآن") }.forEach { r.addView(tv(it, 14f)) }

        r.addView(tv("مؤشرات المخزون", 19f, true))
        r.addView(tv("قيمة المخزون: ${money(db.inventoryValue())}"))
        r.addView(tv("الأصناف منخفضة المخزون: ${db.lowStock()}"))

        r.addView(tv("كشف العملاء والموردين", 19f, true))
        val contacts = db.contacts("عميل") + db.contacts("مورد")
        r.addView(tv(if (contacts.isEmpty()) "لا توجد أطراف مسجلة" else "عدد الأطراف: ${contacts.size}"))

        r.addView(button("تحديث التقرير") { home() })
        r.addView(button("رجوع") { finish() })

        setContentView(ScrollView(this).apply {
            addView(r, ViewGroup.LayoutParams(-1, -1))
        })
    }
}
