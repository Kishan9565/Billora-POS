package com.kishan.billorapos.core.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class PrinterHelper(private val context: Context) {
    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val _connectionState = MutableStateFlow(false)
    val connectionState = _connectionState.asStateFlow()
    private var connectedMac: String? = null

    private fun hasPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    val isConnected: Boolean
        get() = _connectionState.value

    fun getBondedDevices(): List<BluetoothDeviceInfo> {
        check(hasPermission()) { "Allow Nearby devices permission to use the printer." }
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) { disconnect(); return emptyList() }
        return try {
            adapter.bondedDevices.map { device ->
                BluetoothDeviceInfo(device.name ?: "Unknown Device", device.address)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun connect(macAddress: String): Boolean {
        if (!hasPermission()) { disconnect(); return false }
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) { disconnect(); return false }
        if (isConnected && connectedMac == macAddress) return true
        disconnect()
        return try {
            val device = adapter.getRemoteDevice(macAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            connectedMac = macAddress
            _connectionState.value = true
            true
        } catch (e: Exception) {
            disconnect()
            false
        }
    }

    @Synchronized
    fun disconnect(): Boolean {
        _connectionState.value = false
        connectedMac = null
        return try {
            socket?.close()
            socket = null
            true
        } catch (e: Exception) {
            socket = null
            false
        }
    }

    @Synchronized
    fun printReceipt(
        shopName: String,
        address1: String,
        address2: String,
        phone: String,
        items: List<Triple<String, Double, Int>>, // Name, Price, Quantity
        total: Double,
        footer: String,
        timestamp: String,
        discountAmount: Double = 0.0
    ): Boolean {
        if (!hasPermission()) { disconnect(); return false }
        val outSocket = socket ?: return false
        if (!outSocket.isConnected) { disconnect(); return false }

        return try {
            val outputStream = outSocket.outputStream
            val buffer = mutableListOf<Byte>()

            fun writeBytes(bytes: ByteArray) {
                buffer.addAll(bytes.toList())
            }

            fun writeTextLine(text: String) {
                val latin1Bytes = text.map { it.code.toByte() }.toByteArray()
                buffer.addAll(latin1Bytes.toList())
                buffer.addAll(EscPos.LINE_FEED.toList())
            }

            writeBytes(EscPos.INIT)
            writeBytes(EscPos.ALIGN_CENTER)
            writeBytes(EscPos.BOLD_ON)
            writeBytes(EscPos.TEXT_LARGE)
            writeTextLine(shopName)

            writeBytes(EscPos.TEXT_NORMAL)
            writeBytes(EscPos.BOLD_OFF)
            if (address1.isNotEmpty()) writeTextLine(address1)
            if (address2.isNotEmpty()) writeTextLine(address2)
            if (phone.isNotEmpty()) writeTextLine(phone)
            writeTextLine(timestamp)

            writeTextLine("--------------------------------")
            writeBytes(EscPos.ALIGN_LEFT)
            writeTextLine("Item            Price   Total")
            writeTextLine("--------------------------------")

            for (item in items) {
                val name = item.first
                val price = item.second
                val qty = item.third
                val lineTotal = com.kishan.billorapos.core.domain.lineAmount(price, qty).toDouble()

                val qtyName = "${qty}x $name"
                val truncatedName = if (qtyName.length > 16) qtyName.substring(0, 16) else qtyName
                val p1 = truncatedName.padEnd(16)
                val p2 = price.toString().padEnd(8)
                val p3 = lineTotal.toString()

                writeTextLine(p1 + p2 + p3)
            }

            writeTextLine("--------------------------------")
            writeBytes(EscPos.ALIGN_RIGHT)
            writeBytes(EscPos.BOLD_ON)
            if (discountAmount > 0) {
                writeTextLine("Subtotal: ${com.kishan.billorapos.core.domain.checkoutTotals(com.kishan.billorapos.core.domain.totalAmount(items.map { it.second to it.third })).subtotal}")
                writeTextLine("Discount: -$discountAmount")
            }
            writeTextLine("TOTAL: $total")
            writeBytes(EscPos.BOLD_OFF)
            writeBytes(EscPos.LINE_FEED)

            writeBytes(EscPos.ALIGN_CENTER)
            writeTextLine(footer)
            writeBytes(EscPos.LINE_FEED)
            writeBytes(EscPos.LINE_FEED)
            writeBytes(EscPos.LINE_FEED)

            outputStream.write(buffer.toByteArray())
            outputStream.flush()
            true
        } catch (e: Exception) {
            disconnect()
            false
        }
    }

    @Synchronized
    fun testPrint(shopName: String): Boolean {
        if (!hasPermission()) { disconnect(); return false }
        val outSocket = socket ?: return false
        if (!outSocket.isConnected) { disconnect(); return false }
        return try {
            val text = "Test Print\n\n$shopName\n\n----------------\n\n"
            val bytes = text.map { it.code.toByte() }.toByteArray()
            outSocket.outputStream.write(bytes)
            outSocket.outputStream.flush()
            true
        } catch (e: Exception) {
            disconnect()
            false
        }
    }
}
