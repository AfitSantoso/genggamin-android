package com.example.genggaminmobile.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.genggaminmobile.ui.features.home.HomeScreen
import com.example.genggaminmobile.ui.features.loan.LoanApplicationScreen
import com.example.genggaminmobile.ui.features.notification.NotificationScreen
import com.example.genggaminmobile.ui.features.auth.forgot_password.ForgotPasswordScreen
import com.example.genggaminmobile.ui.features.auth.login.LoginScreen
import com.example.genggaminmobile.ui.features.auth.register.RegisterScreen
import com.example.genggaminmobile.ui.features.auth.reset_password.ResetPasswordScreen
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToLoanApp = {
                    navController.navigate(Screen.LoanApplication.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notification.route)
                }
            )
        }
        composable(Screen.Profile.route) {
            // Placeholder for Profile Screen
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Halaman Profil (Dalam Pengembangan)")
            }
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToResetPassword = { token ->
                    navController.navigate(Screen.ResetPassword.createRoute(token))
                }
            )
        }
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("token") {
                    type = NavType.StringType
                    nullable = true
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "genggamin://reset-password?token={token}" },
                navDeepLink { uriPattern = "https://genggamin.com/reset-password?token={token}" }
            )
        ) {
            ResetPasswordScreen(
                onResetSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ResetPassword.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.LoanApplication.route) {
            LoanApplicationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Notification.route) {
            NotificationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
