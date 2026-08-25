package com.h2pro.accounting

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AccountingDb(context: Context) : SQLiteOpenHelper(context, "h2pro.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) { createTables(db); seed(db) }
    private fun createTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS company(id INTEGER PRIMARY KEY, name TEXT, phone TEXT, address TEXT, logo TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS financial_years(id INTEGER PRIMARY KEY AUTOINCREMENT, year INTEGER UNIQUE, months TEXT, status TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS regions(id INTEGER PRIMARY KEY AUTOINCREMENT, country TEXT, province TEXT, city TEXT, district TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS currencies(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, equivalent REAL DEFAULT 1, is_local INTEGER DEFAULT 0, exchange_rate REAL DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT, user_no TEXT UNIQUE, username TEXT, password TEXT, role TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS permissions(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, module TEXT, allowed INTEGER DEFAULT 1, UNIQUE(user_id,module))")
        db.execSQL("CREATE TABLE IF NOT EXISTS banks(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, account_no TEXT, currency TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cashboxes(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, box_no TEXT, currency TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, name TEXT, type TEXT, parent_id INTEGER DEFAULT 0, level INTEGER DEFAULT 1, currency TEXT DEFAULT 'محلي', active INTEGER DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journals(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, description TEXT, reference TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY AUTOINCREMENT, journal_id INTEGER, account_id INTEGER, debit REAL DEFAULT 0, credit REAL DEFAULT 0, currency TEXT DEFAULT 'محلي', rate REAL DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS contacts(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, name TEXT, phone TEXT, address TEXT, balance REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS items(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, name TEXT, buy REAL DEFAULT 0, sale REAL DEFAULT 0, qty REAL DEFAULT 0, min_qty REAL DEFAULT 0, unit TEXT DEFAULT 'قطعة')")
        db.execSQL("CREATE TABLE IF NOT EXISTS documents(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, date TEXT, name TEXT, amount REAL DEFAULT 0, reference TEXT, notes TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS invoice_lines(id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER, item_id INTEGER, qty REAL, price REAL, total REAL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS inventory_movements(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, item_id INTEGER, kind TEXT, qty REAL, price REAL, reference TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS audit_log(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, user_no TEXT, action TEXT, details TEXT)")
    }
    private fun seed(db: SQLiteDatabase) {
        if (count("company", db) == 0L) db.execSQL("INSERT INTO company(id,name,phone,address,logo) VALUES(1,'شركتي التجارية','01-234567','صنعاء، اليمن','')")
        if (count("financial_years", db) == 0L) db.execSQL("INSERT INTO financial_years(year,months,status) VALUES(2026,'1,2,3,4,5,6,7,8,9,10,11,12','مفتوحة')")
        if (count("currencies", db) == 0L) db.execSQL("INSERT INTO currencies(name,equivalent,is_local,exchange_rate) VALUES('ريال يمني',1,1,1)")
        if (count("users", db) == 0L) db.execSQL("INSERT INTO users(user_no,username,password,role) VALUES('1','مدير النظام','1234','مدير النظام')")
        if (count("accounts", db) == 0L) {
            val rows = arrayOf(
                arrayOf("1","الأصول","أصول","0","1"),arrayOf("101","الصندوق","أصول","1","2"),arrayOf("102","البنك","أصول","1","2"),arrayOf("103","المخزون","أصول","1","2"),
                arrayOf("2","الخصوم","خصوم","0","1"),arrayOf("201","الموردون","خصوم","2","2"),arrayOf("3","حقوق الملكية","حقوق ملكية","0","1"),arrayOf("301","رأس المال","حقوق ملكية","3","2"),
                arrayOf("4","الإيرادات","إيرادات","0","1"),arrayOf("401","المبيعات","إيرادات","4","2"),arrayOf("5","المصروفات","مصروفات","0","1"),arrayOf("501","المشتريات","مصروفات","5","2"),arrayOf("502","المصاريف التشغيلية","مصروفات","5","2")
            )
            rows.forEach { a -> db.insert("accounts", null, ContentValues().apply { put("code",a[0]);put("name",a[1]);put("type",a[2]);put("parent_id",a[3].toLong());put("level",a[4].toInt()) }) }
        }
    }
    private fun count(table:String, db:SQLiteDatabase=writableDatabase):Long = db.rawQuery("SELECT COUNT(*) FROM $table",null).use { if(it.moveToFirst()) it.getLong(0) else 0 }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { createTables(db); seed(db) }
    private fun insert(table:String, values:ContentValues)=writableDatabase.insert(table,null,values)
    private fun queryStrings(sql:String,args:Array<String>?=null):List<String>{ val out=mutableListOf<String>(); readableDatabase.rawQuery(sql,args).use{c->while(c.moveToNext())out.add(c.getString(0))}; return out }

    fun login(userNo:String,password:String)=readableDatabase.rawQuery("SELECT id FROM users WHERE user_no=? AND password=?",arrayOf(userNo,password)).use{it.moveToFirst()}
    fun changePassword(userNo:String,newPassword:String)=writableDatabase.update("users",ContentValues().apply{put("password",newPassword)},"user_no=?",arrayOf(userNo))>0
    fun saveUser(userNo:String,name:String,password:String,role:String)=try{insert("users",ContentValues().apply{put("user_no",userNo);put("username",name);put("password",password);put("role",role)})>0}catch(_:Exception){false}
    fun userList(): List<String> = queryStrings("SELECT user_no||' | '||username||' | '||role FROM users ORDER BY user_no")
    fun saveYear(year:Int,months:String,status:String)=try{insert("financial_years",ContentValues().apply{put("year",year);put("months",months);put("status",status)})>0}catch(_:Exception){false}
    fun years(): List<String> = queryStrings("SELECT year||' | '||months||' | '||status FROM financial_years ORDER BY year DESC")
    fun saveRegion(country:String,province:String,city:String,district:String){insert("regions",ContentValues().apply{put("country",country);put("province",province);put("city",city);put("district",district)})}
    fun regions(): List<String> = queryStrings("SELECT country||' | '||province||' | '||city||' | '||district FROM regions ORDER BY country,province,city")
    fun saveCurrency(name:String,equivalent:Double,local:Boolean,rate:Double)=try{insert("currencies",ContentValues().apply{put("name",name);put("equivalent",equivalent);put("is_local",if(local)1 else 0);put("exchange_rate",rate)})>0}catch(_:Exception){false}
    fun currencies(): List<String> = queryStrings("SELECT name||' | معادل: '||equivalent||' | '||CASE WHEN is_local=1 THEN 'محلية' ELSE 'أجنبية' END||' | تحويل: '||exchange_rate FROM currencies ORDER BY name")
    fun saveCompany(name:String,phone:String,address:String,logo:String){writableDatabase.update("company",ContentValues().apply{put("name",name);put("phone",phone);put("address",address);put("logo",logo)},"id=1",null)}
    fun company():Company=readableDatabase.rawQuery("SELECT name,phone,address,logo FROM company WHERE id=1",null).use{if(it.moveToFirst())Company(it.getString(0),it.getString(1),it.getString(2),it.getString(3))else Company("","","","")}
    fun savePermission(userNo:String,module:String,allowed:Boolean){val id=readableDatabase.rawQuery("SELECT id FROM users WHERE user_no=?",arrayOf(userNo)).use{if(it.moveToFirst())it.getLong(0)else 0};if(id==0L)return;writableDatabase.insertWithOnConflict("permissions",null,ContentValues().apply{put("user_id",id);put("module",module);put("allowed",if(allowed)1 else 0)},SQLiteDatabase.CONFLICT_REPLACE)}
    fun addBank(name:String,no:String,currency:String){insert("banks",ContentValues().apply{put("name",name);put("account_no",no);put("currency",currency)})}
    fun addCashbox(name:String,no:String,currency:String){insert("cashboxes",ContentValues().apply{put("name",name);put("box_no",no);put("currency",currency)})}
    fun addAccount(code:String,name:String,type:String,parentId:Long=0,currency:String="محلي")=try{insert("accounts",ContentValues().apply{put("code",code);put("name",name);put("type",type);put("parent_id",parentId);put("level",if(parentId==0L)1 else 2);put("currency",currency)})>0}catch(_:Exception){false}
    fun accounts():List<Account>{val out=mutableListOf<Account>();readableDatabase.rawQuery("SELECT id,code,name,type,parent_id,level,currency FROM accounts WHERE active=1 ORDER BY code",null).use{c->while(c.moveToNext())out.add(Account(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4),c.getInt(5),c.getString(6)))};return out}
    fun saveJournal(date:String,description:String,lines:List<JournalLine>,reference:String=""):Boolean{if(lines.sumOf{it.debit} != lines.sumOf{it.credit}) return false;val d=writableDatabase;d.beginTransaction();return try{val jid=d.insertOrThrow("journals",null,ContentValues().apply{put("date",date);put("description",description);put("reference",reference)});lines.forEach{l->d.insertOrThrow("journal_lines",null,ContentValues().apply{put("journal_id",jid);put("account_id",l.accountId);put("debit",l.debit);put("credit",l.credit);put("currency",l.currency);put("rate",l.rate)})};d.setTransactionSuccessful();true}catch(_:Exception){false}finally{d.endTransaction()}}
    fun journalCount():Long=count("journals")
    fun addContact(kind:String,name:String,phone:String,address:String=""){insert("contacts",ContentValues().apply{put("kind",kind);put("name",name);put("phone",phone);put("address",address)})}
    fun contacts(kind:String): List<String> = queryStrings("SELECT name||CASE WHEN phone='' THEN '' ELSE ' - '||phone END FROM contacts WHERE kind=? ORDER BY name",arrayOf(kind))
    fun addItem(code:String,name:String,buy:Double,sale:Double,qty:Double,minQty:Double,unit:String="قطعة")=try{insert("items",ContentValues().apply{put("code",code);put("name",name);put("buy",buy);put("sale",sale);put("qty",qty);put("min_qty",minQty);put("unit",unit)})>0}catch(_:Exception){false}
    fun items(): List<String> = queryStrings("SELECT code||' - '||name||' | الكمية: '||qty||' '||unit FROM items ORDER BY name")
    fun addDocument(kind:String,name:String,amount:Double,date:String,reference:String="",notes:String="")=insert("documents",ContentValues().apply{put("kind",kind);put("name",name);put("amount",amount);put("date",date);put("reference",reference);put("notes",notes)})
    fun sumDocuments(kind:String):Double=readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM documents WHERE kind=?",arrayOf(kind)).use{if(it.moveToFirst())it.getDouble(0)else 0.0}
    fun inventoryValue():Double=readableDatabase.rawQuery("SELECT COALESCE(SUM(qty*buy),0) FROM items",null).use{if(it.moveToFirst())it.getDouble(0)else 0.0}
    fun lowStock():Long=readableDatabase.rawQuery("SELECT COUNT(*) FROM items WHERE qty<=min_qty",null).use{if(it.moveToFirst())it.getLong(0)else 0}
    fun saveInventoryMovement(itemId:Long,date:String,kind:String,qty:Double,price:Double,ref:String){insert("inventory_movements",ContentValues().apply{put("item_id",itemId);put("date",date);put("kind",kind);put("qty",qty);put("price",price);put("reference",ref)})}
    fun accountBalance(id:Long):Double=readableDatabase.rawQuery("SELECT COALESCE(SUM(debit-credit),0) FROM journal_lines WHERE account_id=?",arrayOf(id.toString())).use{if(it.moveToFirst())it.getDouble(0)else 0.0}
    fun trialBalance(): List<String> = accounts().map{a->"${a.code} - ${a.name}: ${"%.2f".format(accountBalance(a.id))}"}.filter{!it.endsWith(": 0.00")}
    fun audit(user:String,action:String,details:String){insert("audit_log",ContentValues().apply{put("date",System.currentTimeMillis().toString());put("user_no",user);put("action",action);put("details",details)})}
    fun auditLog(): List<String> = queryStrings("SELECT date||' | '||user_no||' | '||action||' | '||details FROM audit_log ORDER BY id DESC LIMIT 100")
}

data class Company(val name:String,val phone:String,val address:String,val logo:String)
data class Account(val id:Long,val code:String,val name:String,val type:String,val parentId:Long=0,val level:Int=1,val currency:String="محلي")
data class JournalLine(val accountId:Long,val debit:Double,val credit:Double,val currency:String="محلي",val rate:Double=1.0)
