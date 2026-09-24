package com.kishan.billorapos.feature.settings.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.kishan.billorapos.core.designsystem.icons.ChevronRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import com.kishan.billorapos.core.designsystem.icons.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishan.billorapos.core.designsystem.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrinterViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToShopDetails: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shopName by viewModel.shopNameFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionScope = rememberCoroutineScope()
    val printerAction = com.kishan.billorapos.core.presentation.rememberPrinterAction(
        onDenied = { permissionScope.launch { snackbarHostState.showSnackbar("Allow Nearby devices permission in app settings to use the printer.") } },
        action = { viewModel.onAction(PrinterAction.RefreshPrinters) }
    )
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(PrinterAction.InitPrinter)
        onPauseOrDispose { }
    }
    val context = LocalContext.current

    ObserveEvents(viewModel.events) { event ->
            when (event) {
                is PrinterEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryColor, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            ProfileHeader(shopName = shopName)
            state.errorMessage?.let { message ->
                Text(message, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "MANAGEMENT")
            
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                SettingsRow(
                    title = "Products",
                    subtitle = "Manage stock and barcodes",
                    icon = Icons.Default.Settings, // qr_code_scanner equivalent
                    onClick = onNavigateToProducts
                )
                HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Color(0xFFF8FAFC))
                SettingsRow(
                    title = "Shop Details",
                    subtitle = "Edit business info & address",
                    icon = Icons.Default.Storefront,
                    onClick = onNavigateToShopDetails
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "HARDWARE")

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                PrintDeviceRow(
                    state = state,
                    onRefresh = printerAction,
                    onOpenBluetoothSettings = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                )
            }

            Text(
                text = "To connect a new device, tap on the Settings gear to pair in phone's Bluetooth settings, then return and hit Refresh.",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ProfileHeader(shopName: String) {
    val initials = remember(shopName) {
        val words = shopName.trim().split(" ").filter { it.isNotEmpty() }
        val res = if (words.isEmpty()) "S" 
        else if (words.size == 1) words[0].take(1).uppercase()
        else words[0].take(1).uppercase() + words[1].take(1).uppercase()
        res
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(elevation = 15.dp, shape = CircleShape, ambientColor = PrimaryColor, spotColor = PrimaryColor)
                .background(PrimaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = shopName.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(PrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun PrintDeviceRow(
    state: PrinterState,
    onRefresh: () -> Unit,
    onOpenBluetoothSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(PrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // printer icon
            Text("🖨️", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Print Device", fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.savedPrinterName ?: "No printer connected",
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.isConnected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2F1))
                            .border(1.dp, Color(0xFFB2DFDB), RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "CONNECTED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00796B))
                    }
                }
            }
        }
        
        if (state.isScanningOrConnecting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryColor)
        } else {
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryColor)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onOpenBluetoothSettings, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
        }
    }
}
