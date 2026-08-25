package com.h2pro.accounting

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/** تهيئة النظام: السنة المالية، الشركة، المناطق، والعملات مرتبطة مباشرة بقاعدة SQLite. */
class SystemSetupActivity : AppCompatActivity() {
    private lateinit var db: AccountingDb
    private val surface = Color.rgb(24,45,60)
    private val gold = Color.rgb(198,161,91)
    private val goldLight = Color.rgb(231,201,130)
    private val bg = Color.rgb(12,28,40)
    private val text = Color.rgb(245,241,232)
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun field(hint:String, value:String="")=EditText(this).apply{this.hint=hint;setText(value);setTextColor(text);setHintTextColor(Color.LTGRAY);textSize=16f;setPadding(dp(12),dp(10),dp(12),dp(10))}
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=15f;setTextColor(goldLight);minHeight=dp(50);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{action()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(16),dp(16),dp(16),dp(16));addView(TextView(this@SystemSetupActivity).apply{text=title;textSize=24f;setTextColor(goldLight);gravity=Gravity.RIGHT;setTypeface(typeface,1)},LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,14)})}
    private fun showMessage(title:String, textValue:String){AlertDialog.Builder(this).setTitle(title).setMessage(textValue.ifBlank{"لا توجد بيانات"}).setPositiveButton("إغلاق",null).show()}
    override fun onCreate(state:Bundle?){super.onCreate(state);db=AccountingDb(this);home()}
    private fun home(){val r=root("تهيئة النظام");r.addView(button("بيانات السنة المالية"){years()},margin());r.addView(button("بيانات الشركة"){company()},margin());r.addView(button("بيانات المناطق"){regions()},margin());r.addView(button("بيانات العملات"){currencies()},margin());r.addView(button("رجوع"){finish()},margin(15));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun margin(top:Int=0)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,top,0,dp(7))}
    private fun actionRow(vararg b:Button)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;b.forEach{addView(it,LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(dp(2),0,dp(2),dp(7))})}}
    private fun section(title:String)=TextView(this).apply{text=title;textSize=19f;setTextColor(goldLight);gravity=Gravity.RIGHT;setTypeface(typeface,1);setPadding(0,dp(8),0,dp(8))}

    private fun years(){
        val r=root("بيانات السنة المالية");val y=field("السنة المالية");val m=field("الشهور (مثال: 1,2,3,...,12)","1,2,3,4,5,6,7,8,9,10,11,12");val s=field("الحالة","مفتوحة");r.addView(section("بيانات السنة"));r.addView(y,margin());r.addView(m,margin());r.addView(s,margin())
        r.addView(actionRow(button("إضافة"){if(y.text.isNotBlank()){Toast.makeText(this,"${if(db.saveYear(y.text.toString().toIntOrNull()?:0,m.text.toString(),s.text.toString()))"تمت الإضافة" else "تعذر الحفظ"}",Toast.LENGTH_SHORT).show();y.text.clear()}},button("تعديل"){val old=y.text.toString();val v=field("السنة الجديدة",old);AlertDialog.Builder(this).setTitle("تعديل السنة").setView(v).setPositiveButton("حفظ"){_,_->Toast.makeText(this,if(db.updateYear(old.toIntOrNull()?:0,v.text.toString().toIntOrNull()?:0,m.text.toString(),s.text.toString()))"تم التعديل" else "لم يتم العثور على السنة",Toast.LENGTH_SHORT).show()}.setNegativeButton("إلغاء",null).show()},button("حذف"){Toast.makeText(this,if(db.deleteYear(y.text.toString().toIntOrNull()?:0))"تم الحذف" else "لم يتم العثور على السنة",Toast.LENGTH_SHORT).show()},button("بحث"){showMessage("نتيجة البحث",db.searchYears(y.text.toString()))},button("تقرير"){showMessage("تقرير السنوات المالية",db.years().joinToString("\n"))}))
        r.addView(button("رجوع"){home()},margin(10));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})
    }

    private fun company(){val c=db.company();val r=root("بيانات الشركة");val n=field("اسم الشركة",c.name);val p=field("رقم الهاتف",c.phone);val a=field("العنوان",c.address);val l=field("مسار/اسم الشعار",c.logo);r.addView(n,margin());r.addView(p,margin());r.addView(a,margin());r.addView(l,margin());r.addView(actionRow(button("إضافة"){db.saveCompany(n.text.toString(),p.text.toString(),a.text.toString(),l.text.toString());Toast.makeText(this,"تم حفظ بيانات الشركة والترويسة",Toast.LENGTH_SHORT).show()},button("تعديل"){db.saveCompany(n.text.toString(),p.text.toString(),a.text.toString(),l.text.toString());Toast.makeText(this,"تم تعديل بيانات الشركة",Toast.LENGTH_SHORT).show()},button("حذف"){db.deleteCompany();n.text.clear();p.text.clear();a.text.clear();l.text.clear();Toast.makeText(this,"تم حذف بيانات الشركة",Toast.LENGTH_SHORT).show()}));r.addView(button("تقرير / معاينة الترويسة"){showMessage("ترويسة التقارير",listOf(n.text.toString(),p.text.toString(),a.text.toString(),"الشعار: ${l.text}").filter{it.isNotBlank()}.joinToString("\n"))},margin());r.addView(button("رجوع"){home()},margin(10));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}

    private fun regions(){val r=root("بيانات المناطق");val c=field("الدولة");val p=field("المحافظة");val city=field("المدينة");val d=field("المنطقة");listOf(c,p,city,d).forEach{r.addView(it,margin())};r.addView(actionRow(button("إضافة"){db.saveRegion(c.text.toString(),p.text.toString(),city.text.toString(),d.text.toString());Toast.makeText(this,"تمت إضافة المنطقة",Toast.LENGTH_SHORT).show()},button("تعديل"){db.updateRegion(c.text.toString(),p.text.toString(),city.text.toString(),d.text.toString(),c.text.toString(),p.text.toString(),city.text.toString(),d.text.toString());Toast.makeText(this,"تم حفظ التعديل",Toast.LENGTH_SHORT).show()},button("حذف"){Toast.makeText(this,if(db.deleteRegion(c.text.toString(),p.text.toString(),city.text.toString(),d.text.toString()))"تم الحذف" else "لم يتم العثور على المنطقة",Toast.LENGTH_SHORT).show()},button("بحث"){showMessage("نتيجة البحث",db.searchRegions(c.text.toString(),p.text.toString(),city.text.toString(),d.text.toString()))},button("تقرير"){showMessage("تقرير المناطق",db.regions().joinToString("\n"))}));r.addView(button("رجوع"){home()},margin(10));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}

    private fun currencies(){val r=root("بيانات العملات");val n=field("اسم العملة");val e=field("السعر المعادل","1");val local=CheckBox(this).apply{text="العملة المحلية";setTextColor(text);gravity=Gravity.RIGHT};val rate=field("سعر التحويل","1");r.addView(n,margin());r.addView(e,margin());r.addView(local,margin());r.addView(rate,margin());r.addView(actionRow(button("إضافة"){if(db.saveCurrency(n.text.toString(),e.text.toString().toDoubleOrNull()?:1,local.isChecked,rate.text.toString().toDoubleOrNull()?:1))Toast.makeText(this,"تمت إضافة العملة",Toast.LENGTH_SHORT).show()},button("تعديل"){Toast.makeText(this,if(db.updateCurrency(n.text.toString(),e.text.toString().toDoubleOrNull()?:1,local.isChecked,rate.text.toString().toDoubleOrNull()?:1))"تم التعديل" else "لم يتم العثور على العملة",Toast.LENGTH_SHORT).show()},button("حذف"){Toast.makeText(this,if(db.deleteCurrency(n.text.toString()))"تم الحذف" else "لم يتم العثور على العملة",Toast.LENGTH_SHORT).show()},button("بحث"){showMessage("نتيجة البحث",db.searchCurrencies(n.text.toString()))},button("تقرير"){showMessage("تقرير العملات",db.currencies().joinToString("\n"))}));r.addView(button("رجوع"){home()},margin(10));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
}
