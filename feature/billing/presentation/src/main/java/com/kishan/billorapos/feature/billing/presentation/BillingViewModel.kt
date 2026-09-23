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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BillingViewModel(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _eventChannel = Channel<BillingEvent>()
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
                            _state.update { it.copy(shopDetails = res.data) }
                        }
                        is Result.Error -> {
                            _state.update { it.copy(shopDetails = Shop.DEFAULT) }
                        }
                    }
                }
            }
            is BillingAction.OnBarcodeDetected -> {
                viewModelScope.launch {
                    when (val res = productRepository.getProductByBarcode(action.rawValue)) {
                        is Result.Success -> {
                            val product = res.data
                            _state.update { s ->
                                val existingIndex = s.cartItems.indexOfFirst { it.product.id == product.id }
                                val updatedList = s.cartItems.toMutableList()
                                if (existingIndex != -1) {
                                    val item = updatedList[existingIndex]
                                    updatedList[existingIndex] = item.copy(quantity = item.quantity + 1)
                                } else {
                                    updatedList.add(CartItem(product, 1))
                                }
                                s.copy(
                                    cartItems = updatedList,
                                    totalAmount = updatedList.sumOf { it.total },
                                    totalQuantity = updatedList.sumOf { it.quantity }
                                )
                            }
                        }
                        is Result.Error -> {
                            _eventChannel.send(BillingEvent.ShowSnackbar("Product not found: ${action.rawValue}", isError = true))
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
                        s.copy(
                            cartItems = updatedList,
                            totalAmount = updatedList.sumOf { it.total },
                            totalQuantity = updatedList.sumOf { it.quantity }
                        )
                    } else s
                }
            }
            is BillingAction.OnRemoveItem -> {
                _state.update { s ->
                    val updatedList = s.cartItems.filterNot { it.product.id == action.productId }
                    s.copy(
                        cartItems = updatedList,
                        totalAmount = updatedList.sumOf { it.total },
                        totalQuantity = updatedList.sumOf { it.quantity }
                    )
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
                viewModelScope.launch {
                    _state.update { it.copy(isPrinting = true, printSuccess = false) }
                    val shop = _state.value.shopDetails
                    if (shop == null) {
                        _eventChannel.send(BillingEvent.ShowSnackbar("Shop details not loaded", isError = true))
                        _state.update { it.copy(isPrinting = false) }
                        return@launch
                    }

                    if (!printerRepository.isConnected) {
                        val savedMac = printerRepository.savedPrinterMac.first()
                        if (savedMac == null) {
                            _eventChannel.send(BillingEvent.ShowSnackbar("Printer not connected & no saved printer found!", isError = true))
                            _state.update { it.copy(isPrinting = false) }
                            return@launch
                        }
                        val connectRes = printerRepository.connect(savedMac)
                        if (!connectRes) {
                            _eventChannel.send(BillingEvent.ShowSnackbar("Failed to auto-connect to printer!", isError = true))
                            _state.update { it.copy(isPrinting = false) }
                            return@launch
                        }
                    }

                    val itemsForPrinter = _state.value.cartItems.map {
                        Triple(it.product.name, it.product.price, it.quantity)
                    }
                    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")
                    val nowStr = LocalDateTime.now().format(formatter)

                    val printRes = printerRepository.printReceipt(
                        shopName = shop.name,
                        address1 = shop.addressLine1,
                        address2 = shop.addressLine2,
                        phone = shop.phoneNumber,
                        items = itemsForPrinter,
                        total = _state.value.totalAmount,
                        footer = shop.footerText,
                        timestamp = nowStr
                    )

                    if (printRes) {
                        _state.update { it.copy(printSuccess = true) }
                        _eventChannel.send(BillingEvent.ShowSnackbar("Printed successfully"))
                    } else {
                        _eventChannel.send(BillingEvent.ShowSnackbar("Print failed: Printer error", isError = true))
                    }
                    _state.update { it.copy(isPrinting = false) }
                }
            }
        }
    }
}
