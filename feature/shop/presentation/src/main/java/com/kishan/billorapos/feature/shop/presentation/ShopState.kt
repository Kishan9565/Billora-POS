package com.kishan.billorapos.feature.shop.presentation

import com.kishan.billorapos.core.domain.Shop

data class ShopState(
    val shop: Shop? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ShopAction {
    object SkipSetup : ShopAction
    object LoadShop : ShopAction
    data class SaveShop(
        val name: String,
        val addressLine1: String,
        val addressLine2: String,
        val phoneNumber: String,
        val upiId: String,
        val footerText: String,
        val completeSetup: Boolean = false
    ) : ShopAction
}

sealed interface ShopEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : ShopEvent
    object SaveSuccess : ShopEvent
}
