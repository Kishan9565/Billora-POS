package com.kishan.billorapos.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_details ORDER BY isActive DESC, name ASC LIMIT 1")
    suspend fun getShop(): ShopEntity?

    @Query("SELECT * FROM shop_details WHERE id = :id")
    suspend fun getById(id: String): ShopEntity?

    @Query("SELECT * FROM shop_details ORDER BY name COLLATE NOCASE")
    fun observeShops(): kotlinx.coroutines.flow.Flow<List<ShopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShop(shop: ShopEntity)
}
