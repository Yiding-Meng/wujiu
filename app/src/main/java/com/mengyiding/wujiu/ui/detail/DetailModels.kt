package com.mengyiding.wujiu.ui.detail

import com.mengyiding.wujiu.domain.model.Category
import java.math.BigDecimal

data class DetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val category: Category? = null,
    val priceCents: Long = 0,
    val purchaseDateEpochDay: Long = 0,
    val inactiveDateEpochDay: Long? = null,
    val usageDays: Long = 1,
    val dailyCost: BigDecimal = BigDecimal.ZERO,
    val notes: String = "",
    val imagePath: String? = null,
    val errorMessage: String? = null,
)
