package com.ravenhub.app.ui.mainscreens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravenhub.app.data.finance.ExpenseCategory
import com.ravenhub.app.data.finance.ExpenseItem
import com.ravenhub.app.data.finance.TransactionType
import com.ravenhub.app.ui.component.CustomBottomSheet
import com.ravenhub.app.ui.component.ExpressiveList
import com.ravenhub.app.ui.component.ExpressiveListItem
import com.ravenhub.app.ui.component.RootAppDialog
import com.ravenhub.app.ui.viewmodel.FinanceViewModel
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: FinanceViewModel = viewModel()) {
    val data by viewModel.data.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Overview, 1 = History & Analytics
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showAddTransaction by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }

    // Confirmation dialog state
    var itemToDelete by remember { mutableStateOf<ExpenseItem?>(null) }

    val calendar = remember { Calendar.getInstance() }
    val currentMonth = remember { calendar.get(Calendar.MONTH) }
    val currentYear = remember { calendar.get(Calendar.YEAR) }

    val monthlyTransactions = remember(data.expenses, currentMonth, currentYear) {
        data.expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }

    val totalIncome = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(monthlyTransactions) {
        monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            val isBlurEnabled = com.ravenhub.app.ui.component.LocalBlurEnabled.current
            val hazeState = com.ravenhub.app.ui.component.LocalAppHazeState.current

            FloatingActionButton(
                onClick = {
                    editingExpense = null
                    showAddTransaction = true
                },
                shape = CircleShape,
                containerColor = if (isBlurEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(if (isBlurEnabled) 0.dp else 6.dp),
                modifier = Modifier
                    .padding(bottom = 100.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .then(
                        if (isBlurEnabled && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) {
                                blurEffect {
                                    blurRadius = 24.dp
                                }
                            }
                        } else Modifier
                    )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add transaction", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))

            // Sub Navigation Tab Bar
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Overview", fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("History & Analytics", fontWeight = FontWeight.Bold)
                }
            }

            if (selectedTab == 0) {
                // --- TAB 0: OVERVIEW ---
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary Balance Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Monthly Net Balance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = currencyFormat.format(netBalance),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = if (netBalance >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Income (+)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                                    Text(currencyFormat.format(totalIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Expenses (-)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                    Text(currencyFormat.format(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Category filter chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All") }
                            )
                        }
                        items(ExpenseCategory.entries.toList()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    val filteredList = if (selectedCategory != null) monthlyTransactions.filter { it.category == selectedCategory } else monthlyTransactions

                    if (filteredList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Payments, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("No transactions this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        ExpressiveList(
                            content = filteredList.sortedByDescending { it.createdAt }.map { expense ->
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            editingExpense = expense
                                            showAddTransaction = true
                                        },
                                        onLongClick = { itemToDelete = expense },
                                        headlineContent = {
                                            Text(
                                                text = expense.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "${expense.category.name.lowercase().replaceFirstChar { it.uppercase() }} · ${if (expense.type == TransactionType.INCOME) "Income" else "Expense"}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = if (expense.type == TransactionType.INCOME) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (expense.type == TransactionType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                            )
                                        },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = (if (expense.type == TransactionType.INCOME) "+" else "-") + currencyFormat.format(expense.amount),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (expense.type == TransactionType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                                )
                                                IconButton(onClick = {
                                                    editingExpense = expense
                                                    showAddTransaction = true
                                                }, modifier = Modifier.size(40.dp)) {
                                                    Icon(Icons.Rounded.Edit, "Edit", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { itemToDelete = expense }, modifier = Modifier.size(40.dp)) {
                                                    Icon(Icons.Rounded.Close, "Delete", Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(80.dp))
                }
            } else {
                // --- TAB 1: HISTORY & ANALYTICS ---
                HistoryAnalyticsSubPage(
                    allExpenses = data.expenses,
                    currencyFormat = currencyFormat,
                    onEdit = { expense ->
                        editingExpense = expense
                        showAddTransaction = true
                    },
                    onDelete = { expense -> itemToDelete = expense }
                )
            }
        }
    }

    // Confirmation Delete Modal (Item 5)
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete '${item.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add/Edit Transaction Bottom Sheet
    RootAppDialog {
        AddTransactionSheet(
            visible = showAddTransaction,
            expenseToEdit = editingExpense,
            onDismiss = { showAddTransaction = false },
            onSave = { title, amount, type, category ->
                if (editingExpense != null) {
                    viewModel.updateTransaction(editingExpense!!.id, title, amount, type, category)
                } else {
                    viewModel.addTransaction(title, amount, type, category)
                }
                showAddTransaction = false
            }
        )
    }
}

@Composable
private fun HistoryAnalyticsSubPage(
    allExpenses: List<ExpenseItem>,
    currencyFormat: NumberFormat,
    onEdit: (ExpenseItem) -> Unit,
    onDelete: (ExpenseItem) -> Unit
) {
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }

    // Group transactions by Month
    val monthlyGroups = remember(allExpenses) {
        allExpenses.groupBy { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.createdAt }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            Pair(year, month)
        }.toList().sortedWith(compareByDescending<Pair<Pair<Int, Int>, List<ExpenseItem>>> { it.first.first }.thenByDescending { it.first.second })
    }

    var selectedGroupIndex by remember { mutableIntStateOf(0) }

    if (monthlyGroups.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Analytics, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Text("No history data available yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val activeGroup = monthlyGroups.getOrNull(selectedGroupIndex) ?: monthlyGroups[0]
    val (yearMonth, items) = activeGroup

    val calendar = remember(yearMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, yearMonth.first)
            set(Calendar.MONTH, yearMonth.second)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val monthLabel = remember(calendar) { monthFormat.format(calendar.time) }

    val monthIncome = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val monthExpense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val monthDifference = monthIncome - monthExpense

    var showTransactionList by remember(selectedGroupIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Selector Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(monthlyGroups.size) { idx ->
                val (ym, _) = monthlyGroups[idx]
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, ym.first)
                    set(Calendar.MONTH, ym.second)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val label = monthFormat.format(cal.time)
                FilterChip(
                    selected = selectedGroupIndex == idx,
                    onClick = { selectedGroupIndex = idx },
                    label = { Text(label) }
                )
            }
        }

        // Detailed Summary Card for Selected Month
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(monthLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Income", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                        Text(currencyFormat.format(monthIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text(currencyFormat.format(monthExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (monthDifference >= 0) "Net Gain" else "Net Loss",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(kotlin.math.abs(monthDifference)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (monthDifference >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }

                    Icon(
                        imageVector = if (monthDifference >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = if (monthDifference >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Collapsible Transactions List
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().clickable { showTransactionList = !showTransactionList }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Transactions (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (showTransactionList) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = showTransactionList) {
            ExpressiveList(
                content = items.sortedByDescending { it.createdAt }.map { expense ->
                    {
                        ExpressiveListItem(
                            onClick = {},
                            headlineContent = {
                                Text(
                                    text = expense.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text("${expense.category.name.lowercase().replaceFirstChar { it.uppercase() }} · ${if (expense.type == TransactionType.INCOME) "Income" else "Expense"}")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (expense.type == TransactionType.INCOME) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (expense.type == TransactionType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = (if (expense.type == TransactionType.INCOME) "+" else "-") + currencyFormat.format(expense.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (expense.type == TransactionType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun AddTransactionSheet(
    visible: Boolean,
    expenseToEdit: ExpenseItem? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, TransactionType, ExpenseCategory) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf(ExpenseCategory.OTHER) }

    LaunchedEffect(visible, expenseToEdit) {
        if (visible) {
            if (expenseToEdit != null) {
                title = expenseToEdit.title
                amountText = if (expenseToEdit.amount % 1.0 == 0.0) expenseToEdit.amount.toLong().toString() else expenseToEdit.amount.toString()
                type = expenseToEdit.type
                category = expenseToEdit.category
            } else {
                title = ""
                amountText = ""
                type = TransactionType.EXPENSE
                category = ExpenseCategory.OTHER
            }
        }
    }

    val amount = amountText.toDoubleOrNull()

    CustomBottomSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = if (expenseToEdit != null) "Edit Transaction" else "Add Transaction",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // Type Selector Toggle (Income vs Expense)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            SegmentedButton(
                selected = type == TransactionType.EXPENSE,
                onClick = { type = TransactionType.EXPENSE },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Expense (-)", fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = type == TransactionType.INCOME,
                onClick = { type = TransactionType.INCOME },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Income (+)", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Description", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    amountText = input
                }
            },
            label = { Text("Amount (Rp)", color = MaterialTheme.colorScheme.onSurfaceVariant) }, singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ExpenseCategory.entries.toList()) { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (title.isNotBlank() && amount != null && amount > 0) onSave(title.trim(), amount, type, category) },
            enabled = title.isNotBlank() && amount != null && amount > 0,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp)
        ) {
            Text(if (expenseToEdit != null) "Save Changes" else "Add Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}
