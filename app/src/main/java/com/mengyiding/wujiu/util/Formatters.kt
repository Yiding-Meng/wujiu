package com.mengyiding.wujiu.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chineseLocale = Locale.SIMPLIFIED_CHINESE
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", chineseLocale)
private val shortDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", chineseLocale)

fun formatMoney(cents: Long, showDecimals: Boolean = true): String {
    val scale = if (showDecimals) 2 else 0
    val value = BigDecimal(cents).movePointLeft(2).setScale(scale, RoundingMode.HALF_UP)
    val formatter = NumberFormat.getNumberInstance(chineseLocale).apply {
        minimumFractionDigits = scale
        maximumFractionDigits = scale
        roundingMode = RoundingMode.HALF_UP
    }
    return "¥${formatter.format(value)}"
}

fun formatMoney(value: BigDecimal): String {
    val yuan = value.movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
    val formatter = NumberFormat.getNumberInstance(chineseLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }
    return "¥${formatter.format(yuan)}"
}

fun formatCompactMoney(cents: Long): String {
    val yuan = BigDecimal(cents).movePointLeft(2).setScale(0, RoundingMode.HALF_UP)
    return "¥${NumberFormat.getNumberInstance(chineseLocale).apply {
        maximumFractionDigits = 0
        roundingMode = RoundingMode.HALF_UP
    }.format(yuan)}"
}

fun formatDate(date: LocalDate): String = dateFormatter.format(date)

fun formatShortDate(date: LocalDate): String = shortDateFormatter.format(date)

fun todayLocalDate(): LocalDate = LocalDate.now()

fun formatUsageDays(days: Long): String = if (days == 1L) "第 1 天" else "第 $days 天"

fun formatDaysCount(days: Long): String = "$days 天"
