package com.example.tracey.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracey.data.ChatMessageEntity
import com.example.tracey.data.Expense
import com.example.tracey.data.ToolItem
import com.example.tracey.data.TraceyDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class TraceyViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TraceyDatabase.getDatabase(application).expenseDao()
    private val prefs = application.getSharedPreferences("tracey_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(prefs.getString("logged_in_user", "") ?: "")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    val expenses: StateFlow<List<Expense>> = _currentUser
        .flatMapLatest { user ->
            if (user.isEmpty()) flowOf(emptyList())
            else dao.getExpensesByUser(user)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val toolItems: StateFlow<List<ToolItem>> = _currentUser
        .flatMapLatest { user ->
            if (user.isEmpty()) flowOf(emptyList())
            else dao.getToolItemsByUser(user)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user.isEmpty()) flowOf(emptyList())
            else dao.getChatMessagesByUser(user)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Budget
    private val _monthlyBudget = MutableStateFlow(loadUserBudget(_currentUser.value))
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    // Multi-Wallet State
    private val _wallets = MutableStateFlow(loadUserWallets(_currentUser.value))
    val wallets: StateFlow<List<String>> = _wallets.asStateFlow()

    private val _selectedWallet = MutableStateFlow("All Wallets")
    val selectedWallet: StateFlow<String> = _selectedWallet.asStateFlow()

    // Time Period & Month/Year Filter State
    private val _selectedPeriod = MutableStateFlow("Month")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val now = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(now.get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(now.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private fun loadUserBudget(username: String): Double {
        if (username.isEmpty()) return 5000.0
        return prefs.getFloat("monthly_budget_$username", 5000f).toDouble()
    }

    private fun loadUserWallets(username: String): List<String> {
        if (username.isEmpty()) return listOf("Main Wallet")
        val raw = prefs.getString("wallets_$username", "Main Wallet|GCash") ?: "Main Wallet|GCash"
        return raw.split("|").filter { it.isNotBlank() }.ifEmpty { listOf("Main Wallet") }
    }

    private fun saveUserWallets(username: String, list: List<String>) {
        prefs.edit().putString("wallets_$username", list.joinToString("|")).apply()
        _wallets.value = list
    }

    fun selectWallet(wallet: String) {
        _selectedWallet.value = wallet
    }

    fun addWallet(name: String) {
        val clean = name.trim()
        val user = _currentUser.value
        val exists = _wallets.value.any { it.equals(clean, ignoreCase = true) }
        if (clean.isNotEmpty() && user.isNotEmpty() && !exists && !clean.equals("All Wallets", ignoreCase = true)) {
            saveUserWallets(user, _wallets.value + clean)
            _selectedWallet.value = clean
        }
    }

    fun renameWallet(oldName: String, newName: String) {
        val clean = newName.trim()
        val user = _currentUser.value
        if (clean.isEmpty() || user.isEmpty() || oldName == clean) return
        val duplicate = _wallets.value.any { it != oldName && it.equals(clean, ignoreCase = true) }
        if (duplicate || clean.equals("All Wallets", ignoreCase = true)) return

        val updated = _wallets.value.map { if (it == oldName) clean else it }
        saveUserWallets(user, updated)
        if (_selectedWallet.value == oldName) _selectedWallet.value = clean
        viewModelScope.launch {
            expenses.value.filter { it.walletName == oldName }.forEach {
                dao.updateExpense(it.copy(walletName = clean))
            }
        }
    }

    fun deleteWallet(walletName: String) {
        val user = _currentUser.value
        val currentList = _wallets.value
        if (user.isEmpty() || currentList.size <= 1 || !currentList.contains(walletName)) return

        val updated = currentList.filterNot { it == walletName }
        val fallbackWallet = updated.first()
        saveUserWallets(user, updated)

        if (_selectedWallet.value == walletName || _selectedWallet.value !in updated) {
            _selectedWallet.value = "All Wallets"
        }

        viewModelScope.launch {
            expenses.value.filter { it.walletName == walletName }.forEach {
                dao.updateExpense(it.copy(walletName = fallbackWallet))
            }
        }
    }

    fun setTimePeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setMonthYear(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun register(username: String, password: String): String? {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) return "Please fill in all fields."
        if (password.length < 4) return "Password must be at least 4 characters."
        if (prefs.getString("user_pass_$cleanUser", null) != null) {
            return "Username already exists. Please sign in."
        }
        prefs.edit()
            .putString("user_pass_$cleanUser", password)
            .putString("logged_in_user", cleanUser)
            .apply()
        _currentUser.value = cleanUser
        _monthlyBudget.value = loadUserBudget(cleanUser)
        _wallets.value = loadUserWallets(cleanUser)
        _selectedWallet.value = "All Wallets"
        return null
    }

    fun login(username: String, password: String): String? {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) return "Please enter both username and password."
        val savedPassword = prefs.getString("user_pass_$cleanUser", null)
            ?: return "Account not found. Please register first."
        if (savedPassword != password) return "Incorrect password."

        prefs.edit().putString("logged_in_user", cleanUser).apply()
        _currentUser.value = cleanUser
        _monthlyBudget.value = loadUserBudget(cleanUser)
        _wallets.value = loadUserWallets(cleanUser)
        _selectedWallet.value = "All Wallets"
        return null
    }

    fun logout() {
        prefs.edit().remove("logged_in_user").apply()
        _currentUser.value = ""
    }

    fun updateMonthlyBudget(newBudget: Double) {
        val user = _currentUser.value
        if (user.isNotEmpty()) {
            prefs.edit().putFloat("monthly_budget_$user", newBudget.toFloat()).apply()
            _monthlyBudget.value = newBudget
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        walletName: String,
        category: String,
        status: String,
        dateMillis: Long,
        notes: String = ""
    ) {
        val user = _currentUser.value
        if (user.isEmpty()) return
        viewModelScope.launch {
            dao.insertExpense(
                Expense(
                    username = user,
                    title = title.trim(),
                    amount = amount,
                    type = type,
                    walletName = walletName,
                    category = category,
                    status = status,
                    dateMillis = dateMillis,
                    notes = notes.trim()
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { dao.updateExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { dao.deleteExpense(expense) }
    }

    // Money Tools Actions
    fun addToolItem(
        toolType: String,
        title: String,
        subtitle: String,
        targetAmount: Double,
        currentAmount: Double = 0.0
    ) {
        val user = _currentUser.value
        if (user.isEmpty()) return
        viewModelScope.launch {
            dao.insertToolItem(
                ToolItem(
                    username = user,
                    toolType = toolType,
                    title = title.trim(),
                    subtitle = subtitle.trim(),
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    isCompleted = currentAmount >= targetAmount && targetAmount > 0
                )
            )
        }
    }

    fun updateToolItem(item: ToolItem) {
        viewModelScope.launch { dao.updateToolItem(item) }
    }

    fun deleteToolItem(item: ToolItem) {
        viewModelScope.launch { dao.deleteToolItem(item) }
    }

    // Chat Persistence Actions
    fun saveChatMessage(
        isUser: Boolean,
        text: String,
        loggedTitle: String? = null,
        loggedAmount: Double? = null,
        loggedType: String? = null,
        loggedCategory: String? = null,
        loggedWallet: String? = null
    ) {
        val user = _currentUser.value
        if (user.isEmpty()) return
        viewModelScope.launch {
            dao.insertChatMessage(
                ChatMessageEntity(
                    username = user,
                    isUser = isUser,
                    text = text,
                    loggedTitle = loggedTitle,
                    loggedAmount = loggedAmount,
                    loggedType = loggedType,
                    loggedCategory = loggedCategory,
                    loggedWallet = loggedWallet
                )
            )
        }
    }

    fun clearChatHistory() {
        val user = _currentUser.value
        if (user.isEmpty()) return
        viewModelScope.launch {
            dao.clearChatMessagesForUser(user)
        }
    }
}