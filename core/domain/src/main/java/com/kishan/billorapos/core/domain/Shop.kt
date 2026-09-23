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
        val DEFAULT = Shop(
            name = "Dinesh Shop",
            addressLine1 = "Samrajpet, Mecheri",
            addressLine2 = "Salem - 636453",
            phoneNumber = "+917010674588",
            upiId = "dineshsowndar@oksbi",
            footerText = "Thank you, Visit again!!!"
        )
    }
}
