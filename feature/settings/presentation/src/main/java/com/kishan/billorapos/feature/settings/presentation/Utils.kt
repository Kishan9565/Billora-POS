package com.kishan.billorapos.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kishan.billorapos.core.domain.Sale
import com.kishan.billorapos.core.domain.SaleLine
import com.kishan.billorapos.core.domain.lineAmount
import java.text.SimpleDateFormat
import java.util.*

internal fun money(value: Double) = "\u20B9${String.format(Locale.getDefault(), "%.2f", value)}"
internal fun date(value: Long) = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(value))

@Composable
internal fun AmountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value)
    }
}

@Composable
internal fun SaleDetailDialog(sale: Sale, lines: List<SaleLine>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sale detail") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { 
                    Text(date(sale.timestamp))
                    Text(sale.paymentMethod) 
                }
                items(lines, key = { it.id }) { line ->
                    Text(line.productName, style = MaterialTheme.typography.titleSmall)
                    AmountRow("${line.quantity} \u00D7 ${money(line.unitPrice)}", money(lineAmount(line.unitPrice, line.quantity).toDouble()))
                }
                item { 
                    HorizontalDivider()
                    AmountRow("Subtotal", money(sale.subtotal))
                    AmountRow("Discount", "\u2212${money(sale.discountAmount)}")
                    AmountRow("Total", money(sale.totalAmount)) 
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
