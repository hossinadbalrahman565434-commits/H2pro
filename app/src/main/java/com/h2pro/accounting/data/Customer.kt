package com.h2pro.accounting.data

data class Customer(
    val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val balance: Double = 0.0
)
