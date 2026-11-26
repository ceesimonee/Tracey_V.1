package com.example.tracey.domain

import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

data class FinancialSnapshot(
    val username: String = "",
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val pendingExpenses: Double = 0.0,
    val monthlyBudget: Double = 5000.0,
    val thisMonthExpenses: Double = 0.0,
    val wallets: List<String> = emptyList(),
    val walletBalances: Map<String, Double> = emptyMap(),
    val categoryTotals: Map<String, Double> = emptyMap(),
    val goalsCount: Int = 0,
    val goalsCurrentTotal: Double = 0.0,
    val goalsTargetTotal: Double = 0.0
)

data class ParsedChatTransaction(
    val title: String,
    val amount: Double,
    val type: String,       // "Expense" or "Income"
    val category: String,
    val wallet: String
)

data class TraceyResponse(
    val replyText: String,
    val loggedTransaction: ParsedChatTransaction? = null
)

private data class KnowledgeTopic(
    val id: String,
    val phrases: List<String>,
    val keywords: List<String>,
    val answer: (FinancialSnapshot) -> String
)

object TraceyKnowledgeEngine {

    private fun Double.php(): String = String.format(Locale.US, "₱%,.2f", this)

    val quickPrompts: List<String> = listOf(
        "How do I start to save money?",
        "How is my financial health?",
        "Check my monthly budget",
        "Show my wallet balances",
        "Apply 50/30/20 to my income",
        "Where am I spending the most?",
        "How do I build an emergency fund?",
        "Snowball vs Avalanche debt",
        "Tips to stop impulse buying",
        "How do I budget my allowance?"
    )

    // Comprehensive Income Action & Source Keywords (App Store + PH E-Wallet Vocabulary)
    private val incomeTriggers = listOf(
        // Wallet Top-Ups & Transfers In
        "cash in", "cash-in", "cashin", "top up", "top-up", "topup",
        "reload", "reloaded", "deposit", "deposited", "credit", "credited",
        "add", "added", "plus", "fund", "funded", "refill", "replenish",
        "put in", "transfer in", "received", "receive", "incoming",
        "dagdag", "pasok", "hulog",
        // Earnings & Pay
        "earn", "earned", "earning", "earnings", "income", "make", "made",
        "salary", "salaries", "sweldo", "suweldo", "sahod", "paycheck",
        "payroll", "payday", "wage", "wages", "pay", "overtime", "ot pay",
        "13th month", "honorarium", "compensation", "stipend",
        // Allowance & Support
        "allowance", "baon", "pocket money", "sustento", "padala",
        "remittance", "support", "pension", "scholarship",
        // Freelance, Business & Investments
        "freelance", "freelancing", "client", "commission", "commissions",
        "project", "gig", "side hustle", "sideline", "raket", "consulting",
        "tutoring", "sale", "sales", "sold", "resell", "profit", "revenue",
        "business", "dividend", "dividends", "interest", "yield", "payout",
        // Gifts, Bonuses & Refunds
        "gift", "gifts", "regalo", "aguinaldo", "pamasko", "bonus", "bonuses",
        "reward", "rewards", "prize", "win", "won", "winnings", "raffle",
        "rebate", "cashback", "cash back", "refund", "refunded",
        "reimbursement", "reimbursed", "tip", "tips", "collect", "collected",
        "bayad utang", "repayment", "got"
    )

    // Comprehensive Expense Action & Category Keywords (App Store + PH Vocabulary)
    private val expenseTriggers = listOf(
        // Deduction & Spending Verbs
        "spent", "spend", "spending", "paid", "paying", "bought", "buy", "buying",
        "purchase", "purchased", "deduct", "deducted", "minus", "subtract",
        "bawas", "binawas", "gastos", "nagastos", "bayad", "binayaran",
        "expense", "expenses", "cost", "charged", "charge", "debit", "debited",
        "checkout", "checked out", "order", "ordered", "send", "sent",
        "transfer out", "cash out", "cash-out", "cashout", "withdraw", "withdrew",
        "loss", "lost", "donate", "donated", "lend", "lent", "pautang",
        // Food & Dining
        "food", "foods", "lunch", "dinner", "breakfast", "brunch", "supper",
        "snack", "snacks", "merienda", "meal", "meals", "eat", "eating", "ate",
        "dine", "dining", "restaurant", "resto", "fastfood", "fast food",
        "jollibee", "mcdo", "mcdonalds", "kfc", "chowking", "mang inasal",
        "starbucks", "coffee", "cafe", "kape", "milk tea", "milktea", "boba",
        "drink", "drinks", "beverage", "water", "juice", "soda", "softdrinks",
        "beer", "alcohol", "bar", "grocery", "groceries", "supermarket",
        "palengke", "market", "ulam", "rice", "bigas", "bread", "pandesal",
        "bakery", "cake", "dessert", "ice cream", "pizza", "burger", "chicken",
        "pork", "beef", "fish", "veggies", "fruits", "grabfood", "foodpanda",
        "takeout", "delivery", "canteen", "karinderya", "carinderia",
        // Transport & Vehicle
        "transport", "transportation", "transpo", "commute", "fare", "pamasahe",
        "jeep", "jeepney", "tricycle", "trike", "pedicab", "bus", "van", "uv",
        "train", "mrt", "lrt", "pnr", "taxi", "cab", "grab", "grabcar",
        "angkas", "joyride", "moveit", "move it", "habal", "gas", "gasoline",
        "diesel", "fuel", "petrol", "unleaded", "shell", "petron", "caltex",
        "oil", "engine oil", "vulcanize", "carwash", "car wash", "parking",
        "toll", "rfid", "autosweep", "easytrip", "flight", "airline", "ticket",
        "ferry", "boat", "ship", "travel", "trip",
        // Bills, Utilities & Subscriptions
        "bill", "bills", "utility", "utilities", "electric", "electricity",
        "kuryente", "meralco", "aleco", "water bill", "tubig", "primewater",
        "internet", "wifi", "fiber", "pldt", "globe", "converge", "dito",
        "smart", "tnt", "tm", "load", "promo", "data", "mobile load",
        "prepaid", "postpaid", "phone bill", "rent", "rental", "upa", "dorm",
        "dormitory", "boarding house", "condo", "association dues", "hoa",
        "subscription", "subscriptions", "netflix", "spotify", "youtube",
        "icloud", "google one", "chatgpt", "canva", "prime", "disney",
        "insurance", "sss", "philhealth", "pagibig", "tax", "taxes",
        "installment", "loan", "utang", "amortization", "credit card",
        "spaylater", "gcredit", "ggives", "billease", "home credit",
        "bank fee", "transfer fee", "service fee", "fee",
        // School & Education
        "school", "tuition", "enrollment", "misc fee", "miscellaneous",
        "book", "books", "textbook", "ebook", "module", "modules", "print",
        "printing", "photocopy", "xerox", "scan", "bind", "binding",
        "notebook", "notebooks", "paper", "bond paper", "pen", "ballpen",
        "pencil", "highlighter", "marker", "folder", "envelope", "calculator",
        "uniform", "lanyard", "thesis", "capstone", "research", "lab",
        "laboratory", "seminar", "workshop", "review", "reviewer", "exam",
        "board exam", "csc", "course", "class",
        // Shopping, Apparel & Personal Care
        "shop", "shopping", "store", "mall", "sm", "ayala", "robinsons",
        "lazada", "shopee", "tiktok shop", "shein", "temu", "amazon", "zalora",
        "uniqlo", "clothes", "clothing", "shirt", "tshirt", "pants", "jeans",
        "shorts", "dress", "jacket", "hoodie", "underwear", "socks", "shoes",
        "sneakers", "sandals", "slippers", "bag", "backpack", "wallet", "watch",
        "cap", "hat", "accessories", "jewelry", "ring", "perfume", "cologne",
        "fragrance", "skincare", "toner", "lotion", "sunscreen", "cleanser",
        "soap", "shampoo", "conditioner", "toothpaste", "deodorant", "makeup",
        "cosmetics", "haircut", "barber", "salon", "gadget", "gadgets",
        "phone", "charger", "cable", "earphones", "airpods", "headset",
        "mouse", "keyboard", "laptop", "case", "tempered glass", "appliance",
        "furniture", "decor", "hardware", "tools",
        // Health, Pets, Entertainment & Misc
        "medicine", "meds", "gamot", "pharmacy", "mercury drug", "watsons",
        "hospital", "clinic", "doctor", "dentist", "dental", "checkup",
        "vitamin", "vitamins", "gym", "fitness", "workout", "basketball",
        "sports", "movie", "cinema", "sine", "game", "games", "gaming",
        "steam", "valorant", "dias", "diamonds", "robux", "pet", "pets",
        "cat", "dog", "cat food", "dog food", "litter", "vet", "grooming",
        "laundry", "labada", "repair", "maintenance", "donation", "tithes",
        "offering", "emergency"
    )

