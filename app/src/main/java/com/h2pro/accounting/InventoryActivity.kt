package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class InventoryActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val surface=Color.rgb(24,45,60)
    private val gold=Color.rgb(198,161,91); private val goldLight=Color.rgb(231,201,130); private val textColor=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun tv(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(textColor);gravity=Gravity.RIGHT;setPadding(dp(4),dp(5),dp(4),dp(5));if(bold)setTypeface(typeface,1)}
    private fun field(h:String)=EditText(this).apply{hint=h;setHintTextColor(Color.LTGRAY);setTextColor(textColor);textSize=15f;gravity=Gravity.RIGHT;setSingleLine(true)}
    private fun btn(s:String,a:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=15f;setTextColor(goldLight);minHeight=dp(50);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{a()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv(title,23f,true))}
    private fun show(r:View){setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    override fun onCreate(b:Bundle?){super.onCreate(b);home()}
    private fun home(){val r=root("إدارة المخزون");r.addView(tv("الأصناف والأرصدة وحركة المخزون",16f,true));r.addView(btn("تهيئة الأصناف"){items()});r.addView(btn("حركة المخزون"){movements()});r.addView(btn("تقرير المخزون"){report()});r.addView(btn("الأصناف منخفضة المخزون"){low()});r.addView(btn("رجوع"){finish()});show(r)}
    private fun items(){val r=root("تهيئة الأصناف");val code=field("رمز الصنف");val name=field("اسم الصنف");val buy=field("سعر الشراء");val sale=field("سعر البيع");val qty=field("الرصيد الافتتاحي");val min=field("الحد الأدنى");val unit=field("الوحدة");listOf(code,name,buy,sale,qty,min,unit).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(50)))};r.addView(btn("إضافة الصنف"){val c=code.text.toString().trim();val n=name.text.toString().trim();if(c.isEmpty()||n.isEmpty()){toast("أدخل رمز واسم الصنف");return@btn};val ok=db.addItem(c,n,buy.text.toString().toDoubleOrNull()?:0.0,sale.text.toString().toDoubleOrNull()?:0.0,qty.text.toString().toDoubleOrNull()?:0.0,min.text.toString().toDoubleOrNull()?:0.0,unit.text.toString().ifBlank{"قطعة"});toast(if(ok)"تم حفظ الصنف" else "رمز الصنف موجود مسبقًا");if(ok)items()});r.addView(tv("الأصناف الحالية",17f,true));db.items().forEach{r.addView(tv(it,14f))};r.addView(btn("رجوع"){home()});show(r)}
    private fun movements(){val r=root("حركة المخزون");r.addView(tv("تسجل الحركة آليًا عند تنفيذ فواتير الشراء والبيع."));r.addView(tv("عدد الأصناف: ${db.items().size}"));r.addView(tv("قيمة المخزون: ${db.inventoryValue()}"));r.addView(tv("منخفضة المخزون: ${db.lowStock()}"));r.addView(btn("تحديث"){movements()});r.addView(btn("رجوع"){home()});show(r)}
    private fun report(){val r=root("تقرير المخزون");r.addView(tv("عدد الأصناف: ${db.items().size}",17f,true));r.addView(tv("قيمة المخزون: ${db.inventoryValue()}",17f,true));r.addView(tv("أصناف منخفضة: ${db.lowStock()}",17f,true));db.items().forEach{r.addView(tv(it,14f))};r.addView(btn("تحديث التقرير"){report()});r.addView(btn("رجوع"){home()});show(r)}
    private fun low(){val r=root("الأصناف منخفضة المخزون");r.addView(tv("إجمالي الأصناف المنخفضة: ${db.lowStock()}",17f,true));r.addView(btn("رجوع"){home()});show(r)}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
