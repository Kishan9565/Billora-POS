package com.kishan.billorapos.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String? = null
)
