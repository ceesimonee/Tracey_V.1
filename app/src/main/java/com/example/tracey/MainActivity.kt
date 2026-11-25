package com.example.tracey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tracey.ui.screens.AboutScreen
import com.example.tracey.ui.screens.LoginScreen
import com.example.tracey.ui.screens.MainHubScreen
import com.example.tracey.ui.screens.OnboardingPagerScreen
import com.example.tracey.ui.screens.WelcomeScreen
import com.example.tracey.ui.theme.TraceyTheme
import com.example.tracey.viewmodel.TraceyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraceyTheme {
                val navController = rememberNavController()
                val traceyViewModel: TraceyViewModel = viewModel()
                val currentUser by traceyViewModel.currentUser.collectAsState()

                val startDestination = if (currentUser.isEmpty()) "welcome" else "dashboard"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("welcome") {
                        WelcomeScreen(
                            onGetStarted = { navController.navigate("onboarding") },
                            onReturningUserLogin = { navController.navigate("login?register=false") }
                        )
                    }
                    composable("onboarding") {
                        OnboardingPagerScreen(
                            onFinishOnboarding = {
                                navController.navigate("login?register=true") {
                                    popUpTo("welcome") { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(
                        route = "login?register={register}",
                        arguments = listOf(
                            navArgument("register") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val isRegister = backStackEntry.arguments?.getBoolean("register") ?: false
                        LoginScreen(
                            initialRegisterMode = isRegister,
                            onLogin = { username, password -> traceyViewModel.login(username, password) },
                            onRegister = { username, password -> traceyViewModel.register(username, password) },
                            onAuthSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dashboard") {
                        MainHubScreen(
                            viewModel = traceyViewModel,
                            onNavigateAbout = { navController.navigate("about") },
                            onLogout = {
                                traceyViewModel.logout()
                                navController.navigate("welcome") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("about") {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}