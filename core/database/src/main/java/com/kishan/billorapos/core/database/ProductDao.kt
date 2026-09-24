package com.kishan.billorapos.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode ORDER BY rowid LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Transaction
    suspend fun insertIfBarcodeAbsent(product: ProductEntity): Boolean {
        if (getByBarcode(product.barcode) != null) return false
        upsert(product)
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Update
    suspend fun update(product: ProductEntity): Int

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)
}
