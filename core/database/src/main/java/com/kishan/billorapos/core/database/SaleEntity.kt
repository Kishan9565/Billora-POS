package com.kishan.billorapos.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val itemCount: Int,
    val paymentMethod: String, // CASH, UPI, CREDIT
    val customerId: String? = null,
    val amountPaid: Double = 0.0,
    val isSettled: Boolean = true
)

@Entity(
    tableName = "sale_lines",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"])]
)
data class SaleLineEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
)

data class TopSellingProduct(
    val productId: String,
    val productName: String,
    val totalQuantity: Int,
    val totalRevenue: Double
)
