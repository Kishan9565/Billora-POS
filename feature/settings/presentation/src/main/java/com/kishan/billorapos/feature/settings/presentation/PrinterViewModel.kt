package com.kishan.billorapos.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.feature.settings.domain.PrinterRepository
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import com.kishan.billorapos.core.domain.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrinterViewModel(
    private val printerRepository: PrinterRepository,
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _isScanningOrConnecting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _eventChannel = Channel<PrinterEvent>()
    val events = _eventChannel.receiveAsFlow()

    private val _shopNameFlow = MutableStateFlow("Elite Groceries")
    val shopNameFlow: StateFlow<String> = _shopNameFlow

    val state: StateFlow<PrinterState> = combine(
        printerRepository.savedPrinterMac,
        printerRepository.savedPrinterName,
        _isScanningOrConnecting,
        _errorMessage
    ) { mac, name, scanning, error ->
        PrinterState(
            savedPrinterMac = mac,
            savedPrinterName = name,
            isConnected = printerRepository.isConnected,
            isScanningOrConnecting = scanning,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrinterState())

    init {
        onAction(PrinterAction.InitPrinter)
        loadShopDetails()
    }

    private fun loadShopDetails() {
        viewModelScope.launch {
            when (val res = shopRepository.getShop()) {
                is Result.Success -> {
                    if (res.data.name.isNotEmpty()) {
                        _shopNameFlow.value = res.data.name
                    }
                }
                is Result.Error -> {}
            }
        }
    }

    fun onAction(action: PrinterAction) {
        when (action) {
            is PrinterAction.InitPrinter -> {
                // Already combined via flows
            }
            is PrinterAction.RefreshPrinters -> {
                viewModelScope.launch {
                    _isScanningOrConnecting.value = true
                    _errorMessage.value = null
                    
                    val pairedDevices = printerRepository.getBondedDevices()
                    if (pairedDevices.isEmpty()) {
                        _errorMessage.value = "No paired devices found."
                        _eventChannel.send(PrinterEvent.ShowSnackbar("No paired devices found.", isError = true))
                        _isScanningOrConnecting.value = false
                        return@launch
                    }

                    var connectedMac: String? = null
                    var connectedName: String? = null
                    for (device in pairedDevices) {
                        if (printerRepository.connect(device.macAddress)) {
                            connectedMac = device.macAddress
                            connectedName = device.name
                            break
                        }
                    }

                    if (connectedMac != null && connectedName != null) {
                        printerRepository.savePrinter(connectedMac, connectedName)
                        _eventChannel.send(PrinterEvent.ShowSnackbar("Connected to printer"))
                    } else {
                        _errorMessage.value = "Could not connect to any paired device."
                        _eventChannel.send(PrinterEvent.ShowSnackbar("Could not connect to any paired device.", isError = true))
                    }
                    _isScanningOrConnecting.value = false
                }
            }
        }
    }
}
