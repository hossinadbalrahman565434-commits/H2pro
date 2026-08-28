package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SalesActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val light=Color.rgb(231,201,130); private val textColor=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun tv(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{ text=s;textSize=size;setTextColor(textColor);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1);setPadding(dp(4),dp(5),dp(4),dp(5)) }
    private fun field(h:String)=EditText(this).apply{ hint=h;setHintTextColor(Color.LTGRAY);setTextColor(textColor);gravity=Gravity.RIGHT;setSingleLine(true);setPadding(dp(10),0,dp(10),0) }
    private fun btn(s:String,a:()->Unit)=Button(this).apply{ text=s;isAllCaps=false;setTextColor(light);minHeight=dp(52);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{a()} }
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv(title,23f,true))}
    private fun show(r:LinearLayout){setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    override fun onCreate(b:Bundle?){super.onCreate(b);home()}
    private fun home(){val r=root("إدارة المبيعات");r.addView(tv("فواتير متعددة الأصناف وتحديث المخزون والقيود تلقائيًا",16f,true));r.addView(btn("فاتورة بيع جديدة"){invoice()});r.addView(btn("العملاء"){customers()});r.addView(btn("تقرير المبيعات"){report()});r.addView(btn("رجوع"){finish()});show(r)}

    private data class LineEditor(val code:EditText,val qty:EditText,val price:EditText,val box:LinearLayout)

    private fun invoice(){
        val r=root("فاتورة بيع جديدة")
        val date=field("التاريخ"); val customer=field("اسم العميل"); val ref=field("رقم الفاتورة / المرجع"); val notes=field("البيان")
        listOf(date,customer,ref,notes).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(3))})}
        r.addView(tv("نوع السداد",14f,true))
        val payment=Spinner(this).apply{adapter=ArrayAdapter(this@SalesActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("نقدي","آجل"))}
        r.addView(payment,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(6))})
        r.addView(tv("الأجل يسجل على حساب العملاء",12f))
        r.addView(tv("أضف أي عدد من الأصناف إلى الفاتورة:",15f,true))
        val linesBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        r.addView(linesBox,LinearLayout.LayoutParams(-1,-2))
        val editors=mutableListOf<LineEditor>()
        fun addLine(){
            val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(8));background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface)}}
            val code=field("رمز الصنف");val qty=field("الكمية");val price=field("سعر البيع")
            listOf(code,qty,price).forEach{box.addView(it,LinearLayout.LayoutParams(-1,dp(48)).apply{setMargins(0,dp(2),0,dp(2))})}
            val editor=LineEditor(code,qty,price,box); editors.add(editor)
            box.addView(btn("حذف هذا الصنف"){editors.remove(editor);linesBox.removeView(box)})
            linesBox.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(5),0,dp(5))})
        }
        addLine(); r.addView(btn("+ إضافة صنف آخر"){addLine()})
        r.addView(btn("حفظ الفاتورة متعددة الأصناف"){
            if(customer.text.isBlank()){toast("أدخل اسم العميل");return@btn}
            val result=editors.mapNotNull{e->
                val id=findH2proItemId(e.code.text.toString().trim());val q=e.qty.text.toString().toDoubleOrNull()?:0.0;val p=e.price.text.toString().toDoubleOrNull()?:0.0
                if(id>0 && q>0 && p>=0) InvoiceLine(id,q,p) else null
            }
            if(result.isEmpty() || result.size!=editors.size){toast("تحقق من رموز الأصناف والكميات والأسعار");return@btn}
            val mode=payment.selectedItem?.toString()?:"نقدي"
            val id=db.saveInvoice("بيع",date.text.toString().ifBlank{"2026-08-26"},customer.text.toString(),result,ref.text.toString(),notes.text.toString(),mode)
            toast(if(id>0)"تم حفظ الفاتورة وتحديث المخزون وCOGS والقيد" else "تعذر حفظ الفاتورة أو الرصيد غير كاف")
            if(id>0)home()
        })
        r.addView(btn("رجوع"){home()});show(r)
    }

    private fun customers(){val r=root("العملاء");val name=field("اسم العميل");val phone=field("الهاتف");val address=field("العنوان");listOf(name,phone,address).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(3))})};r.addView(btn("إضافة العميل"){if(name.text.isBlank()){toast("أدخل اسم العميل");return@btn};db.addContact("عميل",name.text.toString(),phone.text.toString(),address.text.toString());toast("تم حفظ العميل");customers()});r.addView(tv("العملاء الحاليون",17f,true));db.contacts("عميل").forEach{r.addView(tv(it,14f))};r.addView(btn("رجوع"){home()});show(r)}
    private fun report(){val r=root("تقرير المبيعات");r.addView(tv("إجمالي فواتير البيع: ${db.sumDocuments("بيع")}",18f,true));r.addView(tv("قيمة المخزون الحالية: ${db.inventoryValue()}"));r.addView(tv("الأصناف منخفضة المخزون: ${db.lowStock()}"));r.addView(btn("تحديث"){report()});r.addView(btn("رجوع"){home()});show(r)}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
