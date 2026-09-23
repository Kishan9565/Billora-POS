package com.kishan.billorapos.feature.settings.data

import com.kishan.billorapos.core.data.PrinterDataStore
import com.kishan.billorapos.core.printer.BluetoothDeviceInfo
import com.kishan.billorapos.core.printer.PrinterHelper
import com.kishan.billorapos.feature.settings.domain.PrinterRepository
import kotlinx.flow.Flow

class PrinterRepositoryImpl(
    private val printerHelper: PrinterHelper,
    private val printerDataStore: PrinterDataStore
) : PrinterRepository {

    override val savedPrinterMac: Flow<String?> = printerDataStore.printerMac
    override val savedPrinterName: Flow<String?> = printerDataStore.printerName

    override val isConnected: Boolean
        get() = printerHelper.isConnected

    override fun getBondedDevices(): List<BluetoothDeviceInfo> = printerHelper.getBondedDevices()

    override fun connect(macAddress: String): Boolean = printerHelper.connect(macAddress)

    override fun disconnect(): Boolean = printerHelper.disconnect()

    override suspend fun savePrinter(mac: String, name: String) = printerDataStore.savePrinter(mac, name)

    override suspend fun clearPrinter() = printerDataStore.clearPrinter()

    override fun printReceipt(
        shopName: String,
        address1: String,
        address2: String,
        phone: String,
        items: List<Triple<String, Double, Int>>,
        total: Double,
        footer: String,
        timestamp: String
    ): Boolean {
        return printerHelper.printReceipt(shopName, address1, address2, phone, items, total, footer, timestamp)
    }

    override fun testPrint(shopName: String): Boolean = printerHelper.testPrint(shopName)
}
