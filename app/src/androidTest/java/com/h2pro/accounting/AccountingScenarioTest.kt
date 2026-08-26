package com.h2pro.accounting

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountingScenarioTest {
    @Test
    fun completePurchaseSaleReturnCycleKeepsTrialBalanceBalanced() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AccountingDb(context)
        val sql = db.writableDatabase
        listOf("journal_lines", "journals", "invoice_lines", "inventory_movements", "documents", "items").forEach { sql.delete(it, null, null) }

        val cash = db.accounts().first { it.code == "101" }.id
        val capital = db.accounts().first { it.code == "301" }.id
        val testItem = "TEST-H2-${System.currentTimeMillis()}"
        assertTrue(db.addItem(testItem, "صنف اختبار الدورة", 100.0, 150.0, 0.0, 0.0))
        val itemId = sql.rawQuery("SELECT id FROM items WHERE code=?", arrayOf(testItem)).use { c -> assertTrue(c.moveToFirst()); c.getLong(0) }

        assertTrue(db.saveJournal("2026-08-26", "رأس مال اختبار", listOf(JournalLine(cash, 1000.0, 0.0), JournalLine(capital, 0.0, 1000.0))))
        assertTrue(db.saveInvoice("شراء", "2026-08-26", "مورد اختبار", listOf(InvoiceLine(itemId, 10.0, 100.0)) ) > 0)
        assertTrue(db.saveInvoice("بيع", "2026-08-26", "عميل اختبار", listOf(InvoiceLine(itemId, 4.0, 150.0)) ) > 0)
        assertTrue(db.saveReturn("مرتجع بيع", "2026-08-26", "عميل اختبار", listOf(InvoiceLine(itemId, 1.0, 150.0)) ) > 0)
        assertTrue(db.saveReturn("مرتجع شراء", "2026-08-26", "مورد اختبار", listOf(InvoiceLine(itemId, 2.0, 100.0)) ) > 0)

        val qty = sql.rawQuery("SELECT qty FROM items WHERE id=?", arrayOf(itemId.toString())).use { c -> assertTrue(c.moveToFirst()); c.getDouble(0) }
        val totals = db.trialBalanceTotals()
        val inventory = db.accountBalance(db.accounts().first { it.code == "103" }.id)
        val cogs = db.accountBalance(db.accounts().first { it.code == "503" }.id)

        assertEquals(5.0, qty, 0.001)
        assertEquals(500.0, inventory, 0.001)
        assertEquals(300.0, cogs, 0.001)
        assertTrue(totals.balanced)
        assertEquals(totals.debit, totals.credit, 0.001)
    }
}
