package com.kishan.billorapos.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_details")
data class ShopEntity(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val addressLine1: String,
    val addressLine2: String,
    val phoneNumber: String,
    val upiId: String,
    val footerText: String
)
