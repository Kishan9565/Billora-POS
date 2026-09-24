package com.kishan.billorapos.feature.settings.data

import com.kishan.billorapos.core.data.PrinterDataStore
import com.kishan.billorapos.core.printer.BluetoothDeviceInfo
import com.kishan.billorapos.core.printer.PrinterHelper
import com.kishan.billorapos.feature.settings.domain.PrinterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrinterRepositoryImpl(
    private val printerHelper: PrinterHelper,
    private val printerDataStore: PrinterDataStore
) : PrinterRepository {

    override val savedPrinterMac: Flow<String?> = printerDataStore.printerMac
    override val savedPrinterName: Flow<String?> = printerDataStore.printerName

    override val connectionState = printerHelper.connectionState

    override val isConnected: Boolean
        get() = printerHelper.isConnected

    override suspend fun getBondedDevices(): List<BluetoothDeviceInfo> = withContext(Dispatchers.IO) { printerHelper.getBondedDevices() }

    override suspend fun connect(macAddress: String): Boolean = withContext(Dispatchers.IO) { printerHelper.connect(macAddress) }

    override suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) { printerHelper.disconnect() }

    override suspend fun savePrinter(mac: String, name: String) = printerDataStore.savePrinter(mac, name)

    override suspend fun clearPrinter() = printerDataStore.clearPrinter()

    override suspend fun printReceipt(
        shopName: String,
        address1: String,
        address2: String,
        phone: String,
        items: List<Triple<String, Double, Int>>,
        total: Double,
        footer: String,
        timestamp: String
    ): Boolean {
        return withContext(Dispatchers.IO) { printerHelper.printReceipt(shopName, address1, address2, phone, items, total, footer, timestamp) }
    }

    override suspend fun testPrint(shopName: String): Boolean = withContext(Dispatchers.IO) { printerHelper.testPrint(shopName) }
}
