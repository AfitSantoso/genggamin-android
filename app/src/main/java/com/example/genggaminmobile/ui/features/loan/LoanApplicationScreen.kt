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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genggaminmobile.R
import com.example.genggaminmobile.domain.model.LoanLimit
import com.example.genggaminmobile.domain.model.LoanSimulation
import com.example.genggaminmobile.domain.model.Plafond
import com.example.genggaminmobile.ui.components.SignaturePoint
import com.example.genggaminmobile.ui.theme.*
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Launcher for resolution (turning on GPS)
    val resolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // User enabled GPS
            fetchCurrentLocation(fusedLocationClient, viewModel)
        }
    }

    // Function to check settings and prompt user
    fun checkLocationSettings(onSuccess: () -> Unit) {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = com.google.android.gms.location.LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: com.google.android.gms.location.SettingsClient = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            onSuccess()
        }

        task.addOnFailureListener { exception ->
            if (exception is com.google.android.gms.common.api.ResolvableApiException) {
                try {
                    val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(exception.resolution).build()
                    resolutionLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    // Ignore the error.
                }
            }
        }
    }

    // Launcher for location permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            checkLocationSettings {
                fetchCurrentLocation(fusedLocationClient, viewModel)
            }
        }
    }

    // Effect to check and request location when screen is opened
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            checkLocationSettings {
                fetchCurrentLocation(fusedLocationClient, viewModel)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    LoanApplicationContent(
        uiState = uiState,
        onBack = onBack,
        onPlafondSelected = viewModel::onPlafondSelected,
        onAmountChanged = viewModel::onAmountChanged,
        onTenorChanged = viewModel::onTenorChanged,
        onPurposeChanged = viewModel::onPurposeChanged,
        onApplyClicked = viewModel::onApplyLoanClicked,
        onDismissContract = viewModel::dismissContractDialog,
        onContractSigned = viewModel::onContractSigned,
        getContractDisplayInfo = viewModel::getContractDisplayInfo,
        currencyFormatter = currencyFormatter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationContent(
    uiState: LoanApplicationUiState,
    onBack: () -> Unit,
    onPlafondSelected: (Plafond) -> Unit,
    onAmountChanged: (String) -> Unit,
    onTenorChanged: (String) -> Unit,
    onPurposeChanged: (String) -> Unit,
    onApplyClicked: () -> Unit,
    onDismissContract: () -> Unit,
    onContractSigned: (List<SignaturePoint>) -> Unit,
    getContractDisplayInfo: () -> ContractDisplayInfo?,
    currencyFormatter: NumberFormat,
) {
    // Show success dialog after loan is submitted
    if (uiState.success) {
        ModernLoanSuccessDialog(
            onDismiss = onBack,
            isOffline = uiState.isOfflineSubmission,
            offlineMessage = uiState.offlineMessage,
        )
    }

    // Show contract dialog when user clicks apply button
    if (uiState.showContractDialog) {
        val contractInfo = getContractDisplayInfo()
        if (contractInfo != null) {
            LoanContractDialog(
                contractInfo = contractInfo,
                isLoading = uiState.isContractLoading,
                onDismiss = onDismissContract,
                onConfirm = { signaturePath ->
                    onContractSigned(signaturePath)
                },
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.loan_app_title), fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp) },
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
                                    stringResource(R.string.loan_app_select_product),
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
                                onPlafondSelected = onPlafondSelected,
                                currencyFormatter = currencyFormatter,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Show payslip requirement warning for business loans
                            if (uiState.requiresPayslip && !uiState.hasPayslip) {
                                PayslipRequirementAlert(
                                    plafondTitle = uiState.selectedPlafond?.title ?: "",
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (uiState.selectedPlafond != null) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        LoanInputSection(
                                            amount = uiState.amountInput,
                                            onAmountChange = onAmountChanged,
                                            tenor = uiState.tenorInput,
                                            onTenorChange = onTenorChanged,
                                            purpose = uiState.purposeInput,
                                            onPurposeChange = onPurposeChanged,
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
                        onClick = onApplyClicked,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uiState.selectedPlafond != null && !uiState.isLoading && !uiState.isContractLoading,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Draw,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.loan_button_apply_now), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoanApplicationScreenPreview() {
    val dummyPlafond = Plafond(1, "Kredit Mikro", 5000000, 50000000, 12, 1.5, true)
    val dummyPlafonds = listOf(dummyPlafond, Plafond(2, "Kredit Usaha", 10000000, 100000000, 24, 1.2, true))

    val uiState = LoanApplicationUiState(
        plafonds = dummyPlafonds,
        selectedPlafond = dummyPlafond,
        amountInput = "10000000",
        tenorInput = "12",
        simulation = LoanSimulation(983333, 11800000, 1800000, 1.5),
    )

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    LoanApplicationContent(
        uiState = uiState,
        onBack = {},
        onPlafondSelected = {},
        onAmountChanged = {},
        onTenorChanged = {},
        onPurposeChanged = {},
        onApplyClicked = {},
        onDismissContract = {},
        onContractSigned = {},
        getContractDisplayInfo = { null },
        currencyFormatter = currencyFormatter,
    )
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    viewModel: LoanViewModel,
) {
    try {
        // Use High Accuracy to trigger GPS
        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateLocation(location.latitude, location.longitude)
                } else {
                    // Fallback to last known location if current location is null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        lastLocation?.let {
                            viewModel.updateLocation(it.latitude, it.longitude)
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Try last location on failure
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    lastLocation?.let {
                        viewModel.updateLocation(it.latitude, it.longitude)
                    }
                }
            }
    } catch (e: Exception) {
        e.printStackTrace()
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
            Text(stringResource(R.string.loan_header_title), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.loan_header_subtitle), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
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
                                        stringResource(R.string.loan_card_interest, plafond.interestRate.toString()),
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
                                stringResource(R.string.loan_card_limit_label),
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

@Preview(showBackground = true)
@Composable
fun PlafondSelectionListPreview() {
    val dummyPlafond = Plafond(1, "Kredit Mikro", 5000000, 50000000, 12, 1.5, true)
    val dummyPlafonds = listOf(
        dummyPlafond,
        Plafond(2, "Kredit Usaha", 10000000, 100000000, 24, 1.2, true),
    )
    val dummyLimits = listOf(
        LoanLimit(1, 1, "Kredit Mikro", 50000000, 50000000, false),
    )
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    MaterialTheme {
        PlafondSelectionList(
            plafonds = dummyPlafonds,
            limits = dummyLimits,
            selectedPlafond = dummyPlafond,
            onPlafondSelected = {},
            currencyFormatter = currencyFormatter,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                Text(stringResource(R.string.loan_form_title), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }

            ModernLoanTextField(
                value = formatCurrencyInput(amount),
                onValueChange = onAmountChange,
                label = stringResource(R.string.loan_input_amount_label),
                icon = Icons.Outlined.Payments,
                keyboardType = KeyboardType.Number,
                prefix = "Rp ",
                supportingText = stringResource(R.string.loan_input_amount_support, currencyFormatter.format(selectedLimit?.availableLimit ?: selectedPlafond.maxAmount)),
            )

            ModernLoanTextField(
                value = tenor,
                onValueChange = onTenorChange,
                label = stringResource(R.string.loan_input_tenor_label),
                icon = Icons.Outlined.Timer,
                keyboardType = KeyboardType.Number,
                supportingText = stringResource(R.string.loan_input_tenor_support, selectedPlafond.tenorMonth),
            )

            val purposeOther = stringResource(R.string.loan_purpose_other)
            val purposeOptions = listOf(
                stringResource(R.string.loan_purpose_business),
                stringResource(R.string.loan_purpose_education),
                stringResource(R.string.loan_purpose_health),
                stringResource(R.string.loan_purpose_renovation),
                stringResource(R.string.loan_purpose_consumption),
                purposeOther,
            )
            var isPurposeDropdownExpanded by remember { mutableStateOf(false) }
            var isOtherSelected by remember { mutableStateOf(false) }

            LaunchedEffect(purpose) {
                if (purpose.isNotEmpty() && purpose !in purposeOptions) {
                    isOtherSelected = true
                } else if (purpose in purposeOptions && purpose != purposeOther) {
                    isOtherSelected = false
                }
            }

            ExposedDropdownMenuBox(
                expanded = isPurposeDropdownExpanded,
                onExpandedChange = { isPurposeDropdownExpanded = !isPurposeDropdownExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = if (isOtherSelected) purposeOther else purpose,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.loan_input_purpose_label), fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPurposeDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                ExposedDropdownMenu(
                    expanded = isPurposeDropdownExpanded,
                    onDismissRequest = { isPurposeDropdownExpanded = false },
                ) {
                    purposeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                if (option == purposeOther) {
                                    isOtherSelected = true
                                    if (purpose in purposeOptions) {
                                        onPurposeChange("")
                                    }
                                } else {
                                    isOtherSelected = false
                                    onPurposeChange(option)
                                }
                                isPurposeDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            if (isOtherSelected) {
                ModernLoanTextField(
                    value = purpose,
                    onValueChange = onPurposeChange,
                    label = stringResource(R.string.loan_purpose_detail_label),
                    icon = Icons.Outlined.Edit,
                    singleLine = false,
                    minLines = 2,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoanInputSectionPreview() {
    val dummyPlafond = Plafond(1, "Kredit Mikro", 5000000, 50000000, 12, 1.5, true)
    val dummyLimit = LoanLimit(1, 1, "Kredit Mikro", 50000000, 50000000, false)
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    MaterialTheme {
        LoanInputSection(
            amount = "10000000",
            onAmountChange = {},
            tenor = "12",
            onTenorChange = {},
            purpose = "Renovasi Rumah",
            onPurposeChange = {},
            selectedPlafond = dummyPlafond,
            selectedLimit = dummyLimit,
            currencyFormatter = currencyFormatter,
        )
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
                    Text(stringResource(R.string.loan_sim_title), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.loan_card_interest, simulation.interestRate.toString()),
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
                    Text(stringResource(R.string.loan_sim_monthly), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
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
                    label = stringResource(R.string.loan_sim_total_interest),
                    value = currencyFormatter.format(simulation.totalInterest),
                    modifier = Modifier.weight(1f),
                )
                SimulationDetailBox(
                    label = stringResource(R.string.loan_sim_total_repayment),
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
            stringResource(R.string.loan_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.loan_empty_desc),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

/**
 * Alert component that warns users about payslip requirement for business loans.
 * Displayed when a business loan plafond is selected but user hasn't uploaded payslip.
 */
@Composable
fun PayslipRequirementAlert(
    plafondTitle: String,
    modifier: Modifier = Modifier,
) {
    // Color palette
    // Color palette
    val warningColor = WarningColor
    val warningBgColor = WarningBgColor
    val warningTextColor = WarningTextColor

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = warningBgColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Warning Icon
            Surface(
                color = warningColor.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = warningColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = stringResource(R.string.loan_alert_payslip_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = warningTextColor,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = stringResource(R.string.loan_alert_payslip_desc, plafondTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = warningTextColor.copy(alpha = 0.8f),
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action hint
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = warningColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.loan_alert_payslip_action),
                        style = MaterialTheme.typography.labelLarge,
                        color = warningTextColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun ModernLoanSuccessDialog(
    onDismiss: () -> Unit,
    isOffline: Boolean = false,
    offlineMessage: String? = null,
) {
    val iconColor = if (isOffline) WarningColor else SuccessColor
    val iconBgColor = if (isOffline) WarningBgColor else SuccessBgColor
    val icon = if (isOffline) Icons.Default.CloudQueue else Icons.Default.CheckCircle
    val title = if (isOffline) stringResource(R.string.loan_dialog_offline_title) else stringResource(R.string.loan_dialog_success_title)

    val message = when {
        isOffline && !offlineMessage.isNullOrBlank() -> offlineMessage
        isOffline -> stringResource(R.string.loan_dialog_offline_desc)
        else -> stringResource(R.string.loan_dialog_success_desc)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.loan_dialog_button_status), fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Surface(
                color = iconBgColor,
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(50.dp))
                }
            }
        },
        title = { Text(title, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    message,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (isOffline) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = WarningBgColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = WarningColor,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.loan_mode_offline),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningTextColor,
                            )
                        }
                    }
                }
            }
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
