package com.example.genggaminmobile.ui.features.loan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.genggaminmobile.R
import com.example.genggaminmobile.ui.components.SignaturePadWithControls
import com.example.genggaminmobile.ui.components.SignaturePoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class containing contract display information
 */
data class ContractDisplayInfo(
    val customerName: String,
    val customerNik: String,
    val customerAddress: String,
    val amount: Long,
    val tenor: Int,
    val interestRate: Double,
    val monthlyInstallment: Long,
    val totalRepayment: Long,
    val purpose: String,
)

/**
 * Modern contract signing dialog with signature pad
 * Shows loan terms and allows digital signature
 */
@Composable
fun LoanContractDialog(
    contractInfo: ContractDisplayInfo,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (signaturePath: List<SignaturePoint>) -> Unit,
) {
    val scrollState = rememberScrollState()
    val signaturePathState = remember { mutableStateOf<List<SignaturePoint>>(emptyList()) }
    var hasSignature by remember { mutableStateOf(false) }
    var isAgreed by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val currentDate = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                ContractDialogHeader(onDismiss = { if (!isLoading) onDismiss() })

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Contract Title
                    ContractTitle()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Parties Section
                    ContractPartiesSection(contractInfo)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Loan Terms Section
                    ContractTermsSection(contractInfo, currencyFormatter)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Obligations Section
                    ContractObligationsSection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Penalty Section
                    ContractPenaltySection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Default Section
                    ContractDefaultSection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Closing Section
                    ContractClosingSection(currentDate)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Signature Pad
                    SignaturePadWithControls(
                        signaturePathState = signaturePathState,
                        onSignatureChanged = { hasSignature = it },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Agreement Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isAgreed,
                            onCheckedChange = {
                                isAgreed = it
                                showError = null
                            },
                            enabled = !isLoading,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.contract_checkbox_agree),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Error Message
                    AnimatedVisibility(visible = showError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = showError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Footer with Submit Button
                val signatureError = stringResource(R.string.contract_error_signature)
                val termsError = stringResource(R.string.contract_error_terms)
                ContractDialogFooter(
                    isLoading = isLoading,
                    onSubmit = {
                        when {
                            !hasSignature -> {
                                showError = signatureError
                            }
                            !isAgreed -> {
                                showError = termsError
                            }
                            else -> {
                                showError = null
                                onConfirm(signaturePathState.value)
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractDialogHeader(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.contract_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.contract_dialog_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContractTitle() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.contract_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Nomor: LOAN/${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}/PREVIEW",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContractPartiesSection(contractInfo: ContractDisplayInfo) {
    ContractSectionCard(title = stringResource(R.string.contract_parties_title)) {
        // First Party
        Text(
            text = stringResource(R.string.contract_party_first),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Nama: ${stringResource(R.string.contract_party_first_name)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp),
        )
        Text(
            text = "Alamat: ${stringResource(R.string.contract_party_first_address)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Second Party
        Text(
            text = stringResource(R.string.contract_party_second),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Nama: ${contractInfo.customerName}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp),
        )
        Text(
            text = "NIK: ${contractInfo.customerNik}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp),
        )
        Text(
            text = "Alamat: ${contractInfo.customerAddress}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun ContractTermsSection(
    contractInfo: ContractDisplayInfo,
    currencyFormatter: NumberFormat,
) {
    ContractSectionCard(title = stringResource(R.string.contract_terms_title)) {
        ContractTermRow(
            label = stringResource(R.string.contract_term_amount),
            value = currencyFormatter.format(contractInfo.amount),
        )
        ContractTermRow(
            label = stringResource(R.string.contract_term_tenor),
            value = stringResource(R.string.contract_term_tenor_months, contractInfo.tenor),
        )
        ContractTermRow(
            label = stringResource(R.string.contract_term_interest),
            value = stringResource(R.string.contract_term_interest_percent, contractInfo.interestRate),
        )
        ContractTermRow(
            label = stringResource(R.string.contract_term_installment),
            value = currencyFormatter.format(contractInfo.monthlyInstallment),
        )
        ContractTermRow(
            label = stringResource(R.string.contract_term_total),
            value = currencyFormatter.format(contractInfo.totalRepayment),
        )
        ContractTermRow(
            label = stringResource(R.string.contract_term_purpose),
            value = contractInfo.purpose,
        )
    }
}

@Composable
private fun ContractTermRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ContractObligationsSection() {
    ContractSectionCard(title = stringResource(R.string.contract_obligations_title)) {
        Text(
            text = stringResource(R.string.contract_obligation_1, "5"),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        Text(
            text = stringResource(R.string.contract_obligation_2),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        Text(
            text = stringResource(R.string.contract_obligation_3),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        Text(
            text = stringResource(R.string.contract_obligation_4),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun ContractPenaltySection() {
    ContractSectionCard(title = stringResource(R.string.contract_penalty_title)) {
        Text(
            text = stringResource(R.string.contract_penalty_text),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Justify,
        )
    }
}

@Composable
private fun ContractDefaultSection() {
    ContractSectionCard(title = stringResource(R.string.contract_default_title)) {
        Text(
            text = stringResource(R.string.contract_default_text),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Justify,
        )
    }
}

@Composable
private fun ContractClosingSection(currentDate: String) {
    ContractSectionCard(title = stringResource(R.string.contract_closing_title)) {
        Text(
            text = stringResource(R.string.contract_closing_text),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Justify,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.contract_date_place, currentDate),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ContractSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ContractDialogFooter(
    isLoading: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.contract_loading),
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.contract_submit_button),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
