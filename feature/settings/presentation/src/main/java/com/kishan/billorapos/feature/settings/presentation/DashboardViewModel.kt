package com.kishan.billorapos.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.core.domain.*
import com.kishan.billorapos.feature.product.domain.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardState(
    val shopName: String = "",
    val todayRevenue: Double = 0.0,
    val todayTransactionCount: Int = 0,
    val lowStockCount: Int = 0,
    val khataOutstandingTotal: Double = 0.0,
    val printerConnected: Boolean = false
)

class DashboardViewModel(
    private val salesRepository: SalesRepository,
    private val customerRepository: CustomerRepository,
    private val preferences: PosPreferences,
    private val productRepository: ProductRepository,
    private val printerHelper: com.kishan.billorapos.core.printer.PrinterHelper
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + 24 * 60 * 60 * 1000L - 1

        viewModelScope.launch {
            salesRepository.getSalesBetween(startOfToday, endOfToday).collect { salesList ->
                val summary = summarizeSales(salesList)
                _state.update {
                    it.copy(
                        todayRevenue = summary.revenue,
                        todayTransactionCount = summary.count
                    )
                }
            }
        }

        viewModelScope.launch {
            customerRepository.balances().collect { customers ->
                val outstanding = customers.sumOf { it.balance }
                _state.update { it.copy(khataOutstandingTotal = outstanding) }
            }
        }

        viewModelScope.launch {
            combine(preferences.lowStockThreshold, productRepository.getProducts()) { threshold, prods ->
                prods.count { it.stock <= threshold }
            }.collect { count ->
                _state.update { it.copy(lowStockCount = count) }
            }
        }

        viewModelScope.launch {
            printerHelper.connectionState.collect { connected ->
                _state.update { it.copy(printerConnected = connected) }
            }
        }
        
        // Also fetch shop name if available via a preferences query or default flow
        // For simplicity, we can load it from the bonded printers flow or let it be wired up
    }
}
