package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MultiInvoiceActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val light=Color.rgb(231,201,130); private val fg=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun tv(s:String,size:Float=15f,bold:Boolean=false)=TextView(this).apply{ text=s;textSize=size;setTextColor(fg);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1);setPadding(dp(4),dp(6),dp(4),dp(6)) }
    private fun field(h:String)=EditText(this).apply{hint=h;setHintTextColor(Color.LTGRAY);setTextColor(fg);gravity=Gravity.RIGHT;setSingleLine(true)}
    private fun btn(s:String,a:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setTextColor(light);minHeight=dp(50);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{a()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(12),dp(12),dp(12),dp(12));addView(tv(title,22f,true))}
    override fun onCreate(b:Bundle?){super.onCreate(b);invoice()}
    private fun invoice(){
        val kind=intent.getStringExtra("kind") ?: "بيع"; val r=root(if(kind=="بيع")"فاتورة بيع متعددة الأصناف" else "فاتورة شراء متعددة الأصناف")
        val party=field(if(kind=="بيع")"اسم العميل" else "اسم المورد"); val date=field("التاريخ"); val ref=field("رقم الفاتورة / المرجع"); val notes=field("البيان")
        listOf(date,party,ref,notes).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(48)).apply{setMargins(0,dp(3),0,dp(3))})}
        val rows=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        fun addRow(){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL}
            val code=field("رمز الصنف");val qty=field("الكمية");val price=field(if(kind=="بيع")"سعر البيع" else "سعر الشراء")
            row.addView(code,LinearLayout.LayoutParams(0,dp(50),1f));row.addView(qty,LinearLayout.LayoutParams(0,dp(50),1f));row.addView(price,LinearLayout.LayoutParams(0,dp(50),1f));rows.addView(row)
        }
        addRow();r.addView(tv("الأصناف الحالية",15f,true));r.addView(tv(db.items().joinToString("\n"),12f));r.addView(rows)
        r.addView(btn("+ إضافة صنف"){addRow()})
        r.addView(btn("حفظ الفاتورة"){ val lines=mutableListOf<InvoiceLine>(); for(i in 0 until rows.childCount){val row=rows.getChildAt(i) as LinearLayout;val code=row.getChildAt(0) as EditText;val qty=row.getChildAt(1) as EditText;val price=row.getChildAt(2) as EditText;val id=findH2proItemId(code.text.toString().trim());val q=qty.text.toString().toDoubleOrNull()?:0.0;val p=price.text.toString().toDoubleOrNull()?:0.0;if(id>0&&q>0)lines.add(InvoiceLine(id,q,p))};if(party.text.isBlank()||lines.isEmpty()){toast("أدخل الطرف وصنفًا واحدًا على الأقل");return@btn};val id=db.saveInvoice(kind,date.text.toString().ifBlank{"2026-08-26"},party.text.toString(),lines,ref.text.toString(),notes.text.toString());toast(if(id>0)"تم حفظ الفاتورة وتحديث المخزون والقيد" else "تعذر حفظ الفاتورة أو الرصيد غير كاف");if(id>0)finish()})
        r.addView(btn("رجوع"){finish()});setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
