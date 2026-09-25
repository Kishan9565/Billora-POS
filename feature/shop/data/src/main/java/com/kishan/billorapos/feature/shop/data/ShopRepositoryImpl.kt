package com.kishan.billorapos.feature.shop.data

import com.kishan.billorapos.core.database.ShopDao
import com.kishan.billorapos.core.database.ShopEntity
import com.kishan.billorapos.core.domain.DataError
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import java.util.UUID

class ShopRepositoryImpl(
    private val shopDao: ShopDao,
    private val setupStore: com.kishan.billorapos.core.data.ShopSetupDataStore
) : ShopRepository {

    override suspend fun isSetupComplete(): Boolean {
        if (setupStore.isComplete.first()) return true
        if (shopDao.getShop()?.name?.isNotBlank() == true) { setupStore.complete(); return true }
        return false
    }

    override fun profiles() = combine(shopDao.observeShops(), setupStore.activeShopId) { shops, active ->
        val selected = active ?: shops.firstOrNull { it.isActive }?.id ?: shops.firstOrNull()?.id
        shops.map { it.toDomain().copy(isActive = it.id == selected) }
    }
    override suspend fun selectShop(id: String) {
        require(shopDao.getById(id) != null) { "Shop no longer exists" }
        setupStore.selectShop(id)
    }
    override suspend fun addShop(shop: Shop): Result<Unit, DataError.Local> {
        return try {
            val id = UUID.randomUUID().toString()
            shopDao.upsertShop(shop.copy(id = id).toEntity())
            setupStore.selectShop(id)
            Result.Success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.Error(DataError.Local.UNKNOWN, "Unable to add shop") }
    }

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
            val active = setupStore.activeShopId.first()
            val entity = active?.let { shopDao.getById(it) } ?: shopDao.getShop()
            if (entity != null) {
                if (active != entity.id) setupStore.selectShop(entity.id)
                Result.Success(entity.toDomain().copy(isActive = true))
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
            val id = shop.id.takeIf { it.isNotBlank() } ?: setupStore.activeShopId.first() ?: shopDao.getShop()?.id ?: UUID.randomUUID().toString()
            shopDao.upsertShop(shop.copy(id = id).toEntity())
            if (setupStore.activeShopId.first() == null) setupStore.selectShop(id)
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
        footerText = footerText,
        id = id,
        isActive = isActive
    )

    private fun Shop.toEntity() = ShopEntity(
        id = id,
        name = name,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        phoneNumber = phoneNumber,
        upiId = upiId,
        footerText = footerText
    )
}
