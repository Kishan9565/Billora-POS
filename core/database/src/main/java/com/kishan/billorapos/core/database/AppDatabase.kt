package com.kishan.billorapos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class, ShopEntity::class, SaleEntity::class, SaleLineEntity::class, CustomerEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shopDao(): ShopDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
}
