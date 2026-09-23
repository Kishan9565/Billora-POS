package com.kishan.billorapos.feature.shop.data

import com.kishan.billorapos.core.database.ShopDao
import com.kishan.billorapos.core.database.ShopEntity
import com.kishan.billorapos.core.domain.DataError
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.feature.shop.domain.ShopRepository

class ShopRepositoryImpl(
    private val shopDao: ShopDao
) : ShopRepository {

    override suspend fun getShop(): Result<Shop, DataError.Local> {
        return try {
            val entity = shopDao.getShop()
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Success(Shop.DEFAULT)
            }
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN, e.toString())
        }
    }

    override suspend fun updateShop(shop: Shop): Result<Unit, DataError.Local> {
        return try {
            shopDao.upsertShop(shop.toEntity())
            Result.Success(Unit)
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
