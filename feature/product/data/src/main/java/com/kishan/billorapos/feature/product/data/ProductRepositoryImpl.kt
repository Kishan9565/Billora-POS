package com.kishan.billorapos.feature.product.data

import com.kishan.billorapos.core.database.ProductDao
import com.kishan.billorapos.core.database.ProductEntity
import com.kishan.billorapos.core.domain.DataError
import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.feature.product.domain.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override suspend fun importProducts(products: List<Product>): Result<com.kishan.billorapos.feature.product.domain.ImportCounts, DataError.Local> {
        return try {
            val (added, updated) = productDao.importProducts(products.map { it.toEntity() })
            Result.Success(com.kishan.billorapos.feature.product.domain.ImportCounts(added, updated))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, "Unable to import products. No changes were saved.")
        }
    }

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addProduct(product: Product): Result<Unit, DataError.Local> {
        return try {
            if (productDao.insertIfBarcodeAbsent(product.toEntity())) {
                Result.Success(Unit)
            } else {
                Result.Error(DataError.Local.UNKNOWN, "Product with barcode \"${product.barcode}\" already exists!")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit, DataError.Local> {
        return try {
            if (productDao.update(product.toEntity()) > 0) Result.Success(Unit)
            else Result.Error(DataError.Local.UNKNOWN, "Product no longer exists")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun deleteProduct(id: String): Result<Unit, DataError.Local> {
        return try {
            productDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun getProductByBarcode(barcode: String): Result<Product, DataError.Local> {
        return try {
            val matched = productDao.getByBarcode(barcode)
            if (matched != null) {
                Result.Success(matched.toDomain())
            } else {
                Result.Error(DataError.Local.NOT_FOUND, "Product not found")
            }
        } catch (e: CancellationException) {
            throw e
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
