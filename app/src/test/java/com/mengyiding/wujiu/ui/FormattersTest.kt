package com.mengyiding.wujiu.ui

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
    fun `decimal money rounds cents to nearest cent`() {
        assertEquals("¥0.01", formatMoney(BigDecimal("0.505")))
    }

    @Test
    fun `decimal money is formatted from cents`() {
        assertEquals("¥1,800.00", formatMoney(BigDecimal("180000")))
    }
}
