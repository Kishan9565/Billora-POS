package com.kishan.billorapos.feature.billing.domain

import com.kishan.billorapos.core.domain.Product

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val total: Double get() = product.price * quantity
}
