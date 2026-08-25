package com.h2pro.accounting

import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/** إدارة النظام: المستخدمون والصلاحيات وتغيير كلمة السر، مرتبطة مباشرة بقاعدة h2pro.db. */
class SystemAdminActivity : AppCompatActivity() {
    private val db by lazy { AccountingDb(this) }
    private val bg=Color.rgb(12,28,40); private val surface=Color.rgb(24,45,60); private val gold=Color.rgb(198,161,91); private val textColor=Color.rgb(245,241,232)
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun title(s:String)=TextView(this).apply{text=s;textSize=23f;setTextColor(textColor);gravity=Gravity.RIGHT}
    private fun btn(s:String, action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=16f;setTextColor(Color.rgb(231,201,130));minHeight=dp(52);background=GradientDrawable().apply{cornerRadius=dp(8).toFloat();setColor(surface);setStroke(dp(1),gold)};setOnClickListener{action()}}
    private fun field(hint:String)=EditText(this).apply{this.hint=hint;setTextColor(textColor);setHintTextColor(Color.LTGRAY);textSize=16f;gravity=Gravity.RIGHT;setPadding(dp(12),0,dp(12),0)}
    private fun root(t:String):LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(16),dp(16),dp(16),dp(16));addView(title(t),LinearLayout.LayoutParams(-1,dp(55)))}
    private fun add(r:LinearLayout,v:android.view.View)=r.addView(v,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(8))})
    override fun onCreate(b:Bundle?){super.onCreate(b);home()}
    private fun home(){val r=root("إدارة النظام");add(r,btn("بيانات المستخدمين"){users()});add(r,btn("صلاحيات المستخدمين"){permissions()});add(r,btn("تغيير كلمة السر"){password()});add(r,btn("رجوع"){finish()});setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun users(){val r=root("بيانات المستخدمين");val no=field("رقم المستخدم");val name=field("اسم المستخدم");val pass=field("كلمة السر");val role=field("الدور / الصلاحية");val search=field("بحث");add(r,no);add(r,name);add(r,pass);add(r,role);add(r,search)
        add(r,btn("إضافة"){val ok=db.saveUser(no.text.toString().trim(),name.text.toString().trim(),pass.text.toString(),role.text.toString().trim().ifEmpty{"مستخدم"});toast(if(ok)"تمت إضافة المستخدم" else "تعذر الإضافة: رقم المستخدم موجود")})
        add(r,btn("تعديل"){val n=no.text.toString().trim();val cv=ContentValues().apply{put("username",name.text.toString().trim());put("role",role.text.toString().trim())};if(pass.text.isNotEmpty())cv.put("password",pass.text.toString());toast(if(db.writableDatabase.update("users",cv,"user_no=?",arrayOf(n))>0)"تم التعديل" else "المستخدم غير موجود")})
        add(r,btn("حذف"){val n=no.text.toString().trim();toast(if(db.writableDatabase.delete("users","user_no=?",arrayOf(n))>0)"تم الحذف" else "المستخدم غير موجود")})
        add(r,btn("بحث"){val q=search.text.toString();showText(r,"نتيجة البحث",db.readableDatabase.rawQuery("SELECT user_no||' | '||username||' | '||role FROM users WHERE user_no LIKE ? OR username LIKE ? ORDER BY user_no",arrayOf("%$q%","%$q%")).use{c->val a=mutableListOf<String>();while(c.moveToNext())a.add(c.getString(0));a.joinToString("\n")}.ifEmpty{"لا توجد نتائج"})})
        add(r,btn("تقرير"){showText(r,"تقرير المستخدمين",db.userList().joinToString("\n").ifEmpty{"لا توجد بيانات"})});add(r,btn("رجوع"){home()});setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun permissions(){val r=root("صلاحيات المستخدمين");val no=field("رقم المستخدم");val module=field("اسم الوحدة");val allowed=CheckBox(this).apply{text="السماح بالوصول";setTextColor(textColor);isChecked=true};add(r,no);add(r,module);add(r,allowed);add(r,btn("حفظ الصلاحية"){db.savePermission(no.text.toString().trim(),module.text.toString().trim(),allowed.isChecked);toast("تم حفظ الصلاحية")});add(r,btn("عرض صلاحيات المستخدم"){val q=no.text.toString().trim();showText(r,"الصلاحيات",db.readableDatabase.rawQuery("SELECT p.module||' | '||CASE WHEN p.allowed=1 THEN 'مسموح' ELSE 'ممنوع' END FROM permissions p JOIN users u ON u.id=p.user_id WHERE u.user_no=? ORDER BY p.module",arrayOf(q)).use{c->val a=mutableListOf<String>();while(c.moveToNext())a.add(c.getString(0));a.joinToString("\n")}.ifEmpty{"لا توجد صلاحيات محفوظة"})});add(r,btn("رجوع"){home()});setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun password(){val r=root("تغيير كلمة السر");val no=field("رقم المستخدم");val old=field("كلمة السر الحالية");val nw=field("كلمة السر الجديدة");add(r,no);add(r,old);add(r,nw);add(r,btn("حفظ كلمة السر"){val valid=db.readableDatabase.rawQuery("SELECT id FROM users WHERE user_no=? AND password=?",arrayOf(no.text.toString().trim(),old.text.toString())).use{it.moveToFirst()};toast(if(valid&&nw.text.length>=4)if(db.changePassword(no.text.toString().trim(),nw.text.toString()))"تم تغيير كلمة السر" else "تعذر التغيير" else "رقم المستخدم أو كلمة السر الحالية غير صحيحة")});add(r,btn("رجوع"){home()});setContentView(ScrollView(this).apply{addView(r,ViewGroup.LayoutParams(-1,-1))})}
    private fun showText(r:LinearLayout,t:String,s:String){val v=TextView(this).apply{text="$t\n\n$s";setTextColor(textColor);textSize=15f;gravity=Gravity.RIGHT;setPadding(0,dp(8),0,dp(8))};r.addView(v,0,LinearLayout.LayoutParams(-1,-2))}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
