package com.kishan.billorapos.core.domain

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Double,
    val stock: Int = 0
)
