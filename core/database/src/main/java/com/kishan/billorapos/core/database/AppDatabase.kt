package com.kishan.billorapos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class, ShopEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shopDao(): ShopDao
}
