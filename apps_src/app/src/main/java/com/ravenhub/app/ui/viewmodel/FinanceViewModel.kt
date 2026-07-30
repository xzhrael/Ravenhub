package com.ravenhub.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ravenhub.app.data.finance.*
import kotlinx.coroutines.flow.StateFlow

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    val data: StateFlow<FinanceData> = FinanceDataManager.data

    fun reload() {
        FinanceDataManager.load(getApplication())
    }

    fun addTransaction(title: String, amount: Double, type: TransactionType, category: ExpenseCategory) {
        FinanceDataManager.addTransaction(getApplication(), title, amount, type, category)
    }

    fun updateTransaction(id: String, title: String, amount: Double, type: TransactionType, category: ExpenseCategory) {
        FinanceDataManager.updateTransaction(getApplication(), id, title, amount, type, category)
    }

    fun deleteExpense(id: String) {
        FinanceDataManager.deleteExpense(getApplication(), id)
    }
}
