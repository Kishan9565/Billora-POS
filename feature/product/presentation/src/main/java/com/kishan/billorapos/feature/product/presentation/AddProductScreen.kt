package com.kishan.billorapos.feature.product.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishan.billorapos.core.designsystem.InputLabel
import com.kishan.billorapos.core.designsystem.PrimaryButton
import com.kishan.billorapos.core.designsystem.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onLaunchScanner: () -> Unit,
    scannedBarcodeResult: String? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var barcode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var barcodeError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scannedBarcodeResult) {
        if (scannedBarcodeResult != null) {
            barcode = scannedBarcodeResult
            barcodeError = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ProductEvent.ProductActionSuccess -> {
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add Product", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Barcode field
                InputLabel(text = "Barcode")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {
                            barcode = it
                            barcodeError = null
                        },
                        placeholder = { Text("Scan or enter barcode", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = Color.LightGray,
                            containerColor = Color.White,
                            errorBorderColor = Color.Red
                        ),
                        isError = barcodeError != null,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onLaunchScanner,
                        modifier = Modifier
                            .size(50.dp)
                            .background(PrimaryColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Text("📷", color = PrimaryColor, fontSize = 20.sp)
                    }
                }
                if (barcodeError != null) {
                    Text(text = barcodeError ?: "", color = Color.Red, fontSize = 12.dp.value.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
                Text(
                    text = "Tap the icon to open camera scanner",
                    color = Color(0xFF4C669A),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Product Name field
                InputLabel(text = "Product Name")
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        nameError = null
                    },
                    placeholder = { Text("e.g. Basmati Rice", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        containerColor = Color.White,
                        errorBorderColor = Color.Red
                    ),
                    isError = nameError != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )
                if (nameError != null) {
                    Text(text = nameError ?: "", color = Color.Red, fontSize = 12.dp.value.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Price field
                InputLabel(text = "Price")
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceError = null
                    },
                    placeholder = { Text("0.00", color = Color.Gray, fontSize = 13.sp) },
                    prefix = { Text("₹ ", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        containerColor = Color.White,
                        errorBorderColor = Color.Red
                    ),
                    isError = priceError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (priceError != null) {
                    Text(text = priceError ?: "", color = Color.Red, fontSize = 12.dp.value.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Fixed Submit Button at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                PrimaryButton(
                    onPressed = {
                        var hasError = false
                        if (barcode.isBlank()) {
                            barcodeError = "Please enter a barcode"
                            hasError = true
                        }
                        if (productName.isBlank()) {
                            nameError = "Please enter a name"
                            hasError = true
                        }
                        val parsedPrice = price.toDoubleOrNull()
                        if (price.isBlank()) {
                            priceError = "Please enter a price"
                            hasError = true
                        } else if (parsedPrice == null) {
                            priceError = "Please enter a valid number"
                            hasError = true
                        } else if (parsedPrice < 0) {
                            priceError = "Price cannot be negative"
                            hasError = true
                        }

                        if (!hasError && parsedPrice != null) {
                            viewModel.onAction(ProductAction.OnAddProduct(productName, barcode, parsedPrice))
                        }
                    },
                    label = "Add Product"
                )
            }
        }
    }
}
