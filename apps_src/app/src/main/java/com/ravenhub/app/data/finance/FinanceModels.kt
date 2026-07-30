package com.ravenhub.app.data.finance

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TransactionType {
    INCOME, EXPENSE
}

@Serializable
enum class ExpenseCategory {
    FOOD, TRANSPORT, ENTERTAINMENT, SHOPPING, BILLS, SALARY, INVESTMENT, GIFT, OTHER
}

@Serializable
data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class FinanceData(
    val expenses: List<ExpenseItem> = emptyList()
)
