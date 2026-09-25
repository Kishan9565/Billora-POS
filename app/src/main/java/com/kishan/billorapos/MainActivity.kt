package com.kishan.billorapos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan.billorapos.core.designsystem.BilloraPOSTheme
import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.feature.billing.presentation.CheckoutScreen
import com.kishan.billorapos.feature.billing.presentation.HomeScreen
import com.kishan.billorapos.feature.product.presentation.AddProductScreen
import com.kishan.billorapos.feature.product.presentation.EditProductScreen
import com.kishan.billorapos.feature.product.presentation.ProductListScreen
import com.kishan.billorapos.feature.product.presentation.ScannerScreen
import com.kishan.billorapos.feature.settings.presentation.SettingsScreen
import com.kishan.billorapos.feature.shop.presentation.ShopDetailsScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable object StartupRoute
@Serializable object ShopOnboardingRoute
@Serializable object HomeRoute
@Serializable object ScannerRoute
@Serializable object CheckoutRoute
@Serializable object SettingsRoute
@Serializable object ProductListRoute
@Serializable object AddProductRoute
@Serializable data class EditProductRoute(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Double,
    val stock: Int
)
@Serializable object ReportsRoute
@Serializable object KhataRoute
@Serializable object ShopProfilesRoute
@Serializable object AddShopRoute
@Serializable object ShopDetailsRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BilloraPOSTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val billingViewModel: com.kishan.billorapos.feature.billing.presentation.BillingViewModel = koinViewModel()
    val printerViewModel: com.kishan.billorapos.feature.settings.presentation.PrinterViewModel = koinViewModel()

    var scannerResultTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedBarcode by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(navController = navController, startDestination = StartupRoute) {
        composable<StartupRoute> {
            val startup: StartupViewModel = koinViewModel()
            val state by startup.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.complete) {
                state.complete?.let { complete ->
                    navController.navigate(if (complete) HomeRoute else ShopOnboardingRoute) {
                        popUpTo<StartupRoute> { inclusive = true }
                    }
                }
            }
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                if (state.error) androidx.compose.material3.TextButton(onClick = startup::load) { androidx.compose.material3.Text("Unable to read shop setup. Retry") }
                else androidx.compose.material3.CircularProgressIndicator()
            }
        }
        composable<ShopOnboardingRoute> {
            ShopDetailsScreen(viewModel = koinViewModel(), onboarding = true, onNavigateBack = {
                navController.navigate(HomeRoute) {
                    popUpTo<ShopOnboardingRoute> { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable<HomeRoute> {
            HomeScreen(
                viewModel = billingViewModel,
                onNavigateToSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                onNavigateToCheckout = { navController.navigate(CheckoutRoute) { launchSingleTop = true } },
                onAddUnknownProduct = { barcode ->
                    scannedBarcode = barcode
                    scannerResultTarget = "add_product"
                    navController.navigate(AddProductRoute) { launchSingleTop = true }
                }
            )
        }

        composable<ScannerRoute> {
            ScannerScreen(
                onBarcodeScanned = { barcode ->
                    if (scannerResultTarget == "product_list") {
                        scannedBarcode = barcode
                        navController.popBackStack(ProductListRoute, inclusive = false)
                    } else if (scannerResultTarget == "add_product") {
                        scannedBarcode = barcode
                        navController.popBackStack(AddProductRoute, inclusive = false)
                    } else {
                        navController.popBackStack()
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<CheckoutRoute> {
            CheckoutScreen(
                viewModel = billingViewModel,
                onNavigateHomePop = {
                    navController.popBackStack(HomeRoute, inclusive = false)
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                viewModel = printerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProducts = { navController.navigate(ProductListRoute) { launchSingleTop = true } },
                onNavigateToShopDetails = { navController.navigate(ShopProfilesRoute) { launchSingleTop = true } },
                onNavigateToReports = { navController.navigate(ReportsRoute) },
                onNavigateToKhata = { navController.navigate(KhataRoute) }
            )
        }

        composable<ProductListRoute> {
            val barcode = if (scannerResultTarget == "product_list") scannedBarcode else null
            // Reset after consuming
            ProductListScreen(
                viewModel = koinViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate(AddProductRoute) { launchSingleTop = true } },
                onNavigateToEditProduct = { prod ->
                    navController.navigate(EditProductRoute(prod.id, prod.name, prod.barcode, prod.price, prod.stock)) { launchSingleTop = true }
                },
                onLaunchScanner = {
                    scannerResultTarget = "product_list"
                    navController.navigate(ScannerRoute) { launchSingleTop = true }
                },
                scannedBarcodeResult = barcode,
                onBarcodeConsumed = { scannedBarcode = null; scannerResultTarget = null }
            )
        }

        composable<AddProductRoute> {
            val barcode = if (scannerResultTarget == "add_product") scannedBarcode else null
            AddProductScreen(
                viewModel = koinViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onLaunchScanner = {
                    scannerResultTarget = "add_product"
                    navController.navigate(ScannerRoute) { launchSingleTop = true }
                },
                scannedBarcodeResult = barcode,
                onBarcodeConsumed = { scannedBarcode = null; scannerResultTarget = null }
            )
        }

        composable<EditProductRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<EditProductRoute>()
            val product = Product(args.id, args.name, args.barcode, args.price, args.stock)
            EditProductScreen(
                product = product,
                viewModel = koinViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<ReportsRoute> {
            com.kishan.billorapos.feature.settings.presentation.SalesReportScreen(koinViewModel()) { navController.popBackStack() }
        }
        composable<KhataRoute> {
            com.kishan.billorapos.feature.settings.presentation.KhataScreen(koinViewModel()) { navController.popBackStack() }
        }
        composable<ShopProfilesRoute> {
            com.kishan.billorapos.feature.shop.presentation.ShopProfilesScreen(koinViewModel(),
                onBack = { navController.popBackStack() }, onAdd = { navController.navigate(AddShopRoute) },
                onEdit = { navController.navigate(ShopDetailsRoute) })
        }
        composable<AddShopRoute> {
            ShopDetailsScreen(koinViewModel(), onNavigateBack = { navController.popBackStack() }, addNew = true)
        }
        composable<ShopDetailsRoute> {
            ShopDetailsScreen(
                viewModel = koinViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
