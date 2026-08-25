package com.h2pro.accounting

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AccountingDb(context: Context) : SQLiteOpenHelper(context, "h2pro.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE accounts(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, name TEXT, type TEXT)")
        db.execSQL("CREATE TABLE journals(id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, description TEXT)")
        db.execSQL("CREATE TABLE journal_lines(id INTEGER PRIMARY KEY AUTOINCREMENT, journal_id INTEGER, account_id INTEGER, debit REAL DEFAULT 0, credit REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE contacts(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, name TEXT, phone TEXT)")
        db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE, name TEXT, buy REAL DEFAULT 0, sale REAL DEFAULT 0, qty REAL DEFAULT 0, min_qty REAL DEFAULT 0)")
        db.execSQL("CREATE TABLE documents(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, date TEXT, name TEXT, amount REAL DEFAULT 0)")

        val accounts = listOf(
            arrayOf("101", "الصندوق", "أصول"), arrayOf("102", "البنك", "أصول"),
            arrayOf("103", "المخزون", "أصول"), arrayOf("201", "الموردون", "خصوم"),
            arrayOf("301", "رأس المال", "حقوق ملكية"), arrayOf("401", "المبيعات", "إيرادات"),
            arrayOf("501", "المشتريات", "مصروفات"), arrayOf("502", "المصاريف التشغيلية", "مصروفات")
        )
        accounts.forEach { a ->
            val v = ContentValues().apply { put("code", a[0]); put("name", a[1]); put("type", a[2]) }
            db.insert("accounts", null, v)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addAccount(code: String, name: String, type: String): Boolean = try {
        val v = ContentValues().apply { put("code", code); put("name", name); put("type", type) }
        writableDatabase.insertOrThrow("accounts", null, v) > 0
    } catch (_: Exception) { false }

    fun accounts(): List<Account> {
        val out = mutableListOf<Account>()
        readableDatabase.rawQuery("SELECT id,code,name,type FROM accounts ORDER BY code", null).use { c ->
            while (c.moveToNext()) out.add(Account(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)))
        }
        return out
    }

    fun saveJournal(date: String, description: String, lines: List<JournalLine>): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val j = ContentValues().apply { put("date", date); put("description", description) }
            val journalId = db.insertOrThrow("journals", null, j)
            lines.forEach { line ->
                val v = ContentValues().apply {
                    put("journal_id", journalId); put("account_id", line.accountId)
                    put("debit", line.debit); put("credit", line.credit)
                }
                db.insertOrThrow("journal_lines", null, v)
            }
            db.setTransactionSuccessful(); return true
        } catch (_: Exception) { return false } finally { db.endTransaction() }
    }

    fun addContact(kind: String, name: String, phone: String) {
        val v = ContentValues().apply { put("kind", kind); put("name", name); put("phone", phone) }
        writableDatabase.insert("contacts", null, v)
    }

    fun contacts(kind: String): List<String> {
        val out = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT name || CASE WHEN phone='' THEN '' ELSE ' - ' || phone END FROM contacts WHERE kind=? ORDER BY name", arrayOf(kind)).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    fun addItem(code: String, name: String, buy: Double, sale: Double, qty: Double, minQty: Double): Boolean = try {
        val v = ContentValues().apply { put("code", code); put("name", name); put("buy", buy); put("sale", sale); put("qty", qty); put("min_qty", minQty) }
        writableDatabase.insertOrThrow("items", null, v) > 0
    } catch (_: Exception) { false }

    fun items(): List<String> {
        val out = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT code || ' - ' || name || ' | الكمية: ' || qty FROM items ORDER BY name", null).use { c -> while (c.moveToNext()) out.add(c.getString(0)) }
        return out
    }

    fun addDocument(kind: String, name: String, amount: Double, date: String) {
        val v = ContentValues().apply { put("kind", kind); put("name", name); put("amount", amount); put("date", date) }
        writableDatabase.insert("documents", null, v)
    }

    fun count(table: String): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { if (it.moveToFirst()) it.getLong(0) else 0 }

    fun sumDocuments(kind: String): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(amount),0) FROM documents WHERE kind=?", arrayOf(kind)).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }

    fun accountBalance(accountId: Long): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(debit-credit),0) FROM journal_lines WHERE account_id=?", arrayOf(accountId.toString())).use { if (it.moveToFirst()) it.getDouble(0) else 0.0 }

    fun trialBalance(): List<String> {
        val out = mutableListOf<String>()
        accounts().forEach { a ->
            val b = accountBalance(a.id)
            if (b != 0.0) out.add("${a.code} - ${a.name}: ${"%.2f".format(b)}")
        }
        return out
    }
}

data class Account(val id: Long, val code: String, val name: String, val type: String)
data class JournalLine(val accountId: Long, val debit: Double, val credit: Double)
