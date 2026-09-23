package com.kishan.billorapos.feature.product.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishan.billorapos.core.designsystem.InputLabel
import com.kishan.billorapos.core.designsystem.PrimaryButton
import com.kishan.billorapos.core.designsystem.PrimaryColor
import com.kishan.billorapos.core.domain.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    product: Product,
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var productName by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf("%.2f".format(product.price)) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

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
                title = { Text("Edit Product", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Custom 32dp chevron back icon (1dp larger as required by spec)
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryColor, modifier = Modifier.size(32.dp))
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
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                // Read-only barcode display block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, PrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📷", color = PrimaryColor, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "BARCODE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryColor.copy(alpha = 0.7f))
                            Text(text = product.barcode, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = Color.Black)
                        }
                    }
                }

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
                            val updatedProduct = product.copy(name = productName, price = parsedPrice)
                            viewModel.onAction(ProductAction.OnUpdateProduct(updatedProduct))
                        }
                    },
                    label = "Save Changes"
                )
            }
        }
    }
}
