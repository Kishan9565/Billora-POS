package com.kishan.billorapos.core.printer

object EscPos {
    val INIT = byteArrayOf(0x1B, 0x40)
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    val TEXT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00)
    val TEXT_LARGE = byteArrayOf(0x1D, 0x21, 0x11)
    val LINE_FEED = byteArrayOf(0x0A)
}
