package com.ravenhub.app.data.finance

import android.content.Context
import com.ravenhub.app.security.SecureStorageEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object FinanceDataManager {
    private val _data = MutableStateFlow(FinanceData())
    val data = _data.asStateFlow()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var isLoaded = false

    @Synchronized
    fun load(context: Context) {
        val loaded = SecureStorageEngine.loadFinanceSync(context)
        if (loaded != null) {
            _data.value = loaded
            isLoaded = true
        }
    }

    @Synchronized
    private fun persist(context: Context, newData: FinanceData) {
        if (!isLoaded) {
            load(context)
        }
        _data.value = newData
        ioScope.launch {
            SecureStorageEngine.saveFinanceSync(context, newData)
        }
    }

    fun addTransaction(
        context: Context,
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory
    ) {
        val expense = ExpenseItem(
            title = title,
            amount = amount,
            type = type,
            category = category
        )
        persist(context, _data.value.copy(expenses = _data.value.expenses + expense))
    }

    fun updateTransaction(
        context: Context,
        id: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory
    ) {
        persist(
            context,
            _data.value.copy(
                expenses = _data.value.expenses.map { expense ->
                    if (expense.id == id) expense.copy(title = title, amount = amount, type = type, category = category) else expense
                }
            )
        )
    }

    fun deleteExpense(context: Context, id: String) {
        persist(context, _data.value.copy(expenses = _data.value.expenses.filter { it.id != id }))
    }
}
