package com.kishan.billorapos

import com.kishan.billorapos.core.domain.*
import com.kishan.billorapos.core.database.*
import com.kishan.billorapos.feature.billing.presentation.*
import com.kishan.billorapos.feature.product.domain.ProductRepository
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

internal class MemoryPreferences : PosPreferences {
    override val lowStockThreshold = MutableStateFlow(5)
    override suspend fun setLowStockThreshold(value: Int) { lowStockThreshold.value = value }
}
internal class MemoryCustomers : CustomerRepository {
    private val rows = MutableStateFlow<List<Customer>>(emptyList())
    override fun search(query: String) = rows
    override fun balances() = rows
    override suspend fun save(customer: Customer) { rows.value += customer }
}
internal class MemorySales : SalesRepository {
    val rows = mutableMapOf<String, Sale>()
    var calls = 0
    override suspend fun recordSale(sale: Sale, lines: List<SaleLine>) { calls++; rows.putIfAbsent(sale.id, sale) }
    override fun getSalesBetween(start: Long, end: Long) = flowOf(rows.values.toList())
    override fun getTopSelling(start: Long, end: Long) = flowOf(emptyList<TopProduct>())
    override suspend fun getSale(id: String) = rows[id]
    override suspend fun getLines(id: String) = emptyList<SaleLine>()
    override fun getCreditSales(customerId: String) = flowOf(rows.values.filter { it.customerId == customerId })
    override suspend fun recordPayment(customerId: String, amount: Double) { }
}

class FeaturePassTest {
    @Test fun flatPercentageAndClampedDiscountsUseOneTotal() {
        assertEquals(CheckoutTotals(100.0, 25.0, 75.0), checkoutTotals(100.0, 25.0))
        assertEquals(CheckoutTotals(250.0, 25.0, 225.0), checkoutTotals(250.0, 10.0, true))
        assertEquals(0.0, checkoutTotals(10.0, 200.0).totalAmount, 0.0)
        assertEquals(0.0, checkoutTotals(10.0, 200.0, true).totalAmount, 0.0)
        assertEquals(8.99, checkoutTotals(9.99, 10.0, true).totalAmount, 0.0)
        try { checkoutTotals(10.0, Double.NaN); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun reportsUsePersistedNetAmountsAndPaymentMethods() {
        val sales = listOf(sale("a", 100.0, "CASH"), sale("b", 50.0, "UPI"), sale("c", 150.0, "CREDIT"))
        val summary = summarizeSales(sales)
        assertEquals(300.0, summary.revenue, 0.0)
        assertEquals(100.0, summary.average, 0.0)
        assertEquals(150.0, summary.byPayment["CREDIT"]!!, 0.0)
        assertEquals(0.0, summarizeSales(emptyList()).average, 0.0)
    }
    @Test fun saleInsertionIsIdempotentAndStockCannotGoNegative() = runTest {
        val dao = LedgerDao()
        dao.stock["p"] = 2
        val entity = SaleEntity("s", 1, 30.0, 0.0, 30.0, 3, "CASH")
        val lines = listOf(SaleLineEntity("l", "s", "p", "Original name", 3, 10.0))
        dao.recordSaleWithStockUpdate(entity, lines)
        assertEquals(0, dao.stock["p"])
        dao.stock["p"] = 8
        dao.recordSaleWithStockUpdate(entity, lines)
        assertEquals(8, dao.stock["p"])
        assertEquals("Original name", dao.getLinesForSale("s").single().productName)
    }
    @Test fun partialPaymentAllocatesOldestFirstThenFullySettles() = runTest {
        val dao = LedgerDao()
        dao.insertSale(SaleEntity("b", 2, 200.0, 0.0, 200.0, 1, "CREDIT", "c", 0.0, false))
        dao.insertSale(SaleEntity("a", 1, 100.0, 0.0, 100.0, 1, "CREDIT", "c", 0.0, false))
        dao.recordKhataPayment("c", 150.0)
        assertTrue(dao.rows.getValue("a").isSettled)
        assertEquals(50.0, dao.rows.getValue("b").amountPaid, 0.0)
        assertFalse(dao.rows.getValue("b").isSettled)
        dao.recordKhataPayment("c", 150.0)
        assertTrue(dao.rows.values.all { it.isSettled })
        try { dao.recordKhataPayment("c", 1.0); fail() } catch (_: IllegalArgumentException) { }
    }
    private fun sale(id: String, total: Double, method: String) = Sale(id, 1, total + 10, 10.0, total, 1, method)
}

private class LedgerDao : SaleDao {
    val rows = linkedMapOf<String, SaleEntity>()
    val stock = mutableMapOf<String, Int>()
    private val lines = mutableListOf<SaleLineEntity>()
    override suspend fun insertSale(sale: SaleEntity): Long {
        if (rows.containsKey(sale.id)) return -1
        rows[sale.id] = sale
        return rows.size.toLong()
    }
    override suspend fun insertSaleLines(lines: List<SaleLineEntity>) { this.lines += lines }
    override suspend fun decrementStock(productId: String, quantity: Int) { stock[productId] = ((stock[productId] ?: 0) - quantity).coerceAtLeast(0) }
    override fun getSalesBetween(startTime: Long, endTime: Long) = flowOf(rows.values.toList())
    override suspend fun getLinesForSale(saleId: String) = lines.filter { it.saleId == saleId }
    override fun getTopSellingProducts(startTime: Long, endTime: Long) = flowOf(emptyList<TopSellingProduct>())
    override fun getCreditSalesForCustomer(customerId: String) = flowOf(rows.values.filter { it.customerId == customerId })
    override suspend fun updateSale(sale: SaleEntity) { rows[sale.id] = sale }
    override suspend fun getUnsettledCreditSalesForCustomer(customerId: String) = rows.values.filter { it.customerId == customerId && !it.isSettled }.sortedBy { it.timestamp }
    override suspend fun getSale(id: String) = rows[id]
}
