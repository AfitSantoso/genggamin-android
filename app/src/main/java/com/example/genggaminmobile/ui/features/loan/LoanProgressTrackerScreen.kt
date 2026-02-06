package com.example.genggaminmobile.ui.features.loan

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.genggaminmobile.core.service.LoanTimerService
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanProgressTrackerScreen(
    loanId: Long,
    onBack: () -> Unit,
    viewModel: LoanProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Start tracking and foreground service
    LaunchedEffect(loanId, uiState.submissionTime) {
        viewModel.startTracking(loanId)

        // Start Foreground Service when we have submission time
        // Only start if status is not final (still processing)
        val normalizedStatus = uiState.status.lowercase()
        val isFinalStatus = normalizedStatus in listOf("approved", "disbursed", "rejected", "cair", "disetujui", "ditolak")

        if (uiState.submissionTime != null && !isFinalStatus) {
            LoanTimerService.startService(
                context = context,
                loanId = loanId,
                submissionTime = uiState.submissionTime!!,
            )
        }
    }

    // Stop service when final status is reached
    LaunchedEffect(uiState.status) {
        val normalizedStatus = uiState.status.lowercase()
        val isFinalStatus = normalizedStatus in listOf("approved", "disbursed", "rejected", "cair", "disetujui", "ditolak")

        if (isFinalStatus) {
            LoanTimerService.stopService(context)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Status Pengajuan",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Countdown Timer Card - Now uses persistent end time
            CountdownCard(
                context = context,
                loanId = loanId,
                startTime = uiState.submissionTime,
                status = uiState.status,
            )

            // Progress Pulse Animation
            ProgressPulseSection(
                currentStep = uiState.currentStep,
                status = uiState.status,
                statusMessage = uiState.statusMessage,
            )

            // Detailed Progress Steps
            DetailedProgressSteps(
                currentStep = uiState.currentStep,
                status = uiState.status,
                steps = uiState.progressSteps,
            )

            // Informasi Tambahan
            InfoCard()
        }
    }
}

