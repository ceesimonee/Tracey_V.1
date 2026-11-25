package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tracey.data.Expense
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel

@Composable
fun HomeTab(
    viewModel: TraceyViewModel,
    onQuickAdd: (String) -> Unit,
    onEditTransaction: (Expense) -> Unit
) {
    val allExpenses by viewModel.expenses.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showPeriodSheet by remember { mutableStateOf(false) }
    var showMonthDialog by remember { mutableStateOf(false) }
    var showNewWalletDialog by remember { mutableStateOf(false) }
    var renameWalletTarget by remember { mutableStateOf<String?>(null) }
    var showMilestonesDialog by remember { mutableStateOf(false) }
    var showLoungeDialog by remember { mutableStateOf(false) }
    var activeBreakdownType by remember { mutableStateOf("Expense") }

    val periodTransactions = remember(allExpenses, selectedWallet, selectedPeriod, selectedMonth, selectedYear) {
        filterTransactionsByPeriod(allExpenses, selectedWallet, selectedPeriod, selectedMonth, selectedYear)
    }

    val totalExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
    }
    val totalIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == "Income" }.sumOf { it.amount }
    }
    val netChange = totalIncome - totalExpense

    val breakdownItems = remember(periodTransactions, activeBreakdownType) {
        periodTransactions
            .filter { it.type == activeBreakdownType }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Milestones Badge Count
    val unlockedBadges = remember(allExpenses, wallets, monthlyBudget, totalExpense) {
        var count = 0
        if (allExpenses.isNotEmpty()) count++
        if (allExpenses.size >= 5) count++
        if (wallets.size >= 2) count++
        if (totalExpense <= monthlyBudget && allExpenses.any { it.type == "Expense" }) count++
        count
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
    ) {
        // 1. Top Gamified Pills (Milestones & Tracey's Lounge)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmberPendingBg,
                    border = BorderStroke(1.dp, AmberPending.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable { showMilestonesDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AmberPending, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Milestones ($unlockedBadges/4)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGraphite
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier.clickable { showLoungeDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = CharcoalPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tracey's Lounge",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGraphite
                        )
                    }
                }
            }
        }

        // 2. Mascot & Greeting Speech Bubble
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TraceyCatMascot(
                    modifier = Modifier
                        .size(86.dp)
                        .clickable { showLoungeDialog = true }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Hi, $currentUser!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DeepGraphite
                        )
                        Text(
                            text = if (totalExpense > monthlyBudget) "Watch out—over monthly limit!" else "Purring smoothly on budget.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedFurGrey
                        )
                    }
                }
            }
        }

        // 3. Horizontal Multi-Wallet Strip + "+ New Wallet" Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val allBalance = allExpenses.filter { it.type == "Income" }.sumOf { it.amount } -
                        allExpenses.filter { it.type == "Expense" }.sumOf { it.amount }
                WalletCard(
                    name = "All Wallets",
                    balance = allBalance,
                    isSelected = selectedWallet == "All Wallets",
                    canEdit = false,
                    onClick = { viewModel.selectWallet("All Wallets") },
                    onEditClick = {}
                )

                wallets.forEach { wallet ->
                    val walletBalance = allExpenses.filter { it.walletName == wallet && it.type == "Income" }.sumOf { it.amount } -
                            allExpenses.filter { it.walletName == wallet && it.type == "Expense" }.sumOf { it.amount }
                    WalletCard(
                        name = wallet,
                        balance = walletBalance,
                        isSelected = selectedWallet == wallet,
                        canEdit = true,
                        onClick = { viewModel.selectWallet(wallet) },
                        onEditClick = { renameWalletTarget = wallet }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PersianCoatGrey,
                    border = BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier
                        .width(136.dp)
                        .height(94.dp)
                        .clickable { showNewWalletDialog = true }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Wallet", tint = CharcoalPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New wallet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGraphite
                        )
                    }
                }
            }
        }

        // 4. Time Period & Month/Year Selector Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier.clickable { showPeriodSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedPeriod, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MutedFurGrey)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                if (selectedPeriod == "Month" || selectedPeriod == "Year") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhiteSurface,
                        border = BorderStroke(1.dp, SilverBorder),
                        modifier = Modifier.clickable { showMonthDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedPeriod == "Year") "$selectedYear" else "${getMonthNameFull(selectedMonth)} $selectedYear",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 5. Net Change Card (Expense vs Income Summary)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PureWhiteSurface,
                border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net change",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedFurGrey
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PersianCoatGrey,
                            modifier = Modifier.clickable { onQuickAdd("Income") }
                        ) {
                            Text(
                                text = "+ Add Income",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepGraphite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = (if (netChange >= 0) "+" else "-") + formatCurrency(kotlin.math.abs(netChange)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netChange >= 0) SagePaid else SoftRoseDelete
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PersianMistBackground,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeBreakdownType = "Expense" }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Expense", style = MaterialTheme.typography.labelMedium, color = SoftRoseDelete, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SoftRoseDelete, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(formatCurrency(totalExpense), fontWeight = FontWeight.Bold, color = DeepGraphite)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = PersianMistBackground,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeBreakdownType = "Income" }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Income", style = MaterialTheme.typography.labelMedium, color = SagePaid, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SagePaid, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(formatCurrency(totalIncome), fontWeight = FontWeight.Bold, color = DeepGraphite)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Expense / Income Category Breakdown Pills & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Expense", "Income").forEach { tabType ->
                        val isSelected = activeBreakdownType == tabType
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) CharcoalPrimary else PersianCoatGrey,
                            modifier = Modifier.clickable { activeBreakdownType = tabType }
                        ) {
                            Text(
                                text = tabType,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else DeepGraphite
                            )
                        }
                    }
                }
            }
        }

        if (breakdownItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No ${activeBreakdownType.lowercase()} recorded for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedFurGrey
                        )
                    }
                }
            }
        } else {
            items(breakdownItems) { (category, sum) ->
                val denominator = if (activeBreakdownType == "Expense") totalExpense else totalIncome
                val pct = if (denominator > 0) (sum / denominator).toFloat() else 0f

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PersianCoatGrey,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(getCategoryVector(category), contentDescription = null, tint = CharcoalPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(category, fontWeight = FontWeight.SemiBold, color = DeepGraphite)
                            }
                            Text(
                                text = "${formatCurrency(sum)} (${(pct * 100).toInt()}%)",
                                fontWeight = FontWeight.Bold,
                                color = DeepGraphite
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (activeBreakdownType == "Expense") CharcoalPrimary else SagePaid,
                            trackColor = PersianCoatGrey
                        )
                    }
                }
            }
        }
    }

    if (showPeriodSheet) {
        TimePeriodBottomSheet(
            selectedPeriod = selectedPeriod,
            onSelectPeriod = { viewModel.setTimePeriod(it) },
            onDismiss = { showPeriodSheet = false }
        )
    }

    if (showMonthDialog) {
        MonthYearPickerDialog(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            onConfirm = { m, y -> viewModel.setMonthYear(m, y) },
            onDismiss = { showMonthDialog = false }
        )
    }

    if (showNewWalletDialog) {
        var newWalletName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewWalletDialog = false },
            containerColor = PureWhiteSurface,
            title = { Text("Create New Wallet", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newWalletName,
                    onValueChange = { newWalletName = it },
                    label = { Text("Wallet Name (e.g., Maya, Cash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addWallet(newWalletName)
                        showNewWalletDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewWalletDialog = false }) { Text("Cancel", color = MutedFurGrey) }
            }
        )
    }

    if (renameWalletTarget != null) {
        val targetWallet = renameWalletTarget!!
        var editedName by remember(targetWallet) { mutableStateOf(targetWallet) }
        var confirmDelete by remember(targetWallet) { mutableStateOf(false) }
        val canDelete = wallets.size > 1
        val fallbackWallet = wallets.firstOrNull { it != targetWallet } ?: "Main Wallet"

        AlertDialog(
            onDismissRequest = { renameWalletTarget = null },
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = if (confirmDelete) "Delete \"$targetWallet\"?" else "Manage Wallet",
                    fontWeight = FontWeight.Bold,
                    color = DeepGraphite
                )
            },
            text = {
                if (confirmDelete) {
                    Text(
                        text = "This wallet will be removed and its existing transactions will be moved to $fallbackWallet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedFurGrey
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Wallet Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (canDelete) {
                            OutlinedButton(
                                onClick = { confirmDelete = true },
                                border = BorderStroke(1.dp, SoftRoseDelete.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = SoftRoseDelete,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Delete Wallet",
                                    color = SoftRoseDelete,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = "At least one wallet must remain active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedFurGrey
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (confirmDelete) {
                    Button(
                        onClick = {
                            viewModel.deleteWallet(targetWallet)
                            renameWalletTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRoseDelete)
                    ) { Text("Delete", color = androidx.compose.ui.graphics.Color.White) }
                } else {
                    Button(
                        enabled = editedName.trim().isNotEmpty(),
                        onClick = {
                            viewModel.renameWallet(targetWallet, editedName)
                            renameWalletTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
                    ) { Text("Update") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (confirmDelete) confirmDelete = false else renameWalletTarget = null
                    }
                ) {
                    Text(if (confirmDelete) "Back" else "Cancel", color = MutedFurGrey)
                }
            }
        )
    }

    if (showMilestonesDialog) {
        val milestones = listOf(
            Triple("First Pawprint", "Record your first expense or income", allExpenses.isNotEmpty()),
            Triple("Consistent Tracker", "Log 5 or more transactions", allExpenses.size >= 5),
            Triple("Multi-Wallet Pro", "Maintain 2 or more wallets", wallets.size >= 2),
            Triple("Budget Guardian", "Keep monthly spending within budget", totalExpense <= monthlyBudget && allExpenses.any { it.type == "Expense" })
        )
        AlertDialog(
            onDismissRequest = { showMilestonesDialog = false },
            containerColor = PureWhiteSurface,
            title = { Text("Tracey Milestones", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    milestones.forEach { (title, desc, unlocked) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (unlocked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (unlocked) SagePaid else MutedFurGrey
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, color = DeepGraphite)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMilestonesDialog = false }) { Text("Done", color = CharcoalPrimary) }
            }
        )
    }

    if (showLoungeDialog) {
        val ratio = if (monthlyBudget > 0) (totalExpense / monthlyBudget) else 0.0
        val moodTitle = when {
            ratio == 0.0 -> "Tracey is Napping"
            ratio < 0.75 -> "Tracey is Purring Happily!"
            ratio <= 1.0 -> "Tracey's Ears Are Perked Up"
            else -> "Tracey Wants You to Slow Down!"
        }
        val moodDesc = when {
            ratio == 0.0 -> "No spending logged yet this period. Tap + whenever you make a purchase."
            ratio < 0.75 -> "You've used ${(ratio * 100).toInt()}% of your monthly budget. Great financial discipline!"
            ratio <= 1.0 -> "You're at ${(ratio * 100).toInt()}% of your budget. Keep an eye on non-essentials."
            else -> "You've exceeded your monthly budget of ${formatCurrency(monthlyBudget)}."
        }
        AlertDialog(
            onDismissRequest = { showLoungeDialog = false },
            containerColor = PureWhiteSurface,
            title = { Text("Tracey's Lounge", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TraceyCatMascot(modifier = Modifier.size(110.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(moodTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(moodDesc, style = MaterialTheme.typography.bodySmall, color = MutedFurGrey, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoungeDialog = false }) { Text("Close", color = CharcoalPrimary) }
            }
        )
    }
}

@Composable
private fun WalletCard(
    name: String,
    balance: Double,
    isSelected: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) CharcoalPrimary else PureWhiteSurface,
        border = BorderStroke(1.dp, if (isSelected) CharcoalPrimary else SilverBorder),
        modifier = Modifier
            .width(152.dp)
            .height(94.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) androidx.compose.ui.graphics.Color.White else CharcoalPrimary
                )
                if (canEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Wallet",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onEditClick() },
                        tint = if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f) else MutedFurGrey
                    )
                }
            }
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f) else MutedFurGrey,
                    maxLines = 1
                )
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else DeepGraphite,
                    maxLines = 1
                )
            }
        }
    }
}