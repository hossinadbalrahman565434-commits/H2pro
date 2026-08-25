package com.h2pro.accounting

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var db: AccountingDb
    private val prefs by lazy { getSharedPreferences("h2pro_setup", MODE_PRIVATE) }
    private val navy = Color.rgb(20, 42, 58)
    private val bg = Color.rgb(247, 249, 251)
    private val dp: (Int) -> Int = { (it * resources.displayMetrics.density).toInt() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); db=AccountingDb(this); showLogin() }
    private fun base(title:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setBackgroundColor(bg);setPadding(dp(16),dp(18),dp(16),dp(16));window.statusBarColor=navy;addView(text(title,25,true,navy),lp(0,0,0,14))}
    private fun scroll(v:LinearLayout)=ScrollView(this).apply{addView(v,ScrollView.LayoutParams(-1,-1))}
    private fun text(s:String,size:Int,bold:Boolean,color:Int)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);gravity=Gravity.RIGHT;if(bold)setTypeface(typeface,1)}
    private fun button(s:String,a:()->Unit)=Button(this).apply{text=s;textSize=15f;isAllCaps=false;setOnClickListener{a()}}
    private fun lp(l:Int,t:Int,r:Int,b:Int)=LinearLayout.LayoutParams(-1,-2).apply{setMargins(dp(l),dp(t),dp(r),dp(b))}
    private fun input(h:String,number:Boolean=false)=EditText(this).apply{hint=h;textSize=16f;gravity=Gravity.RIGHT;if(number)inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()

    private fun showLogin(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;layoutDirection=LinearLayout.LAYOUT_DIRECTION_RTL;setPadding(dp(28),dp(30),dp(28),dp(30));setBackgroundColor(navy)}
        root.addView(text("H2pro",36,true,Color.WHITE),lp(0,0,0,8));root.addView(text("النظام المحاسبي المتكامل",18,false,Color.WHITE),lp(0,0,0,25))
        val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(22),dp(22),dp(22));setBackgroundColor(Color.WHITE)}
        val user=input("رقم المستخدم");val pass=input("كلمة المرور");pass.inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;card.addView(user);card.addView(pass,lp(0,8,0,12))
        card.addView(button("تسجيل الدخول"){val u=user.text.toString();val p=pass.text.toString();val ok=(u=="1"&&p=="1234")||(u==prefs.getString("user","__none__")&&p==prefs.getString("pass","__none__"));if(ok)showHome()else toast("رقم المستخدم أو كلمة المرور غير صحيحة")},lp(0,4,0,5));card.addView(button("إلغاء"){finish()});root.addView(card,lp(0,10,0,0));setContentView(root)
    }
    private fun showHome(){
        val root=base("H2pro المحاسبي");root.addView(text("القائمة الرئيسية",18,true,Color.DKGRAY),lp(0,0,0,10))
        listOf("تهيئة النظام" to ::showSystemSetup,"إدارة النظام" to ::showSystemManagement,"إدارة الأستاذ العام" to ::showGeneralLedger,"إدارة المخزون" to ::showInventory,"إدارة المشتريات" to ::showPurchases,"إدارة المبيعات" to ::showSales,"أنظمة وتقارير مساعدة" to ::showReports).forEach{root.addView(button(it.first,it.second),lp(0,0,0,7))}
        root.addView(button("تسجيل الخروج",::showLogin),lp(0,14,0,0));setContentView(scroll(root))
    }
    private fun section(title:String,buttons:List<Pair<String,()->Unit>>){val root=base(title);buttons.forEach{root.addView(button(it.first,it.second),lp(0,0,0,7))};root.addView(button("رجوع",::showHome),lp(0,16,0,0));setContentView(scroll(root))}

    private fun showSystemSetup()=section("تهيئة النظام",listOf("بيانات السنة المالية" to ::yearsScreen,"بيانات الشركة" to ::companyScreen,"بيانات المناطق" to ::regionsScreen,"بيانات العملات" to ::currenciesScreen))
    private fun yearsScreen()=genericCrud("بيانات السنة المالية","years",listOf("السنة المالية","الشهور"),true,::showSystemSetup)
    private fun companyScreen(){val root=base("بيانات الشركة");val n=input("اسم الشركة");val p=input("رقم الهاتف");val a=input("العنوان");val l=input("الشعار / اسم ملف الشعار");val old=prefs.getString("company","||||")!!.split("|");if(old.size>=4){n.setText(old[0]);p.setText(old[1]);a.setText(old[2]);l.setText(old[3])};listOf(n,p,a,l).forEach{root.addView(it,lp(0,0,0,5))};root.addView(button("إضافة"){saveCompany(n,p,a,l)},lp(0,8,0,4));root.addView(button("تعديل"){saveCompany(n,p,a,l)},lp(0,0,0,4));root.addView(button("حذف"){prefs.edit().remove("company").apply();listOf(n,p,a,l).forEach{it.text.clear()};toast("تم حذف بيانات الشركة")});root.addView(button("رجوع",::showSystemSetup),lp(0,14,0,0));setContentView(scroll(root))}
    private fun saveCompany(n:EditText,p:EditText,a:EditText,l:EditText){prefs.edit().putString("company","${n.text}|${p.text}|${a.text}|${l.text}").apply();toast("تم حفظ بيانات الشركة")}
    private fun regionsScreen()=genericCrud("بيانات المناطق","regions",listOf("الدولة","المحافظة","المدينة","المنطقة"),true,::showSystemSetup)
    private fun currenciesScreen()=genericCrud("بيانات العملات","currencies",listOf("العملة","السعر المعادل","محلية / أجنبية","سعر التحويل"),true,::showSystemSetup)

    private fun showSystemManagement()=section("إدارة النظام",listOf("بيانات المستخدمين" to ::usersScreen,"صلاحيات المستخدمين" to ::permissionsScreen,"تغيير كلمة السر" to ::changePassword))
    private fun usersScreen()=genericCrud("بيانات المستخدمين","users",listOf("رقم المستخدم","اسم المستخدم","كلمة المرور","الصلاحية"),true,::showSystemManagement)
    private fun permissionsScreen(){val root=base("صلاحيات المستخدمين");val u=input("رقم المستخدم");val r=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("مدير النظام","محاسب","مستخدم"))};root.addView(u);root.addView(r,lp(0,5,0,10));root.addView(text("صلاحيات الوحدات: تهيئة النظام، الأستاذ العام، المخزون، المشتريات، المبيعات، التقارير.",15,false,Color.DKGRAY));root.addView(button("حفظ"){prefs.edit().putString("perm_${u.text}",r.selectedItem.toString()).apply();toast("تم حفظ الصلاحية")});root.addView(button("رجوع",::showSystemManagement),lp(0,15,0,0));setContentView(scroll(root))}
    private fun changePassword(){val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val old=input("كلمة المرور الحالية");val n=input("كلمة المرور الجديدة");val c=input("تأكيد كلمة المرور");listOf(old,n,c).forEach{b.addView(it)};AlertDialog.Builder(this).setTitle("تغيير كلمة السر").setView(b).setPositiveButton("حفظ"){_,_->if(old.text.toString()==prefs.getString("pass","1234")&&n.text.toString()==c.text.toString()&&n.text.isNotBlank()){prefs.edit().putString("pass",n.text.toString()).putString("user",prefs.getString("user","1")).apply();toast("تم تغيير كلمة المرور")}else toast("بيانات كلمة المرور غير صحيحة")}.setNegativeButton("إلغاء",null).show()}

    private fun showGeneralLedger()=section("إدارة الأستاذ العام",listOf("تهيئة الأستاذ العام" to ::ledgerSetup,"مدخلات الأستاذ العام" to ::ledgerInputs,"عمليات الأستاذ العام" to ::ledgerOperations,"التقارير والكشوفات" to ::ledgerReports))
    private fun ledgerSetup(){val root=base("تهيئة الأستاذ العام");root.addView(button("شجرة الحسابات",::showAccounts));root.addView(text("أصول • خصوم • حقوق ملكية • إيرادات • مصروفات\nرئيسي / فرعي\nمحلية / أجنبية",16,false,Color.DKGRAY),lp(0,10,0,10));root.addView(button("رجوع",::showGeneralLedger));setContentView(scroll(root))}
    private fun ledgerInputs()=section("مدخلات الأستاذ العام",listOf("ربط البنوك" to {genericCrud("بيانات البنوك","banks",listOf("اسم البنك","رقم الحساب","العملة"),true,::showGeneralLedger)},"ربط الصناديق" to {genericCrud("بيانات الصناديق","cashboxes",listOf("اسم الصندوق","رقم الصندوق","العملة"),true,::showGeneralLedger)}))
    private fun ledgerOperations()=section("عمليات الأستاذ العام",listOf("قيود يومية" to ::showJournal,"سند صرف" to {documentDialog("صرف")} ,"سند قبض" to {documentDialog("قبض")}))
    private fun ledgerReports()=section("التقارير والكشوفات",listOf("ميزان المراجعة" to ::showTrialBalance,"قائمة الدخل" to ::showIncome,"كشف حركة الحسابات" to ::showTrialBalance))

    private fun showInventory()=section("إدارة المخزون",listOf("الأصناف" to ::showItems,"حركة المخزون" to ::inventoryMovement,"جرد المخزون" to ::inventoryCount,"تقرير المخزون" to ::showItems))
    private fun inventoryMovement(){val r=base("حركة المخزون");r.addView(text("حركة الإدخال والإخراج من المشتريات والمبيعات.",16,false,Color.DKGRAY));r.addView(button("رجوع",::showInventory),lp(0,15,0,0));setContentView(r)}
    private fun inventoryCount(){val r=base("جرد المخزون");r.addView(text(db.items().joinToString("\n\n").ifEmpty{"لا توجد أصناف"},16,false,Color.DKGRAY));r.addView(button("رجوع",::showInventory),lp(0,15,0,0));setContentView(scroll(r))}
    private fun showPurchases()=section("إدارة المشتريات",listOf("فاتورة شراء" to {documentDialog("شراء")},"الموردون" to {showContacts("مورد")},"كشف المشتريات" to ::showPurchaseReport))
    private fun showSales()=section("إدارة المبيعات",listOf("فاتورة بيع" to {documentDialog("بيع")},"العملاء" to {showContacts("عميل")},"كشف المبيعات" to ::showSalesReport))
    private fun showPurchaseReport(){val r=base("تقرير المشتريات");r.addView(text("إجمالي المشتريات: ${money(db.sumDocuments("شراء"))} ريال",19,true,navy));r.addView(button("رجوع",::showPurchases),lp(0,15,0,0));setContentView(r)}
    private fun showSalesReport(){val r=base("تقرير المبيعات");r.addView(text("إجمالي المبيعات: ${money(db.sumDocuments("بيع"))} ريال",19,true,navy));r.addView(button("رجوع",::showSales),lp(0,15,0,0));setContentView(r)}
    private fun showReports()=section("أنظمة وتقارير مساعدة",listOf("ميزان المراجعة" to ::showTrialBalance,"قائمة الدخل" to ::showIncome,"تقرير المبيعات" to ::showSalesReport,"تقرير المشتريات" to ::showPurchaseReport,"سجل العمليات" to ::operationsLog))
    private fun operationsLog(){val r=base("سجل العمليات");r.addView(text("تم تشغيل النظام في ${today()}",16,false,Color.DKGRAY));r.addView(button("رجوع",::showReports),lp(0,15,0,0));setContentView(r)}

    private fun genericCrud(title:String,key:String,fields:List<String>,withSearch:Boolean,back:()->Unit){val root=base(title);val ins=fields.map{input(it)};ins.forEach{root.addView(it,lp(0,0,0,4))};root.addView(button("إضافة"){saveRecord(key,ins);toast("تمت الإضافة")},lp(0,8,0,4));root.addView(button("تعديل"){saveRecord(key,ins);toast("تم التعديل")},lp(0,0,0,4));root.addView(button("حذف"){clearRecords(key);ins.forEach{it.text.clear()};toast("تم الحذف")},lp(0,0,0,4));if(withSearch)root.addView(button("بحث"){showRecords(key,title,back)},lp(0,0,0,4));root.addView(button("تقرير"){showRecords(key,"تقرير $title",back)},lp(0,0,0,4));root.addView(button("رجوع",back),lp(0,15,0,0));setContentView(scroll(root))}
    private fun saveRecord(key:String,ins:List<EditText>){val a=load(key);val o=JSONObject();ins.forEachIndexed{i,e->o.put("f$i",e.text.toString())};a.put(o);prefs.edit().putString(key,a.toString()).apply()}
    private fun load(key:String)=try{JSONArray(prefs.getString(key,"[]"))}catch(_:Exception){JSONArray()}
    private fun clearRecords(key:String){prefs.edit().remove(key).apply()}
    private fun showRecords(key:String,title:String,back:()->Unit){val a=load(key);val r=base(title);r.addView(text(if(a.length()==0)"لا توجد بيانات" else (0 until a.length()).joinToString("\n\n"){i->val o=a.getJSONObject(i);o.keys().asSequence().map{o.getString(it)}.joinToString(" | ")},16,false,Color.DKGRAY));r.addView(button("رجوع",back),lp(0,15,0,0));setContentView(scroll(r))}

    private fun showAccounts(){val r=base("شجرة الحسابات");r.addView(button("＋ إضافة حساب",::addAccountDialog));db.accounts().forEach{a->r.addView(text("${a.code} | ${a.name} | ${a.type}",16,false,Color.DKGRAY),lp(0,5,0,5))};r.addView(button("رجوع",::showGeneralLedger),lp(0,15,0,0));setContentView(scroll(r))}
    private fun addAccountDialog(){val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val c=input("رقم الحساب");val n=input("اسم الحساب");val s=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,arrayOf("أصول","خصوم","حقوق ملكية","إيرادات","مصروفات"))};listOf(c,n).forEach{b.addView(it)};b.addView(s);AlertDialog.Builder(this).setTitle("إضافة حساب").setView(b).setPositiveButton("حفظ"){_,_->if(db.addAccount(c.text.toString(),n.text.toString(),s.selectedItem.toString()))showAccounts()else toast("تعذر إضافة الحساب")}.setNegativeButton("إلغاء",null).show()}
    private fun showJournal(){val r=base("قيد يومية");val d=input("وصف القيد");val a=db.accounts();val s=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,a.map{"${it.code} - ${it.name}"})};val v=input("المبلغ",true);r.addView(d);r.addView(s);r.addView(v);r.addView(button("حفظ القيد"){val x=v.text.toString().toDoubleOrNull()?:0.0;if(x>0&&a.size>1){val one=a[s.selectedItemPosition];val two=a[if(s.selectedItemPosition==0)1 else 0];db.saveJournal(today(),d.text.toString(),listOf(JournalLine(one.id,x,0.0),JournalLine(two.id,0.0,x)));toast("تم حفظ القيد");showHome()}else toast("أدخل مبلغًا صحيحًا")});r.addView(button("رجوع",::showGeneralLedger),lp(0,12,0,0));setContentView(scroll(r))}
    private fun showContacts(k:String){val r=base(if(k=="عميل")"العملاء" else "الموردون");r.addView(button("＋ إضافة"){contactDialog(k)});db.contacts(k).forEach{r.addView(text(it,16,false,Color.DKGRAY),lp(0,5,0,5))};r.addView(button("رجوع",if(k=="عميل")::showSales else ::showPurchases),lp(0,15,0,0));setContentView(scroll(r))}
    private fun contactDialog(k:String){val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val n=input("الاسم");val p=input("الهاتف");b.addView(n);b.addView(p);AlertDialog.Builder(this).setTitle("إضافة $k").setView(b).setPositiveButton("حفظ"){_,_->if(n.text.isNotBlank()){db.addContact(k,n.text.toString(),p.text.toString());showContacts(k)}}.setNegativeButton("إلغاء",null).show()}
    private fun showItems(){val r=base("الأصناف والمخزون");r.addView(button("＋ إضافة صنف",::itemDialog));db.items().forEach{r.addView(text(it,16,false,Color.DKGRAY),lp(0,5,0,5))};r.addView(button("رجوع",::showInventory),lp(0,15,0,0));setContentView(scroll(r))}
    private fun itemDialog(){val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val c=input("رمز الصنف");val n=input("اسم الصنف");val buy=input("سعر الشراء",true);val sale=input("سعر البيع",true);val q=input("الكمية",true);val m=input("الحد الأدنى",true);listOf(c,n,buy,sale,q,m).forEach{b.addView(it)};AlertDialog.Builder(this).setTitle("إضافة صنف").setView(b).setPositiveButton("حفظ"){_,_->db.addItem(c.text.toString(),n.text.toString(),buy.text.toString().toDoubleOrNull()?:0.0,sale.text.toString().toDoubleOrNull()?:0.0,q.text.toString().toDoubleOrNull()?:0.0,m.text.toString().toDoubleOrNull()?:0.0);showItems()}.setNegativeButton("إلغاء",null).show()}
    private fun documentDialog(k:String){val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val n=input("البيان / الطرف");val a=input("المبلغ",true);b.addView(n);b.addView(a);AlertDialog.Builder(this).setTitle("$k").setView(b).setPositiveButton("حفظ"){_,_->val v=a.text.toString().toDoubleOrNull()?:0.0;if(v>0){db.addDocument(if(k=="صرف")"مصروف" else k,n.text.toString(),v,today());toast("تم الحفظ");showHome()}}.setNegativeButton("إلغاء",null).show()}
    private fun showTrialBalance(){val r=base("ميزان المراجعة");r.addView(text(db.trialBalance().ifEmpty{"لا توجد حركة محاسبية بعد"}.joinToString("\n\n"),16,false,Color.DKGRAY));r.addView(button("رجوع",::showReports),lp(0,15,0,0));setContentView(scroll(r))}
    private fun showIncome(){val s=db.sumDocuments("بيع");val p=db.sumDocuments("شراء");val e=db.sumDocuments("مصروف");val r=base("قائمة الدخل");r.addView(text("المبيعات: ${money(s)} ريال\nالمشتريات: ${money(p)} ريال\nالمصروفات: ${money(e)} ريال\n\nصافي الربح: ${money(s-p-e)} ريال",19,true,navy));r.addView(button("رجوع",::showReports),lp(0,15,0,0));setContentView(r)}
    private fun money(v:Double)=String.format(Locale.US,"%,.2f",v)
    private fun today()=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date())
}
