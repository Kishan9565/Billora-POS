package com.kishan.billorapos.core.domain

import java.math.BigDecimal

/** Keep the persisted Double model, but calculate decimal amounts without binary accumulation. */
fun lineAmount(price: Double, quantity: Int): BigDecimal =
    BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity.toLong()))

fun totalAmount(lines: List<Pair<Double, Int>>): Double =
    lines.fold(BigDecimal.ZERO) { sum, (price, quantity) -> sum + lineAmount(price, quantity) }.toDouble()
