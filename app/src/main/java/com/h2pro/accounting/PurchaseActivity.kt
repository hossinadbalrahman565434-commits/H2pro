package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PurchaseActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val light=Color.rgb(231,201,130); private val textColor=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun tv(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{this.text=s;textSize=size;setTextColor(textColor);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1);setPadding(dp(4),dp(5),dp(4),dp(5))}
    private fun field(h:String)=EditText(this).apply{hint=h;setHintTextColor(Color.LTGRAY);setTextColor(textColor);gravity=Gravity.RIGHT;setSingleLine(true);setPadding(dp(10),0,dp(10),0)}
    private fun btn(s:String,a:()->Unit)=Button(this).apply{this.text=s;isAllCaps=false;setTextColor(light);minHeight=dp(52);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{a()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv(title,23f,true))}
    private fun show(r:LinearLayout){setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    override fun onCreate(b:Bundle?){super.onCreate(b);home()}
    private fun home(){val r=root("إدارة المشتريات");r.addView(tv("فواتير الشراء وتحديث المخزون والقيود تلقائيًا",16f,true));r.addView(btn("فاتورة شراء جديدة"){invoice()});r.addView(btn("الموردون"){suppliers()});r.addView(btn("تقرير المشتريات"){report()});r.addView(btn("رجوع"){finish()});show(r)}
    private fun invoice(){val r=root("فاتورة شراء جديدة");val date=field("التاريخ");val supplier=field("اسم المورد");val code=field("رمز الصنف");val qty=field("الكمية");val price=field("سعر الشراء");val ref=field("رقم الفاتورة / المرجع");val notes=field("البيان");listOf(date,supplier,code,qty,price,ref,notes).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(3))})};r.addView(tv("نوع السداد",14f,true));val payment=Spinner(this).apply{adapter=ArrayAdapter(this@PurchaseActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("نقدي","آجل"))};r.addView(payment,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(6))});r.addView(tv("الآجل يسجل على حساب الموردين",12f));r.addView(tv("الأصناف المتاحة: ${db.items().joinToString("، ")}",13f));r.addView(btn("حفظ فاتورة الشراء"){val itemId=findH2proItemId(code.text.toString().trim());val q=qty.text.toString().toDoubleOrNull()?:0.0;val p=price.text.toString().toDoubleOrNull()?:0.0;if(itemId<=0||q<=0||p<0||supplier.text.isBlank()){toast("تحقق من المورد والصنف والكمية والسعر");return@btn};val mode=payment.selectedItem?.toString()?:"نقدي";val id=db.saveInvoice("شراء",date.text.toString().ifBlank{"2026-08-26"},supplier.text.toString(),listOf(InvoiceLine(itemId,q,p)),ref.text.toString(),notes.text.toString(),mode);toast(if(id>0)"تم حفظ الفاتورة وتحديث المخزون والقيد" else "تعذر حفظ الفاتورة");if(id>0)home()});r.addView(btn("رجوع"){home()});show(r)}
    private fun suppliers(){val r=root("الموردون");val name=field("اسم المورد");val phone=field("الهاتف");val address=field("العنوان");listOf(name,phone,address).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,dp(3),0,dp(3))})};r.addView(btn("إضافة المورد"){if(name.text.isBlank()){toast("أدخل اسم المورد");return@btn};db.addContact("مورد",name.text.toString(),phone.text.toString(),address.text.toString());toast("تم حفظ المورد");suppliers()});r.addView(tv("الموردون الحاليون",17f,true));db.contacts("مورد").forEach{r.addView(tv(it,14f))};r.addView(btn("رجوع"){home()});show(r)}
    private fun report(){val r=root("تقرير المشتريات");r.addView(tv("إجمالي فواتير الشراء: ${db.sumDocuments("شراء")}",18f,true));r.addView(tv("قيمة المخزون الحالية: ${db.inventoryValue()}"));r.addView(btn("تحديث"){report()});r.addView(btn("رجوع"){home()});show(r)}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
