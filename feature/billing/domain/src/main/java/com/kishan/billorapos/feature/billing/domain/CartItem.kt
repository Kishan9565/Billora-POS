package com.kishan.billorapos.feature.billing.domain

import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.core.domain.lineAmount

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val total: Double get() = lineAmount(product.price, quantity).toDouble()
}
