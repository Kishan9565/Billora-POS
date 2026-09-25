package com.kishan.billorapos.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers")
    fun getAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("""
        SELECT c.*, SUM(s.totalAmount - s.amountPaid) as balance
        FROM customers c
        LEFT JOIN sales s ON c.id = s.customerId AND s.paymentMethod = 'CREDIT' AND s.isSettled = 0
        GROUP BY c.id
        ORDER BY balance DESC
    """)
    fun getCustomersWithBalances(): Flow<List<CustomerWithBalance>>
}

data class CustomerWithBalance(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val balance: Double?
)
