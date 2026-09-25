package com.kishan.billorapos.feature.billing.presentation

import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.kishan.billorapos.core.designsystem.icons.Print
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.kishan.billorapos.core.presentation.ObserveEvents
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kishan.billorapos.core.designsystem.PrimaryButton
import com.kishan.billorapos.core.designsystem.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: BillingViewModel,
    onNavigateHomePop: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var exporting by remember { androidx.compose.runtime.mutableStateOf(false) }
    val printerAction = com.kishan.billorapos.core.presentation.rememberPrinterAction(
        onDenied = { permissionScope.launch { snackbarHostState.showSnackbar("Allow Nearby devices permission in app settings to use the printer.") } },
        action = { viewModel.onAction(BillingAction.PrintReceiptClick) }
    )
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(BillingAction.LoadShopDetails)
        onPauseOrDispose { }
    }

    fun handleBack() {
        if (state.isPrinting || state.isExporting) return
        viewModel.onAction(BillingAction.OnClearCart)
        onNavigateHomePop()
    }

    BackHandler {
        handleBack()
    }

    ObserveEvents(viewModel.events) { event ->
            when (event) {
                is BillingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
    }

    Scaffold(
        snackbarHost = { com.kishan.billorapos.core.designsystem.BilloraSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryColor, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                    Text("PRODUCT NAME", modifier = Modifier.weight(1f), fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PRICE", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, fontSize = 11.sp)
                    Text("TOTAL", modifier = Modifier.weight(0.65f), textAlign = TextAlign.End, fontSize = 11.sp)
                }
            }
            items(state.cartItems, key = { "product:${it.product.id}" }) { item ->
                Column {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${item.quantity} x ${item.product.name}", modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("\u20B9${"%.2f".format(item.product.price)}", modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, fontSize = 12.sp)
                        Text("\u20B9${"%.2f".format(item.total)}", modifier = Modifier.weight(0.65f), textAlign = TextAlign.End, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                }
            }
            if (state.error != null) {
                item(key = "error") {
                    Text(state.error.orEmpty(), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    androidx.compose.material3.TextButton(onClick = { viewModel.onAction(BillingAction.LoadShopDetails) }) { Text("Retry") }
                }
            }
            val upiId = state.shopDetails?.upiId.orEmpty()
            if (upiId.isNotEmpty() && state.paymentMethod == "UPI") {
                item(key = "payment") {
                    Column(Modifier.fillMaxWidth().background(androidx.compose.material3.MaterialTheme.colorScheme.surface).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scan to Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        val uri = com.kishan.billorapos.feature.billing.domain.paymentUri(upiId, state.shopDetails?.name.orEmpty(), state.totalAmount)
                        val qrResult by androidx.compose.runtime.produceState<Pair<Boolean, Bitmap?>>(true to null, uri) {
                            value = true to null
                            value = false to kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { generateQrCode(uri) }
                        }
                        val qrBitmap = qrResult.second
                        if (qrBitmap != null) {
                            androidx.compose.foundation.Image(qrBitmap!!.asImageBitmap(), contentDescription = "UPI QR Code", modifier = Modifier.size(180.dp))
                        } else {
                            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) { Text(if (qrResult.first) "Preparing payment QR" else "QR Code Error") }
                        }
                    }
                }
            }
            item(key = "options") { CheckoutOptions(state, viewModel::onAction) }
            item(key = "total") {
                if (state.discountAmount > 0) {
                    Text("Subtotal: ₹${"%.2f".format(state.subtotal)}")
                    Text("Discount: -₹${"%.2f".format(state.discountAmount)}")
                }
                Row(Modifier.fillMaxWidth().background(com.kishan.billorapos.core.designsystem.BilloraGradients.Checkout, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GRAND TOTAL", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("\u20B9${"%.2f".format(state.totalAmount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            item(key = "pdf") {
                PrimaryButton(label = "Save / Share PDF", isLoading = state.isExporting,
                    enabled = !state.isPrinting, onPressed = { viewModel.exportPdf(context.applicationContext) })
            }
            if (com.kishan.billorapos.core.presentation.isWhatsAppAvailable(context)) {
                item(key = "whatsapp") {
                    OutlinedButton(onClick = { viewModel.exportPdf(context.applicationContext, true) },
                        enabled = !state.isPrinting && !state.isExporting, modifier = Modifier.fillMaxWidth()) { Text("Share PDF on WhatsApp") }
                }
            }
            item(key = "print") {
                PrimaryButton(onPressed = printerAction, label = "Print Receipt", icon = Icons.Default.Print, isLoading = state.isPrinting, enabled = !state.isExporting)
            }
        }
    }
}
private fun generateQrCode(text: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun CheckoutOptions(state: BillingState, action: (BillingAction) -> Unit) {
    var discountDialog by rememberSaveable { mutableStateOf(false) }
    var discount by rememberSaveable { mutableStateOf("") }
    var percent by rememberSaveable { mutableStateOf(false) }
    var addCustomer by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    val enabled = !state.saleRecorded && !state.isPrinting && !state.isExporting
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Payment method", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CASH", "UPI", "CREDIT").forEach { method ->
                FilterChip(selected = state.paymentMethod == method, enabled = enabled,
                    onClick = { action(BillingAction.PaymentMethod(method)) }, label = { Text(method.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        if (state.paymentMethod == "CREDIT") {
            LaunchedEffect(Unit) { action(BillingAction.SearchCustomers("")) }
            Text(state.customer?.let { "Customer: ${it.name}" } ?: "Choose a customer")
            OutlinedTextField(state.customerQuery, { action(BillingAction.SearchCustomers(it)) }, enabled = enabled,
                label = { Text("Search name or phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            state.customers.take(8).forEach { customer ->
                TextButton(enabled = enabled, onClick = { action(BillingAction.SelectCustomer(customer)) }) {
                    Text("${customer.name}  ${customer.phoneNumber.orEmpty()}")
                }
            }
            TextButton(enabled = enabled, onClick = { addCustomer = true }) { Text("+ New customer") }
        }
        TextButton(enabled = enabled, onClick = { discountDialog = true }) { Text("Apply Discount") }
        if (state.saleRecorded) Text("Sale recorded • Start a new cart to change this sale", color = MaterialTheme.colorScheme.primary)
    }
    if (discountDialog) AlertDialog(onDismissRequest = { discountDialog = false }, title = { Text("Apply Discount") },
        text = { Column {
            Row { FilterChip(!percent, { percent = false }, label = { Text("Flat ₹") }); Spacer(Modifier.width(8.dp)); FilterChip(percent, { percent = true }, label = { Text("Percent %") }) }
            OutlinedTextField(discount, { discount = it }, label = { Text(if (percent) "Percentage" else "Amount") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        } }, confirmButton = { TextButton(onClick = { action(BillingAction.ApplyDiscount(discount, percent)); discountDialog = false }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = { discountDialog = false }) { Text("Cancel") } })
    if (addCustomer) AlertDialog(onDismissRequest = { addCustomer = false }, title = { Text("New customer") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name (required)") }, singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
        } }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { action(BillingAction.AddCustomer(name, phone)); addCustomer = false; name = ""; phone = "" }) { Text("Add") } },
        dismissButton = { TextButton(onClick = { addCustomer = false }) { Text("Cancel") } })
}
