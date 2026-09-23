package com.kishan.billorapos.core.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class PrinterHelper {
    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val isConnected: Boolean
        get() = socket?.isConnected == true

    fun getBondedDevices(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices.map { device ->
                BluetoothDeviceInfo(device.name ?: "Unknown Device", device.address)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun connect(macAddress: String): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (isConnected) return true
        return try {
            val device = adapter.getRemoteDevice(macAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            true
        } catch (e: IOException) {
            disconnect()
            false
        }
    }

    fun disconnect(): Boolean {
        return try {
            socket?.close()
            socket = null
            true
        } catch (e: IOException) {
            socket = null
            false
        }
    }

    fun printReceipt(
        shopName: String,
        address1: String,
        address2: String,
        phone: String,
        items: List<Triple<String, Double, Int>>, // Name, Price, Quantity
        total: Double,
        footer: String,
        timestamp: String
    ): Boolean {
        val outSocket = socket ?: return false
        if (!outSocket.isConnected) return false

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
                val lineTotal = price * qty

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
            false
        }
    }

    fun testPrint(shopName: String): Boolean {
        val outSocket = socket ?: return false
        if (!outSocket.isConnected) return false
        return try {
            val text = "Test Print\n\n$shopName\n\n----------------\n\n"
            val bytes = text.map { it.code.toByte() }.toByteArray()
            outSocket.outputStream.write(bytes)
            outSocket.outputStream.flush()
            true
        } catch (e: Exception) {
            false
        }
    }
}
