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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import com.kishan.billorapos.feature.product.domain.ProductCsv
import com.kishan.billorapos.feature.product.domain.validateProduct
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProductViewModel(
    private val productRepository: ProductRepository,
    preferences: com.kishan.billorapos.core.domain.PosPreferences
) : ViewModel() {
    val lowStockThreshold = preferences.lowStockThreshold.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _isReading = MutableStateFlow(true)
    private val reload = MutableStateFlow(0)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _eventChannel = Channel<ProductEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    val state: StateFlow<ProductState> = combine(
        reload.flatMapLatest { productRepository.getProducts()
            .onStart { _errorMessage.value = null; _isReading.value = true }
            .onEach { _isReading.value = false }
            .catch { error ->
                _errorMessage.value = error.message ?: "Unable to load products"
                _isReading.value = false
                emit(emptyList())
            } },
        _searchQuery,
        _isLoading,
        _errorMessage,
        _isReading
    ) { products, query, loading, error, reading ->
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
            isLoading = loading || reading,
            errorMessage = error,
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductState(isLoading = true))

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        ProductCsv.export(productRepository.getProducts().first())
    }

    fun onAction(action: ProductAction) {
        val isMutation = action !is ProductAction.OnSearchQueryChange && action !is ProductAction.RetryLoad
        if (isMutation && _isLoading.value) return
        if (isMutation) _isLoading.value = true
        when (action) {
            is ProductAction.ImportCsv -> {
                viewModelScope.launch {
                    try {
                        val parsed = withContext(Dispatchers.Default) { ProductCsv.parse(action.content) }
                        when (val result = productRepository.importProducts(parsed.products)) {
                            is Result.Success -> _eventChannel.send(ProductEvent.ShowSnackbar("${result.data.added} added, ${result.data.updated} updated, ${parsed.skipped} skipped"))
                            is Result.Error -> _eventChannel.send(ProductEvent.ShowSnackbar(result.message ?: "Import failed", true))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _eventChannel.send(ProductEvent.ShowSnackbar(e.message ?: "Unable to read CSV", true))
                    } finally { _isLoading.value = false }
                }
            }
            ProductAction.RetryLoad -> reload.value++
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
                    if (action.stock < 0 || !validateProduct(action.name, action.barcode, action.price.toString()).isValid) {
                        _eventChannel.send(ProductEvent.ShowSnackbar("Enter a barcode, name and valid non-negative price", isError = true))
                        _isLoading.value = false
                        return@launch
                    }

                    val newProduct = Product(
                        id = UUID.randomUUID().toString(),
                        name = action.name,
                        barcode = action.barcode,
                        price = action.price,
                        stock = action.stock
                    )
                    when (val result = productRepository.addProduct(newProduct)) {
                        is Result.Success -> {
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
                    if (action.product.stock < 0 || action.product.name.isBlank() || !action.product.price.isFinite() || action.product.price < 0) {
                        _eventChannel.send(ProductEvent.ShowSnackbar("Enter a name and valid non-negative price", isError = true))
                        _isLoading.value = false
                        return@launch
                    }
                    val updated = action.product
                    when (val result = productRepository.updateProduct(updated)) {
                        is Result.Success -> {
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
