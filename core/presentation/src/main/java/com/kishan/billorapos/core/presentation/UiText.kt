package com.kishan.billorapos.core.presentation

import android.content.Context

sealed interface UiText {
    data class DynamicString(val value: String): UiText
    class StringResource(val resId: Int, vararg val args: Any): UiText

    fun asString(context: Context): String {
        return handle(context)
    }

    private fun handle(context: Context): String {
        return when(this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *args)
        }
    }
}
