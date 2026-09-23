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
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    fun handleBack() {
        viewModel.onAction(BillingAction.OnClearCart)
        onNavigateHomePop()
    }

    BackHandler {
        handleBack()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BillingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Content Table
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 420.dp)
            ) {
                item {
                    // Bordered, shadowed itemized table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                    ) {
                        // Header row (background #F8FAFC)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "PRODUCT NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                            Text(text = "PRICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                            Text(text = "TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                        }

                        HorizontalDivider(color = Color(0xFFE5E5EA))

                        // Data rows
                        state.cartItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.quantity} x ${item.product.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${"%.2f".format(item.product.price)}",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "₹${"%.2f".format(item.total)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE5E5EA))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Fixed Bottom Bar with white @ 90% alpha, rounded top corners 24dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(top = 16.dp)
            ) {
                // UPI QR Block - shown only if shop upiId is non-empty
                val upiId = state.shopDetails?.upiId ?: ""
                if (upiId.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Scan to Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, letterSpacing = 1.1.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val shopName = state.shopDetails?.name ?: ""
                        val upiUrl = "upi://pay?pa=$upiId&pn=$shopName&am=${"%.2f".format(state.totalAmount)}&cu=INR"
                        
                        val qrBitmap = remember(upiUrl) { generateQrCode(upiUrl) }
                        if (qrBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI QR Code",
                                modifier = Modifier.size(180.dp)
                            )
                        } else {
                            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                                Text("QR Code Error", color = Color.Red)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                }

                // Grand total row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "GRAND TOTAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.2.sp)
                    Text(text = "₹${"%.2f".format(state.totalAmount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), letterSpacing = (-0.5).sp)
                }

                // Print Receipt PrimaryButton
                PrimaryButton(
                    onPressed = { viewModel.onAction(BillingAction.PrintReceiptClick) },
                    label = "Print Receipt",
                    icon = Icons.Default.Print,
                    isLoading = state.isPrinting
                )
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
