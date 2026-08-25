package com.h2pro.accounting.data

/**
 * نقطة دخول بسيطة لطبقة البيانات في الإصدار الأول.
 * يمكن استبدال التخزين الداخلي لاحقاً بـ Room دون تغيير واجهة التطبيق.
 */
object H2proDatabase {
    private val customers = mutableListOf<Customer>()

    fun addCustomer(customer: Customer) {
        customers += customer.copy(id = (customers.size + 1).toLong())
    }

    fun getCustomers(): List<Customer> = customers.toList()
}
