package com.kishan.billorapos.feature.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopViewModel(
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    private val _eventChannel = Channel<ShopEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        onAction(ShopAction.LoadShop)
    }

    fun onAction(action: ShopAction) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        when (action) {
            ShopAction.SkipSetup -> {
                viewModelScope.launch {
                    when (val result = shopRepository.completeSetup(Shop.EMPTY)) {
                        is Result.Success -> _eventChannel.send(ShopEvent.SaveSuccess)
                        is Result.Error -> _eventChannel.send(ShopEvent.ShowSnackbar(result.message ?: "Unable to skip setup", true))
                    }
                    _state.update { it.copy(isLoading = false) }
                }
            }
            is ShopAction.LoadShop -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true) }
                    when (val result = shopRepository.getShop()) {
                        is Result.Success -> {
                            _state.update { it.copy(shop = result.data, isLoading = false) }
                        }
                        is Result.Error -> {
                            _state.update { it.copy(errorMessage = result.message, isLoading = false) }
                        }
                    }
                }
            }
            is ShopAction.SaveShop -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true) }
                    if (action.name.isBlank() || action.addressLine1.isBlank() || action.phoneNumber.isBlank()) {
                        _state.update { it.copy(isLoading = false) }
                        _eventChannel.send(ShopEvent.ShowSnackbar("Enter a shop name, address and phone number", isError = true))
                        return@launch
                    }
                    val updatedShop = Shop(
                        name = action.name,
                        addressLine1 = action.addressLine1,
                        addressLine2 = action.addressLine2,
                        phoneNumber = action.phoneNumber,
                        upiId = action.upiId,
                        footerText = action.footerText
                    )
                    when (val result = if (action.completeSetup) shopRepository.completeSetup(updatedShop) else shopRepository.updateShop(updatedShop)) {
                        is Result.Success -> {
                            _state.update { it.copy(shop = updatedShop) }
                            _eventChannel.send(ShopEvent.SaveSuccess)
                        }
                        is Result.Error -> {
                            _eventChannel.send(ShopEvent.ShowSnackbar(result.message ?: "Failed to save details", isError = true))
                        }
                    }
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
