package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracey.data.ChatMessageEntity
import com.example.tracey.domain.FinancialSnapshot
import com.example.tracey.domain.TraceyKnowledgeEngine
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun AskTraceyTab(
    viewModel: TraceyViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val selectedWallet by viewModel.selectedWallet.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val toolItems by viewModel.toolItems.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val financialSnapshot = remember(currentUser, expenses, wallets, monthlyBudget, toolItems) {
        val totalIncome = expenses.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpenses = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
        val pendingExpenses = expenses.filter { it.type == "Expense" && it.status == "Pending" }.sumOf { it.amount }

        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH)
        val curYear = now.get(Calendar.YEAR)
        val itemCal = Calendar.getInstance()

        val thisMonthExpenses = expenses
            .filter { it.type == "Expense" }
            .filter {
                itemCal.timeInMillis = it.dateMillis
                itemCal.get(Calendar.MONTH) == curMonth && itemCal.get(Calendar.YEAR) == curYear
            }
            .sumOf { it.amount }

        val walletBalances = wallets.associateWith { w ->
            val wIncome = expenses.filter { it.walletName == w && it.type == "Income" }.sumOf { it.amount }
            val wExpense = expenses.filter { it.walletName == w && it.type == "Expense" }.sumOf { it.amount }
            wIncome - wExpense
        }

        val categoryTotals = expenses
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        FinancialSnapshot(
            username = currentUser,
            totalBalance = totalIncome - totalExpenses,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            pendingExpenses = pendingExpenses,
            monthlyBudget = monthlyBudget,
            thisMonthExpenses = thisMonthExpenses,
            wallets = wallets,
            walletBalances = walletBalances,
            categoryTotals = categoryTotals,
            goalsCount = toolItems.size,
            goalsCurrentTotal = toolItems.sumOf { it.currentAmount },
            goalsTargetTotal = toolItems.sumOf { it.targetAmount }
        )
    }

    val defaultWallet = remember(selectedWallet, wallets) {
        if (selectedWallet == "All Wallets") wallets.firstOrNull() ?: "Main Wallet" else selectedWallet
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        viewModel.saveChatMessage(isUser = true, text = trimmed)

        val response = TraceyKnowledgeEngine.processQuery(
            rawQuery = trimmed,
            snapshot = financialSnapshot,
            defaultWallet = defaultWallet
        )

        val logged = response.loggedTransaction
        if (logged != null) {
            viewModel.addTransaction(
                title = logged.title,
                amount = logged.amount,
                type = logged.type,
                walletName = logged.wallet,
                category = logged.category,
                status = "Paid",
                dateMillis = System.currentTimeMillis(),
                notes = "Logged via Ask Tracey"
            )
            viewModel.saveChatMessage(
                isUser = false,
                text = response.replyText,
                loggedTitle = logged.title,
                loggedAmount = logged.amount,
                loggedType = logged.type,
                loggedCategory = logged.category,
                loggedWallet = logged.wallet
            )
        } else {
            viewModel.saveChatMessage(isUser = false, text = response.replyText)
        }

        inputText = ""
        coroutineScope.launch {
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.lastIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PersianMistBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PersianCoatGrey),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Ask Tracey",
                        tint = CharcoalPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ask Tracey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepGraphite
                    )
                    Text(
                        text = "Smart Budget, Insights & Quick-Log Advisor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedFurGrey
                    )
                }
            }

            if (chatMessages.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearChatHistory() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat History",
                        tint = MutedFurGrey
                    )
                }
            }
        }

        HorizontalDivider(color = SilverBorder)

        // Chat History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "welcome_banner") {
                WelcomeBubble(username = currentUser)
            }

            items(chatMessages) { msg ->
                PersistedChatBubble(message = msg)
            }
        }

        // Quick Prompt Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TraceyKnowledgeEngine.quickPrompts) { prompt ->
                SuggestionChip(
                    onClick = { sendMessage(prompt) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = PureWhiteSurface,
                        labelColor = DeepGraphite
                    ),
                    border = BorderStroke(1.dp, SilverBorder),
                    label = {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Ask budget advice or \"Spent 150 on Lunch\"",
                        color = MutedFurGrey
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhiteSurface,
                    unfocusedContainerColor = PureWhiteSurface,
                    focusedBorderColor = CharcoalPrimary,
                    unfocusedBorderColor = SilverBorder
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { sendMessage(inputText) },
                enabled = inputText.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = CharcoalPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
private fun WelcomeBubble(username: String) {
    val greeting = if (username.isNotBlank()) "Meow, $username!" else "Meow! I'm Tracey."
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = PureWhiteSurface,
            contentColor = DeepGraphite,
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            border = BorderStroke(1.dp, SilverBorder),
            modifier = Modifier.widthIn(max = 325.dp)
        ) {
            Text(
                text = "$greeting I know your live wallet balances, monthly budget, and spending categories. Ask me for financial insights, affordability checks, or type \"Spent 120 on Coffee\" to log a transaction directly.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PersistedChatBubble(message: ChatMessageEntity) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (message.isUser) CharcoalPrimary else PureWhiteSurface
    val contentColor = if (message.isUser) Color.White else DeepGraphite
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = bubbleShape,
            border = if (message.isUser) null else BorderStroke(1.dp, SilverBorder),
            modifier = Modifier.widthIn(max = 325.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!message.isUser && message.loggedAmount != null && message.loggedTitle != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = PersianMistBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SilverBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SagePaid,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.loggedType ?: "Entry"}: ${message.loggedTitle} • ${
                                    String.format(Locale.US, "₱%,.2f", message.loggedAmount)
                                } (${message.loggedWallet ?: "Wallet"})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepGraphite
                            )
                        }
                    }
                }
            }
        }
    }
}