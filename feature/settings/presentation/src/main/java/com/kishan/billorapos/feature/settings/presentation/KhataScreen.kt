package com.kishan.billorapos.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan.billorapos.core.designsystem.*
import com.kishan.billorapos.core.domain.Customer
import com.kishan.billorapos.core.presentation.ObserveEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(viewModel: ManagementViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Customer?>(null) }
    val customer = state.selectedCustomer
    val back = { if (customer != null) viewModel.onAction(ManagementAction.CloseCustomer) else onBack() }
    androidx.activity.compose.BackHandler(customer != null) { back() }
    ObserveEvents(viewModel.events) { if (it is ManagementEvent.Message) snackbar.showSnackbar(it.text) }
    Scaffold(topBar = { TopAppBar(title = { Text(customer?.name ?: "Udhaar / Khata") }, navigationIcon = { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        snackbarHost = { BilloraSnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (customer == null) {
                item { OutlinedTextField(query, { query = it }, label = { Text("Search name or phone") }, modifier = Modifier.fillMaxWidth()) }
                val rows = state.customers.filter { it.name.contains(query, true) || it.phoneNumber.orEmpty().contains(query, true) }
                if (rows.isEmpty()) item { EmptyState(Icons.Default.Person, "No customers found", "Add a customer when choosing Credit at checkout.", Modifier.height(300.dp)) }
                items(rows, key = { it.id }) { row ->
                    BilloraCard(Modifier.fillMaxWidth(), onClick = { viewModel.onAction(ManagementAction.OpenCustomer(row)) }) {
                        AmountRow(row.name, money(row.balance)); Text(row.phoneNumber.orEmpty())
                        GradientStatusChip(if (row.balance > 0) "Outstanding" else "Settled", if (row.balance > 0) BilloraGradients.Danger else BilloraGradients.Success)
                    }
                }
            } else {
                item {
                    GradientCard(if (customer.balance > 0) BilloraGradients.Danger else BilloraGradients.Success, Modifier.fillMaxWidth()) {
                        Text(if (customer.balance > 0) "Outstanding balance" else "All settled", color = Color.White)
                        Text(money(customer.balance), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text(customer.phoneNumber.orEmpty(), color = Color.White)
                    }
                    TextButton(onClick = { editing = customer }) { Text("Edit customer") }
                    PrimaryButton(onPressed = { payment = "" }, label = "Record Payment", enabled = customer.balance > 0 && !state.busy)
                }
                items(state.creditSales, key = { it.id }) { sale ->
                    BilloraCard(Modifier.fillMaxWidth(), onClick = { viewModel.onAction(ManagementAction.OpenSale(sale.id)) }) {
                        Text(date(sale.timestamp), style = MaterialTheme.typography.titleSmall)
                        Text(state.creditLines[sale.id].orEmpty().joinToString { "${it.quantity} × ${it.productName}" })
                        AmountRow("Total", money(sale.totalAmount)); AmountRow("Paid", money(sale.amountPaid))
                        AmountRow("Remaining", money((sale.totalAmount - sale.amountPaid).coerceAtLeast(0.0)))
                        GradientStatusChip(if (sale.isSettled) "Settled" else "Unsettled", if (sale.isSettled) BilloraGradients.Success else BilloraGradients.Danger)
                    }
                }
            }
        }
    }
    if (payment != null) AlertDialog(onDismissRequest = { payment = null }, title = { Text("Record Payment") },
        text = { Column { Text("Applied to oldest unsettled sales first."); OutlinedTextField(payment.orEmpty(), { payment = it }, label = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) } },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { viewModel.onAction(ManagementAction.Payment(payment.orEmpty())); payment = null }) { Text("Record") } },
        dismissButton = { TextButton(onClick = { payment = null }) { Text("Cancel") } })
    editing?.let { edit ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Edit customer") }, text = { Column {
            OutlinedTextField(edit.name, { editing = edit.copy(name = it) }, label = { Text("Name") })
            OutlinedTextField(edit.phoneNumber.orEmpty(), { editing = edit.copy(phoneNumber = it) }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        } }, confirmButton = { TextButton(enabled = edit.name.isNotBlank() && !state.busy, onClick = { viewModel.onAction(ManagementAction.SaveCustomer(edit)); editing = null }) { Text("Save") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
    state.detail?.let { SaleDetailDialog(it, state.lines) { viewModel.onAction(ManagementAction.CloseSale) } }
}
