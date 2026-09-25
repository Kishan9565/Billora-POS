package com.kishan.billorapos.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleLines(lines: List<SaleLineEntity>)

    @Query("UPDATE products SET stock = MAX(0, stock - :quantity) WHERE id = :productId")
    suspend fun decrementStock(productId: String, quantity: Int)

    @Transaction
    suspend fun recordSaleWithStockUpdate(sale: SaleEntity, lines: List<SaleLineEntity>) {
        require(lines.isNotEmpty() && lines.all { it.saleId == sale.id && it.quantity > 0 })
        if (insertSale(sale) == -1L) return
        insertSaleLines(lines)
        for (line in lines) {
            decrementStock(line.productId, line.quantity)
        }
    }

    @Query("SELECT * FROM sales WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId")
    suspend fun getLinesForSale(saleId: String): List<SaleLineEntity>

    @Query("""
        SELECT productId, productName, SUM(quantity) as totalQuantity, SUM(quantity * unitPrice) as totalRevenue 
        FROM sale_lines 
        INNER JOIN sales ON sale_lines.saleId = sales.id
        WHERE sales.timestamp BETWEEN :startTime AND :endTime
        GROUP BY productId, productName
        ORDER BY totalRevenue DESC
    """)
    fun getTopSellingProducts(startTime: Long, endTime: Long): Flow<List<TopSellingProduct>>

    // Khata Queries
    @Query("SELECT * FROM sales WHERE customerId = :customerId AND paymentMethod = 'CREDIT' ORDER BY timestamp ASC")
    fun getCreditSalesForCustomer(customerId: String): Flow<List<SaleEntity>>

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Transaction
    suspend fun recordKhataPayment(customerId: String, amount: Double) {
        require(amount.isFinite() && amount > 0) { "Enter a positive payment" }
        fun decimal(value: Double) = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        var remainingAmount = decimal(amount)
        require(remainingAmount > BigDecimal.ZERO) { "Payment must be at least 0.01" }
        // Get all unsettled credit sales for this customer, oldest first
        val unsettledSales = getUnsettledCreditSalesForCustomer(customerId)
        val balance = unsettledSales.fold(BigDecimal.ZERO) { sum, sale -> sum + decimal(sale.totalAmount) - decimal(sale.amountPaid) }
        require(remainingAmount <= balance) { "Payment exceeds outstanding balance" }
        for (sale in unsettledSales) {
            if (remainingAmount <= BigDecimal.ZERO) break
            val owed = decimal(sale.totalAmount) - decimal(sale.amountPaid)
            if (owed <= BigDecimal.ZERO) continue
            
            if (remainingAmount >= owed) {
                remainingAmount -= owed
                updateSale(sale.copy(amountPaid = sale.totalAmount, isSettled = true))
            } else {
                val newPaid = (decimal(sale.amountPaid) + remainingAmount).toDouble()
                remainingAmount = BigDecimal.ZERO
                updateSale(sale.copy(amountPaid = newPaid, isSettled = false))
            }
        }
    }

    @Query("SELECT * FROM sales WHERE customerId = :customerId AND paymentMethod = 'CREDIT' AND isSettled = 0 ORDER BY timestamp ASC, id ASC")
    suspend fun getUnsettledCreditSalesForCustomer(customerId: String): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSale(id: String): SaleEntity?
}
