package com.kishan.billorapos.feature.shop.domain

import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.DataError

interface ShopRepository {
    fun profiles(): kotlinx.coroutines.flow.Flow<List<Shop>>
    suspend fun selectShop(id: String)
    suspend fun addShop(shop: Shop): Result<Unit, DataError.Local>
    suspend fun isSetupComplete(): Boolean
    suspend fun completeSetup(shop: Shop): Result<Unit, DataError.Local>
    suspend fun getShop(): Result<Shop, DataError.Local>
    suspend fun updateShop(shop: Shop): Result<Unit, DataError.Local>
}
