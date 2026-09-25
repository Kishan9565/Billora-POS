package com.kishan.billorapos.core.printer

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.core.domain.lineAmount
import java.io.File
import java.util.Locale


object ReceiptPdf {
    fun create(
        context: Context,
        shop: Shop,
        items: List<Triple<String, Double, Int>>,
        total: Double,
        timestamp: String,
        discountAmount: Double = 0.0
    ): File {
        val directory = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File.createTempFile("receipt-", ".pdf", directory)
        val document = PdfDocument()
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10f
                typeface = Typeface.MONOSPACE
            }
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(226, 720, pageNumber).create())
            var y = 24f
            fun line(text: String, bold: Boolean = false) {
                paint.typeface = if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
                // Wrap to the actual printable width, including long names and addresses.
                for (paragraph in text.split('\n')) {
                    var remaining = paragraph
                    do {
                        if (y > 696) {
                            document.finishPage(page)
                            page = document.startPage(PdfDocument.PageInfo.Builder(226, 720, ++pageNumber).create())
                            y = 24f
                        }
                        val count = paint.breakText(remaining, true, 202f, null).coerceAtLeast(1).coerceAtMost(remaining.length)
                        page.canvas.drawText(remaining.take(count), 12f, y, paint)
                        y += 15f
                        remaining = remaining.drop(count)
                    } while (remaining.isNotEmpty())
                }
            }
            if (shop.name.isNotBlank()) line(shop.name, true)
            listOf(shop.addressLine1, shop.addressLine2, shop.phoneNumber).filter { it.isNotBlank() }.forEach { line(it) }
            line(timestamp)
            line("--------------------------------")
            items.forEach { (name, price, quantity) ->
                line("${quantity}x $name")
                line("INR ${String.format(Locale.ROOT, "%.2f", price)}  = ${lineAmount(price, quantity).toPlainString()}")
            }
            line("--------------------------------")
            if (discountAmount > 0) {
                line("Subtotal INR ${String.format(Locale.ROOT, "%.2f", com.kishan.billorapos.core.domain.checkoutTotals(com.kishan.billorapos.core.domain.totalAmount(items.map { it.second to it.third })).subtotal)}")
                line("Discount -INR ${String.format(Locale.ROOT, "%.2f", discountAmount)}")
            }
            line("TOTAL INR ${String.format(Locale.ROOT, "%.2f", total)}", true)
            if (shop.footerText.isNotBlank()) line(shop.footerText)
            document.finishPage(page)
            file.outputStream().use { document.writeTo(it) }
            return file
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            document.close()
        }
    }
}
