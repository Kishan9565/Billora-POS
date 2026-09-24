package com.kishan.billorapos.core.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberPrinterAction(onDenied: () -> Unit, action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentAction = rememberUpdatedState(action)
    val denied = rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) currentAction.value() else denied.value()
    }
    return {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        ) currentAction.value()
        else launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }
}
