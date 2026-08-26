package com.h2pro.accounting

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class FinancialReportsActivity : androidx.appcompat.app.AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val fg=Color.rgb(245,241,232)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun tv(s:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(fg);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1);setPadding(dp(4),dp(6),dp(4),dp(6))}
    override fun onCreate(b:Bundle?){super.onCreate(b);home()}
    private fun home(){val r=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(14),dp(14),dp(14),dp(14));addView(tv("التقارير المالية والمساعدة",23f,true));addView(tv("ملخص الفترة الحالية",17f,true));db.financialSummary().forEach{addView(tv(it))};addView(tv("ميزان المراجعة",18f,true));db.trialBalance().ifEmpty{listOf("لا توجد أرصدة حتى الآن")}.forEach{addView(tv(it,14f))};addView(tv("تقرير المخزون",18f,true));addView(tv("قيمة المخزون: ${db.inventoryValue()}"));addView(tv("الأصناف منخفضة المخزون: ${db.lowStock()}"));addView(Button(this@FinancialReportsActivity).apply{text="تحديث";setOnClickListener{home()}});addView(Button(this@FinancialReportsActivity).apply{text="رجوع";setOnClickListener{finish()}})};setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
}