@Composable
fun CountdownCard(
    context: Context,
    loanId: Long,
    startTime: Long?,
    status: String,
) {
    val targetTimeMinutes = 10
    val normalizedStatus = status.lowercase()
    val isFinalStatus = normalizedStatus in listOf("approved", "disbursed", "rejected", "cair", "disetujui", "ditolak")

    // BEST PRACTICE: Simpan END TIME bukan sisa detik
    // Menghitung endTime = startTime + 10 menit
    val endTime = remember(startTime) {
        if (startTime != null) {
            startTime + (targetTimeMinutes * 60 * 1000L)
        } else {
            System.currentTimeMillis() + (targetTimeMinutes * 60 * 1000L)
        }
    }

    // Calculate remaining time from END TIME (ini yang penting!)
    // Tidak peduli aplikasi mati/restart, endTime tetap sama
    var remainingSeconds by remember { mutableStateOf(0) }

    // Update remaining time setiap detik berdasarkan END TIME
    LaunchedEffect(endTime, isFinalStatus) {
        if (!isFinalStatus && endTime > 0) {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val remaining = endTime - currentTime

                remainingSeconds = if (remaining > 0) (remaining / 1000).toInt() else 0

                if (remainingSeconds <= 0) break

                delay(1000L)
            }
        } else if (isFinalStatus) {
            // Final status - stop timer
            remainingSeconds = 0
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val progress = 1f - (remainingSeconds.toFloat() / (targetTimeMinutes * 60))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = if (isFinalStatus) {
                        when {
                            normalizedStatus in listOf("approved", "disbursed", "disetujui", "cair") ->
                                listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                            normalizedStatus in listOf("rejected", "ditolak") ->
                                listOf(Color(0xFFF44336), Color(0xFFC62828))
                            else -> listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    },
                ),
            )
            .padding(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                when {
                    normalizedStatus in listOf("approved", "disbursed", "disetujui", "cair") -> Icons.Default.CheckCircle
                    normalizedStatus in listOf("rejected", "ditolak") -> Icons.Default.Cancel
                    else -> Icons.Default.Timer
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                when {
                    normalizedStatus in listOf("approved", "disbursed", "disetujui", "cair") -> "Pengajuan Disetujui!"
                    normalizedStatus in listOf("rejected", "ditolak") -> "Pengajuan Ditolak"
                    else -> "Estimasi Waktu Keputusan"
                },
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                when {
                    isFinalStatus -> when {
                        normalizedStatus in listOf("approved", "disbursed", "disetujui", "cair") -> "✓ Selesai"
                        normalizedStatus in listOf("rejected", "ditolak") -> "✗ Ditolak"
                        else -> "Selesai"
                    }
                    remainingSeconds > 0 -> String.format("%02d:%02d", minutes, seconds)
                    else -> "Sedang Diproses"
                },
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )

            if (!isFinalStatus) {
                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Mulai",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "10 Menit",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressPulseSection(
    currentStep: Int,
    status: String,
    statusMessage: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedProgressIcon(currentStep = currentStep, status = status)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            statusMessage,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            getDetailedMessage(currentStep, status),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun AnimatedProgressIcon(currentStep: Int, status: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        val normalizedStatus = status.lowercase()
        // Pulse animation active unless final final state (Disbursed or Rejected)
        // APPROVED is still considered "active" because we are waiting for Disbursement
        if (normalizedStatus !in listOf("disbursed", "cair", "rejected", "ditolak")) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape,
                    ),
            )
        }

        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    when (normalizedStatus) {
                        "approved", "disbursed", "disetujui", "cair" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        "rejected", "ditolak" -> Color(0xFFF44336).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = getStepIcon(currentStep, status),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = when (normalizedStatus) {
                    "approved", "disbursed", "disetujui", "cair" -> Color(0xFF4CAF50)
                    "rejected", "ditolak" -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
fun DetailedProgressSteps(
    currentStep: Int,
    status: String,
    steps: List<ProgressStep>,
) {
    val normalizedStatus = status.lowercase()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Proses Pengajuan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )

        steps.forEachIndexed { index, step ->
            // Determine step status based on current progress
            val stepStatus = when {
                // REJECTED case - step 0 completed, step 1 is failed
                normalizedStatus in listOf("rejected", "ditolak") -> {
                    when (index) {
                        0 -> StepStatus.COMPLETED
                        1 -> StepStatus.FAILED
                        else -> StepStatus.PENDING
                    }
                }

                // DISBURSED - all steps are completed
                normalizedStatus in listOf("disbursed", "cair") -> StepStatus.COMPLETED

                // APPROVED - steps 0,1,2 are completed, step 3 is processing (waiting for disburse)
                normalizedStatus in listOf("approved", "disetujui") -> {
                    when {
                        index <= 2 -> StepStatus.COMPLETED
                        index == 3 -> StepStatus.PROCESSING
                        else -> StepStatus.PENDING
                    }
                }

                // UNDER_REVIEW - steps 0 and 1 are completed, step 2 is processing
                normalizedStatus in listOf("under_review", "proses_verifikasi") -> {
                    when {
                        index <= 1 -> StepStatus.COMPLETED
                        index == 2 -> StepStatus.PROCESSING
                        else -> StepStatus.PENDING
                    }
                }

                // SUBMITTED/PENDING - use currentStep from ViewModel
                normalizedStatus in listOf("submitted", "pending", "menunggu") -> {
                    when {
                        index < currentStep -> StepStatus.COMPLETED
                        index == currentStep -> StepStatus.PROCESSING
                        else -> StepStatus.PENDING
                    }
                }

                // Default fallback
                else -> {
                    when {
                        index < currentStep -> StepStatus.COMPLETED
                        index == currentStep -> StepStatus.PROCESSING
                        else -> StepStatus.PENDING
                    }
                }
            }

            ProgressStepItem(
                step = step,
                isActive = stepStatus == StepStatus.PROCESSING,
                isCompleted = stepStatus == StepStatus.COMPLETED,
                isFailed = stepStatus == StepStatus.FAILED,
            )
        }
    }
}

// Helper enum for step status
private enum class StepStatus {
    PENDING, // Not yet started (gray)
    PROCESSING, // Currently processing (blue with spinner)
    COMPLETED, // Done (green with checkmark)
    FAILED, // Rejected (red with X)
}

@Composable
fun ProgressStepItem(
    step: ProgressStep,
    isActive: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isFailed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.15f) // Hijau jika sudah ada timestamp/lewat
                    isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    when {
                        isFailed -> MaterialTheme.colorScheme.error
                        isCompleted -> Color(0xFF4CAF50) // Hijau
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    isFailed -> Icons.Default.Close
                    isCompleted -> Icons.Default.Check
                    else -> step.icon
                },
                contentDescription = null,
                tint = if (isCompleted || isActive || isFailed) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isFailed -> MaterialTheme.colorScheme.error
                    isCompleted -> Color(0xFF1B5E20) // Hijau Tua
                    isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (step.timestamp != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatTimestamp(step.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) {
                        Color(0xFF2E7D32).copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                )
            }
        }

        if (isActive && !isFailed && !isCompleted) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun InfoCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Target kami adalah 10 menit dari pengajuan hingga keputusan!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
        )
    }
}

fun getStepIcon(step: Int, status: String): ImageVector {
    return when {
        status == "REJECTED" -> Icons.Default.Cancel
        status == "APPROVED" || status == "DISBURSED" -> Icons.Default.CheckCircle
        step == 0 -> Icons.Default.Send
        step == 1 -> Icons.Default.Verified
        step == 2 -> Icons.Default.ThumbUp
        step == 3 -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.HourglassEmpty
    }
}

fun getDetailedMessage(step: Int, status: String): String {
    return when {
        status == "REJECTED" -> "Mohon maaf, pengajuan Anda tidak dapat diproses saat ini."
        status == "APPROVED" || status == "DISBURSED" -> "Selamat! Pengajuan Anda telah disetujui."
        step == 0 -> "Pengajuan Anda telah diterima dan sedang diproses."
        step == 1 -> "Sistem sedang memverifikasi skor kredit Anda secara otomatis."
        step == 2 -> "Data sedang diperiksa untuk persetujuan akhir."
        step == 3 -> "Proses pencairan dana sedang berlangsung."
        else -> "Tunggu sebentar..."
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
