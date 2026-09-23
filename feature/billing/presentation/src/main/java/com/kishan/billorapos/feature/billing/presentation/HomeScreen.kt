package com.kishan.billorapos.feature.billing.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kishan.billorapos.core.designsystem.PrimaryButton
import com.kishan.billorapos.core.designsystem.PrimaryColor
import java.time.Instant
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BillingViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToCheckout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BillingEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is BillingEvent.NavigateToCheckout -> onNavigateToCheckout()
                is BillingEvent.NavigateToSettings -> onNavigateToSettings()
            }
        }
    }

    val cooldownMap = remember { remember { mutableMapOf<String, Instant>() } }

    @Suppress("DEPRECATION")
    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(100)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenHeight = maxHeight
            val scannerHeight = screenHeight * 0.4f

            // Scanner Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scannerHeight)
                    .background(Color.Black)
            ) {
                if (hasCameraPermission && state.isCameraOn) {
                    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

                    LaunchedEffect(state.isFlashOn) {
                        cameraControl?.enableTorch(state.isFlashOn)
                    }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build().apply {
                                    setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val options = BarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                    .build()
                                val scanner = BarcodeScanning.getClient(options)

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val barcode = barcodes.firstOrNull()
                                                if (barcode != null) {
                                                    val rawValue = barcode.rawValue
                                                    if (rawValue != null) {
                                                        val now = Instant.now()
                                                        val lastSeen = cooldownMap[rawValue]
                                                        if (lastSeen == null || now.isAfter(lastSeen.plusSeconds(2))) {
                                                            cooldownMap[rawValue] = now
                                                            triggerVibration()
                                                            viewModel.onAction(BillingAction.OnBarcodeDetected(rawValue))
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK,
                                        preview,
                                        analysis
                                    )
                                    cameraControl = camera.cameraControl
                                    cameraControl?.enableTorch(state.isFlashOn)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Center Box
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .align(Alignment.Center)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color.White.copy(alpha = 0.24f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            val accentLength = 32.dp.toPx()
                            val accentThickness = 4.dp.toPx()
                            val cornerAccentColor = Color.Green // accent

                            // Top Left Corner Accents
                            drawLine(cornerAccentColor, Offset(0f, 0f), Offset(accentLength, 0f), accentThickness)
                            drawLine(cornerAccentColor, Offset(0f, 0f), Offset(0f, accentLength), accentThickness)

                            // Top Right Corner Accents
                            drawLine(cornerAccentColor, Offset(size.width, 0f), Offset(size.width - accentLength, 0f), accentThickness)
                            drawLine(cornerAccentColor, Offset(size.width, 0f), Offset(size.width, accentLength), accentThickness)

                            // Bottom Left Corner Accents
                            drawLine(cornerAccentColor, Offset(0f, size.height), Offset(accentLength, size.height), accentThickness)
                            drawLine(cornerAccentColor, Offset(0f, size.height), Offset(0f, size.height - accentLength), accentThickness)

                            // Bottom Right Corner Accents
                            drawLine(cornerAccentColor, Offset(size.width, size.height), Offset(size.width - accentLength, size.height), accentThickness)
                            drawLine(cornerAccentColor, Offset(size.width, size.height), Offset(size.width, size.height - accentLength), accentThickness)
                        }
                    }
                } else {
                    // Camera off state panel #1E293B
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF334155), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.VideocamOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Camera is turned off", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Turn on your camera to start scanning barcodes and items automatically.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(PrimaryColor)
                                .clickable { viewModel.onAction(BillingAction.OnToggleCamera) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Turn on Camera", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Top right control buttons overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                    if (state.isCameraOn) {
                        Spacer(modifier = Modifier.height(12.dp))
                        IconButton(
                            onClick = { viewModel.onAction(BillingAction.OnToggleFlash) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (state.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flash",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(
                        onClick = { viewModel.onAction(BillingAction.OnToggleCamera) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (state.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom panel sheet overlapping by 24dp
            val panelTopOffset = scannerHeight - 24.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight - panelTopOffset)
                    .offset(y = panelTopOffset)
                    .shadow(
                        elevation = 15.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.26f),
                        spotColor = Color.Black.copy(alpha = 0.26f)
                    )
                    .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(48.dp, 4.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Scanned Items", fontSize = 18.sp, fontWeight = FontWeight.W600, color = Color.Black)
                            Text(text = "${state.totalQuantity} items total", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "TOTAL PRICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.2.sp)
                            Text(text = "₹${"%.2f".format(state.totalAmount)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = PrimaryColor)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = Color.LightGray.copy(alpha = 0.5f))

                    // Cart List
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.cartItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color(0xFFF2F2F7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🧺", fontSize = 40.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "List is empty", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scanned items will appear here as you scan them with the camera above.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 16.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.cartItems, key = { it.product.id }) { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                                Text(
                                                    text = item.product.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.W600,
                                                    color = Color.Black,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "₹${"%.2f".format(item.product.price)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Gray
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Color(0xFFF2F2F7)),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.onAction(BillingAction.OnQuantityChange(item.product.id, item.quantity - 1)) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                                }
                                                Text(
                                                    text = item.quantity.toString(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.onAction(BillingAction.OnQuantityChange(item.product.id, item.quantity + 1)) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pinned Review Order Button
                    PrimaryButton(
                        onPressed = if (state.cartItems.isEmpty()) null else { { onNavigateToCheckout() } },
                        label = "Review Order"
                    )
                }
            }
        }
    }
}
