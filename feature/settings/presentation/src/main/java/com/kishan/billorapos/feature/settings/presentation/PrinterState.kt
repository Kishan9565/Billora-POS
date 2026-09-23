package com.kishan.billorapos.feature.settings.presentation

data class PrinterState(
    val savedPrinterMac: String? = null,
    val savedPrinterName: String? = null,
    val isConnected: Boolean = false,
    val isScanningOrConnecting: Boolean = false,
    val errorMessage: String? = null,
    val statusText: String = ""
)

sealed interface PrinterAction {
    object InitPrinter : PrinterAction
    object RefreshPrinters : PrinterAction
}

sealed interface PrinterEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : PrinterEvent
}
