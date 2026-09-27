package com.kishan.billorapos.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan.billorapos.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    shopName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToShopDetails: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToKhata: () -> Unit,
    onNavigateToPrinterSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        BilloraBackground(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero KPI card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(BilloraGradients.PrimaryVertical)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "TODAY'S REVENUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "\u20B9${"%.2f".format(state.todayRevenue)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${state.todayTransactionCount} transactions today",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Text(
                    text = "QUICK ACTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardFeatureCard(
                        title = "Products",
                        subtitle = if (state.lowStockCount > 0) "${state.lowStockCount} low stock" else "Manage catalog",
                        icon = Icons.Default.Inventory2,
                        accentColor = if (state.lowStockCount > 0) WarningColor else PrimaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                    DashboardFeatureCard(
                        title = "Reports",
                        subtitle = "View sales & trends",
                        icon = Icons.Default.BarChart,
                        accentColor = SecondaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReports
                    )
                    DashboardFeatureCard(
                        title = "Udhaar / Khata",
                        subtitle = if (state.khataOutstandingTotal > 0) "\u20B9${"%.2f".format(state.khataOutstandingTotal)} due" else "All settled",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (state.khataOutstandingTotal > 0) DangerColor else SuccessColor,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToKhata
                    )
                    DashboardFeatureCard(
                        title = "Shop Profile",
                        subtitle = shopName,
                        icon = Icons.Default.Storefront,
                        accentColor = PrimaryColor,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToShopDetails
                    )
                    DashboardFeatureCard(
                        title = "Printer",
                        subtitle = if (state.printerConnected) "Connected" else "Not connected",
                        icon = Icons.Default.Print,
                        accentColor = if (state.printerConnected) SuccessColor else Slate400,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPrinterSettings
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DashboardFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = 0.15f),
                spotColor = accentColor.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = accentColor, maxLines = 1)
        }
    }
}
