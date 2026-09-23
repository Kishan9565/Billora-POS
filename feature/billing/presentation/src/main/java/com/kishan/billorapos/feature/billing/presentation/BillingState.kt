package com.kishan.billorapos.feature.billing.presentation

import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.feature.billing.domain.CartItem

data class BillingState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalQuantity: Int = 0,
    val error: String? = null,
    val isPrinting: Boolean = false,
    val printSuccess: Boolean = false,
    val shopDetails: Shop? = null,
    val isCameraOn: Boolean = true,
    val isFlashOn: Boolean = false
)

sealed interface BillingAction {
    data class OnBarcodeDetected(val rawValue: String) : BillingAction
    data class OnQuantityChange(val productId: String, val newQty: Int) : BillingAction
    data class OnRemoveItem(val productId: String) : BillingAction
    object OnToggleCamera : BillingAction
    object OnToggleFlash : BillingAction
    object OnClearCart : BillingAction
    object PrintReceiptClick : BillingAction
    object LoadShopDetails : BillingAction
}

sealed interface BillingEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : BillingEvent
    object NavigateToCheckout : BillingEvent
    object NavigateToSettings : BillingEvent
}
