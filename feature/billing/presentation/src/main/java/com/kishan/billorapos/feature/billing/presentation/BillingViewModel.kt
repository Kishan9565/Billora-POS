package com.kishan.billorapos.feature.billing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.feature.billing.domain.CartItem
import com.kishan.billorapos.feature.product.domain.ProductRepository
import com.kishan.billorapos.feature.settings.domain.PrinterRepository
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.kishan.billorapos.core.domain.totalAmount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.kishan.billorapos.core.domain.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import java.util.UUID

class BillingViewModel(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val printerRepository: PrinterRepository,
    private val salesRepository: SalesRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _eventChannel = Channel<BillingEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()
    private val recordMutex = Mutex()
    private var customerSearch: Job? = null

    init {
        onAction(BillingAction.LoadShopDetails)
    }

    fun onAction(action: BillingAction) {
        val locked = _state.value.isPrinting || _state.value.isExporting || _state.value.saleRecorded
        if (locked && (action is BillingAction.ApplyDiscount || action is BillingAction.PaymentMethod ||
            action is BillingAction.SelectCustomer || action is BillingAction.AddCustomer)) return
        if ((_state.value.isPrinting || _state.value.isExporting) && (action is BillingAction.OnClearCart ||
            action is BillingAction.OnBarcodeDetected || action is BillingAction.OnQuantityChange || action is BillingAction.OnRemoveItem)) return
        when (action) {
            is BillingAction.ApplyDiscount -> {
                val value = action.value.toDoubleOrNull()
                if (value == null || !value.isFinite() || value < 0) {
                    _eventChannel.trySend(BillingEvent.ShowSnackbar("Enter a valid non-negative discount", true))
                } else {
                    val totals = checkoutTotals(_state.value.subtotal, value, action.percentage)
                    _state.update { it.copy(discountAmount = totals.discountAmount, totalAmount = totals.totalAmount) }
                }
            }
            is BillingAction.PaymentMethod -> if (action.method in listOf("CASH", "UPI", "CREDIT")) _state.update { it.copy(paymentMethod = action.method) }
            is BillingAction.SelectCustomer -> _state.update { it.copy(customer = action.customer) }
            is BillingAction.SearchCustomers -> {
                _state.update { it.copy(customerQuery = action.query) }
                customerSearch?.cancel()
                customerSearch = viewModelScope.launch {
                    try { customerRepository.search(action.query).collect { rows -> _state.update { it.copy(customers = rows) } } }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { _eventChannel.send(BillingEvent.ShowSnackbar("Unable to load customers", true)) }
                }
            }
            is BillingAction.AddCustomer -> viewModelScope.launch {
                try {
                    val customer = Customer(UUID.randomUUID().toString(), action.name.trim(), action.phone.trim().takeIf { it.isNotBlank() })
                    customerRepository.save(customer)
                    _state.update { it.copy(customer = customer) }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { _eventChannel.send(BillingEvent.ShowSnackbar(e.message ?: "Unable to save customer", true)) }
            }
            is BillingAction.LoadShopDetails -> {
                viewModelScope.launch {
                    when (val res = shopRepository.getShop()) {
                        is Result.Success -> {
                            _state.update { it.copy(shopDetails = res.data, error = null) }
                        }
                        is Result.Error -> {
                            _state.update { it.copy(shopDetails = null, error = "Unable to load shop details") }
                        }
                    }
                }
            }
            is BillingAction.OnBarcodeDetected -> {
                viewModelScope.launch {
                    when (val res = productRepository.getProductByBarcode(action.rawValue)) {
                        is Result.Success -> {
                            val product = res.data
                            if (!product.price.isFinite() || product.price < 0) {
                                _eventChannel.send(BillingEvent.ShowSnackbar("Product has an invalid price", isError = true))
                                return@launch
                            }
                            _state.update { s ->
                                val existingIndex = s.cartItems.indexOfFirst { it.product.id == product.id }
                                val updatedList = s.cartItems.toMutableList()
                                if (existingIndex != -1) {
                                    val item = updatedList[existingIndex]
                                    if (item.quantity == Int.MAX_VALUE) return@update s
                                    updatedList[existingIndex] = item.copy(quantity = item.quantity + 1)
                                } else {
                                    updatedList.add(CartItem(product, 1))
                                }
                                withCart(s, updatedList)
                            }
                        }
                        is Result.Error -> {
                            val message = if (res.error == com.kishan.billorapos.core.domain.DataError.Local.NOT_FOUND)
                                "Product not found: ${action.rawValue}" else "Unable to look up product. Please retry."
                            _eventChannel.send(BillingEvent.ShowSnackbar(message, isError = true, unknownBarcode = action.rawValue.takeIf { res.error == com.kishan.billorapos.core.domain.DataError.Local.NOT_FOUND }))
                        }
                    }
                }
            }
            is BillingAction.OnQuantityChange -> {
                _state.update { s ->
                    val index = s.cartItems.indexOfFirst { it.product.id == action.productId }
                    if (index != -1) {
                        val updatedList = s.cartItems.toMutableList()
                        if (action.newQty <= 0) {
                            updatedList.removeAt(index)
                        } else {
                            updatedList[index] = updatedList[index].copy(quantity = action.newQty)
                        }
                        withCart(s, updatedList)
                    } else s
                }
            }
            is BillingAction.OnRemoveItem -> {
                _state.update { s ->
                    val updatedList = s.cartItems.filterNot { it.product.id == action.productId }
                    withCart(s, updatedList)
                }
            }
            is BillingAction.OnToggleCamera -> {
                _state.update { it.copy(isCameraOn = !it.isCameraOn) }
            }
            is BillingAction.OnToggleFlash -> {
                _state.update { it.copy(isFlashOn = !it.isFlashOn) }
            }
            is BillingAction.OnClearCart -> {
                _state.update { BillingState(shopDetails = it.shopDetails, isCameraOn = it.isCameraOn) }
            }
            is BillingAction.PrintReceiptClick -> {
                if (_state.value.isPrinting || _state.value.isExporting) return
                val receipt = _state.value
                if (!validateReceipt(receipt)) return
                _state.update { it.copy(isPrinting = true, printSuccess = false) }
                viewModelScope.launch {
                    try {
                        val shop = when (val result = shopRepository.getShop()) {
                            is Result.Success -> result.data
                            is Result.Error -> null
                        }
                        if (shop == null) {
                            _eventChannel.send(BillingEvent.ShowSnackbar("Shop details not loaded", isError = true))
                            return@launch
                        }

                        if (!printerRepository.isConnected) {
                            val savedMac = printerRepository.savedPrinterMac.first()
                            if (savedMac == null) {
                                _eventChannel.send(BillingEvent.ShowSnackbar("Printer not connected & no saved printer found!", isError = true))
                                return@launch
                            }
                            val connectRes = printerRepository.connect(savedMac)
                            if (!connectRes) {
                                _eventChannel.send(BillingEvent.ShowSnackbar("Failed to auto-connect to printer!", isError = true))
                                return@launch
                            }
                        }

                        val itemsForPrinter = receipt.cartItems.map {
                            Triple(it.product.name, it.product.price, it.quantity)
                        }
                        val nowStr = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

                        val printRes = printerRepository.printReceipt(
                            shopName = shop.name,
                            address1 = shop.addressLine1,
                            address2 = shop.addressLine2,
                            phone = shop.phoneNumber,
                            items = itemsForPrinter,
                            total = receipt.totalAmount,
                            footer = shop.footerText,
                            timestamp = nowStr,
                            discountAmount = receipt.discountAmount
                        )

                        if (printRes) {
                            recordReceipt(receipt)
                            _state.update { it.copy(printSuccess = true) }
                            _eventChannel.send(BillingEvent.ShowSnackbar("Printed successfully"))
                        } else {
                            _eventChannel.send(BillingEvent.ShowSnackbar("Print failed: Printer error", isError = true))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _eventChannel.send(BillingEvent.ShowSnackbar("Print failed: ${e.message ?: "Printer unavailable"}", isError = true))
                    } finally {
                        _state.update { it.copy(isPrinting = false) }
                    }
                }
            }
        }
    }

    private fun withCart(current: BillingState, items: List<CartItem>): BillingState {
        val total = totalAmount(items.map { it.product.price to it.quantity })
        val quantity = items.sumOf { it.quantity.toLong() }
        if (!total.isFinite() || quantity > Int.MAX_VALUE) {
            _eventChannel.trySend(BillingEvent.ShowSnackbar("Amount or quantity is too large", isError = true))
            return current
        }
        val totals = checkoutTotals(total, if (current.saleRecorded) 0.0 else current.discountAmount)
        return current.copy(cartItems = items, subtotal = totals.subtotal, discountAmount = totals.discountAmount,
            totalAmount = totals.totalAmount, totalQuantity = quantity.toInt(), saleRecorded = false,
            checkoutId = if (current.saleRecorded) UUID.randomUUID().toString() else current.checkoutId)
    }

    private fun validateReceipt(receipt: BillingState): Boolean {
        val message = when {
            receipt.cartItems.isEmpty() -> "Add items before completing a sale"
            receipt.paymentMethod == "CREDIT" && receipt.customer == null -> "Select a customer for credit"
            else -> null
        }
        if (message != null) _eventChannel.trySend(BillingEvent.ShowSnackbar(message, true))
        return message == null
    }

    suspend fun recordReceipt(receipt: BillingState) = recordMutex.withLock {
        if (_state.value.checkoutId == receipt.checkoutId && _state.value.saleRecorded) return@withLock
        check(validateReceipt(receipt)) { "Checkout is incomplete" }
        val credit = receipt.paymentMethod == "CREDIT"
        val sale = Sale(receipt.checkoutId, System.currentTimeMillis(), receipt.subtotal, receipt.discountAmount,
            receipt.totalAmount, receipt.totalQuantity, receipt.paymentMethod, receipt.customer?.id.takeIf { credit },
            if (credit) 0.0 else receipt.totalAmount, !credit)
        salesRepository.recordSale(sale, receipt.cartItems.map {
            SaleLine(UUID.randomUUID().toString(), sale.id, it.product.id, it.product.name, it.quantity, it.product.price)
        })
        _state.update { if (it.checkoutId == receipt.checkoutId) it.copy(saleRecorded = true) else it }
    }

    fun exportPdf(context: android.content.Context, whatsapp: Boolean = false) {
        if (_state.value.isPrinting || _state.value.isExporting) return
        val receipt = _state.value
        if (!validateReceipt(receipt)) return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val shop = when (val result = shopRepository.getShop()) {
                    is Result.Success -> result.data
                    is Result.Error -> error("Unable to load shop")
                }
                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.kishan.billorapos.core.printer.ReceiptPdf.create(context, shop,
                        receipt.cartItems.map { Triple(it.product.name, it.product.price, it.quantity) },
                        receipt.totalAmount, SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date()), receipt.discountAmount)
                }
                recordReceipt(receipt)
                com.kishan.billorapos.core.presentation.shareFile(context, file, "application/pdf", if (whatsapp) "com.whatsapp" else null)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _eventChannel.send(BillingEvent.ShowSnackbar(e.message ?: "Unable to export receipt", true)) }
            finally { _state.update { it.copy(isExporting = false) } }
        }
    }
}
