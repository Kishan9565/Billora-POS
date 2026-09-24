package com.kishan.billorapos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StartupState(val complete: Boolean? = null, val error: Boolean = false)

class StartupViewModel(private val repository: ShopRepository) : ViewModel() {
    private val _state = MutableStateFlow(StartupState())
    val state = _state.asStateFlow()
    init { load() }

    fun load() {
        _state.value = StartupState()
        viewModelScope.launch {
            try {
                _state.value = StartupState(complete = repository.isSetupComplete())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = StartupState(error = true)
            }
        }
    }
}
