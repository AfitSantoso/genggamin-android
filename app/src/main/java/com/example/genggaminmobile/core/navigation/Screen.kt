package com.example.genggaminmobile.core.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password?token={token}") {
        fun createRoute(token: String) = "reset_password?token=$token"
    }
}
