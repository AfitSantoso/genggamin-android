package com.example.genggaminmobile.ui.features.profile

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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.genggaminmobile.data.model.dto.CustomerProfileRequest
import com.example.genggaminmobile.data.model.dto.EmergencyContactDto
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
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

    // Validation Logic for each step
    val isStepValid = when (uiState.currentStep) {
        1 -> nik.length == 16 && dob.isNotEmpty() && pob.isNotEmpty() && address.isNotEmpty() && phone.isNotEmpty()
        2 -> income.isNotEmpty() && occupation.isNotEmpty() && currentAddress.isNotEmpty() && motherName.isNotEmpty()
        3 -> bankAccount.isNotEmpty() && bankHolder.isNotEmpty()
        4 -> emergencyName.isNotEmpty() && emergencyRelation.isNotEmpty() && emergencyPhone.isNotEmpty()
        5 -> ktpFile != null && selfieFile != null && payslipFile != null
        else -> false
    }

    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { p ->
            nik = p.nik
            dob = p.dateOfBirth
            pob = p.placeOfBirth
            address = p.address
            phone = p.customerPhone
            income = p.monthlyIncome.toString()
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
        }
    }

    if (uiState.isUpdateSuccess) {
        ModernSuccessDialog(onDismiss = {
            viewModel.resetUpdateSuccess()
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
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = uiState.isLoading to (uiState.isEditing || uiState.profile == null),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ProfileContentTransition"
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
                            lastUpdated = uiState.lastUpdated
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
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                item {
                                    Text(
                                        text = getStepTitle(uiState.currentStep),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Mohon lengkapi semua bidang di bawah ini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                item {
                                    when (uiState.currentStep) {
                                        1 -> PersonalDataStep(nik, { nik = it }, dob, { dob = it }, pob, { pob = it }, address, { address = it }, phone, { phone = it })
                                        2 -> FinancialDataStep(income, { income = it }, occupation, { occupation = it }, currentAddress, { currentAddress = it }, motherName, { motherName = it })
                                        3 -> BankDataStep(bankAccount, { bankAccount = it }, bankHolder, { bankHolder = it })
                                        4 -> EmergencyContactStep(emergencyName, { emergencyName = it }, emergencyRelation, { emergencyRelation = it }, emergencyPhone, { emergencyPhone = it })
                                        5 -> DocumentUploadStep(
                                            ktpFile, { ktpFile = it },
                                            selfieFile, { selfieFile = it },
                                            payslipFile, { payslipFile = it }
                                        )
                                    }
                                }
                            }

                            // Bottom Buttons
                            Surface(
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (uiState.currentStep > 1) {
                                        OutlinedButton(
                                            onClick = { viewModel.previousStep() },
                                            modifier = Modifier.weight(1f).height(54.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text("Kembali", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            if (uiState.currentStep < uiState.totalSteps) {
                                                viewModel.nextStep()
                                            } else {
                                                val request = CustomerProfileRequest(
                                                    nik = nik, dateOfBirth = dob, placeOfBirth = pob,
                                                    address = address, phone = phone, monthlyIncome = income.toLongOrNull() ?: 0L,
                                                    occupation = occupation, currentAddress = currentAddress,
                                                    motherMaidenName = motherName, accountNumber = bankAccount,
                                                    accountHolderName = bankHolder,
                                                    emergencyContact = EmergencyContactDto(name = emergencyName, relationship = emergencyRelation, phone = emergencyPhone)
                                                )
                                                viewModel.submitProfile(request, ktpFile, selfieFile, payslipFile)
                                            }
                                        },
                                        enabled = isStepValid && !uiState.isSubmitting, // Button is disabled if invalid or submitting
                                        modifier = Modifier.weight(if (uiState.currentStep > 1) 1.5f else 1f).height(54.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                    ) {
                                        if (uiState.isSubmitting) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text(
                                                if (uiState.currentStep == uiState.totalSteps) "Simpan Profil" else "Lanjut",
                                                fontWeight = FontWeight.Bold
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
    lastUpdated: Long
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
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
                        "Alamat KTP" to profile.address
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernInfoCard(
                    title = "Data Keuangan",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    items = listOf(
                        "Pekerjaan" to profile.occupation,
                        "Pendapatan" to currencyFormatter.format(profile.monthlyIncome),
                        "Nama Ibu" to profile.motherMaidenName
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernInfoCard(
                    title = "Rekening Bank",
                    icon = Icons.Outlined.AccountBalance,
                    items = listOf(
                        "Nomor Rekening" to profile.accountNumber,
                        "Nama Pemilik" to profile.accountHolderName
                    )
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
                            "Telepon" to (ec?.phone ?: "-")
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ModernDocumentSection(
                    ktpPath = profile.ktpImagePath,
                    selfiePath = profile.selfieImagePath,
                    payslipPath = profile.payslipImagePath,
                    lastUpdated = lastUpdated
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(90.dp).border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.fullName ?: "Pengguna Genggamin",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = profile.email ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Verified Member",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
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
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    horizontalArrangement = Arrangement.SpaceBetween
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dokumen & KYC", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DocumentItem(label = "KTP", url = ktpPath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                DocumentItem(label = "Selfie", url = selfiePath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
                DocumentItem(label = "Slip Gaji", url = payslipPath, lastUpdated = lastUpdated, modifier = Modifier.weight(1f))
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
            contentAlignment = Alignment.Center
        ) {
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .memoryCacheKey("$url-$lastUpdated")
                        .build(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val step = index + 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (step <= currentStep) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
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
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun PersonalDataStep(nik: String, onNikChange: (String) -> Unit, dob: String, onDobChange: (String) -> Unit, pob: String, onPobChange: (String) -> Unit, address: String, onAddressChange: (String) -> Unit, phone: String, onPhoneChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(value = nik, onValueChange = onNikChange, label = "NIK (Wajib 16 Digit)", icon = Icons.Outlined.Badge, keyboardType = KeyboardType.Number)
        ModernTextField(value = dob, onValueChange = onDobChange, label = "Tanggal Lahir (YYYY-MM-DD)", icon = Icons.Outlined.CalendarMonth)
        ModernTextField(value = pob, onValueChange = onPobChange, label = "Tempat Lahir", icon = Icons.Outlined.Place)
        ModernTextField(value = address, onValueChange = onAddressChange, label = "Alamat Sesuai KTP", icon = Icons.Outlined.Home, singleLine = false, minLines = 2)
        ModernTextField(value = phone, onValueChange = onPhoneChange, label = "Nomor Telepon", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
    }
}

@Composable
fun FinancialDataStep(income: String, onIncomeChange: (String) -> Unit, occupation: String, onOccupationChange: (String) -> Unit, currentAddress: String, onCurrentAddressChange: (String) -> Unit, motherName: String, onMotherNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernTextField(value = income, onValueChange = onIncomeChange, label = "Pendapatan Per Bulan", icon = Icons.Outlined.Payments, keyboardType = KeyboardType.Number, prefix = "Rp ")
        ModernTextField(value = occupation, onValueChange = onOccupationChange, label = "Pekerjaan", icon = Icons.Outlined.WorkOutline)
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
fun DocumentUploadStep(ktp: File?, onKtpSelect: (File) -> Unit, selfie: File?, onSelfieSelect: (File) -> Unit, payslip: File?, onPayslipSelect: (File) -> Unit) {
    val context = LocalContext.current
    fun uriToFile(uri: Uri): File {
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        return file
    }
    val ktpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onKtpSelect(uriToFile(it)) } }
    val selfieLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onSelfieSelect(uriToFile(it)) } }
    val payslipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onPayslipSelect(uriToFile(it)) } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ModernUploadItem("Foto KTP", ktp != null, onClick = { ktpLauncher.launch("image/*") })
        ModernUploadItem("Foto Selfie + KTP", selfie != null, onClick = { selfieLauncher.launch("image/*") })
        ModernUploadItem("Foto Slip Gaji", payslip != null, onClick = { payslipLauncher.launch("image/*") })
    }
}

@Composable
fun ModernUploadItem(label: String, isUploaded: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isUploaded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isUploaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUploaded) Icons.Default.Check else Icons.Outlined.FileUpload,
                    contentDescription = null,
                    tint = if (isUploaded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(if (isUploaded) "Dokumen terpilih" else "Ketuk untuk unggah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        prefix = if (prefix != null) { { Text(prefix) } } else null,
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

fun getStepTitle(step: Int) = when(step) {
    1 -> "Data Pribadi"
    2 -> "Informasi Keuangan"
    3 -> "Data Rekening"
    4 -> "Kontak Darurat"
    5 -> "Verifikasi Dokumen"
    else -> ""
}
