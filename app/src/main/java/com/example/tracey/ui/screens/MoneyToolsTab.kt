package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import com.example.tracey.data.ToolItem
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel
import java.util.Locale

@Composable
fun MoneyToolsTab(viewModel: TraceyViewModel) {
    var activeTool by remember { mutableStateOf<String?>(null) }
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val toolItems by viewModel.toolItems.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAddToolItemDialog by remember { mutableStateOf(false) }
    var itemToAddAmount by remember { mutableStateOf<ToolItem?>(null) }

    if (activeTool == null) {
        // Main Money Tools Directory
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            item {
                Text("Money Tools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = DeepGraphite)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select a tool to organize your finances", style = MaterialTheme.typography.bodyMedium, color = MutedFurGrey)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ToolDirectoryCard(
                    title = "Budget",
                    subtitle = "Current limit: ${formatCurrency(monthlyBudget)} / month",
                    icon = Icons.Default.PieChart,
                    onClick = { showBudgetDialog = true }
                )
            }
            item {
                val savingsCount = toolItems.count { it.toolType == "Savings" }
                ToolDirectoryCard(
                    title = "Savings",
                    subtitle = "Track your personal savings goals ($savingsCount active)",
                    icon = Icons.Default.Savings,
                    onClick = { activeTool = "Savings" }
                )
            }
            item {
                val debtCount = toolItems.count { it.toolType == "Debt" && !it.isCompleted }
                ToolDirectoryCard(
                    title = "Debt",
                    subtitle = "Track and clear your debts ($debtCount pending)",
                    icon = Icons.Default.CreditCardOff,
                    onClick = { activeTool = "Debt" }
                )
            }
            item {
                val lendCount = toolItems.count { it.toolType == "LendBorrow" && !it.isCompleted }
                ToolDirectoryCard(
                    title = "Lend / Borrow",
                    subtitle = "Track money you've lent or borrowed ($lendCount open)",
                    icon = Icons.Default.Handshake,
                    onClick = { activeTool = "LendBorrow" }
                )
            }
        }
    } else {
        // Interactive Sub-Tool Screen (Savings / Debt / LendBorrow)
        val filteredTools = toolItems.filter { it.toolType == activeTool }
        val toolTitle = when (activeTool) {
            "Savings" -> "Savings Goals"
            "Debt" -> "Debt Tracker"
            else -> "Lend / Borrow"
        }

        Scaffold(
            containerColor = PersianMistBackground,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddToolItemDialog = true },
                    containerColor = CharcoalPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Entry") }
                )
            }
        ) { pad ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { activeTool = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CharcoalPrimary)
                        }
                        Text(toolTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (filteredTools.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PureWhiteSurface,
                            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No entries yet", fontWeight = FontWeight.SemiBold)
                                Text("Tap + Add Entry to create one.", style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                            }
                        }
                    }
                } else {
                    items(filteredTools, key = { it.id }) { tool ->
                        val progress = if (tool.targetAmount > 0) (tool.currentAmount / tool.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PureWhiteSurface,
                            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tool.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (tool.subtitle.isNotBlank()) {
                                            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (tool.isCompleted) SagePaidBg else AmberPendingBg
                                    ) {
                                        Text(
                                            text = if (tool.isCompleted) "Completed" else "In Progress",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tool.isCompleted) SagePaid else AmberPending
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = if (tool.isCompleted) SagePaid else CharcoalPrimary,
                                    trackColor = PersianCoatGrey
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatCurrency(tool.currentAmount)} / ${formatCurrency(tool.targetAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!tool.isCompleted) {
                                            OutlinedButton(
                                                onClick = { itemToAddAmount = tool },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, SilverBorder)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Amount",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = CharcoalPrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Add",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CharcoalPrimary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { viewModel.deleteToolItem(tool) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MutedFurGrey, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Amount Dialog for Savings / Debt / Lend & Borrow
    itemToAddAmount?.let { targetTool ->
        var amountInput by remember(targetTool.id) { mutableStateOf("") }
        val remaining = (targetTool.targetAmount - targetTool.currentAmount).coerceAtLeast(0.0)
        val parsedAddedValue = amountInput.replace(",", "").toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { itemToAddAmount = null },
            containerColor = PureWhiteSurface,
            title = {
                Text(
                    text = "Add Amount to ${targetTool.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepGraphite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current: ${formatCurrency(targetTool.currentAmount)} / ${formatCurrency(targetTool.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedFurGrey
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount to Add (₱)") },
                        placeholder = { Text("e.g. 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (remaining > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PersianCoatGrey,
                            modifier = Modifier.clickable {
                                amountInput = String.format(Locale.US, "%.2f", remaining)
                            }
                        ) {
                            Text(
                                text = "Fill Remaining (${formatCurrency(remaining)})",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepGraphite
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = parsedAddedValue > 0.0,
                    onClick = {
                        if (parsedAddedValue > 0.0) {
                            val nextAmount = (targetTool.currentAmount + parsedAddedValue).coerceAtMost(targetTool.targetAmount)
                            viewModel.updateToolItem(
                                targetTool.copy(
                                    currentAmount = nextAmount,
                                    isCompleted = nextAmount >= targetTool.targetAmount
                                )
                            )
                            itemToAddAmount = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToAddAmount = null }) {
                    Text("Cancel", color = MutedFurGrey)
                }
            }
        )
    }

    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(monthlyBudget.toInt().toString()) }
        val spentThisMonth = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
        val parsedBudget = budgetInput.replace(",", "").toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            containerColor = PureWhiteSurface,
            title = { Text("Monthly Budget Planner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total expenses logged: ${formatCurrency(spentThisMonth)}", style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Monthly Limit (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = parsedBudget > 0.0,
                    onClick = {
                        if (parsedBudget > 0.0) {
                            viewModel.updateMonthlyBudget(parsedBudget)
                            showBudgetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel", color = MutedFurGrey) }
            }
        )
    }

    if (showAddToolItemDialog && activeTool != null) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("") }

        val entryLabel = when (activeTool) {
            "Savings" -> "Savings Goal"
            "Debt" -> "Debt"
            else -> "Lend / Borrow"
        }
        val parsedTarget = targetText.replace(",", "").toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { showAddToolItemDialog = false },
            containerColor = PureWhiteSurface,
            title = { Text("New $entryLabel Entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (activeTool == "Savings") "Goal Name" else "Title / Person") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (activeTool == "LendBorrow") {
                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("Type (e.g., Lent to friend / Borrowed)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Target Amount (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = title.trim().isNotEmpty() && parsedTarget > 0.0,
                    onClick = {
                        if (title.trim().isNotEmpty() && parsedTarget > 0.0) {
                            viewModel.addToolItem(activeTool!!, title.trim(), subtitle.trim(), parsedTarget)
                            showAddToolItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalPrimary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddToolItemDialog = false }) { Text("Cancel", color = MutedFurGrey) }
            }
        )
    }
}

@Composable
private fun ToolDirectoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = PersianCoatGrey,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = CharcoalPrimary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepGraphite)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedFurGrey)
        }
    }
}