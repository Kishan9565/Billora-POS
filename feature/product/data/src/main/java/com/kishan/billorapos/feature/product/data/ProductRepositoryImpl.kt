package com.kishan.billorapos.feature.product.data

import com.kishan.billorapos.core.database.ProductDao
import com.kishan.billorapos.core.database.ProductEntity
import com.kishan.billorapos.core.domain.DataError
import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.feature.product.domain.ProductRepository
import kotlinx.flow.Flow
import kotlinx.flow.map
import kotlinx.flow.first

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addProduct(product: Product): Result<Unit, DataError.Local> {
        return try {
            productDao.upsert(product.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit, DataError.Local> {
        return try {
            productDao.update(product.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit, DataError.Local> {
        return try {
            productDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun getProductByBarcode(barcode: String): Result<Product, DataError.Local> {
        return try {
            val list = productDao.getAll().first()
            val matched = list.firstOrNull { it.barcode == barcode }
            if (matched != null) {
                Result.Success(matched.toDomain())
            } else {
                Result.Error(DataError.Local.UNKNOWN, "Product not found")
            }
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    private fun ProductEntity.toDomain() = Product(
        id = id,
        name = name,
        barcode = barcode,
        price = price,
        stock = stock
    )

    private fun Product.toEntity() = ProductEntity(
        id = id,
        name = name,
        barcode = barcode,
        price = price,
        stock = stock
    )
}
