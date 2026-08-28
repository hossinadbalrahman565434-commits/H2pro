package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CashTransactionActivity : AppCompatActivity() {
    private lateinit var db: AccountingDb
    private val surface = Color.rgb(24,45,60)
    private val gold = Color.rgb(198,161,91)
    private val goldLight = Color.rgb(231,201,130)
    private val bg = Color.rgb(12,28,40)
    private val text = Color.rgb(245,241,232)

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun field(h:String)=EditText(this).apply {
        hint=h; textSize=17f; gravity=Gravity.RIGHT; setTextColor(text); setHintTextColor(Color.LTGRAY)
        setPadding(dp(12),dp(10),dp(12),dp(10)); minHeight=dp(54)
        background=GradientDrawable().apply { cornerRadius=dp(8).toFloat(); setColor(surface); setStroke(dp(1),gold) }
    }
    private fun button(s:String, action:()->Unit)=Button(this).apply {
        text=s; isAllCaps=false; textSize=16f; setTextColor(goldLight); minHeight=dp(54)
        background=GradientDrawable().apply { cornerRadius=dp(8).toFloat(); setColor(surface); setStroke(dp(1),gold) }
        setOnClickListener{action()}
    }
    private fun label(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply {
        text=s; textSize=size; setTextColor(text); gravity=Gravity.RIGHT
        if(bold) setTypeface(typeface,1); setPadding(dp(4),dp(6),dp(4),dp(6))
    }
    private fun margin()=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))}
    private fun dateNow()=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date())
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()

    override fun onCreate(state:Bundle?){super.onCreate(state);db=AccountingDb(this);home()}

    private fun home(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14))}
        root.addView(label("سندات القبض والصرف النقدي",24f,true),margin())
        root.addView(label("إنشاء قيد محاسبي مزدوج تلقائيًا للصندوق",15f,true),margin())
        root.addView(button("سند قبض نقدي"){entry(true)},margin())
        root.addView(button("سند صرف نقدي"){entry(false)},margin())
        root.addView(button("رجوع"){finish()},margin())
        setContentView(ScrollView(this).apply{addView(root,ViewGroup.LayoutParams(-1,-1))})
    }

    private fun entry(receipt:Boolean){
        val accounts=db.accounts().filter{it.code!="101"}
        val cash=db.accounts().firstOrNull{it.code=="101"}
        if(cash==null){toast("حساب الصندوق 101 غير موجود");return}
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14))}
        root.addView(label(if(receipt)"سند قبض نقدي" else "سند صرف نقدي",24f,true),margin())
        val date=field("التاريخ");date.setText(dateNow())
        val amount=field("المبلغ")
        val reference=field("رقم السند / المرجع")
        val description=field("البيان")
        root.addView(date,margin());root.addView(amount,margin())
        root.addView(label(if(receipt)"الحساب المقابل: من أين استلمنا المبلغ" else "الحساب المقابل: لمن/لأي مصروف صُرف المبلغ",14f,true),margin())
        val names=accounts.map{"${it.code} | ${it.name}"}.toTypedArray()
        val spinner=Spinner(this).apply{adapter=ArrayAdapter(this@CashTransactionActivity,android.R.layout.simple_spinner_dropdown_item,names)}
        root.addView(spinner,margin())
        root.addView(reference,margin());root.addView(description,margin())
        root.addView(label(if(receipt)"القيد: مدين الصندوق 101 / دائن الحساب المقابل" else "القيد: مدين الحساب المقابل / دائن الصندوق 101",14f,true),margin())
        root.addView(button("حفظ السند والقيد"){
            val value=amount.text.toString().toDoubleOrNull()?:0.0
            if(value<=0){toast("أدخل مبلغًا صحيحًا");return@button}
            val selected=accounts.getOrNull(spinner.selectedItemPosition)
            if(selected==null){toast("اختر الحساب المقابل");return@button}
            val debit=if(receipt)value else 0.0
            val credit=if(receipt)0.0 else value
            val otherDebit=if(receipt)0.0 else value
            val otherCredit=if(receipt)value else 0.0
            val ok=db.saveJournal(date.text.toString().ifBlank{dateNow()},description.text.toString().ifBlank{if(receipt)"سند قبض نقدي" else "سند صرف نقدي"},listOf(JournalLine(cash.id,debit,credit),JournalLine(selected.id,otherDebit,otherCredit)),reference.text.toString())
            if(ok){toast("تم حفظ السند والقيد المتوازن بنجاح");home()}else toast("تعذر حفظ السند")
        },margin())
        root.addView(button("إلغاء"){home()},margin())
        setContentView(ScrollView(this).apply{addView(root,ViewGroup.LayoutParams(-1,-1))})
    }
}
