package com.kishan.billorapos.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"])]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val barcode: String,
    val price: Double,
    val stock: Int
)
