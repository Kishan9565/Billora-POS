package com.kishan.billorapos.feature.product.domain

import com.kishan.billorapos.core.domain.Product
import java.util.UUID

data class CsvProducts(val products: List<Product>, val skipped: Int)
data class ImportCounts(val added: Int, val updated: Int)

object ProductCsv {
    fun export(products: List<Product>): String = buildString {
        append("name,barcode,price,stock\r\n")
        products.forEach { product ->
            append(listOf(product.name, product.barcode, product.price.toString(), product.stock.toString())
                .joinToString(",") { field ->
                    if (field.any { it == ',' || it == '"' || it == '\r' || it == '\n' })
                        "\"${field.replace("\"", "\"\"")}\"" else field
                })
            append("\r\n")
        }
    }

    fun parse(text: String): CsvProducts {
        val rows = records(text.removePrefix("\uFEFF"))
        require(rows.isNotEmpty()) { "CSV is empty" }
        val header = rows.first()
        val names = header.fields.map { it.trim().lowercase(java.util.Locale.ROOT) }
        require(header.valid && names.distinct().size == names.size && names.containsAll(listOf("name", "barcode", "price"))) {
            "CSV header must contain name,barcode,price (stock is optional)"
        }
        val products = mutableListOf<Product>()
        var skipped = 0
        rows.drop(1).forEach { row ->
            fun field(name: String) = row.fields.getOrNull(names.indexOf(name)).orEmpty()
            val name = field("name")
            val barcode = field("barcode")
            val price = field("price").trim()
            val stockText = field("stock").trim()
            val stock = if (stockText.isEmpty()) 0 else stockText.toIntOrNull()
            if (!row.valid || row.fields.size > names.size || stock == null ||
                !validateProduct(name, barcode, price, stock).isValid) {
                skipped++
            } else {
                products += Product(UUID.randomUUID().toString(), name, barcode, price.toDouble(), stock)
            }
        }
        return CsvProducts(products, skipped)
    }

    private data class Record(val fields: List<String>, val valid: Boolean)

    /** Handles escaped quotes, embedded newlines, CRLF, and malformed individual records. */
    private fun records(text: String): List<Record> {
        val result = mutableListOf<Record>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        var valid = true
        var touched = false
        var i = 0
        fun endField() { fields += field.toString(); field.setLength(0); closed = false }
        fun endRecord() {
            endField()
            if (touched) result += Record(fields.toList(), valid && !quoted)
            fields.clear(); valid = true; touched = false
        }
        while (i < text.length) {
            val c = text[i++]
            if (quoted) {
                if (c == '"') {
                    if (i < text.length && text[i] == '"') { field.append('"'); i++ }
                    else { quoted = false; closed = true }
                } else field.append(c)
                continue
            }
            when (c) {
                ',' -> { touched = true; endField() }
                '\r', '\n' -> {
                    if (c == '\r' && i < text.length && text[i] == '\n') i++
                    endRecord()
                }
                '"' -> {
                    touched = true
                    if (field.isEmpty() && !closed) quoted = true
                    else { valid = false; field.append(c) }
                }
                else -> {
                    touched = true
                    if (closed) valid = false
                    field.append(c)
                }
            }
        }
        if (touched || fields.isNotEmpty() || field.isNotEmpty()) endRecord()
        return result
    }
}
