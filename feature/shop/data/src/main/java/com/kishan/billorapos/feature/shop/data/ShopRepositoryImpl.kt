package com.kishan.billorapos.feature.shop.data

import com.kishan.billorapos.core.database.ShopDao
import com.kishan.billorapos.core.database.ShopEntity
import com.kishan.billorapos.core.domain.DataError
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class ShopRepositoryImpl(
    private val shopDao: ShopDao,
    private val setupStore: com.kishan.billorapos.core.data.ShopSetupDataStore
) : ShopRepository {

    override suspend fun isSetupComplete(): Boolean = setupStore.isComplete.first()

    override suspend fun completeSetup(shop: Shop): Result<Unit, DataError.Local> {
        return try {
            when (val result = updateShop(shop)) {
                is Result.Error -> result
                is Result.Success -> {
                    setupStore.complete()
                    Result.Success(Unit)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, "Unable to complete shop setup. Please retry.")
        }
    }

    override suspend fun getShop(): Result<Shop, DataError.Local> {
        return try {
            val entity = shopDao.getShop()
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Success(Shop.EMPTY)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun updateShop(shop: Shop): Result<Unit, DataError.Local> {
        return try {
            shopDao.upsertShop(shop.toEntity())
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    private fun ShopEntity.toDomain() = Shop(
        name = name,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        phoneNumber = phoneNumber,
        upiId = upiId,
        footerText = footerText
    )

    private fun Shop.toEntity() = ShopEntity(
        id = 0,
        name = name,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        phoneNumber = phoneNumber,
        upiId = upiId,
        footerText = footerText
    )
}
