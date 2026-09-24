package com.kishan.billorapos.feature.product.domain

import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.DataError
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    suspend fun addProduct(product: Product): Result<Unit, DataError.Local>
    suspend fun updateProduct(product: Product): Result<Unit, DataError.Local>
    suspend fun deleteProduct(id: String): Result<Unit, DataError.Local>
    suspend fun getProductByBarcode(barcode: String): Result<Product, DataError.Local>
}
