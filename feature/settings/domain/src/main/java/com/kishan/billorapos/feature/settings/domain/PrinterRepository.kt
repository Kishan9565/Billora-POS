package com.kishan.billorapos.feature.settings.domain

import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.DataError
import com.kishan.billorapos.core.printer.BluetoothDeviceInfo
import kotlinx.flow.Flow

interface PrinterRepository {
    val savedPrinterMac: Flow<String?>
    val savedPrinterName: Flow<String?>
    val isConnected: Boolean
    fun getBondedDevices(): List<BluetoothDeviceInfo>
    fun connect(macAddress: String): Boolean
    fun disconnect(): Boolean
    suspend fun savePrinter(mac: String, name: String)
    suspend fun clearPrinter()
    fun printReceipt(
        shopName: String,
        address1: String,
        address2: String,
        phone: String,
        items: List<Triple<String, Double, Int>>,
        total: Double,
        footer: String,
        timestamp: String
    ): Boolean
    fun testPrint(shopName: String): Boolean
}
