package com.h2pro.accounting

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ReturnsActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val light=Color.rgb(231,201,130); private val fg=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun tv(s:String,size:Float=15f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(fg);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1)}
    private fun field(h:String)=EditText(this).apply{hint=h;setHintTextColor(Color.LTGRAY);setTextColor(fg);gravity=Gravity.RIGHT;setSingleLine(true)}
    private fun btn(s:String,a:()->Unit)=Button(this).apply{text=s;isAllCaps=false;setTextColor(light);minHeight=dp(50);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{a()}}
    override fun onCreate(b:Bundle?){super.onCreate(b);home()}
    private fun home(){val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv("المرتجعات",23f,true));addView(btn("مرتجع مبيعات"){form("مرتجع بيع")});addView(btn("مرتجع مشتريات"){form("مرتجع شراء")});addView(btn("رجوع"){finish()})};setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun form(kind:String){val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv(kind,23f,true))};val party=field(if(kind=="مرتجع بيع")"اسم العميل" else "اسم المورد");val date=field("التاريخ");val code=field("رمز الصنف");val qty=field("الكمية");val price=field("سعر المرتجع");val ref=field("المرجع");listOf(date,party,code,qty,price,ref).forEach{r.addView(it,LinearLayout.LayoutParams(-1,dp(48)).apply{setMargins(0,dp(3),0,dp(3))})};r.addView(tv("الأصناف الحالية: ${db.items().joinToString("، ")}",12f));r.addView(btn("حفظ المرتجع"){val id=findH2proItemId(code.text.toString().trim());val q=qty.text.toString().toDoubleOrNull()?:0.0;val p=price.text.toString().toDoubleOrNull()?:0.0;if(id<=0||q<=0||p<0||party.text.isBlank()){toast("تحقق من البيانات");return@btn};val doc=db.saveReturn(kind,date.text.toString().ifBlank{"2026-08-26"},party.text.toString(),listOf(InvoiceLine(id,q,p)),ref.text.toString());toast(if(doc>0)"تم حفظ المرتجع وتحديث المخزون والقيد" else "تعذر حفظ المرتجع أو الرصيد غير كاف");if(doc>0)home()});r.addView(btn("رجوع"){home()});setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
