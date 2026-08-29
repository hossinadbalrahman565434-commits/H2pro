package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class GeneralLedgerActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg = Color.rgb(12, 28, 40)
    private val surface = Color.rgb(24, 45, 60)
    private val gold = Color.rgb(198, 161, 91)
    private val goldLight = Color.rgb(231, 201, 130)
    private val textColor = Color.rgb(245, 241, 232)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(s: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(textColor)
        gravity = Gravity.RIGHT
        if (bold) setTypeface(typeface, 1)
        setPadding(dp(4), dp(5), dp(4), dp(5))
    }

    private fun btn(s: String, action: () -> Unit) = Button(this).apply {
        text = s
        isAllCaps = false
        textSize = 15f
        setTextColor(goldLight)
        minHeight = dp(52)
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(surface)
            setStroke(dp(1), gold)
        }
        setOnClickListener { action() }
    }

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint
        setHintTextColor(Color.LTGRAY)
        setTextColor(textColor)
        textSize = 15f
        gravity = Gravity.RIGHT
        setSingleLine(true)
        setPadding(dp(10), 0, dp(10), 0)
    }

    private fun root(title: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        setBackgroundColor(bg)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        addView(tv(title, 23f, true), LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(10)
        })
    }

    private fun showRoot(r: View) {
        val scroll = ScrollView(this)
        scroll.addView(r, ViewGroup.LayoutParams(-1, -1))
        setContentView(scroll)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        home()
    }

    private fun home() {
        val r = root("إدارة الأستاذ العام")
        r.addView(tv("تهيئة الحسابات والعمليات والتقارير", 16f, true))
        r.addView(btn("تهيئة الأستاذ العام — شجرة الحسابات") { accounts() })
        r.addView(btn("مدخلات الأستاذ العام — البنوك والصناديق") { cashAndBanks() })
        r.addView(btn("عمليات الأستاذ العام — قيد يومي") { journal() })
        r.addView(btn("سند قبض / سند صرف") { voucher() })
        r.addView(btn("التقارير والكشوفات") { reports() })
        r.addView(btn("رجوع") { finish() })
        showRoot(r)
    }

    private fun accounts() {
        val r = root("شجرة الحسابات")
        r.addView(tv("الحسابات الرئيسية والفرعية", 16f, true))
        val code = field("رمز الحساب")
        val name = field("اسم الحساب")
        val type = field("التصنيف: أصول / خصوم / حقوق ملكية / إيرادات / مصروفات")
        val parent = field("رمز الحساب الرئيسي — اختياري")
        val currency = field("العملة — مثال: ريال يمني")
        listOf(code, name, type, parent, currency).forEach {
            r.addView(it, LinearLayout.LayoutParams(-1, dp(52)).apply {
                setMargins(0, dp(3), 0, dp(3))
            })
        }
        r.addView(btn("إضافة الحساب") {
            val c = code.text.toString().trim()
            val n = name.text.toString().trim()
            if (c.isEmpty() || n.isEmpty()) {
                toast("أدخل رمز واسم الحساب")
                return@btn
            }
            val pid = db.accounts().firstOrNull { it.code == parent.text.toString().trim() }?.id ?: 0L
            val ok = db.addAccount(
                c,
                n,
                type.text.toString().trim().ifEmpty { "أصول" },
                pid,
                currency.text.toString().trim().ifEmpty { "محلي" }
            )
            toast(if (ok) "تم حفظ الحساب" else "تعذر حفظ الحساب")
            if (ok) accounts()
        })
        r.addView(tv("الحسابات الحالية", 16f, true))
        db.accounts().forEach { a ->
            val indent = "  ".repeat((a.level - 1).coerceAtMost(4))
            r.addView(tv("$indent${a.code} — ${a.name} | ${a.type} | ${a.currency}", 15f, a.level == 1))
        }
        r.addView(btn("رجوع") { home() })
        showRoot(r)
    }

    private fun cashAndBanks() {
        val r = root("البنوك والصناديق")
        val bankName = field("اسم البنك")
        val bankNo = field("رقم الحساب")
        val bankCur = field("العملة")
        r.addView(tv("إضافة بنك", 17f, true))
        listOf(bankName, bankNo, bankCur).forEach {
            r.addView(it, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        r.addView(btn("حفظ البنك") {
            if (bankName.text.isBlank()) {
                toast("أدخل اسم البنك")
                return@btn
            }
            db.addBank(bankName.text.toString(), bankNo.text.toString(), bankCur.text.toString().ifBlank { "محلي" })
            toast("تم حفظ البنك")
            cashAndBanks()
        })
        val boxName = field("اسم الصندوق")
        val boxNo = field("رقم الصندوق")
        val boxCur = field("العملة")
        r.addView(tv("إضافة صندوق", 17f, true))
        listOf(boxName, boxNo, boxCur).forEach {
            r.addView(it, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        r.addView(btn("حفظ الصندوق") {
            if (boxName.text.isBlank()) {
                toast("أدخل اسم الصندوق")
                return@btn
            }
            db.addCashbox(boxName.text.toString(), boxNo.text.toString(), boxCur.text.toString().ifBlank { "محلي" })
            toast("تم حفظ الصندوق")
            cashAndBanks()
        })
        r.addView(btn("رجوع") { home() })
        showRoot(r)
    }

    private fun journal() {
        val r = root("قيد يومي جديد")
        r.addView(tv("يجب أن يتساوى إجمالي المدين مع إجمالي الدائن", 14f))
        val date = field("التاريخ — مثال 2026-08-26")
        val desc = field("البيان")
        val ref = field("المرجع")
        val debitCode = field("رمز الحساب المدين")
        val debit = field("المبلغ المدين")
        val creditCode = field("رمز الحساب الدائن")
        val credit = field("المبلغ الدائن")
        listOf(date, desc, ref, debitCode, debit, creditCode, credit).forEach {
            r.addView(it, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        r.addView(btn("حفظ القيد") {
            val da = db.accounts().firstOrNull { it.code == debitCode.text.toString().trim() }
            val ca = db.accounts().firstOrNull { it.code == creditCode.text.toString().trim() }
            val d = debit.text.toString().toDoubleOrNull() ?: 0.0
            val c = credit.text.toString().toDoubleOrNull() ?: 0.0
            if (da == null || ca == null) {
                toast("تحقق من رموز الحسابات")
                return@btn
            }
            if (d <= 0 || abs(d - c) > 0.005) {
                toast("القيد غير متوازن")
                return@btn
            }
            val ok = db.saveJournal(
                date.text.toString().ifBlank { "2026-08-26" },
                desc.text.toString(),
                listOf(
                    JournalLine(da.id, d, 0.0, da.currency, 1.0),
                    JournalLine(ca.id, 0.0, c, ca.currency, 1.0)
                ),
                ref.text.toString()
            )
            toast(if (ok) "تم حفظ القيد بنجاح" else "تعذر حفظ القيد")
            if (ok) home()
        })
        r.addView(tv("الحسابات المتاحة: ${db.accounts().joinToString("، ") { it.code + "=" + it.name }}", 13f))
        r.addView(btn("رجوع") { home() })
        showRoot(r)
    }

    private fun voucher() {
        val r = root("سند قبض / سند صرف")
        val type = Spinner(this).apply {
            adapter = ArrayAdapter(this@GeneralLedgerActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("قبض", "صرف"))
        }
        val date = field("التاريخ")
        val name = field("اسم الطرف")
        val accountCode = field("رمز الحساب المقابل — مثال 501 أو 401")
        val amount = field("المبلغ")
        val ref = field("المرجع")
        val notes = field("البيان")
        r.addView(type, LinearLayout.LayoutParams(-1, dp(50)))
        listOf(date, name, accountCode, amount, ref, notes).forEach {
            r.addView(it, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        r.addView(tv("القبض: مدين الصندوق / دائن الحساب المقابل. الصرف: مدين الحساب المقابل / دائن الصندوق.", 13f))
        r.addView(btn("حفظ السند وإنشاء القيد") {
            val a = amount.text.toString().toDoubleOrNull() ?: 0.0
            val cash = db.accounts().firstOrNull { it.code == "101" }
            val counterpart = db.accounts().firstOrNull { it.code == accountCode.text.toString().trim() }
            val voucherType = type.selectedItem.toString()
            if (a <= 0 || name.text.isBlank() || cash == null || counterpart == null || counterpart.id == cash.id) {
                toast("أدخل الطرف والمبلغ ورمز حساب مقابل صحيح")
                return@btn
            }
            val dateValue = date.text.toString().ifBlank { "2026-08-29" }
            val description = notes.text.toString().ifBlank { "$voucherType: ${name.text}" }
            val lines = if (voucherType == "قبض") {
                listOf(
                    JournalLine(cash.id, a, 0.0, cash.currency, 1.0),
                    JournalLine(counterpart.id, 0.0, a, counterpart.currency, 1.0)
                )
            } else {
                listOf(
                    JournalLine(counterpart.id, a, 0.0, counterpart.currency, 1.0),
                    JournalLine(cash.id, 0.0, a, cash.currency, 1.0)
                )
            }
            if (!db.saveJournal(dateValue, description, lines, ref.text.toString())) {
                toast("تعذر إنشاء القيد — لم يتم حفظ السند")
                return@btn
            }
            val documentId = db.addDocument(voucherType, name.text.toString(), a, dateValue, ref.text.toString(), description)
            toast(if (documentId > 0) "تم حفظ السند والقيد المحاسبي" else "تم إنشاء القيد وتعذر حفظ سجل السند")
            if (documentId > 0) voucher()
        })
        r.addView(btn("رجوع") { home() })
        showRoot(r)
    }

    private fun reports() {
        val r = root("تقارير وكشوفات الأستاذ العام")
        r.addView(tv("ملخص النظام", 18f, true))
        r.addView(tv("عدد الحسابات: ${db.accounts().size}"))
        r.addView(tv("عدد القيود اليومية: ${db.journalCount()}"))
        r.addView(tv("إجمالي سندات القبض: ${db.sumDocuments("قبض")}"))
        r.addView(tv("إجمالي سندات الصرف: ${db.sumDocuments("صرف")}"))
        r.addView(tv("إجمالي فواتير البيع: ${db.sumDocuments("بيع")}"))
        r.addView(tv("إجمالي فواتير الشراء: ${db.sumDocuments("شراء")}"))
        r.addView(tv("قيمة المخزون: ${db.inventoryValue()}"))
        r.addView(btn("تحديث التقرير") { reports() })
        r.addView(btn("رجوع") { home() })
        showRoot(r)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
