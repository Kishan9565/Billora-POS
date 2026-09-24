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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow

class PrinterViewModel(
    private val printerRepository: PrinterRepository,
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _isScanningOrConnecting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _eventChannel = Channel<PrinterEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private val _shopNameFlow = MutableStateFlow("")
    val shopNameFlow: StateFlow<String> = _shopNameFlow.asStateFlow()

    val state: StateFlow<PrinterState> = combine(
        printerRepository.savedPrinterMac.catch { _errorMessage.value = "Unable to load saved printer"; emit(null) },
        printerRepository.savedPrinterName.catch { _errorMessage.value = "Unable to load saved printer"; emit(null) },
        _isScanningOrConnecting,
        _errorMessage,
        printerRepository.connectionState
    ) { mac, name, scanning, error, connected ->
        PrinterState(
            savedPrinterMac = mac,
            savedPrinterName = name,
            isConnected = connected,
            isScanningOrConnecting = scanning,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrinterState())

    init {
        onAction(PrinterAction.InitPrinter)
    }

    private fun loadShopDetails() {
        viewModelScope.launch {
            when (val res = shopRepository.getShop()) {
                is Result.Success -> {
                    _shopNameFlow.value = res.data.name
                }
                is Result.Error -> { _errorMessage.value = "Unable to load shop details" }
            }
        }
    }

    fun onAction(action: PrinterAction) {
        when (action) {
            is PrinterAction.InitPrinter -> {
                loadShopDetails()
            }
            is PrinterAction.RefreshPrinters -> {
                if (_isScanningOrConnecting.value) return
                _isScanningOrConnecting.value = true
                viewModelScope.launch {
                    try {
                        _errorMessage.value = null

                        val pairedDevices = printerRepository.getBondedDevices()
                        if (pairedDevices.isEmpty()) {
                            _errorMessage.value = "No paired devices found."
                            _eventChannel.send(PrinterEvent.ShowSnackbar("No paired devices found.", isError = true))
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
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _errorMessage.value = e.message ?: "Printer connection failed"
                        _eventChannel.send(PrinterEvent.ShowSnackbar(_errorMessage.value!!, isError = true))
                    } finally {
                        _isScanningOrConnecting.value = false
                    }
                }
            }
        }
    }
}
