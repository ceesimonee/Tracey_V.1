package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tracey.data.Expense
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel

@Composable
fun TransactionsTab(
    viewModel: TraceyViewModel,
    onEditTransaction: (Expense) -> Unit
) {
    val allExpenses by viewModel.expenses.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("All") } // All, Paid, Pending
    var showPeriodSheet by remember { mutableStateOf(false) }
    var showMonthDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Expense?>(null) }

    val filteredList = remember(allExpenses, selectedWallet, selectedPeriod, selectedMonth, selectedYear, searchQuery, statusFilter) {
        filterTransactionsByPeriod(allExpenses, selectedWallet, selectedPeriod, selectedMonth, selectedYear)
            .filter { item ->
                (statusFilter == "All" || item.status.equals(statusFilter, ignoreCase = true)) &&
                        (searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                item.category.contains(searchQuery, ignoreCase = true) ||
                                item.notes.contains(searchQuery, ignoreCase = true))
            }
    }

    val totalExpense = filteredList.filter { it.type == "Expense" }.sumOf { it.amount }
    val totalIncome = filteredList.filter { it.type == "Income" }.sumOf { it.amount }
    val netTotal = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
    ) {
        // Header + Search Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGraphite
                )
                IconButton(onClick = {
                    isSearching = !isSearching
                    if (!isSearching) searchQuery = ""
                }) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CharcoalPrimary
                    )
                }
            }

            if (isSearching) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, category, or notes...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Wallet & Period Filter Row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Cycle Wallet Filter Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier.clickable {
                        val options = listOf("All Wallets") + wallets
                        val nextIdx = (options.indexOf(selectedWallet) + 1) % options.size
                        viewModel.selectWallet(options[nextIdx])
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp), tint = CharcoalPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedWallet, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Period Filter Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier.clickable { showPeriodSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedPeriod, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedPeriod == "Year") "$selectedYear" else "${getMonthNameFull(selectedMonth).take(3)} $selectedYear",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Compact 3-Metric Summary Bar (Expense / Income / Net)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PersianCoatGrey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = SoftRoseDelete, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(formatCurrency(totalExpense), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = SoftRoseDelete)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = SagePaid, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(formatCurrency(totalIncome), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = SagePaid)
                    }
                    Text(
                        text = "= ${formatCurrency(netTotal)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                }
            }
        }

        // Status Filter Chips (All / Paid / Pending)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Paid", "Pending").forEach { st ->
                    val selected = statusFilter == st
                    FilterChip(
                        selected = selected,
                        onClick = { statusFilter = st },
                        label = { Text(st) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CharcoalPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = PureWhiteSurface,
                            labelColor = MutedFurGrey
                        )
                    )
                }
            }
        }

        // Transaction Cards or Empty State
        if (filteredList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PersianCoatGrey,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(34.dp), tint = CharcoalPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Nothing recorded this period.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Tap + to add a transaction.", style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                val isPaid = item.status.equals("Paid", ignoreCase = true)
                val isIncome = item.type.equals("Income", ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditTransaction(item) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PersianCoatGrey,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(getCategoryVector(item.category), contentDescription = null, tint = CharcoalPrimary, modifier = Modifier.size(22.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${item.walletName} • ${item.category} • ${formatDateShort(item.dateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedFurGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isIncome) "+" else "-") + formatCurrency(item.amount),
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) SagePaid else DeepGraphite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPaid) SagePaidBg else AmberPendingBg,
                                    modifier = Modifier.clickable {
                                        val toggled = if (isPaid) "Pending" else "Paid"
                                        viewModel.updateExpense(item.copy(status = toggled))
                                    }
                                ) {
                                    Text(
                                        text = item.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPaid) SagePaid else AmberPending
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { itemToDelete = item }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MutedFurGrey, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
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

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = PureWhiteSurface,
            title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${itemToDelete!!.title}\" (${formatCurrency(itemToDelete!!.amount)})?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(itemToDelete!!)
                    itemToDelete = null
                }) {
                    Text("Delete", color = SoftRoseDelete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel", color = MutedFurGrey) }
            }
        )
    }
}