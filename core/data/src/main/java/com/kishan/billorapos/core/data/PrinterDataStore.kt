package com.kishan.billorapos.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "printer_settings")

class PrinterDataStore(private val context: Context) {
    companion object {
        private val PRINTER_MAC = stringPreferencesKey("printer_mac")
        private val PRINTER_NAME = stringPreferencesKey("printer_name")
    }

    val printerMac: Flow<String?> = context.dataStore.data.map { it[PRINTER_MAC] }
    val printerName: Flow<String?> = context.dataStore.data.map { it[PRINTER_NAME] }

    suspend fun savePrinter(mac: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[PRINTER_MAC] = mac
            prefs[PRINTER_NAME] = name
        }
    }

    suspend fun clearPrinter() {
        context.dataStore.edit { prefs ->
            prefs.remove(PRINTER_MAC)
            prefs.remove(PRINTER_NAME)
        }
    }
}
