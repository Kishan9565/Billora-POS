package com.kishan.billorapos.core.domain

import kotlinx.serialization.Serializable

@Serializable
data class Shop(
    val name: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val phoneNumber: String = "",
    val upiId: String = "",
    val footerText: String = ""
) {
    companion object {
        val EMPTY = Shop()
    }
}
