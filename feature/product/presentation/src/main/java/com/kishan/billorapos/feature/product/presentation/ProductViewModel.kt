package com.kishan.billorapos.feature.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.core.domain.Result
import com.kishan.billorapos.feature.product.domain.ProductRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _eventChannel = Channel<ProductEvent>()
    val events = _eventChannel.receiveAsFlow()

    val state: StateFlow<ProductState> = combine(
        productRepository.getProducts(),
        _searchQuery,
        _isLoading,
        _errorMessage
    ) { products, query, loading, error ->
        val filtered = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.barcode.contains(query, ignoreCase = true)
            }
        }
        ProductState(
            products = filtered,
            isLoading = loading,
            errorMessage = error,
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductState())

    fun onAction(action: ProductAction) {
        when (action) {
            is ProductAction.OnSearchQueryChange -> {
                _searchQuery.value = action.query
            }
            is ProductAction.OnDeleteProductClick -> {
                viewModelScope.launch {
                    _isLoading.value = true
                    when (val result = productRepository.deleteProduct(action.id)) {
                        is Result.Success -> {
                            _eventChannel.send(ProductEvent.ShowSnackbar("Product deleted successfully"))
                        }
                        is Result.Error -> {
                            _eventChannel.send(ProductEvent.ShowSnackbar(result.message ?: "Deletion failed", isError = true))
                        }
                    }
                    _isLoading.value = false
                }
            }
            is ProductAction.OnAddProduct -> {
                viewModelScope.launch {
                    _isLoading.value = true
                    val currentList = state.value.products
                    if (currentList.any { it.barcode == action.barcode }) {
                        _eventChannel.send(ProductEvent.ShowSnackbar("Product with barcode \"${action.barcode}\" already exists!", isError = true))
                        _isLoading.value = false
                        return@launch
                    }

                    val newProduct = Product(
                        id = UUID.randomUUID().toString(),
                        name = action.name,
                        barcode = action.barcode,
                        price = action.price,
                        stock = 0
                    )
                    when (val result = productRepository.addProduct(newProduct)) {
                        is Result.Success -> {
                            _eventChannel.send(ProductEvent.ShowSnackbar("Product added successfully"))
                            _eventChannel.send(ProductEvent.ProductActionSuccess)
                        }
                        is Result.Error -> {
                            _eventChannel.send(ProductEvent.ShowSnackbar(result.message ?: "Failed to add product", isError = true))
                        }
                    }
                    _isLoading.value = false
                }
            }
            is ProductAction.OnUpdateProduct -> {
                viewModelScope.launch {
                    _isLoading.value = true
                    // TODO(verify): The source spec mentions that editing a product constructs Product without stock argument, which defaults to 0, silently resetting stock to 0.
                    val updated = action.product.copy(stock = 0)
                    when (val result = productRepository.updateProduct(updated)) {
                        is Result.Success -> {
                            _eventChannel.send(ProductEvent.ShowSnackbar("Product updated successfully"))
                            _eventChannel.send(ProductEvent.ProductActionSuccess)
                        }
                        is Result.Error -> {
                            _eventChannel.send(ProductEvent.ShowSnackbar(result.message ?: "Failed to update product", isError = true))
                        }
                    }
                    _isLoading.value = false
                }
            }
        }
    }
}
