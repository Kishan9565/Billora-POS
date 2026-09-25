package com.kishan.billorapos

import com.kishan.billorapos.core.database.ProductDao
import com.kishan.billorapos.core.database.ProductEntity
import com.kishan.billorapos.core.domain.*
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.printer.BluetoothDeviceInfo
import com.kishan.billorapos.feature.billing.domain.CartItem
import com.kishan.billorapos.feature.billing.domain.paymentUri
import com.kishan.billorapos.feature.billing.presentation.*
import com.kishan.billorapos.feature.product.data.ProductRepositoryImpl
import com.kishan.billorapos.feature.product.domain.ProductRepository
import com.kishan.billorapos.feature.product.presentation.*
import com.kishan.billorapos.feature.settings.domain.PrinterRepository
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import com.kishan.billorapos.feature.shop.presentation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class PosRegressionTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun product(id: String = "p", barcode: String = "123", price: Double = 0.1, stock: Int = 7) =
        Product(id, "Product $id", barcode, price, stock)

    @Test fun decimalLineAndCartTotalsUseEstablishedFormula() {
        assertEquals(0.3, CartItem(product(), 3).total, 0.0)
        assertEquals(0.6, totalAmount(listOf(0.1 to 3, 0.3 to 1)), 0.0)
        assertEquals(0.0, totalAmount(emptyList()), 0.0)
        assertEquals(49.98, CartItem(product(price = 24.99), 2).total, 0.0)
    }

    @Test fun paymentUriEscapesParametersAndUsesDecimalPointInEveryLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val uri = paymentUri("shop+one@bank", "A & B #1", 12.5)
            assertEquals("upi://pay?pa=shop%2Bone%40bank&pn=A%20%26%20B%20%231&am=12.50&cu=INR", uri)
        } finally { Locale.setDefault(previous) }
    }

    @Test fun repositoryRejectsDuplicateBarcodeAndRetainsExistingProduct() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val repository = ProductRepositoryImpl(dao)
        assertTrue(repository.addProduct(product()) is Result.Success)
        assertTrue(repository.addProduct(product(id = "other")) is Result.Error)
        assertEquals(1, dao.rows.value.size)
        assertEquals("p", dao.rows.value.single().id)
        assertEquals(7, repository.getProducts().first().single().stock)
    }

    @Test fun repositoryReportsMissingUpdateAndPropagatesCancellation() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val repository = ProductRepositoryImpl(dao)
        assertTrue(repository.updateProduct(product()) is Result.Error)
        dao.cancelReads = true
        try {
            repository.getProductByBarcode("123")
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun duplicateValidationDoesNotDependOnFilteredUiState() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val repository = ProductRepositoryImpl(dao)
        repository.addProduct(product())
        val vm = ProductViewModel(repository, MemoryPreferences())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.onAction(ProductAction.OnSearchQueryChange("hidden"))
        runCurrent()
        assertTrue(vm.state.value.products.isEmpty())
        vm.onAction(ProductAction.OnAddProduct("Another", "123", 5.0))
        runCurrent()
        assertEquals(1, dao.rows.value.size)
        assertTrue(vm.events.first() is ProductEvent.ShowSnackbar)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun repeatedSaveIsIgnoredAndNonFinitePriceIsRejected() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val vm = ProductViewModel(ProductRepositoryImpl(dao), MemoryPreferences())
        vm.onAction(ProductAction.OnAddProduct("A", "a", 2.0))
        vm.onAction(ProductAction.OnAddProduct("B", "b", 3.0))
        runCurrent()
        assertEquals(1, dao.rows.value.size)
        vm.onAction(ProductAction.OnAddProduct("Bad", "bad", Double.NaN))
        runCurrent()
        assertEquals(1, dao.rows.value.size)
    }

    @Test fun productObservationFailureIsVisibleAndRetryRecovers() = runTest(dispatcher) {
        val dao = MemoryProductDao().apply { failObservation = true }
        val vm = ProductViewModel(ProductRepositoryImpl(dao), MemoryPreferences())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertNotNull(vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
        dao.failObservation = false
        vm.onAction(ProductAction.RetryLoad)
        runCurrent()
        assertNull(vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun editingPreservesExistingStock() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val repository = ProductRepositoryImpl(dao)
        repository.addProduct(product())
        val vm = ProductViewModel(repository, MemoryPreferences())
        vm.onAction(ProductAction.OnUpdateProduct(product().copy(name = "Changed")))
        runCurrent()
        assertEquals("Changed", dao.rows.value.single().name)
        assertEquals(7, dao.rows.value.single().stock)
    }

    @Test fun cartScanQuantityRemovalAndClearStayConsistent() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val repository = ProductRepositoryImpl(dao)
        repository.addProduct(product())
        val vm = BillingViewModel(repository, MemoryShop(), MemoryPrinter(), MemorySales(), MemoryCustomers())
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        assertEquals(2, vm.state.value.totalQuantity)
        vm.onAction(BillingAction.OnQuantityChange("p", 3))
        assertEquals(0.3, vm.state.value.totalAmount, 0.0)
        vm.onAction(BillingAction.OnQuantityChange("p", 0))
        assertTrue(vm.state.value.cartItems.isEmpty())
        assertEquals(0.0, vm.state.value.totalAmount, 0.0)
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        vm.onAction(BillingAction.OnClearCart)
        assertEquals(0, vm.state.value.totalQuantity)
    }

    @Test fun printUsesSingleSnapshotAndIgnoresRepeatedTap() = runTest(dispatcher) {
        val repository = ProductRepositoryImpl(MemoryProductDao())
        repository.addProduct(product())
        val printer = MemoryPrinter().apply { connectionGate = CompletableDeferred() }
        val vm = BillingViewModel(repository, MemoryShop(), printer, MemorySales(), MemoryCustomers())
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        vm.onAction(BillingAction.PrintReceiptClick)
        vm.onAction(BillingAction.PrintReceiptClick)
        runCurrent()
        assertTrue(vm.state.value.isPrinting)
        vm.onAction(BillingAction.OnClearCart)
        printer.connectionGate!!.complete(Unit)
        runCurrent()
        assertEquals(1, printer.prints)
        assertEquals(1, printer.printedItems.single().third)
        assertEquals(0.1, printer.printedTotal, 0.0)
        assertFalse(vm.state.value.isPrinting)
    }

    @Test fun printAndPdfCompletionRecordTheSameCheckoutOnlyOnce() = runTest(dispatcher) {
        val repository = ProductRepositoryImpl(MemoryProductDao())
        repository.addProduct(product(price = 100.0))
        val sales = MemorySales()
        val vm = BillingViewModel(repository, MemoryShop(), MemoryPrinter(), sales, MemoryCustomers())
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        vm.onAction(BillingAction.ApplyDiscount("10", true))
        vm.onAction(BillingAction.PrintReceiptClick)
        runCurrent()
        vm.recordReceipt(vm.state.value)
        assertEquals(1, sales.calls)
        assertEquals(90.0, sales.rows.values.single().totalAmount, 0.0)
        vm.onAction(BillingAction.OnClearCart)
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        vm.recordReceipt(vm.state.value)
        assertEquals(2, sales.rows.size)
    }

    @Test fun overflowingAmountDoesNotCorruptTheCart() = runTest(dispatcher) {
        val repository = ProductRepositoryImpl(MemoryProductDao())
        repository.addProduct(product(price = Double.MAX_VALUE))
        val vm = BillingViewModel(repository, MemoryShop(), MemoryPrinter(), MemorySales(), MemoryCustomers())
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        vm.onAction(BillingAction.OnQuantityChange("p", 2))
        assertEquals(1, vm.state.value.totalQuantity)
        assertTrue(vm.state.value.totalAmount.isFinite())
    }

    @Test fun quantityOverflowDoesNotWrapOrRemoveExistingLines() = runTest(dispatcher) {
        val repository = ProductRepositoryImpl(MemoryProductDao())
        repository.addProduct(product(price = 0.0))
        repository.addProduct(product(id = "second", barcode = "456", price = 0.0))
        val vm = BillingViewModel(repository, MemoryShop(), MemoryPrinter(), MemorySales(), MemoryCustomers())
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        runCurrent()
        vm.onAction(BillingAction.OnQuantityChange("p", Int.MAX_VALUE))
        vm.onAction(BillingAction.OnBarcodeDetected("123"))
        vm.onAction(BillingAction.OnBarcodeDetected("456"))
        runCurrent()
        assertEquals(Int.MAX_VALUE, vm.state.value.totalQuantity)
        assertEquals("p", vm.state.value.cartItems.single().product.id)
    }

    @Test fun failedPrintAlwaysReleasesSubmittingState() = runTest(dispatcher) {
        val printer = MemoryPrinter().apply { fail = true }
        val vm = BillingViewModel(ProductRepositoryImpl(MemoryProductDao()), MemoryShop(), printer, MemorySales(), MemoryCustomers())
        runCurrent()
        vm.onAction(BillingAction.PrintReceiptClick)
        runCurrent()
        assertFalse(vm.state.value.isPrinting)
        assertFalse(vm.state.value.printSuccess)
        assertTrue(vm.events.first() is BillingEvent.ShowSnackbar)
    }

    @Test fun savedShopUpdatesViewModelAndReloadedBillingDetails() = runTest(dispatcher) {
        val shop = MemoryShop()
        val vm = ShopViewModel(shop)
        val billing = BillingViewModel(ProductRepositoryImpl(MemoryProductDao()), shop, MemoryPrinter(), MemorySales(), MemoryCustomers())
        runCurrent()
        vm.onAction(ShopAction.SaveShop("New", "Address", "", "123", "new@bank", "Thanks"))
        runCurrent()
        assertEquals("New", vm.state.value.shop!!.name)
        billing.onAction(BillingAction.LoadShopDetails)
        runCurrent()
        assertEquals("new@bank", billing.state.value.shopDetails!!.upiId)
    }
    @Test fun csvImportUpdatesBarcodePreservingIdentityAndNormalAddStillRejectsIt() = runTest(dispatcher) {
        val dao = MemoryProductDao()
        val repo = ProductRepositoryImpl(dao)
        repo.addProduct(product())
        val result = repo.importProducts(listOf(product(id = "new-id", price = 9.0, stock = 12), product(id = "new", barcode = "456")))
        assertTrue(result is Result.Success)
        val counts = (result as Result.Success).data
        assertEquals(1, counts.added)
        assertEquals(1, counts.updated)
        assertEquals("p", dao.rows.value.first { it.barcode == "123" }.id)
        assertEquals(12, dao.rows.value.first { it.barcode == "123" }.stock)
        assertTrue(repo.addProduct(product(id = "duplicate")) is Result.Error)
    }

    @Test fun duplicateBarcodesWithinImportUpdateEarlierRows() = runTest(dispatcher) {
        val repo = ProductRepositoryImpl(MemoryProductDao())
        val result = repo.importProducts(listOf(product(), product(id = "second", price = 3.0))) as Result.Success
        assertEquals(1, result.data.added)
        assertEquals(1, result.data.updated)
        assertEquals(3.0, repo.getProducts().first().single().price, 0.0)
    }

    @Test fun unknownBarcodeOffersAddWithOriginalBarcode() = runTest(dispatcher) {
        val vm = BillingViewModel(ProductRepositoryImpl(MemoryProductDao()), MemoryShop(), MemoryPrinter(), MemorySales(), MemoryCustomers())
        vm.onAction(BillingAction.OnBarcodeDetected("00123"))
        runCurrent()
        val event = vm.events.first() as BillingEvent.ShowSnackbar
        assertTrue(event.isError)
        assertEquals("00123", event.unknownBarcode)
    }

    @Test fun onboardingSkipPersistsEmptyAndCompletesSetup() = runTest(dispatcher) {
        val shop = MemoryShop()
        val vm = ShopViewModel(shop)
        runCurrent()
        vm.onAction(ShopAction.SkipSetup)
        runCurrent()
        assertEquals(Shop.EMPTY, shop.shop)
        assertTrue(shop.isSetupComplete())
        assertEquals(ShopEvent.SaveSuccess, vm.events.first())
    }

    @Test fun onboardingSaveReusesValidationAndOnlyCompletesAfterValidSave() = runTest(dispatcher) {
        val shop = MemoryShop()
        val vm = ShopViewModel(shop)
        runCurrent()
        vm.onAction(ShopAction.SaveShop("", "Street", "", "123", "", "", completeSetup = true))
        runCurrent()
        assertFalse(shop.complete)
        vm.onAction(ShopAction.SaveShop("Store", "Street", "", "123", "", "", completeSetup = true))
        runCurrent()
        assertTrue(shop.complete)
        assertEquals("Store", shop.shop.name)
    }

    @Test fun startupDoesNotExposeHomeBeforePreferenceRead() = runTest(dispatcher) {
        val vm = StartupViewModel(MemoryShop())
        assertNull(vm.state.value.complete)
        runCurrent()
        assertEquals(false, vm.state.value.complete)
    }

}

