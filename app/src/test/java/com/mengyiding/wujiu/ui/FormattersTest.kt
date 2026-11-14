package com.mengyiding.wujiu.ui

import com.mengyiding.wujiu.util.formatCompactMoney
import com.mengyiding.wujiu.util.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FormattersTest {
    @Test
    fun `money formatter keeps two decimals and grouping`() {
        assertEquals("¥8,699.00", formatMoney(869_900))
    }

    @Test
    fun `decimal money rounds cents using single half-up pass`() {
        assertEquals("¥0.01", formatMoney(BigDecimal("0.505")))
        assertEquals("¥0.01", formatMoney(BigDecimal("0.500")))
        assertEquals("¥0.01", formatMoney(BigDecimal("1.499")))
        assertEquals("¥1.01", formatMoney(BigDecimal("100.500")))
        assertEquals("¥1.01", formatMoney(BigDecimal("101.4967")))
    }

    @Test
    fun `integer and compact formatters use half-up without decimals`() {
        assertEquals("¥2", formatMoney(150L, showDecimals = false))
        assertEquals("¥1", formatMoney(149L, showDecimals = false))
        assertEquals("¥2", formatCompactMoney(150L))
    }

    @Test
    fun `decimal money is formatted from cents`() {
        assertEquals("¥1,800.00", formatMoney(BigDecimal("180000")))
    }
}
