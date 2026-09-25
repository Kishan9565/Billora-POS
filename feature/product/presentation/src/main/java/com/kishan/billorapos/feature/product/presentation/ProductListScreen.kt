package com.kishan.billorapos.feature.product.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kishan.billorapos.core.designsystem.icons.QrCodeScanner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.kishan.billorapos.core.presentation.ObserveEvents
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishan.billorapos.core.designsystem.PrimaryColor
import com.kishan.billorapos.core.domain.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Product) -> Unit,
    onLaunchScanner: () -> Unit,
    scannedBarcodeResult: String? = null,
    onBarcodeConsumed: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val threshold by viewModel.lowStockThreshold.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var fileBusy by remember { mutableStateOf(false) }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            fileBusy = true
            scope.launch {
                try {
                    val content = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("Unable to open CSV")
                    }
                    viewModel.onAction(ProductAction.ImportCsv(content))
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: "Unable to open CSV")
                } finally { fileBusy = false }
            }
        }
    }

    var productToDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(scannedBarcodeResult) {
        if (scannedBarcodeResult != null) {
            val matchedProduct = state.products.firstOrNull { it.barcode == scannedBarcodeResult }
            if (matchedProduct != null) {
                viewModel.onAction(ProductAction.OnSearchQueryChange(matchedProduct.name))
            } else {
                viewModel.onAction(ProductAction.OnSearchQueryChange(scannedBarcodeResult))
            }
            onBarcodeConsumed()
        }
    }

    ObserveEvents(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
    }

    Scaffold(
        snackbarHost = { com.kishan.billorapos.core.designsystem.BilloraSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Product Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryColor, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProduct,
                containerColor = PrimaryColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(enabled = !fileBusy && !state.isLoading, onClick = {
                    fileBusy = true
                    scope.launch {
                        try {
                            val csv = viewModel.exportCsv()
                            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val directory = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                                java.io.File.createTempFile("products-", ".csv", directory).apply { writeText(csv, Charsets.UTF_8) }
                            }
                            com.kishan.billorapos.core.presentation.shareFile(context, file, "text/csv")
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "Unable to export CSV")
                        } finally { fileBusy = false }
                    }
                }) { Text("Export as CSV") }
                TextButton(enabled = !fileBusy && !state.isLoading, onClick = {
                    // Some file managers label CSV as text/plain, Excel or octet-stream.
                    try { importLauncher.launch(arrayOf("*/*")) }
                    catch (e: Exception) { scope.launch { snackbarHostState.showSnackbar("No document picker available") } }
                }) { Text("Import from CSV") }
            }
            // Search row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onAction(ProductAction.OnSearchQueryChange(it)) },
                    placeholder = { Text("Scan or enter barcode", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onLaunchScanner,
                    modifier = Modifier
                        .size(50.dp)
                        .background(PrimaryColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    // Try to draw a qr code scanner icon or fallback
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode", tint = PrimaryColor)
                }
            }
            Text(
                text = "Tap the icon to open camera scanner",
                color = Color(0xFF4C669A),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoading && state.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (state.errorMessage != null && state.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.errorMessage}", color = androidx.compose.material3.MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        TextButton(onClick = { viewModel.onAction(ProductAction.RetryLoad) }) { Text("Retry") }
                    }
                }
            } else if (state.products.isEmpty()) {
                com.kishan.billorapos.core.designsystem.EmptyState(Icons.Default.Search,
                    if (state.searchQuery.isNotEmpty()) "No matching products" else "No products yet",
                    if (state.searchQuery.isNotEmpty()) "Try another name or barcode." else "Add your first product to start selling.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth().animateItem().shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = PrimaryColor.copy(alpha = 0.15f), spotColor = PrimaryColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    com.kishan.billorapos.core.designsystem.StockBadge(product.stock, threshold)
                                    Text(text = product.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "â‚¹${"%.2f".format(product.price)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { onNavigateToEditProduct(product) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(PrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryColor, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { productToDelete = product },
                                        enabled = !state.isLoading,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete ${productToDelete?.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productToDelete?.let { viewModel.onAction(ProductAction.OnDeleteProductClick(it.id)) }
                        productToDelete = null
                    }
                ) {
                    Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
