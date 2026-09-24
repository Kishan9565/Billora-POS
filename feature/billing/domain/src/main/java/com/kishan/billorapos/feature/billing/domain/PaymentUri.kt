package com.kishan.billorapos.feature.billing.domain

import java.net.URLEncoder
import java.util.Locale

fun paymentUri(upiId: String, shopName: String, amount: Double): String {
    fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    return "upi://pay?pa=${encode(upiId)}&pn=${encode(shopName)}&am=${String.format(Locale.ROOT, "%.2f", amount)}&cu=INR"
}