    // Words stripped from the generated transaction title so titles stay clean
    private val noiseWords = listOf(
        "spent", "spend", "spending", "paid", "paying", "bought", "buy", "buying",
        "purchase", "purchased", "deduct", "deducted", "minus", "subtract",
        "bawas", "binawas", "gastos", "nagastos", "bayad", "binayaran",
        "earned", "earn", "earning", "received", "receive", "got",
        "add", "added", "plus", "log", "logged", "record", "recorded",
        "expense", "expenses", "income", "on", "for", "from", "to", "into",
        "in", "at", "via", "using", "with", "my", "the", "a", "an", "php", "pesos", "peso"
    )

    fun processQuery(
        rawQuery: String,
        snapshot: FinancialSnapshot,
        defaultWallet: String
    ): TraceyResponse {
        val query = rawQuery.trim()
        val lower = query.lowercase(Locale.getDefault())

        if (lower.isBlank()) {
            return TraceyResponse(
                "Ask me anything about saving money, budgeting, debt payoff, or type \"150 food\" or \"cash in 200\" to update your wallet."
            )
        }

        // 1. Dynamic Purchase Affordability Check ("Can I afford 1500?", "Should I buy shoes for 2500?")
        if (lower.containsAny("afford", "should i buy", "can i buy", "worth buying", "too expensive")) {
            val amount = extractAmount(lower)
            if (amount != null && amount > 0) {
                return TraceyResponse(evaluateAffordability(amount, snapshot))
            }
        }

        // 2. Dynamic Savings Goal Timeline ("How long to save 10000?", "Save up for 5000")
        if (lower.containsAny("how long to save", "how long will it take to save", "save up for", "reach my goal of")) {
            val target = extractAmount(lower)
            if (target != null && target > 0) {
                return TraceyResponse(evaluateSavingsTimeline(target, snapshot))
            }
        }

        // 3. Natural-Language & Shorthand Wallet Logging ("cash in 200", "150 food", "sweldo 15k", "-80 jeep")
        val parsedTx = tryParseTransaction(query, lower, snapshot.wallets, defaultWallet)
        if (parsedTx != null) {
            val updatedBalance = if (parsedTx.type == "Income") {
                snapshot.totalBalance + parsedTx.amount
            } else {
                snapshot.totalBalance - parsedTx.amount
            }
            val actionLabel = if (parsedTx.type == "Income") "Added to" else "Deducted from"
            val reply = buildString {
                appendLine("$actionLabel ${parsedTx.wallet}:")
                appendLine("• Item: ${parsedTx.title}")
                appendLine("• Amount: ${if (parsedTx.type == "Income") "+" else "-"}${parsedTx.amount.php()} (${parsedTx.category})")
                append("Updated Combined Balance: ${updatedBalance.php()}")
            }
            return TraceyResponse(replyText = reply, loggedTransaction = parsedTx)
        }

        // 4. Score against the Expanded Financial Knowledge Base
        val tokens = tokenize(lower)
        val bestTopic = knowledgeTopics
            .map { topic -> topic to scoreTopic(lower, tokens, topic) }
            .filter { (_, score) -> score >= 3 }
            .maxByOrNull { (_, score) -> score }
            ?.first

        if (bestTopic != null) {
            return TraceyResponse(bestTopic.answer(snapshot))
        }

        // 5. Smart Context-Aware Fallback
        return TraceyResponse(
            buildString {
                appendLine("I didn't find an exact topic for \"$query\", but here is your current snapshot:")
                appendLine("• Combined Balance: ${snapshot.totalBalance.php()}")
                appendLine("• This Month's Spending: ${snapshot.thisMonthExpenses.php()} / ${snapshot.monthlyBudget.php()} budget")
                appendLine()
                append("Try asking \"How do I start to save money?\", \"Check my monthly budget\", \"Can I afford 800?\", or type \"150 food\" / \"cash in 200\" to update your wallet.")
            }
        )
    }

