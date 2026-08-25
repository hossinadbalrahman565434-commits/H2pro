package com.h2pro.accounting

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/** مركز النظام المحاسبي: الوحدات الرئيسية مع انتقال مباشر إلى الشاشات الفعلية. */
class ModulesActivity : AppCompatActivity() {
    private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val goldLight=Color.rgb(231,201,130); private val bg=Color.rgb(12,28,40); private val text=Color.rgb(245,241,232)
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun label(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(text);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1)}
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=16f;setTextColor(goldLight);minHeight=dp(54);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{action()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(16),dp(16),dp(16),dp(16));addView(label(title,24f,true),LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,14)})}
    private fun margin(t:Int=0)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,t,0,dp(7))}
    override fun onCreate(state:Bundle?){super.onCreate(state);home()}
    private fun home(){val r=root("H2pro المحاسبي");r.addView(label("مركز الوحدات المحاسبية",18f,true),margin());r.addView(button("تهيئة النظام"){startActivity(Intent(this,SystemSetupActivity::class.java))},margin());r.addView(button("إدارة النظام"){show("إدارة النظام",listOf("بيانات المستخدمين: إضافة / تعديل / حذف / بحث / تقرير","صلاحيات المستخدمين","تغيير كلمة السر"))},margin());r.addView(button("إدارة الأستاذ العام"){show("إدارة الأستاذ العام",listOf("تهيئة الأستاذ العام: شجرة الحسابات الرئيسية والفرعية والعملات","مدخلات الأستاذ العام: ربط البنوك والصناديق وبياناتها","عمليات الأستاذ العام: قيود يومية / سند قبض / سند صرف","التقارير والكشوفات"))},margin());r.addView(button("إدارة المخزون"){show("إدارة المخزون",listOf("بيانات الأصناف","حركات المخزون","الجرد والتسويات","تقارير المخزون"))},margin());r.addView(button("إدارة المشتريات"){show("إدارة المشتريات",listOf("فواتير الشراء","الموردون","مرتجعات المشتريات","تقارير المشتريات"))},margin());r.addView(button("إدارة المبيعات"){show("إدارة المبيعات",listOf("فواتير البيع","العملاء","مرتجعات المبيعات","تقارير المبيعات"))},margin());r.addView(button("أنظمة وتقارير مساعدة"){show("أنظمة وتقارير مساعدة",listOf("ميزان المراجعة","قائمة الدخل","الكشوفات","سجل العمليات","تقارير عامة"))},margin());setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun show(title:String,items:List<String>){val r=root(title);items.forEach{r.addView(button(it){Toast.makeText(this,"سيتم ربط الوظيفة في المرحلة التالية: $it",Toast.LENGTH_SHORT).show()},margin())};r.addView(button("رجوع"){home()},margin(10));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
}
