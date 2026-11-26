**# 🐱 Tracey — Personal Budget & Expense Tracker

**Tracey** is a minimalist, offline-first personal finance and budget assistant for Android built with **Kotlin** and **Jetpack Compose (Material 3)**. Styled around a calm grey Persian cat mascot and tailored for Philippine Peso (`₱`) tracking, Tracey combines multi-wallet management, on-device OCR receipt scanning, natural-language expense logging, and goal tracking in one lightweight app.

---

## ✨ Key Features

* **Multi-Wallet Management:** Track your overall net balance under **All Wallets** or manage individual accounts (e.g., Main Wallet, GCash, Maya, Cash) with support for creating, renaming, and safely deleting wallets.
* **Expense & Income Tracking:** Log transactions by category (*Food, Transport, Bills, School, Shopping, Allowance, Freelance,* etc.), tag entries as **Paid** or **Pending**, and view real-time category share breakdowns filtered by Day, Week, Month, or Year.
* **Smart Receipt Scanner (On-Device OCR):** Capture full-resolution receipt photos via Camera (`FileProvider`) or import from Gallery. Powered by **Google ML Kit Text Recognition** with spatial row-alignment parsing, VAT suffix handling, and automatic `Cash − Change` cross-verification.
* **Ask Tracey (Chat Assistant):** Log expenses or check wallet balances conversationally (e.g., `"2 pc chicken 185"` or `"Wallet balance?"`) with persistent chat history.
* **Money Tools:**
  * **Monthly Budget Planner:** Set a monthly spending limit and monitor real-time usage.
  * **Savings Goals:** Track target amounts with custom Peso deposits and one-tap remaining balance completion.
  * **Debt Tracker & Lend / Borrow:** Record money owed or lent out with progress bars and status badges.
* **Milestones & Tracey's Lounge:** Unlockable financial tracking badges and dynamic mood feedback based on your monthly budget ratio.
* **Onboarding & Local Authentication:** Includes a 5-page interactive walkthrough and multi-user local sign-up/sign-in support.

---

## 🛠️ Tech Stack & Architecture

Tracey follows the **MVVM (Model–View–ViewModel)** architecture pattern with reactive `StateFlow` state management:

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin | 100% Kotlin codebase |
| **UI Framework** | Jetpack Compose & Material 3 | Declarative UI, adaptive theming, `LazyColumn` lists, and custom components |
| **Architecture** | MVVM + Kotlin Coroutines / `StateFlow` | Unidirectional data flow via `TraceyViewModel` |
| **Local Database** | Room Persistence Library (SQLite) | Offline storage for `Expense`, `ToolItem`, and `ChatMessageEntity` tables |
| **Computer Vision / OCR** | Google ML Kit Text Recognition | On-device receipt text extraction and spatial line alignment |
| **Camera & Storage** | Activity Result APIs & `FileProvider` | Secure full-resolution receipt image capture |
| **Build System** | Gradle (Kotlin DSL) | Dependency and build configuration |

---

## 📂 Project Structure

```text
app/src/main/
├── java/com/example/tracey/
│   ├── data/                  # Room database, DAO, and entities (Expense, ToolItem, ChatMessageEntity)
│   ├── ui/
│   │   ├── screens/           # Compose screens & dialogs (HomeTab, TransactionsTab, MoneyToolsTab,
│   │   │                      # AskTraceyTab, ReceiptScannerDialog, OnBoardingScreen, LoginScreen, AboutScreen)
│   │   └── theme/             # Custom Persian-grey color palette, typography, and Material 3 theme
│   ├── viewmodel/             # TraceyViewModel managing UI state, wallet logic, and database operations
│   └── MainActivity.kt        # App entry point and navigation host
└── res/
    ├── drawable/              # Mascot artwork and adaptive icon layers
    ├── mipmap-*/              # Launcher icons
    ├── values/                # Colors, strings, and base themes
    └── xml/                   # FileProvider paths (file_paths.xml) and backup rules



🚀 Getting Started
Prerequisites
Android Studio (Ladybug or newer recommended)

JDK 17 or higher

Android SDK (Min SDK 24 / Target SDK 35+)

**Installation**
Clone the repository:

Bash
git clone [https://github.com/ceesimonee/Tracey.git](https://github.com/ceesimonee/Tracey.git)
Open the project folder in Android Studio.

Let Gradle sync and download the required dependencies (Compose, Room, ML Kit).

Select an Android Emulator or physical device and click Run (▶).

👤 Author
Symon Cristoffer B. Cano

GitHub: @ceesimonee


To add this to your repository in Android Studio, switch the sidebar to **Project** vi**
