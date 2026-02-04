package com.example.genggaminmobile.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.genggaminmobile.ui.features.auth.forgot_password.ForgotPasswordScreen
import com.example.genggaminmobile.ui.features.auth.login.LoginScreen
import com.example.genggaminmobile.ui.features.auth.register.RegisterScreen
import com.example.genggaminmobile.ui.features.auth.reset_password.ResetPasswordScreen
import com.example.genggaminmobile.ui.features.home.HomeScreen
import com.example.genggaminmobile.ui.features.loan.LoanApplicationScreen
import com.example.genggaminmobile.ui.features.notification.NotificationScreen
import com.example.genggaminmobile.ui.features.profile.ProfileScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(Screen.Home.route, Screen.Profile.route, Screen.LoanHistory.route)
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                ModernNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            if (route == Screen.Home.route) {
                                navController.popBackStack(Screen.Home.route, false)
                            } else {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
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
                    },
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
                    },
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
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.LoanHistory.route)
                    },
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToResetPassword = { token ->
                        navController.navigate(Screen.ResetPassword.createRoute(token))
                    },
                )
            }
            composable(
                route = Screen.ResetPassword.route,
                arguments = listOf(
                    navArgument("token") {
                        type = NavType.StringType
                        nullable = true
                    },
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "genggamin://reset-password?token={token}" },
                    navDeepLink { uriPattern = "https://genggamin.com/reset-password?token={token}" },
                ),
            ) {
                ResetPasswordScreen(
                    onResetSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ResetPassword.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.LoanApplication.route) {
                LoanApplicationScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Screen.Notification.route) {
                NotificationScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Screen.LoanHistory.route) {
                com.example.genggaminmobile.ui.features.loan.LoanHistoryScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
fun ModernNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Push up above gesture/nav bar
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val items = listOf(
                NavItem(Screen.Home.route, "Home", Icons.Default.Home, Icons.Outlined.Home),
                NavItem(Screen.LoanHistory.route, "Pinjaman", Icons.Default.List, Icons.Outlined.List),
                NavItem(Screen.Profile.route, "Profil", Icons.Default.Person, Icons.Outlined.Person),
            )

            items.forEach { item ->
                val selected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp),
                        )
                        if (selected) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)
