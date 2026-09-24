package com.kishan.billorapos.feature.billing.presentation

import android.graphics.Bitmap
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryColor, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(12.dp)) {
                    Text("PRODUCT NAME", modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color.DarkGray)
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
                    HorizontalDivider(color = Color(0xFFE5E5EA))
                }
            }
            if (state.error != null) {
                item(key = "error") {
                    Text(state.error.orEmpty(), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    androidx.compose.material3.TextButton(onClick = { viewModel.onAction(BillingAction.LoadShopDetails) }) { Text("Retry") }
                }
            }
            val upiId = state.shopDetails?.upiId.orEmpty()
            if (upiId.isNotEmpty()) {
                item(key = "payment") {
                    Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
            item(key = "total") {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("GRAND TOTAL", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Text("\u20B9${"%.2f".format(state.totalAmount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            item(key = "pdf") {
                PrimaryButton(label = "Save as PDF", isLoading = exporting, onPressed = {
                    val receipt = state
                    val shop = receipt.shopDetails
                    if (!exporting) {
                        exporting = true
                        permissionScope.launch {
                            try {
                                check(shop != null) { "Shop details not loaded. Please retry." }
                                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    com.kishan.billorapos.core.printer.ReceiptPdf.create(
                                        context.applicationContext, shop,
                                        receipt.cartItems.map { Triple(it.product.name, it.product.price, it.quantity) },
                                        receipt.totalAmount,
                                        java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                                    )
                                }
                                com.kishan.billorapos.core.presentation.shareFile(context, file, "application/pdf")
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(e.message ?: "Unable to save PDF")
                            } finally { exporting = false }
                        }
                    }
                })
            }
            item(key = "print") {
                PrimaryButton(onPressed = printerAction, label = "Print Receipt", icon = Icons.Default.Print, isLoading = state.isPrinting)
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
