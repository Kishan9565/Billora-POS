package com.kishan.billorapos.core.data

import com.kishan.billorapos.core.database.*
import com.kishan.billorapos.core.domain.*
import kotlinx.coroutines.flow.map

class SalesRepositoryImpl(private val dao: SaleDao) : SalesRepository {
    override suspend fun recordSale(sale: Sale, lines: List<SaleLine>) {
        require(sale.paymentMethod in listOf("CASH", "UPI", "CREDIT"))
        require(sale.paymentMethod != "CREDIT" || !sale.customerId.isNullOrBlank())
        require(sale.totalAmount.isFinite() && sale.totalAmount >= 0)
        dao.recordSaleWithStockUpdate(sale.entity(), lines.map { SaleLineEntity(it.id, it.saleId, it.productId, it.productName, it.quantity, it.unitPrice) })
    }
    override fun getSalesBetween(start: Long, end: Long) = dao.getSalesBetween(start, end).map { rows -> rows.map { it.domain() } }
    override fun getTopSelling(start: Long, end: Long) = dao.getTopSellingProducts(start, end).map { rows -> rows.map { TopProduct(it.productId, it.productName, it.totalQuantity, it.totalRevenue) } }
    override suspend fun getSale(id: String) = dao.getSale(id)?.domain()
    override suspend fun getLines(id: String) = dao.getLinesForSale(id).map { SaleLine(it.id, it.saleId, it.productId, it.productName, it.quantity, it.unitPrice) }
    override fun getCreditSales(customerId: String) = dao.getCreditSalesForCustomer(customerId).map { rows -> rows.map { it.domain() } }
    override suspend fun recordPayment(customerId: String, amount: Double) = dao.recordKhataPayment(customerId, amount)
    private fun Sale.entity() = SaleEntity(id, timestamp, subtotal, discountAmount, totalAmount, itemCount, paymentMethod, customerId, amountPaid, isSettled)
    private fun SaleEntity.domain() = Sale(id, timestamp, subtotal, discountAmount, totalAmount, itemCount, paymentMethod, customerId, amountPaid, isSettled)
}
class CustomerRepositoryImpl(private val dao: CustomerDao) : CustomerRepository {
    override fun search(query: String) = dao.search(query).map { rows -> rows.map { Customer(it.id, it.name, it.phoneNumber) } }
    override fun balances() = dao.getCustomersWithBalances().map { rows -> rows.map { Customer(it.id, it.name, it.phoneNumber, it.balance ?: 0.0) } }
    override suspend fun save(customer: Customer) {
        require(customer.name.isNotBlank()) { "Customer name is required" }
        dao.upsert(CustomerEntity(customer.id, customer.name.trim(), customer.phoneNumber?.trim()?.takeIf { it.isNotEmpty() }))
    }
}
