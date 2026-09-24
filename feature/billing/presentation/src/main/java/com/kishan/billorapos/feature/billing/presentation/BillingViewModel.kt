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

class BillingViewModel(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _eventChannel = Channel<BillingEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        onAction(BillingAction.LoadShopDetails)
    }

    fun onAction(action: BillingAction) {
        when (action) {
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
                            _eventChannel.send(BillingEvent.ShowSnackbar(message, isError = true))
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
                _state.update {
                    it.copy(
                        cartItems = emptyList(),
                        totalAmount = 0.0,
                        totalQuantity = 0,
                        printSuccess = false
                    )
                }
            }
            is BillingAction.PrintReceiptClick -> {
                if (_state.value.isPrinting) return
                val receipt = _state.value
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
                            timestamp = nowStr
                        )

                        if (printRes) {
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
        return current.copy(cartItems = items, totalAmount = total, totalQuantity = quantity.toInt())
    }
}