private class MemoryProductDao : ProductDao {
    val rows = MutableStateFlow<List<ProductEntity>>(emptyList())
    var cancelReads = false
    var failObservation = false
    override fun getAll(): Flow<List<ProductEntity>> = flow {
        if (failObservation) throw IllegalStateException("Read failed")
        emitAll(rows)
    }
    override suspend fun getByBarcode(barcode: String): ProductEntity? {
        if (cancelReads) throw CancellationException("Cancelled")
        return rows.value.firstOrNull { it.barcode == barcode }
    }
    override suspend fun upsert(product: ProductEntity) {
        rows.value = rows.value.filterNot { it.id == product.id } + product
    }
    override suspend fun update(product: ProductEntity): Int {
        if (rows.value.none { it.id == product.id }) return 0
        rows.value = rows.value.map { if (it.id == product.id) product else it }
        return 1
    }
    override suspend fun deleteById(id: String) { rows.value = rows.value.filterNot { it.id == id } }
}

private class MemoryShop : ShopRepository {
    override fun profiles() = flowOf(listOf(shop))
    override suspend fun selectShop(id: String) { }
    override suspend fun addShop(shop: Shop) = updateShop(shop)
    var complete = false
    override suspend fun isSetupComplete() = complete
    override suspend fun completeSetup(shop: Shop): Result<Unit, DataError.Local> {
        this.shop = shop
        complete = true
        return Result.Success(Unit)
    }
    var shop = Shop(name = "Shop", upiId = "shop@bank")
    override suspend fun getShop(): Result<Shop, DataError.Local> = Result.Success(shop)
    override suspend fun updateShop(shop: Shop): Result<Unit, DataError.Local> {
        this.shop = shop
        return Result.Success(Unit)
    }
}

