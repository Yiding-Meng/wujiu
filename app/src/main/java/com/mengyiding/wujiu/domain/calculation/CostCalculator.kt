package com.mengyiding.wujiu.domain.calculation

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object CostCalculator {
    fun usageDays(
        purchaseEpochDay: Long,
        inactiveEpochDay: Long?,
        todayEpochDay: Long,
    ): Long {
        val endEpochDay = inactiveEpochDay ?: todayEpochDay
        return (endEpochDay - purchaseEpochDay + 1).coerceAtLeast(1)
    }

    fun dailyCostCents(priceCents: Long, usageDays: Long): BigDecimal =
        BigDecimal(priceCents)
            .divide(BigDecimal(usageDays.coerceAtLeast(1)), 4, RoundingMode.HALF_UP)

    fun todayActiveCostCents(
        items: List<Triple<Long, Long, Long?>>,
        todayEpochDay: Long,
    ): BigDecimal =
        items
            .filter { (_, _, inactive) -> inactive == null }
            .fold(BigDecimal.ZERO) { total, (price, purchase, _) ->
                total + dailyCostCents(
                    price,
                    usageDays(purchase, null, todayEpochDay),
                )
            }

    fun averageUsageDays(
        items: List<Pair<Long, Long?>>,
        todayEpochDay: Long,
    ): Long {
        if (items.isEmpty()) return 0
        val total = items.sumOf { (purchase, inactive) ->
            usageDays(purchase, inactive, todayEpochDay)
        }
        return total / items.size
    }

    fun epochDayOf(date: LocalDate): Long = date.toEpochDay()
}
