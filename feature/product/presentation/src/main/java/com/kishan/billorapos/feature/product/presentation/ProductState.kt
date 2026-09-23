package com.kishan.billorapos.feature.product.presentation

import com.kishan.billorapos.core.domain.Product

data class ProductState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = ""
)

sealed interface ProductAction {
    data class OnSearchQueryChange(val query: String) : ProductAction
    data class OnDeleteProductClick(val id: String) : ProductAction
    data class OnAddProduct(val name: String, val barcode: String, val price: Double) : ProductAction
    data class OnUpdateProduct(val product: Product) : ProductAction
}

sealed interface ProductEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : ProductEvent
    object ProductActionSuccess : ProductEvent
}
