package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tracey.data.Expense
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainHubScreen(
    viewModel: TraceyViewModel,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showReceiptScanner by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Expense?>(null) }
    var defaultTransactionType by remember { mutableStateOf("Expense") }

    val wallets by viewModel.wallets.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()

    Scaffold(
        containerColor = PersianMistBackground,
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Receipt Camera FAB
                    SmallFloatingActionButton(
                        onClick = { showReceiptScanner = true },
                        shape = CircleShape,
                        containerColor = PureWhiteSurface,
                        contentColor = CharcoalPrimary
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Scan Receipt")
                    }

                    // Manual Add FAB
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            defaultTransactionType = "Expense"
                            showTransactionDialog = true
                        },
                        shape = CircleShape,
                        containerColor = CharcoalPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(26.dp))
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhiteSurface,
                tonalElevation = 0.dp
            ) {
                val navItems = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("History", Icons.Default.ReceiptLong, 1),
                    Triple("Ask Tracey", Icons.Default.Pets, 2),
                    Triple("Tools", Icons.Default.AccountBalanceWallet, 3),
                    Triple("Settings", Icons.Default.Settings, 4)
                )
                navItems.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CharcoalPrimary,
                            selectedTextColor = CharcoalPrimary,
                            indicatorColor = PersianCoatGrey,
                            unselectedIconColor = MutedFurGrey,
                            unselectedTextColor = MutedFurGrey
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeTab(
                    viewModel = viewModel,
                    onQuickAdd = { type ->
                        editingTransaction = null
                        defaultTransactionType = type
                        showTransactionDialog = true
                    },
                    onEditTransaction = {
                        editingTransaction = it
                        showTransactionDialog = true
                    }
                )
                1 -> TransactionsTab(
                    viewModel = viewModel,
                    onEditTransaction = {
                        editingTransaction = it
                        showTransactionDialog = true
                    }
                )
                2 -> AskTraceyTab(viewModel = viewModel)
                3 -> MoneyToolsTab(viewModel = viewModel)
                4 -> SettingsTab(
                    viewModel = viewModel,
                    onNavigateAbout = onNavigateAbout,
                    onLogout = onLogout
                )
            }
        }
    }

    if (showReceiptScanner) {
        ReceiptScannerDialog(
            wallets = wallets,
            initialWallet = if (selectedWallet == "All Wallets") wallets.firstOrNull() ?: "Main Wallet" else selectedWallet,
            onDismiss = { showReceiptScanner = false },
            onSaveTransaction = { title, amount, type, wallet, category, status, dateMillis, notes ->
                viewModel.addTransaction(title, amount, type, wallet, category, status, dateMillis, notes)
            }
        )
    }

    if (showTransactionDialog) {
        TransactionFormDialog(
            existing = editingTransaction,
            initialType = defaultTransactionType,
            wallets = wallets,
            initialWallet = if (selectedWallet == "All Wallets") wallets.firstOrNull() ?: "Main Wallet" else selectedWallet,
            onDismiss = { showTransactionDialog = false },
            onSave = { title, amount, type, wallet, category, status, dateMillis, notes ->
                if (editingTransaction == null) {
                    viewModel.addTransaction(title, amount, type, wallet, category, status, dateMillis, notes)
                } else {
                    viewModel.updateExpense(
                        editingTransaction!!.copy(
                            title = title,
                            amount = amount,
                            type = type,
                            walletName = wallet,
                            category = category,
                            status = status,
                            dateMillis = dateMillis,
                            notes = notes
                        )
                    )
                }
                showTransactionDialog = false
            }
        )
    }
}

