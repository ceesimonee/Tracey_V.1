package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracey.data.ChatMessageEntity
import com.example.tracey.data.Expense
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel
import java.util.Locale

data class TraceyBotReply(
    val text: String,
    val loggedTitle: String? = null,
    val loggedAmount: Double? = null,
    val loggedType: String? = null,
    val loggedCategory: String? = null,
    val loggedWallet: String? = null
)

@Composable
fun AskTraceyTab(viewModel: TraceyViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val savedMessages by viewModel.chatMessages.collectAsState()

    var activeChatWallet by remember(selectedWallet, wallets) {
        mutableStateOf(
            if (selectedWallet == "All Wallets") wallets.firstOrNull() ?: "Main Wallet" else selectedWallet
        )
    }

    var inputText by remember { mutableStateOf("") }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to newest message whenever chat updates or tab opens
    LaunchedEffect(savedMessages.size) {
        if (savedMessages.isNotEmpty()) {
            listState.animateScrollToItem(savedMessages.size)
        }
    }

    fun sendUserMessage(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        viewModel.saveChatMessage(isUser = true, text = trimmed)
        inputText = ""

        val defaultTargetWallet = if (activeChatWallet == "All Wallets") {
            wallets.firstOrNull() ?: "Main Wallet"
        } else {
            activeChatWallet
        }

        val reply = processChatCommand(
            input = trimmed,
            selectedScopeWallet = activeChatWallet,
            defaultTargetWallet = defaultTargetWallet,
            wallets = wallets,
            expenses = expenses,
            monthlyBudget = monthlyBudget,
            onUpdateBudget = { newLimit ->
                viewModel.updateMonthlyBudget(newLimit)
            },
            onRecordTransaction = { title, amount, type, targetWallet, category, status ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    walletName = targetWallet,
                    category = category,
                    status = status,
                    dateMillis = System.currentTimeMillis(),
                    notes = "Logged via Tracey Chat"
                )
            }
        )

        viewModel.saveChatMessage(
            isUser = false,
            text = reply.text,
            loggedTitle = reply.loggedTitle,
            loggedAmount = reply.loggedAmount,
            loggedType = reply.loggedType,
            loggedCategory = reply.loggedCategory,
            loggedWallet = reply.loggedWallet
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Header Bar with Wallet Selector & Clear History Button
        Surface(
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    TraceyCatMascot(modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Ask Tracey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepGraphite)
                        Text("Offline Budget & Receipt Assistant", style = MaterialTheme.typography.labelSmall, color = MutedFurGrey)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Cycle Wallet Scope
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PersianCoatGrey,
                        modifier = Modifier.clickable {
                            val scopeOptions = wallets + "All Wallets"
                            val nextIdx = (scopeOptions.indexOf(activeChatWallet) + 1) % scopeOptions.size
                            activeChatWallet = scopeOptions[nextIdx]
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp), tint = CharcoalPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(activeChatWallet, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DeepGraphite)
                        }
                    }

                    if (savedMessages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = MutedFurGrey,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Persistent Chat Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Permanent Welcome Greeting Bubble
            item(key = "welcome_greeting") {
                ChatBubbleItem(
                    message = ChatMessageEntity(
                        username = currentUser,
                        isUser = false,
                        text = "Meow, $currentUser! Tell me what you spent or earned (e.g., \"lunch 150\" or \"received 200 on gcash\"), tap the camera icon to scan a receipt, or ask about your wallet balance!"
                    )
                )
            }

            items(savedMessages, key = { it.id }) { msg ->
                ChatBubbleItem(message = msg)
            }
        }

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Wallet balance?",
                "How much did I spend?",
                "Top category?",
                "Lunch 150",
                "Received 200 on GCash"
            )
            suggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder),
                    modifier = Modifier.clickable { sendUserMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepGraphite
                    )
                }
            }
        }

        // Bottom Input Bar + Camera Receipt Button
        Surface(
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PersianCoatGrey,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { showScannerDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Scan Receipt", tint = CharcoalPrimary, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("e.g. Dinner 180 or Wallet balance?", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendUserMessage(inputText) }),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = CharcoalPrimary,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { sendUserMessage(inputText) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showScannerDialog) {
        val scanTargetWallet = if (activeChatWallet == "All Wallets") {
            wallets.firstOrNull() ?: "Main Wallet"
        } else {
            activeChatWallet
        }
        ReceiptScannerDialog(
            wallets = wallets,
            initialWallet = scanTargetWallet,
            onDismiss = { showScannerDialog = false },
            onSaveTransaction = { title, amount, type, wallet, category, status, dateMillis, notes ->
                viewModel.addTransaction(title, amount, type, wallet, category, status, dateMillis, notes)
                viewModel.saveChatMessage(
                    isUser = false,
                    text = "Scanned and saved your receipt directly to $wallet!",
                    loggedTitle = title,
                    loggedAmount = amount,
                    loggedType = type,
                    loggedCategory = category,
                    loggedWallet = wallet
                )
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = PureWhiteSurface,
            title = { Text("Clear Chat History?", fontWeight = FontWeight.Bold) },
            text = { Text("This deletes your saved chat messages, but keeps all logged wallet transactions.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChatHistory()
                    showClearDialog = false
                }) {
                    Text("Clear", color = SoftRoseDelete, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = MutedFurGrey)
                }
            }
        )
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessageEntity) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = if (message.isUser) CharcoalPrimary else PureWhiteSurface,
            border = if (message.isUser) null else BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
            modifier = Modifier.widthIn(max = 295.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else DeepGraphite
                )

                if (message.loggedTitle != null && message.loggedAmount != null) {
                    val isIncome = message.loggedType == "Income"
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PersianMistBackground,
                        border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PersianCoatGrey,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getCategoryVector(message.loggedCategory ?: "Other"),
                                        contentDescription = null,
                                        tint = CharcoalPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(message.loggedTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("${message.loggedWallet} • ${message.loggedCategory}", style = MaterialTheme.typography.labelSmall, color = MutedFurGrey)
                            }
                            Text(
                                text = (if (isIncome) "+" else "-") + formatCurrency(message.loggedAmount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isIncome) SagePaid else DeepGraphite
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun processChatCommand(
    input: String,
    selectedScopeWallet: String,
    defaultTargetWallet: String,
    wallets: List<String>,
    expenses: List<Expense>,
    monthlyBudget: Double,
    onUpdateBudget: (Double) -> Unit,
    onRecordTransaction: (title: String, amount: Double, type: String, targetWallet: String, category: String, status: String) -> Unit
): TraceyBotReply {
    val lower = input.lowercase(Locale.getDefault())

    val mentionedWallet = wallets.firstOrNull { walletName ->
        lower.contains(walletName.lowercase(Locale.getDefault()))
    }
    val effectiveScopeWallet = mentionedWallet ?: selectedScopeWallet
    val resolvedLogWallet = mentionedWallet ?: defaultTargetWallet

    val scopedRecords = if (effectiveScopeWallet == "All Wallets") {
        expenses
    } else {
        expenses.filter { it.walletName.equals(effectiveScopeWallet, ignoreCase = true) }
    }

    val totalExpense = scopedRecords.filter { it.type == "Expense" }.sumOf { it.amount }
    val totalIncome = scopedRecords.filter { it.type == "Income" }.sumOf { it.amount }
    val actualWalletBalance = totalIncome - totalExpense
    val allExpensesSum = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
    val remainingMonthlyLimit = monthlyBudget - allExpensesSum

    val numberRegex = Regex("""(?:₱|php\s*)?(\d+(?:,\d{3})*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)

    if (lower.contains("set budget") || lower.contains("change budget") || lower.contains("update budget")) {
        val newLimit = numberRegex.find(input)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
        if (newLimit != null && newLimit > 0) {
            onUpdateBudget(newLimit)
            return TraceyBotReply(text = "Updated your monthly spending limit to ${formatCurrency(newLimit)}!")
        }
    }

    if (lower.contains("wallet") || lower.contains("balance") || lower.contains("remaining") || lower.contains("left") || lower.contains("money") || lower.contains("budget")) {
        val balanceLine = if (actualWalletBalance >= 0) {
            "In $effectiveScopeWallet, you currently have ${formatCurrency(actualWalletBalance)} (${formatCurrency(totalIncome)} income − ${formatCurrency(totalExpense)} spent)."
        } else {
            "In $effectiveScopeWallet, your balance is -${formatCurrency(kotlin.math.abs(actualWalletBalance))} (${formatCurrency(totalExpense)} spent vs. ${formatCurrency(totalIncome)} income)."
        }

        val limitLine = "\n\nYour optional monthly spending limit is ${formatCurrency(monthlyBudget)} (${formatCurrency(remainingMonthlyLimit)} left under cap). Say \"set budget 1000\" anytime to adjust it."

        return TraceyBotReply(text = balanceLine + limitLine)
    }

    if (lower.contains("spend") || lower.contains("spent") || lower.contains("total expense")) {
        return TraceyBotReply(
            text = "In $effectiveScopeWallet, you've spent ${formatCurrency(totalExpense)} across ${scopedRecords.count { it.type == "Expense" }} expense(s). Your remaining wallet balance is ${formatCurrency(actualWalletBalance)}."
        )
    }

    if (lower.contains("income") || lower.contains("earned")) {
        return TraceyBotReply(
            text = "In $effectiveScopeWallet, your total recorded income is ${formatCurrency(totalIncome)}."
        )
    }

    if (lower.contains("category") || lower.contains("most") || lower.contains("top") || lower.contains("breakdown")) {
        val topCat = scopedRecords
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .maxByOrNull { it.value }

        return if (topCat == null) {
            TraceyBotReply(text = "No expenses logged in $effectiveScopeWallet yet! Try typing \"Coffee 120\" to record one.")
        } else {
            TraceyBotReply(
                text = "Your highest spending category in $effectiveScopeWallet is ${topCat.key} at ${formatCurrency(topCat.value)}."
            )
        }
    }

    if (lower.contains("pending") || lower.contains("unpaid")) {
        val pendingItems = scopedRecords.filter { it.status.equals("Pending", ignoreCase = true) }
        val pendingSum = pendingItems.sumOf { it.amount }
        return if (pendingItems.isEmpty()) {
            TraceyBotReply(text = "Purrfect! You have zero pending bills in $effectiveScopeWallet.")
        } else {
            TraceyBotReply(
                text = "You have ${pendingItems.size} pending item(s) in $effectiveScopeWallet totaling ${formatCurrency(pendingSum)}."
            )
        }
    }

    val match = numberRegex.findAll(input).lastOrNull()
    val amount = match?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()

    if (amount != null && amount > 0) {
        val isIncome = listOf(
            "received", "recieved", "income", "salary", "allowance",
            "earned", "bonus", "refund", "got paid", "sweldo",
            "cash in", "cash-in", "top up", "topup", "baon", "padala", "deposited"
        ).any { lower.contains(it) }

        val type = if (isIncome) "Income" else "Expense"
        val status = if (listOf("pending", "unpaid", "due", "later").any { lower.contains(it) }) "Pending" else "Paid"

        val category = when {
            isIncome && lower.contains("salary") -> "Salary"
            isIncome && lower.contains("freelance") -> "Freelance"
            isIncome && lower.contains("gift") -> "Gift"
            isIncome -> "Allowance"
            listOf("food", "lunch", "dinner", "breakfast", "coffee", "snack", "milk tea", "jollibee", "mcdo", "grocery", "meal", "eat").any { lower.contains(it) } -> "Food"
            listOf("grab", "jeep", "trike", "tricycle", "bus", "gas", "fuel", "fare", "ride", "commute").any { lower.contains(it) } -> "Transport"
            listOf("bill", "wifi", "internet", "electric", "water", "rent", "load", "data").any { lower.contains(it) } -> "Bills"
            listOf("school", "print", "book", "tuition", "project", "paper", "pen").any { lower.contains(it) } -> "School"
            listOf("shop", "clothes", "shoes", "shirt", "bag", "lazada", "shopee").any { lower.contains(it) } -> "Shopping"
            else -> "Other"
        }

        var cleanedTitle = input.replace(match.value, "")
        if (mentionedWallet != null) {
            cleanedTitle = cleanedTitle.replace(Regex(Regex.escape(mentionedWallet), RegexOption.IGNORE_CASE), "")
        }
        cleanedTitle = cleanedTitle
            .replace(Regex("""\b(spent|paid|bought|for|on|in|to|from|via|received|recieved|earned|got|php|pesos|pending|unpaid)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .ifBlank { if (isIncome) "Cash In" else category }

        onRecordTransaction(cleanedTitle, amount, type, resolvedLogWallet, category, status)

        val walletIncomeBefore = expenses.filter { it.walletName.equals(resolvedLogWallet, ignoreCase = true) && it.type == "Income" }.sumOf { it.amount }
        val walletExpenseBefore = expenses.filter { it.walletName.equals(resolvedLogWallet, ignoreCase = true) && it.type == "Expense" }.sumOf { it.amount }
        val updatedWalletBalance = if (isIncome) {
            (walletIncomeBefore + amount) - walletExpenseBefore
        } else {
            walletIncomeBefore - (walletExpenseBefore + amount)
        }

        val actionVerb = if (isIncome) "Added" else "Deducted"
        val quip = "$actionVerb ${formatCurrency(amount)} under $category in $resolvedLogWallet. New $resolvedLogWallet balance: ${formatCurrency(updatedWalletBalance)}."

        return TraceyBotReply(
            text = quip,
            loggedTitle = cleanedTitle,
            loggedAmount = amount,
            loggedType = type,
            loggedCategory = category,
            loggedWallet = resolvedLogWallet
        )
    }

    return TraceyBotReply(
        text = "Try telling me an amount like \"Coffee 120\" or \"Received 200 on GCash\", tap the camera button to scan a receipt, or ask \"Wallet balance?\""
    )
}