    private fun tryParseTransaction(
        original: String,
        lower: String,
        wallets: List<String>,
        defaultWallet: String
    ): ParsedChatTransaction? {
        // Skip questions, hypotheticals, and budget ratio queries so they go to financial Q&A
        val questionStarters = listOf(
            "how ", "what ", "why ", "when ", "where ", "who ", "which ",
            "can ", "could ", "should ", "would ", "is ", "are ", "am ",
            "do ", "does ", "did ", "will ", "if ", "suppose ",
            "calculate ", "compute ", "check ", "explain ", "tell ", "show ", "help "
        )
        if (
            lower.endsWith("?") ||
            questionStarters.any { lower.startsWith(it) } ||
            lower.contains("50/30/20") ||
            lower.contains("50 30 20") ||
            lower.contains("70/20/10")
        ) {
            return null
        }

        val amount = extractAmount(lower) ?: return null
        if (amount <= 0.0) return null

        val startsWithPlus = lower.startsWith("+")
        val startsWithMinus = lower.startsWith("-")

        val incomeScore = incomeTriggers.count { containsWord(lower, it) } + (if (startsWithPlus) 2 else 0)
        val expenseScore = expenseTriggers.count { containsWord(lower, it) } + (if (startsWithMinus) 2 else 0)

        val wordCount = lower.split(Regex("""\s+""")).filter { it.isNotBlank() }.size
        val isShortEntry = wordCount in 1..6

        // Require at least one recognizable trigger or a concise shorthand entry (e.g. "150 haircut")
        if (incomeScore == 0 && expenseScore == 0 && !isShortEntry) {
            return null
        }

        // Determine Income vs Expense (explicit +/- or higher trigger match wins; ties default to Expense)
        val isIncome = when {
            startsWithPlus -> true
            startsWithMinus -> false
            incomeScore > expenseScore -> true
            else -> false
        }
        val type = if (isIncome) "Income" else "Expense"

        // Detect target wallet if mentioned in the message (e.g., "cash in 200 gcash" or "150 food main wallet")
        val matchedWallet = wallets
            .sortedByDescending { it.length }
            .firstOrNull { w -> containsWord(lower, w.lowercase(Locale.getDefault())) }
            ?: defaultWallet

        // Build a clean human-readable title
        var cleaned = original
            .replace(Regex("""^[+-]\s*"""), "")
            .replace(Regex("""(?<!\w)(?:₱|php)?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?\s*k?(?!\w)""", RegexOption.IGNORE_CASE), " ")
            .replace("₱", " ")

        noiseWords.forEach { word ->
            cleaned = cleaned.replace(Regex("""(?<!\w)${Regex.escape(word)}(?!\w)""", RegexOption.IGNORE_CASE), " ")
        }
        wallets.forEach { w ->
            cleaned = cleaned.replace(Regex("""(?<!\w)${Regex.escape(w)}(?!\w)""", RegexOption.IGNORE_CASE), " ")
        }

        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()

        val title = when {
            cleaned.isNotBlank() -> cleaned.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
            containsWord(lower, "cash in") || containsWord(lower, "cash-in") || containsWord(lower, "cashin") -> "Cash In"
            containsWord(lower, "top up") || containsWord(lower, "top-up") || containsWord(lower, "topup") -> "Top Up"
            containsWord(lower, "reload") -> "Wallet Reload"
            containsWord(lower, "deposit") -> "Deposit"
            isIncome -> "Wallet Top-Up"
            else -> "Wallet Expense"
        }

        val category = inferCategory(lower, isIncome)

        return ParsedChatTransaction(
            title = title,
            amount = amount,
            type = type,
            category = category,
            wallet = matchedWallet
        )
    }

