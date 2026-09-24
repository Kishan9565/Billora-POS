package com.kishan.billorapos.feature.product.presentation

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kishan.billorapos.core.designsystem.PrimaryColor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    LifecycleResumeEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
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

    var isScanned by remember { mutableStateOf(false) }

    @Suppress("DEPRECATION")
    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(200)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryColor, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                com.kishan.billorapos.core.presentation.BarcodePreview(
                    modifier = Modifier.fillMaxSize()
                ) { rawValue ->
                    if (!isScanned) {
                        isScanned = true
                        triggerVibration()
                        onBarcodeScanned(rawValue)
                    }
                }
                // Simple 250x250dp square overlay, 2dp solid green border with 4 small 15x15dp white-bordered corner squares
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.Center)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color.Green,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        val squareSize = 15.dp.toPx()
                        val strokeW = 1.dp.toPx()

                        // Top-left
                        drawRect(Color.White, topLeft = Offset(0f, 0f), size = Size(squareSize, squareSize), style = Stroke(strokeW))
                        // Top-right
                        drawRect(Color.White, topLeft = Offset(size.width - squareSize, 0f), size = Size(squareSize, squareSize), style = Stroke(strokeW))
                        // Bottom-left
                        drawRect(Color.White, topLeft = Offset(0f, size.height - squareSize), size = Size(squareSize, squareSize), style = Stroke(strokeW))
                        // Bottom-right
                        drawRect(Color.White, topLeft = Offset(size.width - squareSize, size.height - squareSize), size = Size(squareSize, squareSize), style = Stroke(strokeW))
                    }
                }

                Text(
                    text = "Align barcode within frame",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .fillMaxWidth()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.TextButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera access to scan barcodes", textAlign = TextAlign.Center) }
            }
        }
    }
}
