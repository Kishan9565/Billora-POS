package com.kishan.billorapos.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan.billorapos.core.designsystem.*
import com.kishan.billorapos.core.domain.*
import com.kishan.billorapos.core.presentation.ObserveEvents
import java.text.SimpleDateFormat
import java.util.*

internal fun money(value: Double) = "₹${String.format(Locale.getDefault(), "%.2f", value)}"
internal fun date(value: Long) = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(value))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(viewModel: ManagementViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var custom by remember { mutableStateOf(false) }
    ObserveEvents(viewModel.events) { if (it is ManagementEvent.Message) snackbar.showSnackbar(it.text) }
    Scaffold(topBar = { TopAppBar(title = { Text("Sales Reports") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        snackbarHost = { BilloraSnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Today", "This Week", "This Month").forEach { label -> FilterChip(state.range == label, { viewModel.onAction(ManagementAction.Range(label)) }, label = { Text(label) }) }
            }
                TextButton(onClick = { custom = true }) { Text("Custom: ${date(state.start).substringBefore(',')} – ${date(state.end).substringBefore(',')}") }
            }
            item {
                val summary = summarizeSales(state.sales)
                GradientCard(BilloraGradients.PrimaryDiagonal, Modifier.fillMaxWidth()) {
                    Text("Revenue", color = Color.White)
                    Text(money(summary.revenue), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text("${summary.count} transactions • Average ${money(summary.average)}", color = Color.White)
                }
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item { Text("Payment breakdown", style = MaterialTheme.typography.titleMedium)
                summarizeSales(state.sales).byPayment.forEach { (method, total) -> AmountRow(method, money(total)) }
            }
            if (!state.loading && state.sales.isEmpty()) item { EmptyState(Icons.Default.List, "No sales yet", "Completed receipts in this date range appear here.", Modifier.height(280.dp)) }
            if (state.top.isNotEmpty()) item { Text("Top products", style = MaterialTheme.typography.titleLarge) }
            items(state.top.withIndex().toList(), key = { "top:${it.value.productId}:${it.value.productName}" }) { (index, product) ->
                BilloraCard(Modifier.fillMaxWidth()) {
                    AmountRow("${index + 1}. ${product.productName}", money(product.revenue))
                    Text("${product.quantity} sold", style = MaterialTheme.typography.bodySmall)
                    val max = state.top.maxOf { it.revenue }.coerceAtLeast(0.01)
                    Box(Modifier.fillMaxWidth().height(5.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))) {
                        Box(Modifier.fillMaxWidth((product.revenue / max).toFloat().coerceIn(0f, 1f)).height(5.dp).background(PrimaryColor, RoundedCornerShape(3.dp)))
                    }
                }
            }
            if (state.sales.isNotEmpty()) item { Text("Transactions", style = MaterialTheme.typography.titleLarge) }
            items(state.sales, key = { it.id }) { sale ->
                BilloraCard(Modifier.fillMaxWidth(), onClick = { viewModel.onAction(ManagementAction.OpenSale(sale.id)) }) {
                    AmountRow(date(sale.timestamp), money(sale.totalAmount))
                    Text("${sale.paymentMethod} • ${sale.itemCount} items", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (custom) CustomRangeDialog(onDismiss = { custom = false }) { start, end -> viewModel.onAction(ManagementAction.Range("Custom", start, end)); custom = false }
    state.detail?.let { SaleDetailDialog(it, state.lines) { viewModel.onAction(ManagementAction.CloseSale) } }
}

@Composable
internal fun AmountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f)); Spacer(Modifier.width(12.dp)); Text(value)
    }
}

@Composable
internal fun SaleDetailDialog(sale: Sale, lines: List<SaleLine>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Sale detail") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(date(sale.timestamp)); Text(sale.paymentMethod) }
            items(lines, key = { it.id }) { line ->
                Text(line.productName, style = MaterialTheme.typography.titleSmall)
                AmountRow("${line.quantity} × ${money(line.unitPrice)}", money(lineAmount(line.unitPrice, line.quantity).toDouble()))
            }
            item { HorizontalDivider(); AmountRow("Subtotal", money(sale.subtotal)); AmountRow("Discount", "−${money(sale.discountAmount)}"); AmountRow("Total", money(sale.totalAmount)) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(onDismiss: () -> Unit, onApply: (Long, Long) -> Unit) {
    var start by remember { mutableStateOf<Long?>(null) }
    val picker = rememberDatePickerState()
    fun localDay(utc: Long): Long {
        val source = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
        return Calendar.getInstance().apply { clear(); set(source.get(Calendar.YEAR), source.get(Calendar.MONTH), source.get(Calendar.DAY_OF_MONTH)) }.timeInMillis
    }
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(enabled = picker.selectedDateMillis != null && (start == null || localDay(picker.selectedDateMillis!!) >= start!!), onClick = {
            val selected = localDay(picker.selectedDateMillis!!)
            if (start == null) { start = selected; picker.selectedDateMillis = null } else onApply(start!!, selected)
        }) { Text(if (start == null) "Next: End date" else "Apply") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) {
        Text(if (start == null) "Start date" else "End date", Modifier.padding(24.dp))
        DatePicker(picker)
    }
}
