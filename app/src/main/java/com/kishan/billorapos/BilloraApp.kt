package com.kishan.billorapos

import android.app.Application
import androidx.room.Room
import com.kishan.billorapos.core.data.PrinterDataStore
import com.kishan.billorapos.core.data.ShopSetupDataStore
import com.kishan.billorapos.core.database.AppDatabase
import com.kishan.billorapos.core.printer.PrinterHelper
import com.kishan.billorapos.feature.billing.presentation.BillingViewModel
import com.kishan.billorapos.feature.product.data.ProductRepositoryImpl
import com.kishan.billorapos.feature.product.domain.ProductRepository
import com.kishan.billorapos.feature.product.presentation.ProductViewModel
import com.kishan.billorapos.feature.settings.data.PrinterRepositoryImpl
import com.kishan.billorapos.feature.settings.domain.PrinterRepository
import com.kishan.billorapos.feature.settings.presentation.PrinterViewModel
import com.kishan.billorapos.feature.shop.data.ShopRepositoryImpl
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import com.kishan.billorapos.feature.shop.presentation.ShopViewModel
import org.koin.android.ext.koin.androidContext
import com.kishan.billorapos.feature.settings.presentation.ManagementViewModel
import com.kishan.billorapos.feature.shop.presentation.ShopProfilesViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class BilloraApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val appModule = module {
            // Room DB
            single {
                Room.databaseBuilder(
                    androidContext(),
                    AppDatabase::class.java,
                    "billora_pos.db"
                ).addMigrations(com.kishan.billorapos.core.database.MIGRATION_1_2).build()
            }
            single { get<AppDatabase>().productDao() }
            single { get<AppDatabase>().shopDao() }
            single { get<AppDatabase>().saleDao() }
            single { get<AppDatabase>().customerDao() }
            single<com.kishan.billorapos.core.domain.SalesRepository> { com.kishan.billorapos.core.data.SalesRepositoryImpl(get()) }
            single<com.kishan.billorapos.core.domain.CustomerRepository> { com.kishan.billorapos.core.data.CustomerRepositoryImpl(get()) }
            single<com.kishan.billorapos.core.domain.PosPreferences> { com.kishan.billorapos.core.data.PosPreferencesImpl(get()) }

            // DataStore / Core Printer
            singleOf(::PrinterDataStore)
            singleOf(::ShopSetupDataStore)
            singleOf(::PrinterHelper)

            // Repositories
            singleOf(::ProductRepositoryImpl) bind ProductRepository::class
            singleOf(::ShopRepositoryImpl) bind ShopRepository::class
            singleOf(::PrinterRepositoryImpl) bind PrinterRepository::class

            // ViewModels
            viewModelOf(::ProductViewModel)
            viewModelOf(::ShopViewModel)
            viewModelOf(::StartupViewModel)
            viewModelOf(::PrinterViewModel)
            viewModelOf(::BillingViewModel)
            viewModelOf(::ManagementViewModel)
            viewModelOf(::ShopProfilesViewModel)
        }

        startKoin {
            androidContext(this@BilloraApp)
            modules(appModule)
        }
    }
}
