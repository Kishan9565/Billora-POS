package com.kishan.billorapos.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_details WHERE id = 0")
    suspend fun getShop(): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShop(shop: ShopEntity)
}
