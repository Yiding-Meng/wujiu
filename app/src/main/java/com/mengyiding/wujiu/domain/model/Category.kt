package com.mengyiding.wujiu.domain.model

data class Category(
    val id: Long,
    val name: String,
    val iconKey: String,
    val isBuiltIn: Boolean,
)
