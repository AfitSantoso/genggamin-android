package com.example.genggaminmobile.ui.features.loan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.genggaminmobile.R
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationScreen(
    onBack: () -> Unit,
    viewModel: LoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_app_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.success) {
                AlertDialog(
                    onDismissRequest = onBack,
                    title = { Text(stringResource(R.string.loan_app_success)) },
                    text = { Text(stringResource(R.string.loan_app_success_msg)) },
                    confirmButton = {
                        Button(onClick = onBack) {
                            Text("OK")
                        }
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Select Plafond
                    Text(stringResource(R.string.loan_app_plafond), style = MaterialTheme.typography.titleMedium)
                    // Simple Dropdown or Radio Group for simplicity
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedPlafond?.title ?: "Pilih Plafond",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            uiState.plafonds.forEach { plafond ->
                                DropdownMenuItem(
                                    text = { Text("${plafond.title} (Bunga: ${plafond.interestRate}%)") },
                                    onClick = {
                                        viewModel.onPlafondSelected(plafond)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = viewModel::onAmountChanged,
                        label = { Text(stringResource(R.string.loan_app_amount)) },
                        placeholder = { Text(stringResource(R.string.loan_app_amount_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            if (uiState.selectedPlafond != null) {
                                Text("Maks: ${currencyFormatter.format(uiState.selectedPlafond!!.maxAmount)}")
                            }
                        }
                    )

                    // Tenor
                    OutlinedTextField(
                        value = uiState.tenorInput,
                        onValueChange = viewModel::onTenorChanged,
                        label = { Text(stringResource(R.string.loan_app_tenor)) },
                        placeholder = { Text(stringResource(R.string.loan_app_tenor_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            if (uiState.selectedPlafond != null) {
                                Text("Maks: ${uiState.selectedPlafond!!.tenorMonth} Bulan")
                            }
                        }
                    )

                    // Purpose
                    OutlinedTextField(
                        value = uiState.purposeInput,
                        onValueChange = viewModel::onPurposeChanged,
                        label = { Text(stringResource(R.string.loan_app_purpose)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = viewModel::submitLoan,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text(stringResource(R.string.loan_app_submit_confirm))
                    }
                }
            }
        }
    }
}
