package com.mengyiding.wujiu

import com.mengyiding.wujiu.domain.calculation.CostCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class CostCalculatorTest {
    private val today = LocalDate.of(2026, 10, 18).toEpochDay()

    @Test
    fun `purchase day counts as day one`() {
        assertEquals(
            1L,
            CostCalculator.usageDays(today, null, today),
        )
    }

    @Test
    fun `usage includes both purchase and today`() {
        assertEquals(
            11L,
            CostCalculator.usageDays(today - 10, null, today),
        )
    }

    @Test
    fun `inactive freezes usage days`() {
        assertEquals(
            6L,
            CostCalculator.usageDays(today - 20, today - 15, today + 50),
        )
    }

    @Test
    fun `future purchase still clamps to one day`() {
        assertEquals(
            1L,
            CostCalculator.usageDays(today + 2, null, today),
        )
    }

    @Test
    fun `daily cost divides cents`() {
        assertEquals(
            0,
            CostCalculator.dailyCostCents(100_000, 300)
                .compareTo(BigDecimal("333.3333")),
        )
    }

    @Test
    fun `today cost excludes inactive items`() {
        val result = CostCalculator.todayActiveCostCents(
            items = listOf(
                Triple(100_000, today - 9, null),
                Triple(50_000, today - 4, today - 1),
            ),
            todayEpochDay = today,
        )
        assertEquals(0, result.compareTo(BigDecimal("10000")))
    }

    @Test
    fun `average usage days uses all items`() {
        val result = CostCalculator.averageUsageDays(
            items = listOf(
                today - 9 to null,
                today - 4 to today - 1,
            ),
            todayEpochDay = today,
        )
        assertEquals(7L, result)
    }
}