    private fun inferCategory(lower: String, isIncome: Boolean): String {
        if (isIncome) {
            return when {
                containsAnyWord(lower, "allowance", "baon", "stipend", "pocket money", "sustento", "padala", "remittance", "support", "pension", "scholarship") -> "Allowance"
                containsAnyWord(lower, "salary", "salaries", "sweldo", "suweldo", "sahod", "paycheck", "payroll", "payday", "wage", "wages", "pay", "overtime", "ot pay", "13th month", "honorarium", "compensation") -> "Salary"
                containsAnyWord(lower, "freelance", "freelancing", "client", "commission", "commissions", "project", "gig", "side hustle", "sideline", "raket", "consulting", "tutoring", "sale", "sales", "sold", "resell", "profit", "revenue", "business", "dividend", "dividends", "interest", "yield", "payout") -> "Freelance"
                containsAnyWord(lower, "gift", "gifts", "regalo", "aguinaldo", "pamasko", "bonus", "bonuses", "reward", "rewards", "prize", "win", "won", "winnings", "raffle", "rebate", "cashback", "cash back", "refund", "refunded", "reimbursement", "reimbursed", "tip", "tips") -> "Gift"
                else -> "Other"
            }
        }
        return when {
            containsAnyWord(
                lower,
                "food", "foods", "lunch", "dinner", "breakfast", "brunch", "supper",
                "snack", "snacks", "merienda", "meal", "meals", "eat", "eating", "ate",
                "dine", "dining", "restaurant", "resto", "fastfood", "fast food",
                "jollibee", "mcdo", "mcdonalds", "kfc", "chowking", "mang inasal",
                "starbucks", "coffee", "cafe", "kape", "milk tea", "milktea", "boba",
                "drink", "drinks", "beverage", "water", "juice", "soda", "softdrinks",
                "beer", "alcohol", "bar", "grocery", "groceries", "supermarket",
                "palengke", "market", "ulam", "rice", "bigas", "bread", "pandesal",
                "bakery", "cake", "dessert", "ice cream", "pizza", "burger", "chicken",
                "pork", "beef", "fish", "veggies", "fruits", "grabfood", "foodpanda",
                "takeout", "delivery", "canteen", "karinderya", "carinderia"
            ) -> "Food"

            containsAnyWord(
                lower,
                "transport", "transportation", "transpo", "commute", "fare", "pamasahe",
                "jeep", "jeepney", "tricycle", "trike", "pedicab", "bus", "van", "uv",
                "train", "mrt", "lrt", "pnr", "taxi", "cab", "grab", "grabcar",
                "angkas", "joyride", "moveit", "move it", "habal", "gas", "gasoline",
                "diesel", "fuel", "petrol", "unleaded", "shell", "petron", "caltex",
                "oil", "engine oil", "vulcanize", "carwash", "car wash", "parking",
                "toll", "rfid", "autosweep", "easytrip", "flight", "airline", "ticket",
                "ferry", "boat", "ship", "travel", "trip"
            ) -> "Transport"

            containsAnyWord(
                lower,
                "bill", "bills", "utility", "utilities", "electric", "electricity",
                "kuryente", "meralco", "aleco", "water bill", "tubig", "primewater",
                "internet", "wifi", "fiber", "pldt", "globe", "converge", "dito",
                "smart", "tnt", "tm", "load", "promo", "data", "mobile load",
                "prepaid", "postpaid", "phone bill", "rent", "rental", "upa", "dorm",
                "dormitory", "boarding house", "condo", "association dues", "hoa",
                "subscription", "subscriptions", "netflix", "spotify", "youtube",
                "icloud", "google one", "chatgpt", "canva", "prime", "disney",
                "insurance", "sss", "philhealth", "pagibig", "tax", "taxes",
                "installment", "loan", "utang", "amortization", "credit card",
                "spaylater", "gcredit", "ggives", "billease", "home credit",
                "bank fee", "transfer fee", "service fee", "fee"
            ) -> "Bills"

            containsAnyWord(
                lower,
                "school", "tuition", "enrollment", "misc fee", "miscellaneous",
                "book", "books", "textbook", "ebook", "module", "modules", "print",
                "printing", "photocopy", "xerox", "scan", "bind", "binding",
                "notebook", "notebooks", "paper", "bond paper", "pen", "ballpen",
                "pencil", "highlighter", "marker", "folder", "envelope", "calculator",
                "uniform", "lanyard", "thesis", "capstone", "research", "lab",
                "laboratory", "seminar", "workshop", "review", "reviewer", "exam",
                "board exam", "csc", "course", "class"
            ) -> "School"

            containsAnyWord(
                lower,
                "shop", "shopping", "store", "mall", "sm", "ayala", "robinsons",
                "lazada", "shopee", "tiktok shop", "shein", "temu", "amazon", "zalora",
                "uniqlo", "clothes", "clothing", "shirt", "tshirt", "pants", "jeans",
                "shorts", "dress", "jacket", "hoodie", "underwear", "socks", "shoes",
                "sneakers", "sandals", "slippers", "bag", "backpack", "wallet", "watch",
                "cap", "hat", "accessories", "jewelry", "ring", "perfume", "cologne",
                "fragrance", "skincare", "toner", "lotion", "sunscreen", "cleanser",
                "soap", "shampoo", "conditioner", "toothpaste", "deodorant", "makeup",
                "cosmetics", "haircut", "barber", "salon", "gadget", "gadgets",
                "phone", "charger", "cable", "earphones", "airpods", "headset",
                "mouse", "keyboard", "laptop", "case", "tempered glass", "appliance",
                "furniture", "decor", "hardware", "tools"
            ) -> "Shopping"

            else -> "Other"
        }
    }