/**
 * Shared Add / Edit Transaction Dialog (Supports Expense & Income, Wallets, Status, DatePicker)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    existing: Expense?,
    initialType: String,
    wallets: List<String>,
    initialWallet: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, String, Long, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(existing?.type ?: initialType) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountText by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var selectedWallet by remember { mutableStateOf(existing?.walletName ?: initialWallet) }

    val expenseCategories = listOf("Food", "Transport", "Bills", "School", "Shopping", "Other")
    val incomeCategories = listOf("Allowance", "Salary", "Freelance", "Gift", "Other")
    val activeCategories = if (selectedType == "Income") incomeCategories else expenseCategories

    var selectedCategory by remember {
        mutableStateOf(
            existing?.category?.takeIf { activeCategories.contains(it) } ?: activeCategories.first()
        )
    }
    var selectedStatus by remember { mutableStateOf(existing?.status ?: "Paid") }
    var selectedDateMillis by remember { mutableStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhiteSurface,
        title = {
            Text(
                text = if (existing == null) "Record Transaction" else "Edit Transaction",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Expense vs Income Segmented Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Expense", "Income").forEach { typeOption ->
                        val isSelected = selectedType == typeOption
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CharcoalPrimary else PersianCoatGrey,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedType = typeOption
                                    selectedCategory = if (typeOption == "Income") incomeCategories.first() else expenseCategories.first()
                                }
                        ) {
                            Text(
                                text = typeOption,
                                modifier = Modifier.padding(vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else DeepGraphite,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(if (selectedType == "Income") "Income Source" else "Expense Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMsg = null },
                    label = { Text("Amount (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Wallet", style = MaterialTheme.typography.labelMedium, color = MutedFurGrey)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wallets.forEach { w ->
                        FilterChip(
                            selected = selectedWallet == w,
                            onClick = { selectedWallet = w },
                            label = { Text(w) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CharcoalPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelMedium, color = MutedFurGrey)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CharcoalPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Text("Status", style = MaterialTheme.typography.labelMedium, color = MutedFurGrey)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Paid", "Pending").forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (st == "Paid") SagePaid else AmberPending,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date: ${formatDateShort(selectedDateMillis)}", color = DeepGraphite)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = SoftRoseDelete, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isBlank()) {
                        errorMsg = "Please enter a title."
                    } else if (amount == null || amount <= 0) {
                        errorMsg = "Please enter a valid amount."
                    } else {
                        onSave(title, amount, selectedType, selectedWallet, selectedCategory, selectedStatus, selectedDateMillis, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedFurGrey)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("OK", color = CharcoalPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MutedFurGrey)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Shared Time Period Bottom Sheet (Day, Week, Month, Year, All Time)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePeriodBottomSheet(
    selectedPeriod: String,
    onSelectPeriod: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val periods = listOf(
        Pair("Day", Icons.Default.Today),
        Pair("Week", Icons.Default.DateRange),
        Pair("Month", Icons.Default.CalendarMonth),
        Pair("Year", Icons.Default.Event),
        Pair("All Time", Icons.Default.AllInclusive)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PureWhiteSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Time Period",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepGraphite
            )
            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                periods.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (label, icon) ->
                            val isSelected = selectedPeriod == label
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) PersianCoatGrey else PureWhiteSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CharcoalPrimary else SilverBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(86.dp)
                                    .clickable {
                                        onSelectPeriod(label)
                                        onDismiss()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) CharcoalPrimary else MutedFurGrey,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = DeepGraphite
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Shared Month & Year Grid Picker Dialog
 */
@Composable
fun MonthYearPickerDialog(
    selectedMonth: Int,
    selectedYear: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempYear by remember { mutableIntStateOf(selectedYear) }
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhiteSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Select Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { tempYear-- }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Year")
                    }
                    Text(
                        text = tempYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                    IconButton(onClick = { tempYear++ }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Year")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                months.chunked(3).forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthName ->
                            val monthIndex = rowIndex * 3 + colIndex
                            val isSelected = monthIndex == selectedMonth && tempYear == selectedYear
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) CharcoalPrimary else PersianMistBackground,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onConfirm(monthIndex, tempYear)
                                        onDismiss()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else DeepGraphite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MutedFurGrey)
            }
        }
    )
}

/**
 * Shared Date Filtering & Formatting Helpers
 */
fun filterTransactionsByPeriod(
    list: List<Expense>,
    wallet: String,
    period: String,
    month: Int,
    year: Int
): List<Expense> {
    val walletFiltered = if (wallet == "All Wallets") list else list.filter { it.walletName == wallet }
    if (period == "All Time") return walletFiltered

    val cal = Calendar.getInstance()
    val todayCal = Calendar.getInstance()

    return walletFiltered.filter { item ->
        cal.timeInMillis = item.dateMillis
        when (period) {
            "Day" -> cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
            "Week" -> cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.WEEK_OF_YEAR) == todayCal.get(Calendar.WEEK_OF_YEAR)
            "Month" -> cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            "Year" -> cal.get(Calendar.YEAR) == year
            else -> true
        }
    }
}

fun getMonthNameFull(monthIndex: Int): String {
    val names = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    return names.getOrElse(monthIndex) { "November" }
}

fun formatCurrency(amount: Double): String {
    return String.format(Locale.US, "₱%,.2f", amount)
}

fun formatDateShort(millis: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
}

fun getCategoryVector(category: String): ImageVector {
    return when (category) {
        "Food" -> Icons.Default.Restaurant
        "Transport" -> Icons.Default.DirectionsCar
        "Bills" -> Icons.Default.Receipt
        "School" -> Icons.Default.MenuBook
        "Shopping" -> Icons.Default.ShoppingBag
        "Allowance", "Salary" -> Icons.Default.Work
        "Freelance" -> Icons.Default.LaptopMac
        "Gift" -> Icons.Default.CardGiftcard
        else -> Icons.Default.Payments
    }
}