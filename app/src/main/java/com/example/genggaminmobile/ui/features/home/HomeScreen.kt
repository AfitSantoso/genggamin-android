package com.example.genggaminmobile.ui.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.genggaminmobile.R
import com.example.genggaminmobile.domain.model.Loan
import com.example.genggaminmobile.domain.model.Plafond
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLoanApp: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(stringResource(R.string.nav_home)) },
                    selected = true,
                    onClick = { /* Already on Home */ }
                )
                 NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Pinjaman") },
                    label = { Text(stringResource(R.string.nav_my_loans)) },
                    selected = false,
                    onClick = { /* Navigate to Loans List */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(stringResource(R.string.nav_profile)) },
                    selected = false,
                    onClick = {
                         if (uiState.isLoggedIn) onNavigateToProfile() else onNavigateToLogin()
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.isLoggedIn) stringResource(R.string.greeting_welcome) else stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Genggamin Mobile",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(badge = { /* Badge logic handled later */ }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notification")
                        }
                    }
                }
            }

            // Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.home_available_balance),
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalLimit = uiState.loanLimits.sumOf { it.availableLimit }
                            Text(
                                text = currencyFormatter.format(totalLimit),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!uiState.isLoggedIn) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onNavigateToLogin,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(stringResource(R.string.home_login_button))
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(
                        icon = Icons.Default.MonetizationOn,
                        label = stringResource(R.string.home_action_loan),
                        onClick = {
                            if (uiState.isLoggedIn) {
                                if (uiState.hasProfile) onNavigateToLoanApp() else viewModel.onPlafondClick(Plafond(0,"",0,0,0,0.0,false)) // Trigger prompt
                            } else {
                                onNavigateToLogin()
                            }
                        }
                    )
                    QuickActionItem(
                        icon = Icons.Default.History,
                        label = stringResource(R.string.home_action_history),
                        onClick = { /* Navigate to History */ }
                    )
                    QuickActionItem(
                        icon = Icons.Default.ContactSupport,
                        label = "Bantuan",
                        onClick = { /* Help */ }
                    )
                     QuickActionItem(
                        icon =IfLogedIn(uiState.isLoggedIn, Icons.Default.Logout, Icons.Default.Login),
                        label = if(uiState.isLoggedIn) "Keluar" else "Masuk",
                        onClick = { if(uiState.isLoggedIn) viewModel.logout() else onNavigateToLogin() }
                    )
                }
            }

            // Banner Image
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.header),
                        contentDescription = "Banner Promo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Active Loans Section
            if (uiState.activeLoans.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nav_my_loans),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.activeLoans) { loan ->
                    ActiveLoanItem(loan, currencyFormatter)
                }
            }

            // Recommendations (Plafonds)
            item {
                Text(
                    text = stringResource(R.string.home_available_plafonds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (uiState.isLoading) {
                 item { CircularProgressIndicator() }
            } else {
                 items(uiState.plafonds) { plafond ->
                    PlafondItemModern(
                        plafond = plafond,
                        onClick = {
                             if (uiState.isLoggedIn) {
                                if (uiState.hasProfile) onNavigateToLoanApp() else viewModel.onPlafondClick(plafond)
                            } else {
                                onNavigateToLogin()
                            }
                        },
                        currencyFormatter = currencyFormatter
                    )
                }
            }
        }
        
        if (uiState.showProfilePrompt) {
            AlertDialog(
                onDismissRequest = viewModel::dismissProfilePrompt,
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = { Text(stringResource(R.string.home_complete_profile_title)) },
                text = { Text(stringResource(R.string.home_complete_profile_message)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.dismissProfilePrompt()
                        onNavigateToProfile()
                    }) {
                        Text(stringResource(R.string.home_complete_profile_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissProfilePrompt) {
                        Text(stringResource(R.string.home_complete_profile_later))
                    }
                }
            )
        }

        if (uiState.showPromoPopup) {
            Dialog(
                onDismissRequest = viewModel::dismissPromoPopup,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .wrapContentHeight()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.popup),
                            contentDescription = "Promo",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    IconButton(
                        onClick = viewModel::dismissPromoPopup,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun PlafondItemModern(plafond: Plafond, onClick: () -> Unit, currencyFormatter: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plafond.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Max ${currencyFormatter.format(plafond.maxAmount)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${plafond.interestRate}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Bunga", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ActiveLoanItem(loan: Loan, currencyFormatter: NumberFormat) {
     Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(loan.purpose ?: "Pinjaman", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(loan.status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(currencyFormatter.format(loan.amount), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text("${loan.tenorMonths} Bulan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

fun IfLogedIn(isLoggedIn: Boolean, trueVal: ImageVector, falseVal: ImageVector): ImageVector {
    return if (isLoggedIn) trueVal else falseVal
}
