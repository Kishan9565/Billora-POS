package com.kishan.billorapos

import com.kishan.billorapos.core.domain.Product
import com.kishan.billorapos.feature.product.domain.ProductCsv
import com.kishan.billorapos.feature.product.domain.validateProduct
import org.junit.Assert.*
import org.junit.Test

class ProductCsvTest {
    @Test fun quotedNamesNewlinesUnicodeAndLeadingZeroBarcodesRoundTrip() {
        val product = Product("id", "Tea, \"special\"\r\nதமிழ்", "00123", 12.5, 7)
        val parsed = ProductCsv.parse(ProductCsv.export(listOf(product)))
        assertEquals(0, parsed.skipped)
        assertEquals(product.copy(id = parsed.products.single().id), parsed.products.single())
    }

    @Test fun headersIgnoreCaseAndBomAndStockDefaultsToZero() {
        val parsed = ProductCsv.parse("\uFEFFPRICE,BARCODE,Name\r\n0,001,Free item\r\n")
        assertEquals(0, parsed.skipped)
        assertEquals("001", parsed.products.single().barcode)
        assertEquals(0, parsed.products.single().stock)
    }

    @Test fun invalidRowsAreSkippedWhileValidRowsSurvive() {
        val parsed = ProductCsv.parse("name,barcode,price,stock\nGood,1,2,3\nBad,2,NaN,0\nBad,3,-1,0\nBad,4,1,-1\nBad,5,1,2147483648\n,6,1,0\nBad,,1,0\nBad,7,1,no\nGood,8,0,\n")
        assertEquals(7, parsed.skipped)
        assertEquals(2, parsed.products.size)
        assertEquals(0, parsed.products.last().stock)
    }

    @Test fun malformedQuotedRecordDoesNotDiscardFollowingValidRecord() {
        val parsed = ProductCsv.parse("name,barcode,price\n\"bad\"oops,1,3\nGood,2,4\n")
        assertEquals(1, parsed.skipped)
        assertEquals("Good", parsed.products.single().name)
    }

    @Test fun unterminatedQuoteIsSkipped() {
        val parsed = ProductCsv.parse("name,barcode,price\n\"unfinished,1,2")
        assertEquals(1, parsed.skipped)
        assertTrue(parsed.products.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingRequiredHeaderIsRejected() { ProductCsv.parse("name,price\nTea,2") }

    @Test fun sharedValidationPreservesZeroPriceAndRejectsNonFiniteValues() {
        assertTrue(validateProduct("Tea", "001", "0").isValid)
        listOf("NaN", "Infinity", "-1", "", "oops").forEach {
            assertFalse(validateProduct("Tea", "001", it).isValid)
        }
    }
}
