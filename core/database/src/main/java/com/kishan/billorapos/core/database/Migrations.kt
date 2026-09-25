package com.kishan.billorapos.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sales (id TEXT NOT NULL PRIMARY KEY, timestamp INTEGER NOT NULL, subtotal REAL NOT NULL, discountAmount REAL NOT NULL, totalAmount REAL NOT NULL, itemCount INTEGER NOT NULL, paymentMethod TEXT NOT NULL, customerId TEXT, amountPaid REAL NOT NULL, isSettled INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sale_lines (id TEXT NOT NULL PRIMARY KEY, saleId TEXT NOT NULL, productId TEXT NOT NULL, productName TEXT NOT NULL, quantity INTEGER NOT NULL, unitPrice REAL NOT NULL, FOREIGN KEY(saleId) REFERENCES sales(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_lines_saleId ON sale_lines(saleId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS customers (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, phoneNumber TEXT)")
        db.execSQL("CREATE TABLE shop_profiles_new (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, addressLine1 TEXT NOT NULL, addressLine2 TEXT NOT NULL, phoneNumber TEXT NOT NULL, upiId TEXT NOT NULL, footerText TEXT NOT NULL, isActive INTEGER NOT NULL)")
        db.execSQL("INSERT INTO shop_profiles_new SELECT ?, name, addressLine1, addressLine2, phoneNumber, upiId, footerText, 1 FROM shop_details WHERE id = 0", arrayOf(UUID.randomUUID().toString()))
        db.execSQL("DROP TABLE shop_details")
        db.execSQL("ALTER TABLE shop_profiles_new RENAME TO shop_details")
    }
}
