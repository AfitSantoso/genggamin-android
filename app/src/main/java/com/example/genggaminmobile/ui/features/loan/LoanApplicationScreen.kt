package com.example.genggaminmobile.ui.features.loan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.model.LoanSimulation
import com.example.genggaminmobile.domain.model.Plafond
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationScreen(
    onBack: () -> Unit,
    viewModel: LoanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Launcher for location permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocation(fusedLocationClient, viewModel)
        }
    }

    // Effect to check and request location when screen is opened
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchCurrentLocation(fusedLocationClient, viewModel)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    if (uiState.success) {
        ModernLoanSuccessDialog(onDismiss = onBack)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pengajuan Pinjaman", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading && uiState.plafonds.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item {
                        LoanHeaderSection()
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Pilih Produk Pinjaman",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.2.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            PlafondSelectionList(
                                plafonds = uiState.plafonds,
                                limits = uiState.limits,
                                selectedPlafond = uiState.selectedPlafond,
                                onPlafondSelected = viewModel::onPlafondSelected,
                                currencyFormatter = currencyFormatter,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (uiState.selectedPlafond != null) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        LoanInputSection(
                                            amount = uiState.amountInput,
                                            onAmountChange = viewModel::onAmountChanged,
                                            tenor = uiState.tenorInput,
                                            onTenorChange = viewModel::onTenorChanged,
                                            purpose = uiState.purposeInput,
                                            onPurposeChange = viewModel::onPurposeChanged,
                                            selectedPlafond = uiState.selectedPlafond!!,
                                            selectedLimit = uiState.selectedLimit,
                                            currencyFormatter = currencyFormatter,
                                        )

                                        if (uiState.simulation != null) {
                                            ModernSimulationCard(
                                                simulation = uiState.simulation!!,
                                                currencyFormatter = currencyFormatter,
                                            )
                                        }

                                        if (uiState.error != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptySelectionState()
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 24.dp,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Button(
                        onClick = viewModel::submitLoan,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uiState.selectedPlafond != null && !uiState.isLoading,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Ajukan Pinjaman Sekarang", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: LoanViewModel,
) {
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
            location?.let {
                viewModel.updateLocation(it.latitude, it.longitude)
            }
        }
}

@Composable
fun LoanHeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 16.dp)) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Solusi Keuangan Anda", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text("Proses cepat, aman, dan terpercaya", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlafondSelectionList(
    plafonds: List<Plafond>,
    limits: List<LoanLimit>,
    selectedPlafond: Plafond?,
    onPlafondSelected: (Plafond) -> Unit,
    currencyFormatter: NumberFormat,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(plafonds) { plafond ->
            val limit = limits.find { it.plafondId == plafond.id.toLong() }
            val isSelected = selectedPlafond?.id == plafond.id
            val isAvailable = limit == null || (limit.availableLimit > 0 && !limit.isLocked)

            ElevatedCard(
                onClick = { onPlafondSelected(plafond) },
                modifier = Modifier
                    .width(260.dp)
                    .height(160.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Transparent,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    ),
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    ),
                                )
                            },
                        )
                        .then(
                            if (!isSelected) {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(28.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    plafond.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        "Bunga ${plafond.interestRate}%",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        Column {
                            Text(
                                "Sisa Limit Tersedia",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val limitValue = limit?.availableLimit ?: plafond.maxAmount
                            Text(
                                text = currencyFormatter.format(limitValue),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isSelected) Color.White else if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanInputSection(
    amount: String,
    onAmountChange: (String) -> Unit,
    tenor: String,
    onTenorChange: (String) -> Unit,
    purpose: String,
    onPurposeChange: (String) -> Unit,
    selectedPlafond: Plafond,
    selectedLimit: LoanLimit?,
    currencyFormatter: NumberFormat,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Formulir Pengajuan", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }

            ModernLoanTextField(
                value = formatCurrencyInput(amount),
                onValueChange = onAmountChange,
                label = "Jumlah Pinjaman",
                icon = Icons.Outlined.Payments,
                keyboardType = KeyboardType.Number,
                prefix = "Rp ",
                supportingText = "Min: Rp 400.000, Max: ${currencyFormatter.format(selectedLimit?.availableLimit ?: selectedPlafond.maxAmount)}",
            )

            ModernLoanTextField(
                value = tenor,
                onValueChange = onTenorChange,
                label = "Jangka Waktu (Bulan)",
                icon = Icons.Outlined.Timer,
                keyboardType = KeyboardType.Number,
                supportingText = "Maksimal ${selectedPlafond.tenorMonth} Bulan",
            )

            ModernLoanTextField(
                value = purpose,
                onValueChange = onPurposeChange,
                label = "Tujuan Penggunaan Dana",
                icon = Icons.Outlined.Info,
                singleLine = false,
                minLines = 2,
            )
        }
    }
}

@Composable
fun ModernSimulationCard(
    simulation: LoanSimulation,
    currencyFormatter: NumberFormat,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Estimasi Cicilan", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "Bunga ${simulation.interestRate}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                        ),
                    )
                    .padding(24.dp),
            ) {
                Column {
                    Text("Angsuran Per Bulan", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currencyFormatter.format(simulation.monthlyInstallment),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SimulationDetailBox(
                    label = "Total Bunga",
                    value = currencyFormatter.format(simulation.totalInterest),
                    modifier = Modifier.weight(1f),
                )
                SimulationDetailBox(
                    label = "Total Bayar",
                    value = currencyFormatter.format(simulation.totalRepayment),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun SimulationDetailBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
fun ModernLoanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.Medium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        prefix = if (prefix != null) {
            { Text(prefix, fontWeight = FontWeight.SemiBold) }
        } else {
            null
        },
        supportingText = if (supportingText != null) {
            { Text(supportingText, style = MaterialTheme.typography.labelSmall) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun EmptySelectionState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            shape = CircleShape,
            modifier = Modifier.size(100.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Mulai Pengajuan Anda",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pilih salah satu produk pinjaman di atas\nuntuk melihat estimasi dan simulasi.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun ModernLoanSuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Lihat Status Pinjaman", fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Surface(
                color = Color(0xFFE8F5E9),
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(50.dp))
                }
            }
        },
        title = { Text("Pengajuan Terkirim!", fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        text = {
            Text(
                "Pinjaman Anda sedang kami proses. Tim kami akan segera melakukan verifikasi data Anda dalam waktu 1x24 jam.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    )
}

fun formatCurrencyInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return try {
        val parsed = digits.toLong()
        NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed)
    } catch (e: Exception) {
        digits
    }
}
