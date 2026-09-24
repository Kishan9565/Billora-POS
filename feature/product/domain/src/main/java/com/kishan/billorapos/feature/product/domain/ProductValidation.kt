package com.kishan.billorapos.feature.product.domain

data class ProductValidation(
    val nameError: String? = null,
    val barcodeError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null
) {
    val isValid get() = nameError == null && barcodeError == null && priceError == null && stockError == null
}

fun validateProduct(name: String, barcode: String, price: String, stock: Int = 0): ProductValidation {
    val parsedPrice = price.toDoubleOrNull()
    return ProductValidation(
        nameError = if (name.isBlank()) "Please enter a name" else null,
        barcodeError = if (barcode.isBlank()) "Please enter a barcode" else null,
        priceError = when {
            price.isBlank() -> "Please enter a price"
            parsedPrice == null || !parsedPrice.isFinite() -> "Please enter a valid number"
            parsedPrice < 0 -> "Price cannot be negative"
            else -> null
        },
        stockError = if (stock < 0) "Stock cannot be negative" else null
    )
}
