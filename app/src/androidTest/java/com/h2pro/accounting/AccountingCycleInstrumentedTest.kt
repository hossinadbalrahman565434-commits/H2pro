package com.h2pro.accounting

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountingCycleInstrumentedTest {
    @Test
    fun fullCashCycleKeepsInventoryAndTrialBalanceCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("h2pro-test.db")
        val db = AccountingDb(context)
        val itemCode = "TEST-${System.currentTimeMillis()}"
        assertTrue(db.addItem(itemCode, "اختبار دورة محاسبية", 100.0, 150.0, 0.0, 0.0))
        val itemId = context.openOrCreateDatabase("h2pro.db", 0, null).use { sql ->
            sql.rawQuery("SELECT id FROM items WHERE code=?", arrayOf(itemCode)).use { c ->
                assertTrue(c.moveToFirst())
                c.getLong(0)
            }
        }
        assertTrue(db.saveInvoice("شراء", "2026-08-26", "مورد الاختبار", listOf(InvoiceLine(itemId, 10.0, 100.0))) > 0)
        assertTrue(db.saveInvoice("بيع", "2026-08-26", "عميل الاختبار", listOf(InvoiceLine(itemId, 4.0, 150.0))) > 0)
        assertTrue(db.saveReturn("مرتجع بيع", "2026-08-26", "عميل الاختبار", listOf(InvoiceLine(itemId, 1.0, 150.0))) > 0)
        assertTrue(db.saveReturn("مرتجع شراء", "2026-08-26", "مورد الاختبار", listOf(InvoiceLine(itemId, 2.0, 100.0))) > 0)

        val sql = context.openOrCreateDatabase("h2pro.db", 0, null)
        val qty = sql.rawQuery("SELECT qty FROM items WHERE id=?", arrayOf(itemId.toString())).use { c -> c.moveToFirst(); c.getDouble(0) }
        sql.close()
        assertEquals(5.0, qty, 0.0001)

        val totals = db.trialBalanceTotals()
        assertTrue("ميزان المراجعة غير متوازن: ${totals.debit} مقابل ${totals.credit}", totals.balanced)
        assertEquals(totals.debit, totals.credit, 0.005)
        db.close()
    }

    @Test
    fun creditSalesAndPurchasesCreateReceivableAndPayableDocuments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AccountingDb(context)
        val itemCode = "CREDIT-${System.currentTimeMillis()}"
        assertTrue(db.addItem(itemCode, "اختبار آجل", 50.0, 80.0, 0.0, 0.0))
        val itemId = context.openOrCreateDatabase("h2pro.db", 0, null).use { sql ->
            sql.rawQuery("SELECT id FROM items WHERE code=?", arrayOf(itemCode)).use { c -> c.moveToFirst(); c.getLong(0) }
        }
        assertTrue(db.saveInvoice("شراء", "2026-08-26", "مورد آجل", listOf(InvoiceLine(itemId, 2.0, 50.0)), paymentMode = "آجل") > 0)
        assertTrue(db.saveInvoice("بيع", "2026-08-26", "عميل آجل", listOf(InvoiceLine(itemId, 1.0, 80.0)), paymentMode = "آجل") > 0)
        assertEquals(80.0, db.customerSupplierBalance("عميل", "عميل آجل"), 0.005)
        assertEquals(100.0, db.customerSupplierBalance("مورد", "مورد آجل"), 0.005)
        assertTrue(db.trialBalanceTotals().balanced)
        db.close()
    }
}
