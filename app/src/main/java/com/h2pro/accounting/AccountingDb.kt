package com.h2pro.accounting

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Locale
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AccountingDb(context: Context) : SQLiteOpenHelper(context, "h2pro.db", null, 6) {
    private var sessionYear: Int? = null
    private fun isSessionYearOpen(): Boolean { val y=sessionYear ?: return false; return readableDatabase.rawQuery("SELECT 1 FROM financial_years WHERE year=? AND status='مفتوحة' LIMIT 1",arrayOf(y.toString())).use{it.moveToFirst()} }
    private fun isSessionDateOpen(date: String): Boolean {
        val y = sessionYear ?: return false
        val normalized = date.trim()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val parsed = try { formatter.parse(normalized) } catch (_: Exception) { null } ?: return false
        if (formatter.format(parsed) != normalized) return false
        val parts = normalized.split("-")
        if (parts.size != 3) return false
        val dateYear = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        if (dateYear != y || month !in 1..12) return false
        return readableDatabase.rawQuery("SELECT months FROM financial_years WHERE year=? AND status='مفتوحة' LIMIT 1", arrayOf(y.toString())).use {
            it.moveToFirst() && it.getString(0).split(",").mapNotNull { m -> m.trim().toIntOrNull() }.contains(month)
        }
    }
    override fun onCreate(db: SQLiteDatabase) { createTables(db); seed(db); ensureRequiredAccounts(db) }
    private fun createTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS company(id INTEGER PRIMARY KEY, name TEXT, phone TEXT, address TEXT, logo TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS financial_years(id INTEGER PRIMARY KEY AUTOINCREMENT, year INTEGER UNIQUE, months TEXT, status TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS regions(id INTEGER PRIMARY KEY AUTOINCREMENT, country TEXT, province TEXT, city TEXT, district TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS currencies(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, equivalent REAL DEFAULT 1, is_local INTEGER DEFAULT 0, exchange_rate REAL DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT, user_no TEXT UNIQUE, username TEXT, password TEXT, role TEXT, active INTEGER DEFAULT 1, password_hash TEXT, password_salt TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS permissions(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, module TEXT, allowed INTEGER DEFAULT 1, UNIQUE(user_id,module))")
        db.execSQL("CREATE TABLE IF NOT EXISTS banks(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, account_no TEXT, currency TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cashboxes(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, box_no TEXT, currency TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, name TEXT, type TEXT, parent_id INTEGER DEFAULT 0, level INTEGER DEFAULT 1, currency TEXT DEFAULT 'محلي', active INTEGER DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journals(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, description TEXT, reference TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY AUTOINCREMENT, journal_id INTEGER, account_id INTEGER, debit REAL DEFAULT 0, credit REAL DEFAULT 0, currency TEXT DEFAULT 'محلي', rate REAL DEFAULT 1)")
        db.execSQL("CREATE TABLE IF NOT EXISTS contacts(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, name TEXT, phone TEXT, address TEXT, balance REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE IF NOT EXISTS items(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, name TEXT, buy REAL DEFAULT 0, sale REAL DEFAULT 0, qty REAL DEFAULT 0, min_qty REAL DEFAULT 0, unit TEXT DEFAULT 'قطعة')")
        db.execSQL("CREATE TABLE IF NOT EXISTS documents(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, date TEXT, name TEXT, amount REAL DEFAULT 0, reference TEXT, notes TEXT, payment_mode TEXT DEFAULT 'نقدي')")
        addColumnIfMissing(db, "documents", "payment_mode", "TEXT DEFAULT 'نقدي'")
        addColumnIfMissing(db, "users", "active", "INTEGER DEFAULT 1")
        addColumnIfMissing(db, "users", "password_hash", "TEXT")
        addColumnIfMissing(db, "users", "password_salt", "TEXT")
        db.execSQL("CREATE TABLE IF NOT EXISTS invoice_lines(id INTEGER PRIMARY KEY AUTOINCREMENT, document_id INTEGER, item_id INTEGER, qty REAL, price REAL, total REAL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS inventory_movements(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, item_id INTEGER, kind TEXT, qty REAL, price REAL, reference TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS audit_log(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, user_no TEXT, action TEXT, details TEXT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_lines_account ON journal_lines(account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_lines_document ON invoice_lines(document_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_inventory_item ON inventory_movements(item_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_documents_kind_date ON documents(kind,date)")
    }
    private fun addColumnIfMissing(db: SQLiteDatabase, table: String, column: String, definition: String) {
        val exists = db.rawQuery("PRAGMA table_info($table)", null).use { c -> var found = false; while (c.moveToNext()) if (c.getString(1).equals(column, true)) { found = true; break }; found }
        if (!exists) db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
    }
    private fun seed(db: SQLiteDatabase) {
        if (count("company", db) == 0L) db.execSQL("INSERT INTO company(id,name,phone,address,logo) VALUES(1,'شركتي التجارية','01-234567','صنعاء، اليمن','')")
        if (count("financial_years", db) == 0L) db.execSQL("INSERT INTO financial_years(year,months,status) VALUES(2026,'1,2,3,4,5,6,7,8,9,10,11,12','مفتوحة')")
        if (count("currencies", db) == 0L) db.execSQL("INSERT INTO currencies(name,equivalent,is_local,exchange_rate) VALUES('ريال يمني',1,1,1)")
        if (count("users", db) == 0L) { val v=ContentValues().apply { put("user_no","1"); put("username","مدير النظام"); put("password",""); put("role","مدير النظام"); put("active",1) }; val s=randomSalt(); v.put("password_salt",s); v.put("password_hash",hashPassword("1234",s)); db.insert("users",null,v) }
        if (count("accounts", db) == 0L) {
            val rows = listOf(arrayOf("1","الأصول","أصول","0","1"),arrayOf("101","الصندوق","أصول","1","2"),arrayOf("102","البنك","أصول","1","2"),arrayOf("103","المخزون","أصول","1","2"),arrayOf("2","الخصوم","خصوم","0","1"),arrayOf("201","الموردون","خصوم","2","2"),arrayOf("3","حقوق الملكية","حقوق ملكية","0","1"),arrayOf("301","رأس المال","حقوق ملكية","3","2"),arrayOf("4","الإيرادات","إيرادات","0","1"),arrayOf("401","المبيعات","إيرادات","4","2"),arrayOf("402","مرتجعات المبيعات","إيرادات","4","2"),arrayOf("5","المصروفات","مصروفات","0","1"),arrayOf("501","المشتريات","مصروفات","5","2"),arrayOf("502","المصاريف التشغيلية","مصروفات","5","2"),arrayOf("503","تكلفة المبيعات","مصروفات","5","2"))
            rows.forEach { a -> db.insert("accounts", null, ContentValues().apply { put("code", a[0]); put("name", a[1]); put("type", a[2]); put("parent_id", a[3].toLong()); put("level", a[4].toInt()) }) }
        }
    }
    private fun ensureRequiredAccounts(db: SQLiteDatabase) {
        val assets = findAccountId(db, "1"); val liabilities = findAccountId(db, "2"); val income = findAccountId(db, "4"); val expenses = findAccountId(db, "5")
        val required = listOf(arrayOf("103","المخزون","أصول",assets,2),arrayOf("201","الموردون","خصوم",liabilities,2),arrayOf("202","العملاء","أصول",assets,2),arrayOf("401","المبيعات","إيرادات",income,2),arrayOf("402","مرتجعات المبيعات","إيرادات",income,2),arrayOf("503","تكلفة المبيعات","مصروفات",expenses,2))
        required.forEach { a -> if (findAccountId(db, a[0].toString()) == 0L) db.insert("accounts", null, ContentValues().apply { put("code", a[0].toString()); put("name", a[1].toString()); put("type", a[2].toString()); put("parent_id", a[3].toString().toLong()); put("level", a[4].toString().toInt()); put("currency", "محلي") }) }
    }
    private fun count(table: String, db: SQLiteDatabase = writableDatabase): Long = db.rawQuery("SELECT COUNT(*) FROM $table", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { createTables(db); migratePasswords(db); seed(db); ensureRequiredAccounts(db) }
    private fun migratePasswords(db: SQLiteDatabase) { db.rawQuery("SELECT id,password,password_hash,password_salt FROM users",null).use { c -> while(c.moveToNext()){ val id=c.getLong(0); val legacy=c.getString(1)?:""; val hash=c.getString(2); val salt=c.getString(3); if(hash.isNullOrBlank()&&legacy.isNotBlank()){ val s=if(salt.isNullOrBlank()) randomSalt() else salt; db.update("users",ContentValues().apply{put("password","");put("password_salt",s);put("password_hash",hashPassword(legacy,s))},"id=?",arrayOf(id.toString())) } } } }
    private fun randomSalt(): String { val b=ByteArray(16); SecureRandom().nextBytes(b); return android.util.Base64.encodeToString(b,android.util.Base64.NO_WRAP) }
    private fun hashPassword(password:String,salt:String):String { val spec=PBEKeySpec(password.toCharArray(),android.util.Base64.decode(salt,android.util.Base64.NO_WRAP),120000,256); return try{android.util.Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,android.util.Base64.NO_WRAP)}finally{spec.clearPassword()} }
    private fun insert(table: String, values: ContentValues) = writableDatabase.insert(table, null, values)
    private fun queryStrings(sql: String, args: Array<String>? = null): List<String> = buildList { readableDatabase.rawQuery(sql, args).use { c -> while (c.moveToNext()) add(c.getString(0)) } }
    fun login(userNo:String,password:String,year:Int):Boolean { val open=readableDatabase.rawQuery("SELECT id FROM financial_years WHERE year=? AND status='مفتوحة' LIMIT 1",arrayOf(year.toString())).use{it.moveToFirst()}; if(!open)return false; val ok=readableDatabase.rawQuery("SELECT password_hash,password_salt,active FROM users WHERE user_no=? LIMIT 1",arrayOf(userNo)).use{if(!it.moveToFirst()||it.getInt(2)!=1)return false; val h=it.getString(0); val s=it.getString(1); !h.isNullOrBlank()&&!s.isNullOrBlank()&&hashPassword(password,s)==h}; if(ok) sessionYear=year; return ok }
    fun changePassword(userNo:String,newPassword:String):Boolean { val s=randomSalt(); return writableDatabase.update("users",ContentValues().apply{put("password","");put("password_salt",s);put("password_hash",hashPassword(newPassword,s))},"user_no=?",arrayOf(userNo))>0 }
    fun saveUser(userNo:String,name:String,password:String,role:String)=try{val s=randomSalt();insert("users",ContentValues().apply{put("user_no",userNo);put("username",name);put("password","");put("password_salt",s);put("password_hash",hashPassword(password,s));put("role",role);put("active",1)})>0}catch(_:Exception){false}
    fun setUserActive(userNo:String, active:Boolean):Boolean = writableDatabase.update("users",ContentValues().apply{put("active",if(active)1 else 0)},"user_no=?",arrayOf(userNo))>0
    fun userList(): List<String> = queryStrings("SELECT user_no||' | '||username||' | '||role||' | '||CASE WHEN active=1 THEN 'نشط' ELSE 'موقوف' END FROM users ORDER BY user_no")
    fun saveYear(year: Int, months: String, status: String): Boolean {
        if (year < 1900 || year > 2200) return false
        if (months.isBlank()) return false
        if (status != "مفتوحة" && status != "مغلقة") return false
        val normalizedMonths = months.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..12 }.distinct().sorted()
        if (normalizedMonths.isEmpty()) return false
        if (normalizedMonths.size != months.split(",").size) return false
        val db = writableDatabase
        return try {
            db.insertOrThrow("financial_years", null, ContentValues().apply {
                put("year", year)
                put("months", normalizedMonths.joinToString(","))
                put("status", status)
            }) > 0
        } catch (_: Exception) { false }
    }
    fun years(): List<String> = queryStrings("SELECT year||' | '||months||' | '||status FROM financial_years ORDER BY year DESC")
    fun saveRegion(country: String, province: String, city: String, district: String) { insert("regions", ContentValues().apply { put("country", country); put("province", province); put("city", city); put("district", district) }) }
    fun regions(): List<String> = queryStrings("SELECT country||' | '||province||' | '||city||' | '||district FROM regions ORDER BY country,province,city")
    fun saveCurrency(name: String, equivalent: Double, local: Boolean, rate: Double): Boolean {
        if (name.isBlank() || equivalent <= 0 || rate <= 0) return false
        val db = writableDatabase
        if (db.rawQuery("SELECT COUNT(*) FROM currencies WHERE name=?", arrayOf(name.trim())).use { it.moveToFirst() && it.getLong(0) > 0 }) return false
        if (local && db.rawQuery("SELECT COUNT(*) FROM currencies WHERE is_local=1", null).use { it.moveToFirst() && it.getLong(0) > 0 }) return false
        return try {
            db.insertOrThrow("currencies", null, ContentValues().apply {
                put("name", name.trim())
                put("equivalent", equivalent)
                put("is_local", if (local) 1 else 0)
                put("exchange_rate", rate)
            }) > 0
        } catch (_: Exception) { false }
    }
    fun currencies(): List<String> = queryStrings("SELECT name||' | معادل: '||equivalent||' | '||CASE WHEN is_local=1 THEN 'محلية' ELSE 'أجنبية' END||' | تحويل: '||exchange_rate FROM currencies ORDER BY name")
    fun saveCompany(name: String, phone: String, address: String, logo: String) { writableDatabase.update("company", ContentValues().apply { put("name", name); put("phone", phone); put("address", address); put("logo", logo) }, "id=1", null) }
    fun company(): Company = readableDatabase.rawQuery("SELECT name,phone,address,logo FROM company WHERE id=1", null).use { if (it.moveToFirst()) Company(it.getString(0), it.getString(1), it.getString(2), it.getString(3)) else Company("","","","") }
    fun hasPermission(userNo: String, module: String): Boolean { val role=readableDatabase.rawQuery("SELECT role FROM users WHERE user_no=? AND active=1 LIMIT 1",arrayOf(userNo)).use{if(it.moveToFirst())it.getString(0) else ""}; if(role=="مدير النظام") return true; return readableDatabase.rawQuery("SELECT allowed FROM permissions p JOIN users u ON u.id=p.user_id WHERE u.user_no=? AND (p.module=? OR p.module=?) LIMIT 1",arrayOf(userNo,module,module.substringBefore("."))).use{it.moveToFirst()&&it.getInt(0)==1} }
    fun savePermission(userNo: String, module: String, allowed: Boolean) { val id = readableDatabase.rawQuery("SELECT id FROM users WHERE user_no=?", arrayOf(userNo)).use { if (it.moveToFirst()) it.getLong(0) else 0 }; if (id == 0L) return; writableDatabase.insertWithOnConflict("permissions", null, ContentValues().apply { put("user_id", id); put("module", module); put("allowed", if (allowed) 1 else 0) }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun addBank(name: String, no: String, currency: String) { insert("banks", ContentValues().apply { put("name", name); put("account_no", no); put("currency", currency) }) }
    fun addCashbox(name: String, no: String, currency: String) { insert("cashboxes", ContentValues().apply { put("name", name); put("box_no", no); put("currency", currency) }) }
    fun addAccount(code: String, name: String, type: String, parentId: Long = 0, currency: String = "محلي") = try { insert("accounts", ContentValues().apply { put("code", code); put("name", name); put("type", type); put("parent_id", parentId); put("level", if (parentId == 0L) 1 else 2); put("currency", currency) }) > 0 } catch (_: Exception) { false }
    fun accounts(): List<Account> = buildList { readableDatabase.rawQuery("SELECT id,code,name,type,parent_id,level,currency FROM accounts WHERE active=1 ORDER BY code", null).use { c -> while (c.moveToNext()) add(Account(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getInt(5), c.getString(6))) } }
    fun saveJournal(date: String, description: String, lines: List<JournalLine>, reference: String = ""): Boolean {
        if (!isSessionYearOpen() || !isSessionDateOpen(date)) return false
        val debit = lines.sumOf { it.debit }; val credit = lines.sumOf { it.credit }; if (lines.size < 2 || debit <= 0 || abs(debit - credit) > 0.005) return false
        val d = writableDatabase; d.beginTransaction(); return try { val jid = d.insertOrThrow("journals", null, ContentValues().apply { put("date", date); put("description", description); put("reference", reference) }); lines.forEach { l -> addLine(d, jid, l.accountId, l.debit, l.credit, l.currency, l.rate) }; d.setTransactionSuccessful(); true } catch (_: Exception) { false } finally { d.endTransaction() }
    }
    fun journalCount(): Long = count("journals")
    fun addContact(kind: String, name: String, phone: String, address: String = "") { insert("contacts", ContentValues().apply { put("kind", kind); put("name", name); put("phone", phone); put("address", address) }) }
    fun contacts(kind: String): List<String> = queryStrings("SELECT name||CASE WHEN phone='' THEN '' ELSE ' - '||phone END FROM contacts WHERE kind=? ORDER BY name", arrayOf(kind))
    fun addItem(code: String, name: String, buy: Double, sale: Double, qty: Double, minQty: Double, unit: String = "قطعة") = try { insert("items", ContentValues().apply { put("code", code); put("name", name); put("buy", buy); put("sale", sale); put("qty", qty); put("min_qty", minQty); put("unit", unit) }) > 0 } catch (_: Exception) { false }
    fun items(): List<String> = queryStrings("SELECT code||' - '||name||' | الكمية: '||qty||' '||unit FROM items ORDER BY name")
    fun addDocument(kind: String, name: String, amount: Double, date: String, reference: String = "", notes: String = "") = if (isSessionYearOpen() && isSessionDateOpen(date)) insert("documents", ContentValues().apply { put("kind", kind); put("name", name); put("amount", amount); put("date", date); put("reference", reference); put("notes", notes); put("payment_mode", "نقدي") }) else -1
    fun sumDocuments(kind: String): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM documents WHERE kind=?", arrayOf(kind)).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
    fun inventoryValue(): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(qty*buy),0) FROM items", null).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
    fun lowStock(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM items WHERE qty<=min_qty", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }
    fun saveInventoryMovement(itemId: Long, date: String, kind: String, qty: Double, price: Double, ref: String) { if (!isSessionYearOpen() || !isSessionDateOpen(date)) return; insert("inventory_movements", ContentValues().apply { put("item_id", itemId); put("date", date); put("kind", kind); put("qty", qty); put("price", price); put("reference", ref) }) }

    fun saveInvoice(kind: String, date: String, party: String, lines: List<InvoiceLine>, reference: String = "", notes: String = "", paymentMode: String = "نقدي"): Long {
        if (!isSessionYearOpen() || !isSessionDateOpen(date)) return -1
        if (kind != "شراء" && kind != "بيع") return -1
        if (lines.isEmpty() || lines.any { it.qty <= 0 || it.price < 0 }) return -1
        val total = lines.sumOf { it.qty * it.price }; if (total <= 0) return -1
        val sale = kind == "بيع"; val credit = paymentMode == "آجل"; val d = writableDatabase; d.beginTransaction()
        return try {
            lines.forEach { line -> d.rawQuery("SELECT qty FROM items WHERE id=?", arrayOf(line.itemId.toString())).use { c -> if (!c.moveToFirst()) throw IllegalArgumentException("الصنف غير موجود"); if (sale && c.getDouble(0) < line.qty) throw IllegalArgumentException("الرصيد غير كاف للصنف ${line.itemId}") } }
            val doc = d.insertOrThrow("documents", null, ContentValues().apply { put("kind", kind); put("date", date); put("name", party); put("amount", total); put("reference", reference); put("notes", notes); put("payment_mode", paymentMode) })
            var totalCost = 0.0
            lines.forEach { line ->
                val cost = d.rawQuery("SELECT buy FROM items WHERE id=?", arrayOf(line.itemId.toString())).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
                totalCost += cost * line.qty
                d.insertOrThrow("invoice_lines", null, ContentValues().apply { put("document_id", doc); put("item_id", line.itemId); put("qty", line.qty); put("price", line.price); put("total", line.qty * line.price) })
                val delta = if (sale) -line.qty else line.qty
                val values = ContentValues().apply { if (kind == "شراء") put("buy", line.price); if (kind == "بيع") put("sale", line.price) }
                d.update("items", values, "id=?", arrayOf(line.itemId.toString()))
                d.execSQL("UPDATE items SET qty=qty+? WHERE id=?", arrayOf(delta, line.itemId.toString()))
                saveInventoryMovementWithDb(d, line.itemId, date, kind, line.qty, if (sale) cost else line.price, reference)
            }
            val cash = findAccountId(d, "101"); val inventory = findAccountId(d, "103"); val revenue = findAccountId(d, "401"); val ar = findAccountId(d, "202"); val ap = findAccountId(d, "201"); val cogs = findAccountId(d, "503")
            require(inventory > 0)
            val jid = d.insertOrThrow("journals", null, ContentValues().apply { put("date", date); put("description", if (sale) "فاتورة بيع: $party" else "فاتورة شراء: $party"); put("reference", reference) })
            if (sale) { val debitAccount = if (credit) ar else cash; require(debitAccount > 0 && revenue > 0 && cogs > 0); addLine(d, jid, debitAccount, total, 0.0); addLine(d, jid, revenue, 0.0, total); addLine(d, jid, cogs, totalCost, 0.0); addLine(d, jid, inventory, 0.0, totalCost) }
            else { val creditAccount = if (credit) ap else cash; require(creditAccount > 0); addLine(d, jid, inventory, total, 0.0); addLine(d, jid, creditAccount, 0.0, total) }
            d.setTransactionSuccessful(); doc
        } catch (_: Exception) { -1 } finally { d.endTransaction() }
    }

    fun saveReturn(kind: String, date: String, party: String, lines: List<InvoiceLine>, reference: String = "", notes: String = "", paymentMode: String = "نقدي"): Long {
        if (!isSessionYearOpen() || !isSessionDateOpen(date)) return -1
        if (kind != "مرتجع شراء" && kind != "مرتجع بيع") return -1
        if (lines.isEmpty() || lines.any { it.qty <= 0 || it.price < 0 }) return -1
        val purchaseReturn = kind == "مرتجع شراء"; val credit = paymentMode == "آجل"; val d = writableDatabase; d.beginTransaction()
        return try {
            var totalCost = 0.0
            lines.forEach { line -> d.rawQuery("SELECT qty,buy FROM items WHERE id=?", arrayOf(line.itemId.toString())).use { c -> if (!c.moveToFirst()) throw IllegalArgumentException("الصنف غير موجود"); val stock = c.getDouble(0); val cost = c.getDouble(1); if (purchaseReturn && stock < line.qty) throw IllegalArgumentException("الرصيد غير كاف للمرتجع"); totalCost += cost * line.qty } }
            val total = lines.sumOf { it.qty * it.price }; if (total <= 0) throw IllegalArgumentException("قيمة غير صحيحة")
            val doc = d.insertOrThrow("documents", null, ContentValues().apply { put("kind", kind); put("date", date); put("name", party); put("amount", total); put("reference", reference); put("notes", notes); put("payment_mode", paymentMode) })
            lines.forEach { line ->
                d.insertOrThrow("invoice_lines", null, ContentValues().apply { put("document_id", doc); put("item_id", line.itemId); put("qty", line.qty); put("price", line.price); put("total", line.qty * line.price) })
                val delta = if (purchaseReturn) -line.qty else line.qty
                d.execSQL("UPDATE items SET qty=qty+? WHERE id=?", arrayOf(delta, line.itemId.toString()))
                saveInventoryMovementWithDb(d, line.itemId, date, kind, line.qty, if (purchaseReturn) line.price else totalCost / line.qty, reference)
            }
            val cash = findAccountId(d, "101"); val inventory = findAccountId(d, "103"); val salesReturn = findAccountId(d, "402"); val ar = findAccountId(d, "202"); val ap = findAccountId(d, "201"); val cogs = findAccountId(d, "503")
            val jid = d.insertOrThrow("journals", null, ContentValues().apply { put("date", date); put("description", "$kind: $party"); put("reference", reference) })
            if (purchaseReturn) { val debit = if (credit) ap else cash; require(debit > 0 && inventory > 0); addLine(d, jid, debit, total, 0.0); addLine(d, jid, inventory, 0.0, total) }
            else { val creditAccount = if (credit) ar else cash; require(salesReturn > 0 && creditAccount > 0 && inventory > 0 && cogs > 0); addLine(d, jid, salesReturn, total, 0.0); addLine(d, jid, creditAccount, 0.0, total); addLine(d, jid, inventory, totalCost, 0.0); addLine(d, jid, cogs, 0.0, totalCost) }
            d.setTransactionSuccessful(); doc
        } catch (_: Exception) { -1 } finally { d.endTransaction() }
    }

    private fun saveInventoryMovementWithDb(d: SQLiteDatabase, itemId: Long, date: String, kind: String, qty: Double, price: Double, ref: String) { d.insertOrThrow("inventory_movements", null, ContentValues().apply { put("item_id", itemId); put("date", date); put("kind", kind); put("qty", qty); put("price", price); put("reference", ref) }) }
    private fun addLine(d: SQLiteDatabase, journalId: Long, accountId: Long, debit: Double, credit: Double, currency: String = "محلي", rate: Double = 1.0) { d.insertOrThrow("journal_lines", null, ContentValues().apply { put("journal_id", journalId); put("account_id", accountId); put("debit", debit); put("credit", credit); put("currency", currency); put("rate", rate) }) }
    private fun findAccountId(d: SQLiteDatabase, code: String): Long = d.rawQuery("SELECT id FROM accounts WHERE code=?", arrayOf(code)).use { if (it.moveToFirst()) it.getLong(0) else 0 }
    fun invoiceTotal(documentId: Long): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(total),0) FROM invoice_lines WHERE document_id=?", arrayOf(documentId.toString())).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
    fun invoiceLines(documentId: Long): List<String> = queryStrings("SELECT i.code||' - '||i.name||' | '||l.qty||' × '||l.price||' = '||l.total FROM invoice_lines l JOIN items i ON i.id=l.item_id WHERE l.document_id=?", arrayOf(documentId.toString()))
    fun customerSupplierBalance(kind: String, name: String): Double { val positive = if (kind == "عميل") "بيع" else "شراء"; val negative = if (kind == "عميل") "مرتجع بيع" else "مرتجع شراء"; return readableDatabase.rawQuery("SELECT COALESCE(SUM(CASE WHEN kind=? THEN amount WHEN kind=? THEN -amount ELSE 0 END),0) FROM documents WHERE name=? AND payment_mode='آجل'", arrayOf(positive, negative, name)).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 } }
    fun accountBalance(id: Long): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(debit-credit),0) FROM journal_lines WHERE account_id=?", arrayOf(id.toString())).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }
    fun trialBalance(): List<String> = accounts().map { a -> val b = accountBalance(a.id); "${a.code} - ${a.name}: ${if (b >= 0) "مدين ${"%.2f".format(b)}" else "دائن ${"%.2f".format(-b)}"}" }.filter { !it.endsWith("مدين 0.00") && !it.endsWith("دائن 0.00") }
    fun trialBalanceTotals(): TrialBalanceTotals { val row = readableDatabase.rawQuery("SELECT COALESCE(SUM(debit),0),COALESCE(SUM(credit),0) FROM journal_lines", null).use { if (it.moveToFirst()) doubleArrayOf(it.getDouble(0), it.getDouble(1)) else doubleArrayOf(0.0, 0.0) }; return TrialBalanceTotals(row[0], row[1], abs(row[0] - row[1]) < 0.005) }
    fun financialSummary(): List<String> = listOf("إجمالي المبيعات: ${sumDocuments("بيع")}", "مرتجعات المبيعات: ${sumDocuments("مرتجع بيع")}", "صافي المبيعات: ${sumDocuments("بيع") - sumDocuments("مرتجع بيع")}", "إجمالي المشتريات: ${sumDocuments("شراء")}", "مرتجعات المشتريات: ${sumDocuments("مرتجع شراء")}", "صافي المشتريات: ${sumDocuments("شراء") - sumDocuments("مرتجع شراء")}", "تكلفة المبيعات: ${accountBalance(findAccountId(readableDatabase, "503"))}", "قيمة المخزون: ${inventoryValue()}", "الأصناف منخفضة المخزون: ${lowStock()}", "عدد القيود: ${journalCount()}")
    fun audit(user: String, action: String, details: String) { insert("audit_log", ContentValues().apply { put("date", System.currentTimeMillis().toString()); put("user_no", user); put("action", action); put("details", details) }) }
    fun auditLog(): List<String> = queryStrings("SELECT date||' | '||user_no||' | '||action||' | '||details FROM audit_log ORDER BY id DESC LIMIT 100")
}

data class Company(val name: String, val phone: String, val address: String, val logo: String)
data class Account(val id: Long, val code: String, val name: String, val type: String, val parentId: Long = 0, val level: Int = 1, val currency: String = "محلي")
data class JournalLine(val accountId: Long, val debit: Double, val credit: Double, val currency: String = "محلي", val rate: Double = 1.0)
data class InvoiceLine(val itemId: Long, val qty: Double, val price: Double)
data class TrialBalanceTotals(val debit: Double, val credit: Double, val balanced: Boolean)
