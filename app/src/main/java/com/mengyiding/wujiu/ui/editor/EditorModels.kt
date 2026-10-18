package com.mengyiding.wujiu.ui.editor

import com.mengyiding.wujiu.domain.model.Category

data class EditorUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val assetId: Long = 0,
    val name: String = "",
    val categoryId: Long? = null,
    val priceInput: String = "",
    val purchaseDateEpochDay: Long = java.time.LocalDate.now().toEpochDay(),
    val notes: String = "",
    val imagePath: String? = null,
    val categories: List<Category> = emptyList(),
    val nameError: String? = null,
    val priceError: String? = null,
    val categoryError: String? = null,
    val dateError: String? = null,
    val notesError: String? = null,
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val isSaved: Boolean = false,
)