    private fun containsWord(text: String, term: String): Boolean {
        val pattern = Regex("""(?<!\w)${Regex.escape(term)}(?!\w)""", RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(text)
    }

    private fun containsAnyWord(text: String, vararg terms: String): Boolean {
        return terms.any { containsWord(text, it) }
    }

    private fun tokenize(lower: String): Set<String> {
        return lower
            .replace(Regex("""[^a-z0-9\s/]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun scoreTopic(lower: String, tokens: Set<String>, topic: KnowledgeTopic): Int {
        var score = 0
        for (phrase in topic.phrases) {
            if (lower.contains(phrase)) {
                score += 5
            }
        }
        for (kw in topic.keywords) {
            if (kw in tokens) {
                score += 2
            } else if (kw.length >= 5 && tokens.any { it.startsWith(kw) }) {
                score += 1
            }
        }
        return score
    }

    private val knowledgeTopics: List<KnowledgeTopic> = listOf(
        // 1. How to Start Saving Money
        KnowledgeTopic(
            id = "start_saving",
            phrases = listOf(
                "start to save", "start saving", "save money", "how to save",
                "begin saving", "ways to save", "tips to save", "saving tips", "save more"
            ),
            keywords = listOf("save", "saving", "savings", "start", "ipon", "money", "beginner"),
            answer = { snapshot ->
                val suggestedStarter = if (snapshot.totalBalance > 500) {
                    (snapshot.totalBalance * 0.15).coerceAtLeast(100.0)
                } else {
                    100.0
                }
                buildString {
                    appendLine("Starting to save is easiest when you keep it automatic and small at first:")
                    appendLine()
                    appendLine("1. Pay Yourself First")
                    appendLine("Move savings out of your spending wallet the moment you get allowance or income—before buying anything else. Even starting with ${suggestedStarter.php()} from your current ${snapshot.totalBalance.php()} balance builds momentum.")
                    appendLine()
                    appendLine("2. Separate Your Savings Wallet")
                    appendLine("Keep a dedicated \"Savings\" wallet in Tracey so your daily spending cash and savings never mix.")
                    appendLine()
                    appendLine("3. Use the 10% -> 20% Ramp-Up")
                    appendLine("Start by saving 10% of every peso you receive this month, then raise it to 15%–20% once your routine feels natural.")
                    appendLine()
                    append("4. Track Every Peso Daily: Logging small snacks and rides reveals quick leaks you can redirect straight into savings.")
                }
            }
        ),

        // 2. Live Financial Health & Overall Summary
        KnowledgeTopic(
            id = "financial_health",
            phrases = listOf("financial health", "how am i doing", "my status", "my overview", "my money", "net worth", "summary"),
            keywords = listOf("health", "status", "overview", "report", "summary", "doing"),
            answer = { snapshot ->
                val income = snapshot.totalIncome
                val expenses = snapshot.totalExpenses
                val net = income - expenses
                val savingsRate = if (income > 0) ((net / income) * 100.0) else 0.0

                buildString {
                    appendLine("Financial Health Overview:")
                    appendLine("• Combined Balance: ${snapshot.totalBalance.php()}")
                    appendLine("• Total Income: ${income.php()}")
                    appendLine("• Total Expenses: ${expenses.php()}")
                    if (snapshot.pendingExpenses > 0) {
                        appendLine("• Pending Items: ${snapshot.pendingExpenses.php()}")
                    }
                    if (income > 0) {
                        appendLine("• Net Savings Rate: ${String.format(Locale.US, "%.1f", savingsRate)}%")
                    }
                    if (snapshot.goalsCount > 0) {
                        appendLine("• Active Goals: ${snapshot.goalsCurrentTotal.php()} / ${snapshot.goalsTargetTotal.php()} (${snapshot.goalsCount} items)")
                    }
                    appendLine()
                    when {
                        income == 0.0 && expenses == 0.0 ->
                            append("Log your income and daily expenses to unlock personalized savings rate and spending insights.")
                        expenses > income ->
                            append("Warning: Total expenses exceed recorded income by ${(expenses - income).php()}. Review top categories to cut back.")
                        savingsRate >= 20.0 ->
                            append("Strong performance—saving ${String.format(Locale.US, "%.1f", savingsRate)}% of your income beats the standard 20% target.")
                        else ->
                            append("Positive cash flow, though below the 20% savings benchmark. Trimming small daily wants can lift your savings rate.")
                    }
                }
            }
        ),

        // 3. Monthly Budget & Daily Safe-to-Spend Check
        KnowledgeTopic(
            id = "monthly_budget",
            phrases = listOf("monthly budget", "budget left", "remaining budget", "safe to spend", "daily limit", "over budget", "my budget"),
            keywords = listOf("budget", "remaining", "limit", "monthly", "allowance"),
            answer = { snapshot ->
                val budget = snapshot.monthlyBudget
                val spent = snapshot.thisMonthExpenses
                val remaining = budget - spent
                val pctUsed = if (budget > 0) (spent / budget) * 100.0 else 0.0

                val cal = Calendar.getInstance()
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val today = cal.get(Calendar.DAY_OF_MONTH)
                val daysLeft = max(daysInMonth - today + 1, 1)
                val safePerDay = if (remaining > 0) remaining / daysLeft else 0.0

                buildString {
                    appendLine("Monthly Budget Status:")
                    appendLine("• Monthly Budget: ${budget.php()}")
                    appendLine("• Spent This Month: ${spent.php()} (${String.format(Locale.US, "%.1f", pctUsed)}%)")
                    appendLine("• Remaining Budget: ${remaining.php()}")
                    appendLine("• Safe Daily Limit ($daysLeft days left): ${safePerDay.php()}/day")
                    appendLine()
                    when {
                        remaining < 0 -> append("Alert: You exceeded your monthly budget by ${(-remaining).php()}. Pause non-essential spending for the rest of the month.")
                        pctUsed >= 85.0 -> append("Heads up: You've used over 85% of your monthly budget. Stick closely to your ${safePerDay.php()}/day limit.")
                        else -> append("You're on track! Keeping daily spending under ${safePerDay.php()} will finish the month within budget.")
                    }
                }
            }
        ),

        // 4. Wallet Balances & Multi-Wallet Strategy
        KnowledgeTopic(
            id = "wallet_balances",
            phrases = listOf("wallet balance", "wallet balances", "my wallets", "how much do i have", "organize wallets", "separate accounts"),
            keywords = listOf("wallet", "wallets", "gcash", "balance", "balances", "envelope"),
            answer = { snapshot ->
                buildString {
                    if (snapshot.wallets.isNotEmpty()) {
                        appendLine("Your Wallets (${snapshot.wallets.size}):")
                        snapshot.wallets.forEach { wallet ->
                            val bal = snapshot.walletBalances[wallet] ?: 0.0
                            appendLine("• $wallet: ${bal.php()}")
                        }
                        appendLine("• Combined Total: ${snapshot.totalBalance.php()}")
                        appendLine()
                    }
                    appendLine("Best Multi-Wallet Setup:")
                    appendLine("• Daily Wallet: Strictly for food, fare, and everyday cash.")
                    appendLine("• Bills Wallet: Reserved for rent, load, internet, and fixed obligations.")
                    append("• Savings Wallet: Untouched funds for emergencies and long-term goals.")
                }
            }
        ),

        // 5. 50/30/20 & Budgeting Methods
        KnowledgeTopic(
            id = "fifty_thirty_twenty",
            phrases = listOf("50/30/20", "50 30 20", "budget rule", "split my income", "how should i budget", "budgeting method", "zero based"),
            keywords = listOf("50/30/20", "allocate", "split", "rule", "method", "zero-based"),
            answer = { snapshot ->
                val baseIncome = when {
                    snapshot.totalIncome > 0 -> snapshot.totalIncome
                    snapshot.monthlyBudget > 0 -> snapshot.monthlyBudget
                    else -> 15000.0
                }
                val label = if (snapshot.totalIncome > 0) "recorded income (${baseIncome.php()})" else "monthly budget (${baseIncome.php()})"
                val needs = baseIncome * 0.50
                val wants = baseIncome * 0.30
                val savings = baseIncome * 0.20

                buildString {
                    appendLine("50/30/20 Split Based on Your $label:")
                    appendLine()
                    appendLine("• 50% Needs (${needs.php()}): Food, commute, utilities, school essentials, rent.")
                    appendLine("• 30% Wants (${wants.php()}): Dining out, shopping, hobbies, entertainment.")
                    appendLine("• 20% Savings & Debt (${savings.php()}): Emergency fund, goals, and debt payoff.")
                    appendLine()
                    append("Alternative: If your income is tight, try 70/20/10 (70% Essentials, 20% Wants, 10% Savings) until your income grows.")
                }
            }
        ),

        // 6. Top Spending & Expense Leak Analysis
        KnowledgeTopic(
            id = "top_spending",
            phrases = listOf("spending the most", "top expense", "top expenses", "where is my money", "expense breakdown", "highest category", "biggest expense"),
            keywords = listOf("spending", "overspending", "breakdown", "category", "categories", "leak", "leaks"),
            answer = { snapshot ->
                if (snapshot.categoryTotals.isEmpty()) {
                    "No expenses recorded yet. Once you add expenses under Food, Transport, Bills, School, or Shopping, I'll break down your top spending areas."
                } else {
                    val sorted = snapshot.categoryTotals.entries.sortedByDescending { it.value }
                    val total = max(sorted.sumOf { it.value }, 1.0)

                    buildString {
                        appendLine("Top Expense Categories:")
                        sorted.take(5).forEachIndexed { index, entry ->
                            val pct = (entry.value / total) * 100.0
                            appendLine("${index + 1}. ${entry.key}: ${entry.value.php()} (${String.format(Locale.US, "%.1f", pct)}%)")
                        }
                        val top = sorted.first()
                        appendLine()
                        append("Biggest leak: \"${top.key}\" accounts for ${String.format(Locale.US, "%.1f", (top.value / total) * 100.0)}% of your spending. Setting a weekly cap here gives the fastest savings boost.")
                    }
                }
            }
        ),

        // 7. Student & Allowance Budgeting
        KnowledgeTopic(
            id = "student_budget",
            phrases = listOf("budget my allowance", "student budget", "save as a student", "college budget", "weekly allowance", "baon"),
            keywords = listOf("student", "allowance", "baon", "college", "school", "weekly"),
            answer = {
                buildString {
                    appendLine("How to Manage a Student Allowance Without Running Short:")
                    appendLine()
                    appendLine("1. Divide by School Days: Subtract fixed commute and print/project costs first, then divide the remainder by the number of school days in the week.")
                    appendLine("2. Bring Water & Packed Snacks: Buying drinks and snacks on campus every day easily drains ₱500–₱1,000 a week.")
                    appendLine("3. Save Coins & Leftover Daily Cash: Any unspent daily allowance at the end of the day goes straight into your Savings wallet.")
                    append("4. Plan Project/Thesis Costs Early: Set aside a small school sinking fund so printing or materials never wipe out your food money.")
                }
            }
        ),

        // 8. Emergency Fund Guide
        KnowledgeTopic(
            id = "emergency_fund",
            phrases = listOf("emergency fund", "rainy day", "safety net"),
            keywords = listOf("emergency", "urgent", "backup", "cushion"),
            answer = { snapshot ->
                val monthlyBurn = when {
                    snapshot.thisMonthExpenses > 0 -> snapshot.thisMonthExpenses
                    snapshot.totalExpenses > 0 -> snapshot.totalExpenses
                    else -> snapshot.monthlyBudget
                }
                val starter = 5000.0
                val threeMonths = monthlyBurn * 3
                val sixMonths = monthlyBurn * 6
                buildString {
                    appendLine("An emergency fund protects you from unexpected bills without going into debt:")
                    appendLine()
                    appendLine("• Starter Milestone: ${starter.php()} (covers minor repairs, meds, or urgent travel)")
                    appendLine("• 3-Month Target: ${threeMonths.php()} (based on ${monthlyBurn.php()}/mo)")
                    appendLine("• 6-Month Target: ${sixMonths.php()}")
                    appendLine()
                    append("Rule: Only touch your emergency fund for true emergencies—medical needs, urgent repairs, or sudden loss of income.")
                }
            }
        ),

        // 9. Debt Payoff (Snowball vs Avalanche)
        KnowledgeTopic(
            id = "debt_payoff",
            phrases = listOf("pay off debt", "snowball", "avalanche", "credit card", "get out of debt"),
            keywords = listOf("debt", "debts", "loan", "loans", "utang", "installment", "spaylater", "gcredit", "interest"),
            answer = {
                buildString {
                    appendLine("Two Proven Ways to Clear Debt Faster:")
                    appendLine()
                    appendLine("1. Debt Avalanche (Saves the Most Money)")
                    appendLine("Pay minimums on every debt, then put all extra cash toward the balance with the highest interest rate first.")
                    appendLine()
                    appendLine("2. Debt Snowball (Fastest Motivation)")
                    appendLine("Pay minimums on every debt, then wipe out the smallest balance first. Roll that freed-up payment into the next smallest.")
                    appendLine()
                    append("Important: Pause new BNPL/installments until existing balances are paid off.")
                }
            }
        ),

        // 10. Impulse Buying & Temptation Control
        KnowledgeTopic(
            id = "impulse_buying",
            phrases = listOf("impulse buying", "stop buying", "stop spending", "add to cart", "tempted to buy", "control spending"),
            keywords = listOf("impulse", "tempted", "shopping", "lazada", "shopee", "checkout", "urge"),
            answer = {
                buildString {
                    appendLine("5 Ways to Beat Impulse Buying:")
                    appendLine()
                    appendLine("1. The 24-Hour / 7-Day Rule: Wait 24 hours for items under ₱1,000 and 7 days for bigger purchases. Most urges fade.")
                    appendLine("2. Calculate in Work/Allowance Hours: Ask \"How many days of allowance or work does this cost?\"")
                    appendLine("3. Unlink Saved Cards & Wallets: Adding friction at checkout stops emotional one-tap spending.")
                    appendLine("4. Unsubscribe From Promo Alerts: Flash sales create fake urgency for things you weren't planning to buy.")
                    append("5. Keep a \"Wants Wishlist\": Write items down instead of buying immediately; review the list once a month.")
                }
            }
        ),

        // 11. Saving on Food, Coffee & Delivery
        KnowledgeTopic(
            id = "food_savings",
            phrases = listOf("save on food", "food budget", "eating out", "food delivery", "milk tea", "coffee habit"),
            keywords = listOf("food", "groceries", "grocery", "delivery", "grabfood", "foodpanda", "coffee", "dining"),
            answer = {
                buildString {
                    appendLine("How to Cut Food Costs Without Feeling Deprived:")
                    appendLine()
                    appendLine("• Cap Delivery Apps: Delivery fees + markups often add 30%–45% to a meal. Reserve delivery for true busy days.")
                    appendLine("• Batch-Cook Staples: Cooking simple meals at home for 2–3 portions costs a fraction of takeout.")
                    appendLine("• Set a Weekly Treat Budget: Give yourself a fixed guilt-free coffee/snack allowance each week instead of buying randomly.")
                    append("• Never Shop Hungry: Make a short grocery list before buying so snacks don't fill the basket.")
                }
            }
        ),

        // 12. Sinking Funds & Big Purchase Planning
        KnowledgeTopic(
            id = "sinking_funds",
            phrases = listOf("sinking fund", "sinking funds", "save for a", "big purchase", "upcoming expense"),
            keywords = listOf("sinking", "goal", "goals", "target", "milestone", "laptop", "phone", "travel"),
            answer = {
                buildString {
                    appendLine("A Sinking Fund prevents big planned expenses from wrecking your monthly budget:")
                    appendLine()
                    appendLine("1. Pick the target item & cost (e.g., ₱6,000 for gadget upgrade, travel, or tuition).")
                    appendLine("2. Divide by the weeks/months left (e.g., ₱6,000 ÷ 6 months = ₱1,000/month or ₱250/week).")
                    appendLine("3. Track it in Tracey's Money Tools tab so you see your progress bar grow every week.")
                }
            }
        ),

        // 13. Surviving "Petsa de Peligro" / Tight Cash Weeks
        KnowledgeTopic(
            id = "petsa_de_peligro",
            phrases = listOf("petsa de peligro", "running out of money", "tight budget", "low on cash", "survive the week"),
            keywords = listOf("broke", "short", "peligro", "tight", "survive", "stretch"),
            answer = { snapshot ->
                buildString {
                    appendLine("Emergency Cash-Stretch Plan:")
                    appendLine()
                    appendLine("1. Freeze All Wants Immediately: No takeout, milk tea, or online shopping until your next income arrives.")
                    appendLine("2. Check Pantry & Existing Supplies: Use food, toiletries, and load you already have at home first.")
                    appendLine("3. Set a Hard Daily Cap: Divide your remaining ${snapshot.totalBalance.php()} by the days left until payday and refuse to cross it.")
                    append("4. Review Pending Items: Check if any non-urgent pending expense can be rescheduled.")
                }
            }
        ),

        // 14. Needs vs. Wants Decision Check
        KnowledgeTopic(
            id = "needs_vs_wants",
            phrases = listOf("needs vs wants", "need vs want", "need or want", "essential vs"),
            keywords = listOf("needs", "wants", "essential", "priority", "prioritize"),
            answer = {
                buildString {
                    appendLine("Quick Filter for Needs vs. Wants:")
                    appendLine()
                    appendLine("• Needs (Must-Haves): Basic food, shelter, utilities, essential transport, medicine, and required school/work tools.")
                    appendLine("• Upgrades (Disguised Wants): Branded coffee instead of home brew, express delivery, or upgrading a working phone.")
                    appendLine("• Pure Wants: Entertainment, impulse fashion, games, and convenience splurges.")
                    appendLine()
                    append("Rule: Cover 100% of Needs + Savings first; fund Wants only from what remains.")
                }
            }
        ),

        // 15. Beginner Investing, Digital Banks & Compound Interest
        KnowledgeTopic(
            id = "investing_basics",
            phrases = listOf("compound interest", "grow my money", "digital bank", "passive income", "start investing", "how to invest"),
            keywords = listOf("invest", "investing", "investment", "compound", "interest", "mp2", "stocks", "funds"),
            answer = {
                buildString {
                    appendLine("Smart Steps to Start Growing Your Money:")
                    appendLine()
                    appendLine("1. Clear High-Interest Debt First: Paying off a 3%–5% monthly interest debt gives a guaranteed return.")
                    appendLine("2. Park Cash in High-Yield Savings: Keep emergency funds in a regulated, high-interest digital savings account instead of a 0.1% traditional account.")
                    appendLine("3. Harness Compound Interest: Reinvesting your returns lets your earnings generate their own earnings over time—starting early in your 20s matters more than starting big.")
                    appendLine("4. Stick to Regulated Beginner Vehicles: Look into government-backed savings (like Pag-IBIG MP2) or diversified index funds for 3–5+ year goals.")
                    appendLine()
                    append("Warning: Avoid any platform promising \"guaranteed fast double-your-money\" returns.")
                }
            }
        ),

        // 16. Inflation Explained Simply
        KnowledgeTopic(
            id = "inflation",
            phrases = listOf("what is inflation", "why are prices rising", "purchasing power"),
            keywords = listOf("inflation", "prices", "expensive", "purchasing"),
            answer = {
                buildString {
                    appendLine("What Inflation Means for Your Wallet:")
                    appendLine()
                    appendLine("• Inflation is the gradual rise in prices over time, meaning ₱1,000 buys slightly less next year than it does today.")
                    appendLine("• Cash sitting idle in a zero-interest wallet slowly loses purchasing power.")
                    append("• How to Beat It: Keep short-term savings in high-yield accounts, avoid lifestyle creep, and invest long-term money in assets that outpace inflation.")
                }
            }
        ),

        // 17. Increasing Income & Side Hustles
        KnowledgeTopic(
            id = "increase_income",
            phrases = listOf("earn more", "extra income", "side hustle", "make more money", "increase income"),
            keywords = listOf("earn", "income", "hustle", "freelance", "sideline", "skills"),
            answer = {
                buildString {
                    appendLine("Practical Ways to Boost Your Income:")
                    appendLine()
                    appendLine("• Monetize Digital Skills: Offer web/app development, UI design, video editing, tutoring, or tech troubleshooting.")
                    appendLine("• Declutter & Resell: Sell unused gadgets, books, or clothes to turn clutter into instant emergency fund cash.")
                    append("• Protect the Upside: Whenever you earn extra income, save at least 50% of it instead of raising your daily spending.")
                }
            }
        ),

        // 18. Common Money Mistakes in Your 20s
        KnowledgeTopic(
            id = "money_mistakes",
            phrases = listOf("money mistakes", "financial mistakes", "mistakes in your 20s", "bad money habits"),
            keywords = listOf("mistake", "mistakes", "habits", "avoid", "20s"),
            answer = {
                buildString {
                    appendLine("Top Money Mistakes to Avoid Early:")
                    appendLine()
                    appendLine("1. Not Tracking Small Daily Expenses: Untracked ₱50–₱150 purchases quietly eat thousands every month.")
                    appendLine("2. Treating BNPL / Credit Limits as \"Free Money\": Always ask if you could buy it in cash today.")
                    appendLine("3. Having Zero Emergency Buffer: One unexpected repair forces you into high-interest debt.")
                    append("4. Lifestyle Inflation: Spending every peso of a new allowance or paycheck raise instead of raising your savings rate.")
                }
            }
        ),

        // 19. App Features & Receipt Scanner Guide
        KnowledgeTopic(
            id = "app_help",
            phrases = listOf("how to use", "what can you do", "scan receipt", "receipt scanner", "commands"),
            keywords = listOf("scan", "receipt", "ocr", "camera", "feature", "features", "help", "commands"),
            answer = {
                buildString {
                    appendLine("Everything I Can Help You With in Tracey:")
                    appendLine()
                    appendLine("• Add to Wallet: Type \"cash in 200\", \"sweldo 15k\", \"allowance 1000\", \"refund 250\", or \"+200\".")
                    appendLine("• Deduct from Wallet: Type \"150 food\", \"gas 300\", \"meralco 1200\", \"shopee 450\", or \"-80 jeep\".")
                    appendLine("• Live Financial Insights: Ask \"How is my financial health?\", \"Check my monthly budget\", \"Show my wallet balances\", or \"Where am I spending the most?\"")
                    appendLine("• Smart Calculators: Ask \"Can I afford 1200?\", \"How long to save 10000?\", or \"Apply 50/30/20 to my income\".")
                    append("• Receipt OCR Tip: Flatten crumpled receipts under bright light and keep the total amount clearly inside the frame.")
                }
            }
        ),

        // 20. Greetings
        KnowledgeTopic(
            id = "greeting",
            phrases = listOf("good morning", "good afternoon", "good evening", "who are you", "what are you"),
            keywords = listOf("hello", "hi", "hey", "meow", "yo", "kumusta"),
            answer = { snapshot ->
                val nameGreeting = if (snapshot.username.isNotBlank()) " ${snapshot.username}" else ""
                "Meow$nameGreeting! Your combined balance is ${snapshot.totalBalance.php()} across ${snapshot.wallets.size} wallet(s). Type \"cash in 200\" to add funds, \"150 food\" to deduct an expense, or ask me any budget question."
            }
        )
    )

    private fun evaluateAffordability(amount: Double, snapshot: FinancialSnapshot): String {
        val balance = snapshot.totalBalance
        if (balance <= 0.0) {
            return "Your current recorded balance is ${balance.php()}, so spending ${amount.php()} would leave you negative. Hold off until new income comes in."
        }
        val ratio = (amount / balance) * 100.0
        val remaining = balance - amount
        val budgetRemaining = snapshot.monthlyBudget - snapshot.thisMonthExpenses

        return buildString {
            appendLine("Purchase Check for ${amount.php()}:")
            appendLine("• Available Balance: ${balance.php()}")
            appendLine("• Balance After Purchase: ${remaining.php()} (${String.format(Locale.US, "%.1f", ratio)}% of balance)")
            if (snapshot.monthlyBudget > 0) {
                appendLine("• Monthly Budget Left: ${budgetRemaining.php()}")
            }
            appendLine()
            when {
                amount > balance -> append("Verdict: Not affordable right now—you are short by ${(amount - balance).php()}.")
                snapshot.monthlyBudget > 0 && amount > budgetRemaining ->
                    append("Verdict: You have enough cash, but this will push you over your ${snapshot.monthlyBudget.php()} monthly budget.")
                ratio > 50.0 ->
                    append("Verdict: High impact—this uses over half your available money. Wait 24 hours and confirm bills are covered first.")
                ratio > 20.0 ->
                    append("Verdict: Affordable, though noticeable. Make sure it fits your Wants allocation.")
                else ->
                    append("Verdict: Safely affordable! You will still keep a healthy ${remaining.php()} buffer.")
            }
        }
    }

    private fun evaluateSavingsTimeline(target: Double, snapshot: FinancialSnapshot): String {
        val monthlySurplus = (snapshot.totalIncome - snapshot.totalExpenses).coerceAtLeast(0.0)
        val suggestedWeekly = 250.0
        val suggestedMonthly = if (monthlySurplus >= 500.0) monthlySurplus * 0.5 else 1000.0

        val weeksAt250 = ceil(target / suggestedWeekly).toInt()
        val monthsAtRate = ceil(target / suggestedMonthly).toInt()

        return buildString {
            appendLine("Savings Timeline for ${target.php()}:")
            appendLine("• At ₱250/week: ~$weeksAt250 weeks")
            appendLine("• At ${suggestedMonthly.php()}/month: ~$monthsAtRate month(s)")
            if (snapshot.totalBalance > 0) {
                val gap = (target - snapshot.totalBalance).coerceAtLeast(0.0)
                if (gap == 0.0) {
                    appendLine("• Note: Your current combined balance (${snapshot.totalBalance.php()}) already covers this goal!")
                } else {
                    appendLine("• Gap from current balance: ${gap.php()}")
                }
            }
            appendLine()
            append("Tip: Add ${target.php()} as a goal in Tracey's Money Tools tab to track every deposit.")
        }
    }

    private fun extractAmount(text: String): Double? {
        val cleaned = text.replace("₱", " ")
        val regex = Regex("""(?<!\w)(?:php)?\s*(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d+))?\s*(k)?(?!\w)""", RegexOption.IGNORE_CASE)
        val match = regex.find(cleaned) ?: return null

        val wholePart = match.groupValues[1].replace(",", "")
        val decimalPart = match.groupValues[2]
        val hasKSuffix = match.groupValues[3].equals("k", ignoreCase = true)

        val numberStr = if (decimalPart.isNotEmpty()) "$wholePart.$decimalPart" else wholePart
        val baseValue = numberStr.toDoubleOrNull() ?: return null
        return if (hasKSuffix) baseValue * 1000.0 else baseValue
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}