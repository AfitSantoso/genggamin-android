package com.example.genggaminmobile.ui.features.loan

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.genggaminmobile.domain.model.Loan
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanHistoryScreen(
    onBack: () -> Unit,
    onViewProgress: (Long) -> Unit = {},
    viewModel: LoanHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    var selectedLoan by remember { mutableStateOf<Loan?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showDetailSheet by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle cancel success/error feedback
    LaunchedEffect(uiState.cancelSuccess, uiState.cancelError) {
        if (uiState.cancelSuccess) {
            showDetailSheet = false
            selectedLoan = null
            snackbarHostState.showSnackbar(
                message = "Pengajuan berhasil dibatalkan",
                duration = SnackbarDuration.Short,
            )
            viewModel.clearCancelState()
        }
        uiState.cancelError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearCancelState()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (uiState.isHistoryView) "Riwayat Pinjaman" else "Pinjaman Aktif",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLoans() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // View Switcher (Active vs History)
            ViewSwitcher(
                isHistoryView = uiState.isHistoryView,
                onViewChange = { viewModel.setHistoryView(it) },
            )

            if (uiState.isHistoryView) {
                FilterSection(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.setFilter(it) },
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = uiState.isLoading to uiState.filteredLoans.isEmpty(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "HistoryContentTransition",
                ) { (isLoading, isEmpty) ->
                    when {
                        isLoading && uiState.filteredLoans.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        }
                        uiState.error != null && uiState.filteredLoans.isEmpty() -> {
                            ErrorState(error = uiState.error!!, onRetry = { viewModel.loadLoans() })
                        }
                        isEmpty -> {
                            EmptyState(isHistory = uiState.isHistoryView)
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                item {
                                    LoanSummaryHeader(uiState.filteredLoans, currencyFormatter, uiState.isHistoryView)
                                }

                                items(uiState.filteredLoans) { loan ->
                                    ModernLoanItem(
                                        loan = loan,
                                        currencyFormatter = currencyFormatter,
                                        modifier = Modifier
                                            .padding(horizontal = 20.dp)
                                            .clickable {
                                                selectedLoan = loan
                                                showDetailSheet = true
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailSheet && selectedLoan != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            LoanDetailContent(
                loan = selectedLoan!!,
                currencyFormatter = currencyFormatter,
                onClose = { showDetailSheet = false },
                onViewProgress = onViewProgress,
                onCancelOffline = {
                    showCancelDialog = true
                },
                isCancelling = uiState.isCancelling,
            )
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog && selectedLoan != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = {
                Text(
                    "Batalkan Pengajuan?",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "Pengajuan pinjaman ini akan dihapus secara permanen. Anda dapat mengajukan kembali kapan saja.",
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        // Pass localId - for offline loans without remoteId, use the loan.id
                        // Since offline loans use localId stored in Room
                        val loanToCancel = selectedLoan
                        if (loanToCancel != null) {
                            // For offline loans, the id field comes from localId mapping
                            viewModel.cancelOfflineLoan(loanToCancel.id ?: 0L)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Tidak")
                }
            },
        )
    }
}

@Composable
fun ViewSwitcher(isHistoryView: Boolean, onViewChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (!isHistoryView) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onViewChange(false) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Pinjaman Aktif",
                color = if (!isHistoryView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (!isHistoryView) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isHistoryView) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onViewChange(true) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Riwayat",
                color = if (isHistoryView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isHistoryView) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun FilterSection(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf(
        "ALL" to "Semua",
        "ACTIVE" to "Aktif",
        "REJECTED" to "Ditolak",
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { (id, label) ->
            val isSelected = selectedFilter == id
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(id) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Text("Coba Lagi")
        }
    }
}

@Composable
fun EmptyState(isHistory: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isHistory) Icons.Outlined.History else Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isHistory) "Belum Ada Riwayat" else "Tidak Ada Pinjaman Aktif",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isHistory) "Anda belum memiliki riwayat peminjaman saat ini." else "Saat ini Anda tidak memiliki pinjaman yang sedang berjalan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoanSummaryHeader(loans: List<Loan>, currencyFormatter: NumberFormat, isHistory: Boolean) {
    val totalAmount = loans.sumOf { it.amount.toDouble() }.toLong()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            )
            .padding(24.dp),
    ) {
        Column {
            Text(
                if (isHistory) "Total Keseluruhan" else "Total Pinjaman Aktif",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = currencyFormatter.format(totalAmount),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "${loans.size} Transaksi",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun ModernLoanItem(loan: Loan, currencyFormatter: NumberFormat, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = getStatusColor(loan.status).first.copy(alpha = 0.1f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getStatusIcon(loan.status),
                                contentDescription = null,
                                tint = getStatusColor(loan.status).second,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = loan.purpose ?: "Pinjaman Personal",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Ref: #${loan.id.toString().takeLast(6)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ModernStatusChip(status = loan.status)
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Jumlah Pinjaman", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = currencyFormatter.format(loan.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun LoanDetailContent(
    loan: Loan,
    currencyFormatter: NumberFormat,
    onClose: () -> Unit,
    onViewProgress: (Long) -> Unit = {},
    onCancelOffline: () -> Unit = {},
    isCancelling: Boolean = false,
) {
    val formattedDate = remember(loan.date) {
        if (loan.date != null) {
            try {
                // Mencoba memparsing format standar ISO dari backend atau format lokal yang disimpan
                val inputFormat = if (loan.date.contains("T")) {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                } else {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                }
                val date = inputFormat.parse(loan.date)
                if (date != null) {
                    SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(date)
                } else {
                    loan.date
                }
            } catch (e: Exception) {
                loan.date
            }
        } else {
            "-"
        }
    }

    // Check if this is an offline pending loan
    val isOfflinePending = loan.status.contains("Offline", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            "Detail Pinjaman",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Process Path (Stepper)
        LoanProcessPath(status = loan.status)

        Spacer(modifier = Modifier.height(32.dp))

        // Info Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DetailRow("Nomor Referensi", "#${loan.id}")
                DetailRow("Tujuan", loan.purpose ?: "Personal")
                DetailRow("Jumlah Pinjaman", currencyFormatter.format(loan.amount))
                DetailRow("Tenor", "${loan.tenorMonths} Bulan")
                DetailRow("Suku Bunga", "${loan.interestRate ?: 0.0}%")
                DetailRow("Tanggal Pengajuan", formattedDate)
                DetailRow("Status", loan.status.uppercase(), color = getStatusColor(loan.status).second)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Show "Lihat Progress" button if loan is still in progress (including APPROVED, before Disbursed)
        val isInProgress = loan.status.lowercase() in listOf("submitted", "pending", "menunggu", "under_review", "proses_verifikasi", "approved", "disetujui")

        if (isInProgress) {
            Button(
                onClick = { loan.id?.let { onViewProgress(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lihat Progress Real-Time", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Show "Batalkan Pengajuan" button only for offline pending loans
        if (isOfflinePending) {
            OutlinedButton(
                onClick = onCancelOffline,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isCancelling,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Membatalkan...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.Outlined.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan Pengajuan", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text("Tutup", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LoanProcessPath(status: String) {
    val statusLower = status.lowercase()
    val currentStep = when (statusLower) {
        "submitted", "pending", "menunggu" -> 1
        "under_review", "proses_verifikasi" -> 2
        "approved", "disetujui" -> 3
        "disbursed", "cair" -> 4
        "rejected", "ditolak" -> -1
        else -> 0
    }

    val steps = listOf("Submit", "Verifikasi", "Disetujui", "Cair")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, title ->
                val stepIndex = index + 1
                // Logika ceklis (isCompleted):
                // Jika sudah melewati langkah tersebut atau status saat ini adalah langkah tersebut.
                val isCompleted = if (currentStep == -1) {
                    stepIndex < 2 // Jika ditolak, hanya langkah pertama (Submit) yang mungkin centang
                } else {
                    stepIndex <= currentStep
                }

                val isCurrent = stepIndex == currentStep
                val isFailed = statusLower in listOf("rejected", "ditolak") && stepIndex == 2

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Connector Line
                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 40.dp)
                                    .width(60.dp)
                                    .height(2.dp)
                                    .background(
                                        if (isCompleted && stepIndex < currentStep) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                            )
                        }

                        // Step Circle
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = when {
                                isFailed -> MaterialTheme.colorScheme.error
                                isCompleted -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = if (!isCompleted && !isFailed) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCompleted && !isFailed) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else if (isFailed) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (statusLower in listOf("rejected", "ditolak")) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Pengajuan Anda ditolak pada tahap verifikasi.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ModernStatusChip(status: String) {
    val (backgroundColor, contentColor) = getStatusColor(status)

    Surface(
        color = backgroundColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            letterSpacing = 0.5.sp,
        )
    }
}

fun getStatusColor(status: String): Pair<Color, Color> = when (status.lowercase()) {
    "approved", "disetujui", "disbursed", "cair" -> Color(0xFF4CAF50) to Color(0xFF1B5E20)
    "submitted", "pending", "menunggu", "under_review" -> Color(0xFFFF9800) to Color(0xFFE65100)
    "rejected", "ditolak" -> Color(0xFFF44336) to Color(0xFFB71C1C)
    else -> Color(0xFF9E9E9E) to Color(0xFF424242)
}

fun getStatusIcon(status: String): ImageVector = when (status.lowercase()) {
    "approved", "disetujui", "disbursed", "cair" -> Icons.Default.CheckCircle
    "submitted", "pending", "menunggu", "under_review" -> Icons.Default.Schedule
    "rejected", "ditolak" -> Icons.Default.Cancel
    else -> Icons.Default.HelpOutline
}
