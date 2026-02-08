package com.example.genggaminmobile.ui.features.profile

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.genggaminmobile.data.model.dto.CustomerProfileRequest
import com.example.genggaminmobile.data.model.dto.EmergencyContactDto
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // Form State
    var nik by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var pob by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var currentAddress by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var bankHolder by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyRelation by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }

    var ktpFile by remember { mutableStateOf<File?>(null) }
    var selfieFile by remember { mutableStateOf<File?>(null) }
    var payslipFile by remember { mutableStateOf<File?>(null) }

    // Track if existing documents have been deleted by user
    var existingKtpDeleted by remember { mutableStateOf(false) }
    var existingSelfieDeleted by remember { mutableStateOf(false) }
    var existingPayslipDeleted by remember { mutableStateOf(false) }

    // Calculate effective upload status (considering both new files and existing non-deleted ones)
    val isKtpAvailable = ktpFile != null || (!uiState.profile?.ktpImagePath.isNullOrEmpty() && !existingKtpDeleted)
    val isSelfieAvailable = selfieFile != null || (!uiState.profile?.selfieImagePath.isNullOrEmpty() && !existingSelfieDeleted)
    // Payslip is optional, so we don't block validation on it

    // Validation Logic for each step
    val isStepValid = when (uiState.currentStep) {
        1 -> nik.length == 16 && dob.isNotEmpty() && pob.isNotEmpty() && address.isNotEmpty() && phone.isNotEmpty()
        2 -> income.isNotEmpty() && occupation.isNotEmpty() && currentAddress.isNotEmpty() && motherName.isNotEmpty()
        3 -> bankAccount.isNotEmpty() && bankHolder.isNotEmpty()
        4 -> emergencyName.isNotEmpty() && emergencyRelation.isNotEmpty() && emergencyPhone.isNotEmpty()
        // Step 5: KTP and Selfie are required, Payslip is OPTIONAL
        5 -> isKtpAvailable && isSelfieAvailable
        else -> false
    }

    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { p ->
            nik = p.nik
            dob = p.dateOfBirth
            pob = p.placeOfBirth
            address = p.address
            phone = p.customerPhone
            income = formatCurrencyInput(p.monthlyIncome.toString())
            occupation = p.occupation
            currentAddress = p.currentAddress
            motherName = p.motherMaidenName
            bankAccount = p.accountNumber
            bankHolder = p.accountHolderName
            p.emergencyContacts.firstOrNull()?.let { e ->
                emergencyName = e.name ?: ""
                emergencyRelation = e.relationship ?: ""
                emergencyPhone = e.phone ?: ""
            }

            // Reset file states and delete flags when profile is loaded (fresh data from server)
            ktpFile = null
            selfieFile = null
            payslipFile = null
            existingKtpDeleted = false
            existingSelfieDeleted = false
            existingPayslipDeleted = false
        }
    }

    if (uiState.isUpdateSuccess) {
        ModernSuccessDialog(onDismiss = {
            viewModel.resetUpdateSuccess()
            // Reload profile to get the latest data from server
            // Backend now correctly handles deletePayslip flag
            viewModel.loadProfile()
        })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditing && uiState.profile != null) {
                            viewModel.cancelEditing()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = uiState.isLoading to (uiState.isEditing || uiState.profile == null),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ProfileContentTransition",
            ) { (isLoading, isEditingMode) ->
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        }
                    }
                    !isEditingMode && uiState.profile != null -> {
                        ProfileSummaryView(
                            profile = uiState.profile!!,
                            onEdit = { viewModel.startEditing() },
                            currencyFormatter = currencyFormatter,
                            lastUpdated = uiState.lastUpdated,
                        )
                    }
                    else -> {
                        // Form Mode
                        Column(modifier = Modifier.fillMaxSize()) {
                            ModernStepIndicator(currentStep = uiState.currentStep, totalSteps = uiState.totalSteps)

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                            ) {
                                item {
                                    Text(
                                        text = getStepTitle(uiState.currentStep),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = "Mohon lengkapi semua bidang di bawah ini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                item {
                                    when (uiState.currentStep) {
                                        1 -> PersonalDataStep(nik, { nik = it }, dob, { dob = it }, pob, { pob = it }, address, { address = it }, phone, { phone = it })
                                        2 -> FinancialDataStep(income, { income = formatCurrencyInput(it) }, occupation, { occupation = it }, currentAddress, { currentAddress = it }, motherName, { motherName = it })
                                        3 -> BankDataStep(bankAccount, { bankAccount = it }, bankHolder, { bankHolder = it })
                                        4 -> EmergencyContactStep(emergencyName, { emergencyName = it }, emergencyRelation, { emergencyRelation = it }, emergencyPhone, { emergencyPhone = it })
                                        5 -> DocumentUploadStep(
                                            ktp = ktpFile,
                                            onKtpSelect = {
                                                ktpFile = it
                                                existingKtpDeleted = false
                                            },
                                            onKtpDelete = {
                                                ktpFile = null
                                                existingKtpDeleted = true
                                            },
                                            selfie = selfieFile,
                                            onSelfieSelect = {
                                                selfieFile = it
                                                existingSelfieDeleted = false
                                            },
                                            onSelfieDelete = {
                                                selfieFile = null
                                                existingSelfieDeleted = true
                                            },
                                            payslip = payslipFile,
                                            onPayslipSelect = {
                                                payslipFile = it
                                                existingPayslipDeleted = false
                                            },
                                            onPayslipDelete = {
                                                payslipFile = null
                                                existingPayslipDeleted = true
                                            },
                                            existingProfile = uiState.profile,
                                            existingKtpDeleted = existingKtpDeleted,
                                            existingSelfieDeleted = existingSelfieDeleted,
                                            existingPayslipDeleted = existingPayslipDeleted,
                                            lastUpdated = uiState.lastUpdated,
                                        )
                                    }
                                }
                            }

                            // Bottom Buttons
                            Surface(
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 16.dp,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (uiState.currentStep > 1) {
                                        OutlinedButton(
                                            onClick = { viewModel.previousStep() },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            shape = RoundedCornerShape(16.dp),
                                        ) {
                                            Text("Kembali", fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (uiState.currentStep < uiState.totalSteps) {
                                                viewModel.nextStep()
                                            } else {
                                                // Determine if user wants to delete existing payslip
                                                // This is true when: existing payslip was marked as deleted AND no new payslip was uploaded
                                                val shouldDeletePayslip = existingPayslipDeleted && payslipFile == null

                                                val request = CustomerProfileRequest(
                                                    nik = nik, dateOfBirth = dob, placeOfBirth = pob,
                                                    address = address, phone = phone, monthlyIncome = income.replace(".", "").toLongOrNull() ?: 0L,
                                                    occupation = occupation, currentAddress = currentAddress,
                                                    motherMaidenName = motherName, accountNumber = bankAccount,
                                                    accountHolderName = bankHolder,
                                                    emergencyContact = EmergencyContactDto(name = emergencyName, relationship = emergencyRelation, phone = emergencyPhone),
                                                    deletePayslip = shouldDeletePayslip,
                                                )
                                                viewModel.submitProfile(request, ktpFile, selfieFile, payslipFile)
                                            }
                                        },
                                        enabled = isStepValid && !uiState.isSubmitting, // Button is disabled if invalid or submitting
                                        modifier = Modifier.weight(if (uiState.currentStep > 1) 1.5f else 1f).height(54.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                    ) {
                                        if (uiState.isSubmitting) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text(
                                                if (uiState.currentStep == uiState.totalSteps) "Simpan Profil" else "Lanjut",
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSummaryView(
    profile: com.example.genggaminmobile.data.model.dto.CustomerProfileResponse,
    onEdit: () -> Unit,
    currencyFormatter: NumberFormat,
    lastUpdated: Long,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                HeaderSection(profile)
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))

                    ModernInfoCard(
                        title = "Data Pribadi",
                        icon = Icons.Outlined.Person,
                        items = listOf(
                            "NIK" to profile.nik,
                            "Tanggal Lahir" to profile.dateOfBirth,
                            "Tempat Lahir" to profile.placeOfBirth,
                            "Telepon" to profile.customerPhone,
                            "Alamat KTP" to profile.address,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernInfoCard(
                        title = "Data Keuangan",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        items = listOf(
                            "Pekerjaan" to profile.occupation,
                            "Pendapatan" to currencyFormatter.format(profile.monthlyIncome),
                            "Nama Ibu" to profile.motherMaidenName,
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernInfoCard(
                        title = "Rekening Bank",
                        icon = Icons.Outlined.AccountBalance,
                        items = listOf(
                            "Nomor Rekening" to profile.accountNumber,
                            "Nama Pemilik" to profile.accountHolderName,
                        ),
                    )

                    if (profile.emergencyContacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val ec = profile.emergencyContacts.firstOrNull()
                        ModernInfoCard(
                            title = "Kontak Darurat",
                            icon = Icons.Outlined.ContactPhone,
                            items = listOf(
                                "Nama" to (ec?.name ?: "-"),
                                "Hubungan" to (ec?.relationship ?: "-"),
                                "Telepon" to (ec?.phone ?: "-"),
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernDocumentSection(
                        ktpPath = profile.ktpImagePath,
                        selfiePath = profile.selfieImagePath,
                        payslipPath = profile.payslipImagePath,
                        lastUpdated = lastUpdated,
                    )
                }
            }
        }

        // Bottom Button - Fixed at bottom
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Perbarui Profil", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeaderSection(profile: com.example.genggaminmobile.data.model.dto.CustomerProfileResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(90.dp).border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.fullName ?: "Pengguna Genggamin",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = profile.email ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    "Verified Member",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun ModernInfoCard(title: String, icon: ImageVector, items: List<Pair<String, String>>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            items.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (index < items.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ModernDocumentSection(ktpPath: String?, selfiePath: String?, payslipPath: String?, lastUpdated: Long) {
    // Check if payslip exists (not null and not blank)
    val hasPayslip = !payslipPath.isNullOrBlank()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dokumen & KYC", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic layout based on whether payslip exists
            if (hasPayslip) {
                // Show all 3 documents
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DocumentItem(label = "KTP", url = ktpPath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                    DocumentItem(label = "Selfie", url = selfiePath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                    DocumentItem(label = "Slip Gaji", url = payslipPath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                }
            } else {
                // Only show required documents (KTP and Selfie)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DocumentItem(label = "KTP", url = ktpPath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                    DocumentItem(label = "Selfie", url = selfiePath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                }
                // Optional: Show info that payslip is not uploaded
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Slip gaji tidak diunggah (opsional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItem(label: String, url: String?, lastUpdated: Long, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!url.isNullOrBlank()) {
                // Key for recomposition when url or lastUpdated changes
                key(url, lastUpdated) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(if (url.startsWith("/")) File(url) else url)
                            .memoryCacheKey("$url-$lastUpdated")
                            .diskCacheKey("$url-$lastUpdated")
                            .crossfade(true)
                            .build(),
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Icon(Icons.Outlined.ImageNotSupported, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ModernStepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(totalSteps) { index ->
            val step = index + 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (step <= currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                    ),
            )
        }
    }
}

@Composable
fun ModernSuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Siap!")
            }
        },
        icon = {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
        },
        title = { Text("Berhasil!", fontWeight = FontWeight.Bold) },
        text = { Text("Profil Anda telah berhasil diperbarui. Data sudah aman tersimpan.") },
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
fun PersonalDataStep(nik: String, onNikChange: (String) -> Unit, dob: String, onDobChange: (String) -> Unit, pob: String, onPobChange: (String) -> Unit, address: String, onAddressChange: (String) -> Unit, phone: String, onPhoneChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(value = nik, onValueChange = onNikChange, label = "NIK (Wajib 16 Digit)", icon = Icons.Outlined.Badge, keyboardType = KeyboardType.Number)

        // Date of Birth with DatePicker
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.YEAR, -18)
        val maxDate = calendar.timeInMillis

        ModernDatePickerField(
            value = dob,
            onValueChange = onDobChange,
            label = "Tanggal Lahir",
            icon = Icons.Outlined.CalendarMonth,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= maxDate
                }
                override fun isSelectableYear(year: Int): Boolean {
                    return year <= calendar.get(java.util.Calendar.YEAR)
                }
            },
        )

        ModernTextField(value = pob, onValueChange = onPobChange, label = "Tempat Lahir", icon = Icons.Outlined.Place)
        ModernTextField(value = address, onValueChange = onAddressChange, label = "Alamat Sesuai KTP", icon = Icons.Outlined.Home, singleLine = false, minLines = 2)
        ModernTextField(value = phone, onValueChange = onPhoneChange, label = "Nomor Telepon", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(selectableDates = selectableDates)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(20.dp))
                }
            },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            shape = RoundedCornerShape(16.dp),
            enabled = false, // Disable manual typing
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = Color.Transparent,
            ),
        )

        // Invisible overlay to capture clicks since the field is disabled
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true },
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onValueChange(sdf.format(Date(it)))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun FinancialDataStep(income: String, onIncomeChange: (String) -> Unit, occupation: String, onOccupationChange: (String) -> Unit, currentAddress: String, onCurrentAddressChange: (String) -> Unit, motherName: String, onMotherNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(value = income, onValueChange = onIncomeChange, label = "Pendapatan Per Bulan", icon = Icons.Outlined.Payments, keyboardType = KeyboardType.Number, prefix = "Rp ")
        OccupationInput(value = occupation, onValueChange = onOccupationChange)
        ModernTextField(value = motherName, onValueChange = onMotherNameChange, label = "Nama Ibu Kandung", icon = Icons.Outlined.Face)
        ModernTextField(value = currentAddress, onValueChange = onCurrentAddressChange, label = "Alamat Tinggal Sekarang", icon = Icons.Outlined.LocationOn, singleLine = false, minLines = 2)
    }
}

@Composable
fun BankDataStep(account: String, onAccountChange: (String) -> Unit, holder: String, onHolderChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(value = account, onValueChange = onAccountChange, label = "Nomor Rekening", icon = Icons.Outlined.Numbers, keyboardType = KeyboardType.Number)
        ModernTextField(value = holder, onValueChange = onHolderChange, label = "Nama Pemilik Rekening", icon = Icons.Outlined.Person)
    }
}

@Composable
fun EmergencyContactStep(name: String, onNameChange: (String) -> Unit, relation: String, onRelationChange: (String) -> Unit, phone: String, onPhoneChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(value = name, onValueChange = onNameChange, label = "Nama Kontak Darurat", icon = Icons.Outlined.AccountCircle)
        ModernTextField(value = relation, onValueChange = onRelationChange, label = "Hubungan", icon = Icons.Outlined.People)
        ModernTextField(value = phone, onValueChange = onPhoneChange, label = "Nomor Telepon", icon = Icons.Outlined.Call, keyboardType = KeyboardType.Phone)
    }
}

@Composable
fun DocumentUploadStep(
    ktp: File?,
    onKtpSelect: (File) -> Unit,
    onKtpDelete: () -> Unit,
    selfie: File?,
    onSelfieSelect: (File) -> Unit,
    onSelfieDelete: () -> Unit,
    payslip: File?,
    onPayslipSelect: (File) -> Unit,
    onPayslipDelete: () -> Unit,
    existingProfile: com.example.genggaminmobile.data.model.dto.CustomerProfileResponse? = null,
    existingKtpDeleted: Boolean = false,
    existingSelfieDeleted: Boolean = false,
    existingPayslipDeleted: Boolean = false,
    lastUpdated: Long = 0,
) {
    val context = LocalContext.current
    var showSheetForKtp by remember { mutableStateOf(false) }
    var showSheetForSelfie by remember { mutableStateOf(false) }
    var showSheetForPayslip by remember { mutableStateOf(false) }

    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var currentPickingType by remember { mutableStateOf<String?>(null) }

    fun createTempFile(): File {
        return File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = uriToFile(context, it)
            when (currentPickingType) {
                "ktp" -> onKtpSelect(file)
                "selfie" -> onSelfieSelect(file)
                "payslip" -> onPayslipSelect(file)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                // Fix image orientation before passing to callback
                val fixedFile = fixImageOrientation(file)
                when (currentPickingType) {
                    "ktp" -> onKtpSelect(fixedFile)
                    "selfie" -> onSelfieSelect(fixedFile)
                    "payslip" -> onPayslipSelect(fixedFile)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val file = createTempFile()
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera(type: String) {
        currentPickingType = type
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchGallery(type: String) {
        currentPickingType = type
        galleryLauncher.launch("image/*")
    }

    // Calculate if documents are uploaded (considering new files and existing ones not deleted)
    val isKtpUploaded = ktp != null || (!existingProfile?.ktpImagePath.isNullOrEmpty() && !existingKtpDeleted)
    val isSelfieUploaded = selfie != null || (!existingProfile?.selfieImagePath.isNullOrEmpty() && !existingSelfieDeleted)
    val isPayslipUploaded = payslip != null || (!existingProfile?.payslipImagePath.isNullOrEmpty() && !existingPayslipDeleted)

    // Get preview path (prioritize new file, then existing if not deleted)
    val ktpPreviewPath = when {
        ktp != null -> ktp.absolutePath
        !existingKtpDeleted -> existingProfile?.ktpImagePath
        else -> null
    }
    val selfiePreviewPath = when {
        selfie != null -> selfie.absolutePath
        !existingSelfieDeleted -> existingProfile?.selfieImagePath
        else -> null
    }
    val payslipPreviewPath = when {
        payslip != null -> payslip.absolutePath
        !existingPayslipDeleted -> existingProfile?.payslipImagePath
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernUploadItem(
            label = "Foto KTP",
            isUploaded = isKtpUploaded,
            previewPath = ktpPreviewPath,
            lastUpdated = lastUpdated,
            isOptional = false,
            onDelete = if (isKtpUploaded) onKtpDelete else null,
            onClick = { showSheetForKtp = true },
        )
        ModernUploadItem(
            label = "Foto Selfie + KTP",
            isUploaded = isSelfieUploaded,
            previewPath = selfiePreviewPath,
            lastUpdated = lastUpdated,
            isOptional = false,
            onDelete = if (isSelfieUploaded) onSelfieDelete else null,
            onClick = { showSheetForSelfie = true },
        )
        ModernUploadItem(
            label = "Foto Slip Gaji",
            isUploaded = isPayslipUploaded,
            previewPath = payslipPreviewPath,
            lastUpdated = lastUpdated,
            isOptional = true, // Payslip is optional, especially for business loan applicants
            onDelete = if (isPayslipUploaded) onPayslipDelete else null,
            onClick = { showSheetForPayslip = true },
        )

        // Info card for optional payslip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Slip gaji bersifat opsional. Jika Anda wiraswasta atau tidak memiliki slip gaji, Anda dapat melewati dokumen ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }

    if (showSheetForKtp) {
        ImagePickerSheet(
            onDismiss = { showSheetForKtp = false },
            onCamera = {
                launchCamera("ktp")
                showSheetForKtp = false
            },
            onGallery = {
                launchGallery("ktp")
                showSheetForKtp = false
            },
        )
    }
    if (showSheetForSelfie) {
        ImagePickerSheet(
            onDismiss = { showSheetForSelfie = false },
            onCamera = {
                launchCamera("selfie")
                showSheetForSelfie = false
            },
            onGallery = {
                launchGallery("selfie")
                showSheetForSelfie = false
            },
        )
    }
    if (showSheetForPayslip) {
        ImagePickerSheet(
            onDismiss = { showSheetForPayslip = false },
            onCamera = {
                launchCamera("payslip")
                showSheetForPayslip = false
            },
            onGallery = {
                launchGallery("payslip")
                showSheetForPayslip = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Pilih Sumber Foto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PickerOption(Icons.Default.CameraAlt, "Kamera", onClick = onCamera)
                PickerOption(Icons.Default.PhotoLibrary, "Galeri", onClick = onGallery)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PickerOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

fun uriToFile(context: Context, uri: Uri): File {
    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
    // Fix orientation for gallery images as well
    return fixImageOrientation(file)
}

/**
 * Fix image orientation based on EXIF data
 * Camera photos often have rotation metadata that needs to be applied
 * to display the image in the correct portrait orientation.
 * This function reads the EXIF data, rotates the bitmap if needed, and saves it back to the file.
 */
fun fixImageOrientation(file: File): File {
    return try {
        val exifInterface = android.media.ExifInterface(file.absolutePath)
        val orientation = exifInterface.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_UNDEFINED,
        )

        val rotationDegrees = when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // Also check for flip orientations
        val needsHorizontalFlip = orientation == android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL
        val needsVerticalFlip = orientation == android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL

        if (rotationDegrees == 0f && !needsHorizontalFlip && !needsVerticalFlip) {
            // No rotation needed
            return file
        }

        // Decode bitmap
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)

        // Create transformation matrix
        val matrix = android.graphics.Matrix()

        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees)
        }

        if (needsHorizontalFlip) {
            matrix.postScale(-1f, 1f)
        }

        if (needsVerticalFlip) {
            matrix.postScale(1f, -1f)
        }

        // Create rotated bitmap
        val rotatedBitmap = android.graphics.Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )

        // Save rotated bitmap back to file
        FileOutputStream(file).use { out ->
            rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }

        // Clear EXIF orientation tag since we already applied it
        val newExif = android.media.ExifInterface(file.absolutePath)
        newExif.setAttribute(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_NORMAL.toString(),
        )
        newExif.saveAttributes()

        file
    } catch (e: Exception) {
        android.util.Log.e("ProfileScreen", "Error fixing image orientation", e)
        file
    }
}

@Composable
fun ModernUploadItem(
    label: String,
    isUploaded: Boolean,
    previewPath: String? = null,
    lastUpdated: Long = 0,
    isOptional: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isUploaded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!previewPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(if (previewPath.startsWith("/")) File(previewPath) else previewPath)
                            .memoryCacheKey("$previewPath-$lastUpdated")
                            .build(),
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Overlay check icon for better UX
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    if (isOptional) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = "Opsional",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = when {
                        isUploaded -> "Dokumen sudah tersedia"
                        isOptional -> "Opsional, ketuk untuk unggah"
                        else -> "Ketuk untuk unggah"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Delete button - only show when there's an uploaded image
            if (isUploaded && onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp),
                        ),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hapus gambar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        prefix = if (prefix != null) {
            { Text(prefix) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

fun getStepTitle(step: Int) = when (step) {
    1 -> "Data Pribadi"
    2 -> "Informasi Keuangan"
    3 -> "Data Rekening"
    4 -> "Kontak Darurat"
    5 -> "Verifikasi Dokumen"
    else -> ""
}

@Composable
fun OccupationInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val options = listOf(
        "Karyawan Swasta Tetap",
        "Karyawan Swasta Kontrak",
        "PNS / Pegawai Pemerintah",
        "Wiraswasta / UMKM",
        "Profesional (Dokter, Akuntan, dll)",
        "Freelancer / Driver Online",
        "Pensiunan",
        "Lainnya",
    )

    var expanded by remember { mutableStateOf(false) }
    var internalManualMode by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value.isNotEmpty() && !options.contains(value)) {
            internalManualMode = true
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (internalManualMode) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Pekerjaan (Lainnya)") },
                leadingIcon = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        internalManualMode = false
                        onValueChange("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Batal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        } else {
            // Dropdown Mode
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pekerjaan") },
                leadingIcon = { Icon(Icons.Outlined.WorkOutline, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(24.dp))
                },
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape = RoundedCornerShape(16.dp),
            )

            // Overlay for click
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true },
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            if (option == "Lainnya") {
                                internalManualMode = true
                                onValueChange("")
                            } else {
                                internalManualMode = false
                                onValueChange(option)
                            }
                        },
                    )
                }
            }
        }
    }
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
