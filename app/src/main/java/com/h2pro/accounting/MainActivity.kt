package com.h2pro.accounting

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var db: AccountingDb
    private val navy = Color.rgb(20, 42, 58)
    private val bg = Color.rgb(247, 249, 251)
    private val dp: (Int) -> Int = { (it * resources.displayMetrics.density).toInt() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AccountingDb(this)
        showDashboard()
    }

    private fun base(title: String): LinearLayout {
        window.statusBarColor = navy
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(18), dp(16), dp(16))
            addView(text(title, 26, true, navy), lp(0, 0, 0, 14))
        }
    }

    private fun scroll(content: LinearLayout): ScrollView = ScrollView(this).apply {
        addView(content, ScrollView.LayoutParams(-1, -1))
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 15f
        setOnClickListener { action() }
        isAllCaps = false
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int): TextView = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); gravity = Gravity.RIGHT
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun lp(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }

    private fun input(hint: String, number: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint; textSize = 16f; gravity = Gravity.RIGHT
        if (number) inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun showDashboard() {
        val root = base("H2pro المحاسبي")
        root.addView(text("نظام محاسبي عربي متكامل", 16, false, Color.DKGRAY), lp(0, 0, 0, 14))
        val sales = db.sumDocuments("بيع")
        val purchases = db.sumDocuments("شراء")
        val expenses = db.sumDocuments("مصروف")
        addCard(root, "إجمالي المبيعات", money(sales))
        addCard(root, "إجمالي المشتريات", money(purchases))
        addCard(root, "المصروفات", money(expenses))
        addCard(root, "صافي الحركة", money(sales - purchases - expenses))
        root.addView(text("الوحدات المحاسبية", 20, true, navy), lp(0, 18, 0, 8))
        val menu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        menu.addView(button("📒 دليل الحسابات", ::showAccounts), lp(0, 0, 0, 5))
        menu.addView(button("🧾 قيد يومية جديد", ::showJournal), lp(0, 0, 0, 5))
        menu.addView(button("👥 العملاء", { showContacts("عميل") }), lp(0, 0, 0, 5))
        menu.addView(button("🚚 الموردون", { showContacts("مورد") }), lp(0, 0, 0, 5))
        menu.addView(button("📦 المخزون", ::showItems), lp(0, 0, 0, 5))
        menu.addView(button("💰 فاتورة بيع", { documentDialog("بيع") }), lp(0, 0, 0, 5))
        menu.addView(button("🛒 فاتورة شراء", { documentDialog("شراء") }), lp(0, 0, 0, 5))
        menu.addView(button("💸 مصروف جديد", { documentDialog("مصروف") }), lp(0, 0, 0, 5))
        menu.addView(button("📊 ميزان المراجعة", ::showTrialBalance), lp(0, 0, 0, 5))
        menu.addView(button("📈 قائمة الدخل", ::showIncome), lp(0, 0, 0, 5))
        root.addView(menu)
        setContentView(scroll(root))
    }

    private fun addCard(root: LinearLayout, title: String, value: String) {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(10)); setBackgroundColor(Color.WHITE) }
        card.addView(text(title, 15, false, Color.DKGRAY))
        card.addView(text("$value ريال", 23, true, navy), lp(0, 4, 0, 0))
        root.addView(card, lp(0, 0, 0, 8))
    }

    private fun showAccounts() {
        val root = base("دليل الحسابات")
        root.addView(button("＋ إضافة حساب", ::addAccountDialog), lp(0, 0, 0, 10))
        db.accounts().forEach { a -> root.addView(text("${a.code}  |  ${a.name}  |  ${a.type}", 16, false, Color.DKGRAY), lp(0, 5, 0, 5)) }
        root.addView(button("رجوع للرئيسية", ::showDashboard), lp(0, 16, 0, 0))
        setContentView(scroll(root))
    }

    private fun addAccountDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0); layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL }
        val code = input("رقم الحساب"); val name = input("اسم الحساب")
        val types = arrayOf("أصول", "خصوم", "حقوق ملكية", "إيرادات", "مصروفات")
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, types) }
        box.addView(code); box.addView(name); box.addView(spinner)
        AlertDialog.Builder(this).setTitle("إضافة حساب").setView(box).setPositiveButton("حفظ") { _, _ ->
            if (db.addAccount(code.text.toString(), name.text.toString(), spinner.selectedItem.toString())) showAccounts() else toast("تعذر إضافة الحساب")
        }.setNegativeButton("إلغاء", null).show()
    }

    private fun showJournal() {
        val root = base("قيد يومية جديد")
        val desc = input("وصف القيد")
        val accounts = db.accounts()
        val names = accounts.map { "${it.code} - ${it.name}" }.toTypedArray()
        val sp1 = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names) }
        val debit = input("مدين", true); val credit = input("دائن", true)
        root.addView(text("التاريخ: ${today()}", 15, false, Color.DKGRAY), lp(0, 0, 0, 8))
        root.addView(desc); root.addView(text("الحساب", 14, true, navy)); root.addView(sp1); root.addView(debit); root.addView(credit)
        root.addView(button("حفظ القيد", {
            val d = debit.text.toString().toDoubleOrNull() ?: 0.0; val c = credit.text.toString().toDoubleOrNull() ?: 0.0
            if (d <= 0 && c <= 0) { toast("أدخل مبلغًا"); return@button }
            val other = accounts.getOrNull(if (sp1.selectedItemPosition == 0) 1 else 0)
            val selected = accounts[sp1.selectedItemPosition]
            val lines = if (d > 0) listOf(JournalLine(selected.id, d, 0.0), JournalLine(other!!.id, 0.0, d)) else listOf(JournalLine(other!!.id, c, 0.0), JournalLine(selected.id, 0.0, c))
            if (db.saveJournal(today(), desc.text.toString(), lines)) { toast("تم حفظ القيد"); showDashboard() } else toast("فشل حفظ القيد")
        }), lp(0, 12, 0, 5))
        root.addView(button("رجوع", ::showDashboard))
        setContentView(scroll(root))
    }

    private fun showContacts(kind: String) {
        val root = base(if (kind == "عميل") "العملاء" else "الموردون")
        root.addView(button("＋ إضافة", { contactDialog(kind) }), lp(0, 0, 0, 10))
        db.contacts(kind).forEach { root.addView(text(it, 17, false, Color.DKGRAY), lp(0, 5, 0, 5)) }
        root.addView(button("رجوع", ::showDashboard), lp(0, 15, 0, 0))
        setContentView(scroll(root))
    }

    private fun contactDialog(kind: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0); layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL }
        val name = input("الاسم"); val phone = input("الهاتف"); box.addView(name); box.addView(phone)
        AlertDialog.Builder(this).setTitle("إضافة ${if (kind == "عميل") "عميل" else "مورد"}").setView(box).setPositiveButton("حفظ") { _, _ ->
            if (name.text.isNotBlank()) { db.addContact(kind, name.text.toString(), phone.text.toString()); showContacts(kind) }
        }.setNegativeButton("إلغاء", null).show()
    }

    private fun showItems() {
        val root = base("المخزون")
        root.addView(button("＋ إضافة صنف", ::itemDialog), lp(0, 0, 0, 10))
        db.items().forEach { root.addView(text(it, 16, false, Color.DKGRAY), lp(0, 5, 0, 5)) }
        root.addView(button("رجوع", ::showDashboard), lp(0, 15, 0, 0))
        setContentView(scroll(root))
    }

    private fun itemDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0); layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL }
        val code = input("رمز الصنف"); val name = input("اسم الصنف"); val buy = input("سعر الشراء", true); val sale = input("سعر البيع", true); val qty = input("الكمية", true); val min = input("الحد الأدنى", true)
        listOf(code, name, buy, sale, qty, min).forEach { box.addView(it) }
        AlertDialog.Builder(this).setTitle("إضافة صنف").setView(box).setPositiveButton("حفظ") { _, _ ->
            val ok = db.addItem(code.text.toString(), name.text.toString(), buy.text.toString().toDoubleOrNull() ?: 0.0, sale.text.toString().toDoubleOrNull() ?: 0.0, qty.text.toString().toDoubleOrNull() ?: 0.0, min.text.toString().toDoubleOrNull() ?: 0.0)
            if (ok) showItems() else toast("تعذر إضافة الصنف")
        }.setNegativeButton("إلغاء", null).show()
    }

    private fun documentDialog(kind: String) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0); layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL }
        val name = input(if (kind == "مصروف") "بيان المصروف" else "اسم العميل/المورد"); val amount = input("المبلغ", true); box.addView(name); box.addView(amount)
        AlertDialog.Builder(this).setTitle("${kind} جديدة").setView(box).setPositiveButton("حفظ") { _, _ ->
            val v = amount.text.toString().toDoubleOrNull() ?: 0.0
            if (v > 0) { db.addDocument(kind, name.text.toString(), v, today()); toast("تم الحفظ"); showDashboard() } else toast("أدخل مبلغًا صحيحًا")
        }.setNegativeButton("إلغاء", null).show()
    }

    private fun showTrialBalance() {
        val root = base("ميزان المراجعة")
        val rows = db.trialBalance()
        root.addView(text(if (rows.isEmpty()) "لا توجد حركة محاسبية بعد" else rows.joinToString("\n\n"), 17, false, Color.DKGRAY))
        root.addView(button("رجوع", ::showDashboard), lp(0, 20, 0, 0)); setContentView(scroll(root))
    }

    private fun showIncome() {
        val sales = db.sumDocuments("بيع"); val purchases = db.sumDocuments("شراء"); val expenses = db.sumDocuments("مصروف")
        val root = base("قائمة الدخل")
        root.addView(text("المبيعات: ${money(sales)} ريال\nالمشتريات: ${money(purchases)} ريال\nالمصروفات: ${money(expenses)} ريال\n\nصافي الربح التقريبي: ${money(sales - purchases - expenses)} ريال", 19, true, navy))
        root.addView(button("رجوع", ::showDashboard), lp(0, 20, 0, 0)); setContentView(scroll(root))
    }

    private fun money(v: Double) = String.format(Locale.US, "%,.2f", v)
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
