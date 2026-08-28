package com.h2pro.accounting

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ModulesActivity : AppCompatActivity() {
    private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val goldLight=Color.rgb(231,201,130); private val bg=Color.rgb(12,28,40); private val textColor=Color.rgb(245,241,232)
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun label(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{this.text=s;textSize=size;setTextColor(textColor);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1)}
    private fun button(s:String,action:()->Unit)=Button(this).apply{this.text=s;isAllCaps=false;textSize=16f;setTextColor(goldLight);minHeight=dp(54);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{action()}}
    private fun root(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(16),dp(16),dp(16),dp(16));addView(label(title,24f,true),LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,14)})}
    private fun margin(t:Int=0)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,t,0,dp(7))}
    override fun onCreate(state:Bundle?){super.onCreate(state);home()}
    private fun home(){
        val r=root("H2pro")
        r.addView(label("مركز الوحدات المحاسبية",18f,true),margin())
        r.addView(button("تهيئة النظام"){startActivity(Intent(this,SystemSetupActivity::class.java))},margin())
        r.addView(button("إدارة النظام"){startActivity(Intent(this,SystemAdminActivity::class.java))},margin())
        r.addView(button("إدارة الأستاذ العام"){startActivity(Intent(this,GeneralLedgerActivity::class.java))},margin())
        r.addView(button("سندات القبض والصرف النقدي"){startActivity(Intent(this,CashTransactionActivity::class.java))},margin())
        r.addView(button("إدارة المخزون"){startActivity(Intent(this,InventoryActivity::class.java))},margin())
        r.addView(button("إدارة المشتريات"){startActivity(Intent(this,PurchaseActivity::class.java))},margin())
        r.addView(button("فاتورة شراء متعددة الأصناف"){startActivity(Intent(this,MultiInvoiceActivity::class.java).putExtra("kind","شراء"))},margin())
        r.addView(button("إدارة المبيعات"){startActivity(Intent(this,SalesActivity::class.java))},margin())
        r.addView(button("فاتورة بيع متعددة الأصناف"){startActivity(Intent(this,MultiInvoiceActivity::class.java).putExtra("kind","بيع"))},margin())
        r.addView(button("المرتجعات"){startActivity(Intent(this,ReturnsActivity::class.java))},margin())
        r.addView(button("التقارير المالية"){startActivity(Intent(this,FinancialReportsActivity::class.java))},margin())
        r.addView(button("أنظمة وتقارير مساعدة"){show("أنظمة وتقارير مساعدة",listOf("ميزان المراجعة","قائمة الدخل","الكشوفات","سجل العمليات","تقارير عامة"))},margin())
        setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})
    }
    private fun show(title:String,items:List<String>){val r=root(title);items.forEach{item->r.addView(button(item){Toast.makeText(this,"سيتم ربط الوظيفة في المرحلة التالية: $item",Toast.LENGTH_SHORT).show()},margin())};r.addView(button("رجوع"){home()},margin(10));setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
}
