package com.kishan.billorapos.core.presentation

import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Owns and releases exactly this destination's camera use cases and scanner. */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodePreview(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false,
    onBarcode: (String) -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember(context) { PreviewView(context) }
    val callback by rememberUpdatedState(onBarcode)
    var control by remember { mutableStateOf<CameraControl?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(control, torchEnabled) { control?.enableTorch(torchEnabled) }
    DisposableEffect(previewView, owner) {
        val active = AtomicBoolean(true)
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient()
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        analysis.setAnalyzer(executor) { proxy ->
            val mediaImage = proxy.image
            if (!active.get() || mediaImage == null) {
                proxy.close()
            } else {
                try {
                    scanner.process(InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            if (active.get() && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                codes.firstOrNull()?.rawValue?.let(callback)
                            }
                        }
                        .addOnFailureListener {
                            if (active.get()) error = "Barcode scanning failed. Reopen the camera to retry."
                        }
                        .addOnCompleteListener { proxy.close() }
                } catch (_: Exception) {
                    proxy.close()
                }
            }
        }
        future.addListener({
            if (active.get()) {
                try {
                    provider = future.get()
                    control = provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis).cameraControl
                } catch (_: Exception) {
                    error = "Camera unavailable. Check permission and reopen the camera."
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active.set(false)
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            control = null
            scanner.close()
            executor.shutdown()
        }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        error?.let { Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center)) }
    }
}
