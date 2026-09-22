package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PurchaseActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40)
    private val surface=Color.rgb(24,45,60)
    private val gold=Color.rgb(198,161,91)
    private val light=Color.rgb(231,201,130)
    private val fg=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
    private fun tv(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(fg);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1);setPadding(dp(4),dp(5),dp(4),dp(5))}
    private fun field(h:String)=EditText(this).apply{hint=h;setHintTextColor(Color.LTGRAY);setTextColor(fg);gravity=Gravity.RIGHT;setSingleLine(true);setPadding(dp(10),0,dp(10),0)}
    private fun btn(s:String,a:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setTextColor(light);minHeight=dp(52);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{a()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv(title,23f,true))}
    private fun show(r:LinearLayout){setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private data class LineEditor(val code:EditText,val qty:EditText,val price:EditText,val box:LinearLayout)

    override fun onCreate(b:Bundle?){super.onCreate(b);home()}

    private fun home(){
        val r=root("إدارة المشتريات")
        r.addView(tv("فواتير شراء متعددة الأصناف وتحديث المخزون والقيود تلقائيًا",16f,true))
        r.addView(btn("فاتورة شراء جديدة"){invoice()})
        r.addView(btn("الموردون"){suppliers()})
        r.addView(btn("سجل فواتير الشراء"){history()})
        r.addView(btn("تقرير المشتريات"){report()})
        r.addView(btn("رجوع"){finish()})
        show(r)
    }

    private fun invoice(){
        val r=root("فاتورة شراء جديدة")
        val date=field("التاريخ yyyy-MM-dd").apply{setText(today())}
        val supplier=field("اسم المورد")
        val ref=field("رقم الفاتورة / المرجع")
        val notes=field("البيان")
        listOf(date,supplier,ref,notes).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(3))})}
        r.addView(tv("نوع السداد",14f,true))
        val payment=Spinner(this).apply{adapter=ArrayAdapter(this@PurchaseActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("نقدي","آجل"))}
        r.addView(payment,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(6))})
        r.addView(tv("الآجل يسجل على حساب الموردين",12f))
        val linesBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        r.addView(linesBox,LinearLayout.LayoutParams(-1,-2))
        val editors=mutableListOf<LineEditor>()
        fun addLine(){
            val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(8));background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface)}}
            val code=field("رمز الصنف");val qty=field("الكمية");val price=field("سعر الشراء")
            listOf(code,qty,price).forEach{box.addView(it,LinearLayout.LayoutParams(-1,dp(48)).apply{setMargins(0,dp(2),0,dp(2))})}
            val editor=LineEditor(code,qty,price,box);editors.add(editor)
            box.addView(btn("حذف هذا الصنف"){editors.remove(editor);linesBox.removeView(box)})
            linesBox.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(5),0,dp(5))})
        }
        addLine()
        r.addView(btn("+ إضافة صنف آخر"){addLine()})
        val total=tv("الإجمالي: 0.00",18f,true);r.addView(total)
        r.addView(btn("حساب الإجمالي"){
            val v=editors.sumOf{(it.qty.text.toString().toDoubleOrNull()?:0.0)*(it.price.text.toString().toDoubleOrNull()?:0.0)}
            total.text="الإجمالي: " + String.format(Locale.US,"%,.2f",v)
        })
        r.addView(btn("حفظ فاتورة الشراء"){
            val party=supplier.text.toString().trim()
            val d=date.text.toString().trim()
            if(party.isBlank()){toast("أدخل اسم المورد");return@btn}
            if(d.isBlank()){toast("أدخل التاريخ");return@btn}
            val lines=editors.mapNotNull{
                val id=findH2proItemId(it.code.text.toString().trim())
                val q=it.qty.text.toString().toDoubleOrNull()?:0.0
                val p=it.price.text.toString().toDoubleOrNull()?:0.0
                if(id>0&&q>0&&p>=0)InvoiceLine(id,q,p)else null
            }
            if(lines.isEmpty()||lines.size!=editors.size){toast("تحقق من رموز الأصناف والكميات والأسعار");return@btn}
            val mode=payment.selectedItem?.toString()?:"نقدي"
            val id=db.saveInvoice("شراء",d,party,lines,ref.text.toString().trim(),notes.text.toString().trim(),mode)
            toast(if(id>0)"تم حفظ الفاتورة وتحديث المخزون والقيد" else "تعذر حفظ الفاتورة؛ تحقق من التاريخ والسنة والبيانات")
            if(id>0)home()
        })
        r.addView(btn("رجوع"){home()})
        show(r)
    }

    private fun suppliers(){
        val r=root("الموردون")
        val name=field("اسم المورد");val phone=field("الهاتف");val address=field("العنوان")
        listOf(name,phone,address).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(3))})}
        r.addView(btn("إضافة المورد"){
            val n=name.text.toString().trim()
            if(n.isBlank()){toast("أدخل اسم المورد");return@btn}
            db.addContact("مورد",n,phone.text.toString().trim(),address.text.toString().trim())
            toast("تم حفظ المورد");suppliers()
        })
        r.addView(tv("الموردون الحاليون",17f,true))
        db.contacts("مورد").forEach{r.addView(tv(it,14f))}
        r.addView(btn("رجوع"){home()})
        show(r)
    }

    private fun history(){
        val r=root("سجل فواتير الشراء")
        val rows=db.purchaseDocuments()
        if(rows.isEmpty())r.addView(tv("لا توجد فواتير شراء مسجلة."))
        else rows.forEach{r.addView(tv(it,14f),LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(4),0,dp(4))})}
        r.addView(btn("تحديث"){history()});r.addView(btn("رجوع"){home()});show(r)
    }

    private fun report(){
        val gross=db.sumDocuments("شراء")
        val returns=db.sumDocuments("مرتجع شراء")
        val r=root("تقرير المشتريات")
        r.addView(tv("إجمالي فواتير الشراء: " + String.format(Locale.US,"%,.2f",gross),18f,true))
        r.addView(tv("مرتجعات المشتريات: " + String.format(Locale.US,"%,.2f",returns),17f,true))
        r.addView(tv("صافي المشتريات: " + String.format(Locale.US,"%,.2f",gross-returns),18f,true))
        r.addView(tv("قيمة المخزون الحالية: " + String.format(Locale.US,"%,.2f",db.inventoryValue()),16f,true))
        r.addView(btn("تحديث"){report()});r.addView(btn("رجوع"){home()});show(r)
    }
}
