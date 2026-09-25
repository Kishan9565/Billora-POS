package com.kishan.billorapos.core.domain

import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

data class Sale(val id: String, val timestamp: Long, val subtotal: Double, val discountAmount: Double,
    val totalAmount: Double, val itemCount: Int, val paymentMethod: String, val customerId: String? = null,
    val amountPaid: Double = 0.0, val isSettled: Boolean = true)
data class SaleLine(val id: String, val saleId: String, val productId: String, val productName: String, val quantity: Int, val unitPrice: Double)
data class TopProduct(val productId: String, val productName: String, val quantity: Int, val revenue: Double)
data class Customer(val id: String, val name: String, val phoneNumber: String? = null, val balance: Double = 0.0)

interface SalesRepository {
    suspend fun recordSale(sale: Sale, lines: List<SaleLine>)
    fun getSalesBetween(start: Long, end: Long): Flow<List<Sale>>
    fun getTopSelling(start: Long, end: Long): Flow<List<TopProduct>>
    suspend fun getSale(id: String): Sale?
    suspend fun getLines(id: String): List<SaleLine>
    fun getCreditSales(customerId: String): Flow<List<Sale>>
    suspend fun recordPayment(customerId: String, amount: Double)
}
interface CustomerRepository {
    fun search(query: String): Flow<List<Customer>>
    fun balances(): Flow<List<Customer>>
    suspend fun save(customer: Customer)
}
interface PosPreferences {
    val lowStockThreshold: Flow<Int>
    suspend fun setLowStockThreshold(value: Int)
}
data class CheckoutTotals(val subtotal: Double, val discountAmount: Double, val totalAmount: Double)
fun checkoutTotals(subtotal: Double, discount: Double = 0.0, percentage: Boolean = false): CheckoutTotals {
    require(subtotal.isFinite() && subtotal >= 0 && discount.isFinite() && discount >= 0)
    val base = BigDecimal.valueOf(subtotal).setScale(2, RoundingMode.HALF_UP)
    val raw = if (percentage) base.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100)) else BigDecimal.valueOf(discount)
    val applied = raw.setScale(2, RoundingMode.HALF_UP).coerceIn(BigDecimal.ZERO, base)
    return CheckoutTotals(base.toDouble(), applied.toDouble(), base.subtract(applied).toDouble())
}
data class SalesSummary(val revenue: Double, val count: Int, val average: Double, val byPayment: Map<String, Double>)
fun summarizeSales(sales: List<Sale>): SalesSummary {
    fun sum(rows: List<Sale>) = rows.fold(BigDecimal.ZERO) { a, s -> a + BigDecimal.valueOf(s.totalAmount) }.toDouble()
    val revenue = sum(sales)
    return SalesSummary(revenue, sales.size, if (sales.isEmpty()) 0.0 else revenue / sales.size,
        listOf("CASH", "UPI", "CREDIT").associateWith { method -> sum(sales.filter { it.paymentMethod == method }) })
}