private class MemoryPrinter : PrinterRepository {
    override val savedPrinterMac = MutableStateFlow<String?>("00:11:22:33:44:55")
    override val savedPrinterName = MutableStateFlow<String?>("Printer")
    override val connectionState = MutableStateFlow(false)
    override val isConnected get() = connectionState.value
    var connectionGate: CompletableDeferred<Unit>? = null
    var fail = false
    var prints = 0
    var printedItems = emptyList<Triple<String, Double, Int>>()
    var printedTotal = 0.0
    override suspend fun getBondedDevices() = emptyList<BluetoothDeviceInfo>()
    override suspend fun connect(macAddress: String): Boolean {
        connectionGate?.await()
        if (fail) throw IllegalStateException("Disconnected")
        connectionState.value = true
        return true
    }
    override suspend fun disconnect(): Boolean { connectionState.value = false; return true }
    override suspend fun savePrinter(mac: String, name: String) { savedPrinterMac.value = mac; savedPrinterName.value = name }
    override suspend fun clearPrinter() { savedPrinterMac.value = null; savedPrinterName.value = null }
    override suspend fun testPrint(shopName: String) = true
    override suspend fun printReceipt(shopName: String, address1: String, address2: String, phone: String,
        items: List<Triple<String, Double, Int>>, total: Double, footer: String, timestamp: String, discountAmount: Double): Boolean {
        prints++
        printedItems = items
        printedTotal = total
        return true
    }
}
