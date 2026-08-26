package com.h2pro.accounting

import android.content.Context

/** قراءة رقم الصنف من قاعدة H2pro دون تغيير واجهة AccountingDb. */
fun Context.findH2proItemId(code: String): Long {
    if (code.isBlank()) return -1L
    val db = openOrCreateDatabase("h2pro.db", Context.MODE_PRIVATE, null)
    return try {
        db.rawQuery("SELECT id FROM items WHERE code=? LIMIT 1", arrayOf(code)).use { if (it.moveToFirst()) it.getLong(0) else -1L }
    } finally { db.close() }
}
