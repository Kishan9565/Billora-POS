package com.kishan.billorapos.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.billorapos.core.domain.*
import com.kishan.billorapos.feature.product.domain.ProductRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class ManagementState(
    val range: String = "Today", val start: Long = 0, val end: Long = 0,
    val sales: List<Sale> = emptyList(), val top: List<TopProduct> = emptyList(),
    val customers: List<Customer> = emptyList(), val selectedCustomer: Customer? = null,
    val creditSales: List<Sale> = emptyList(), val creditLines: Map<String, List<SaleLine>> = emptyMap(),
    val detail: Sale? = null, val lines: List<SaleLine> = emptyList(),
    val loading: Boolean = true, val busy: Boolean = false, val error: String? = null,
    val threshold: Int = 5, val lowStockCount: Int = 0
)
sealed interface ManagementAction {
    data class Range(val label: String, val start: Long? = null, val end: Long? = null) : ManagementAction
    data class OpenSale(val id: String) : ManagementAction
    object CloseSale : ManagementAction
    data class OpenCustomer(val customer: Customer) : ManagementAction
    object CloseCustomer : ManagementAction
    data class Payment(val amount: String) : ManagementAction
    data class SaveCustomer(val customer: Customer) : ManagementAction
    data class Threshold(val value: String) : ManagementAction
}
sealed interface ManagementEvent { data class Message(val text: String) : ManagementEvent }

class ManagementViewModel(private val sales: SalesRepository, private val customers: CustomerRepository,
    private val preferences: PosPreferences, private val products: ProductRepository) : ViewModel() {
    private val mutable = MutableStateFlow(ManagementState())
    val state = mutable.asStateFlow()
    private val channel = Channel<ManagementEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    private var rangeJob: Job? = null
    private var customerJob: Job? = null
    init {
        onAction(ManagementAction.Range("Today"))
        viewModelScope.launch {
            try { customers.balances().collect { rows -> mutable.update { it.copy(customers = rows,
                selectedCustomer = it.selectedCustomer?.let { selected -> rows.find { c -> c.id == selected.id } }) } } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = "Unable to read customers") } }
        }
        viewModelScope.launch {
            try { combine(preferences.lowStockThreshold, products.getProducts()) { t, p -> t to p.count { it.stock <= t } }
                .collect { (t, count) -> mutable.update { it.copy(threshold = t, lowStockCount = count) } } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { channel.send(ManagementEvent.Message("Unable to read stock alerts")) }
        }
    }
    fun onAction(action: ManagementAction) {
        when (action) {
            is ManagementAction.Range -> {
                val (start, end) = reportRange(action.label, action.start, action.end)
                if (start > end) { channel.trySend(ManagementEvent.Message("End date must follow start date")); return }
                rangeJob?.cancel()
                mutable.update { it.copy(range = action.label, start = start, end = end, loading = true, error = null) }
                rangeJob = viewModelScope.launch {
                    try { combine(sales.getSalesBetween(start, end), sales.getTopSelling(start, end)) { s, t -> s to t }
                        .collect { (s, t) -> mutable.update { it.copy(sales = s, top = t, loading = false) } } }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { mutable.update { it.copy(loading = false, error = "Unable to load sales. Select a range to retry.") } }
                }
            }
            is ManagementAction.OpenSale -> work {
                val sale = sales.getSale(action.id)
                val lines = sales.getLines(action.id)
                mutable.update { it.copy(detail = sale, lines = lines) }
            }
            ManagementAction.CloseSale -> mutable.update { it.copy(detail = null, lines = emptyList()) }
            is ManagementAction.OpenCustomer -> {
                customerJob?.cancel()
                mutable.update { it.copy(selectedCustomer = action.customer, creditSales = emptyList(), creditLines = emptyMap()) }
                customerJob = viewModelScope.launch {
                    try { sales.getCreditSales(action.customer.id).collect { rows ->
                        val lines = rows.associate { it.id to sales.getLines(it.id) }
                        mutable.update { it.copy(creditSales = rows, creditLines = lines) }
                    } } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { channel.send(ManagementEvent.Message("Unable to load credit sales")) }
                }
            }
            ManagementAction.CloseCustomer -> { customerJob?.cancel(); mutable.update { it.copy(selectedCustomer = null) } }
            is ManagementAction.Payment -> {
                val customer = state.value.selectedCustomer ?: return
                work {
                    val amount = action.amount.toDoubleOrNull()
                    require(amount != null && amount.isFinite() && amount > 0) { "Enter a valid positive amount" }
                    sales.recordPayment(customer.id, checkoutTotals(amount).totalAmount)
                    channel.send(ManagementEvent.Message("Payment recorded"))
                }
            }
            is ManagementAction.SaveCustomer -> work { customers.save(action.customer) }
            is ManagementAction.Threshold -> work {
                val value = action.value.toIntOrNull()
                require(value != null && value >= 0) { "Enter a non-negative whole number" }
                preferences.setLowStockThreshold(value)
            }
        }
    }
    private fun work(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { channel.send(ManagementEvent.Message(e.message ?: "Unable to save. Please retry.")) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}

fun reportRange(label: String, customStart: Long? = null, customEnd: Long? = null): Pair<Long, Long> {
    fun day(time: Long) = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val now = System.currentTimeMillis()
    val start = day(customStart ?: now)
    when (label) {
        "This Week" -> { val delta = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7; start.add(Calendar.DAY_OF_MONTH, -delta) }
        "This Month" -> start.set(Calendar.DAY_OF_MONTH, 1)
    }
    val end = day(customEnd ?: now).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis - 1
    return start.timeInMillis to end
}
