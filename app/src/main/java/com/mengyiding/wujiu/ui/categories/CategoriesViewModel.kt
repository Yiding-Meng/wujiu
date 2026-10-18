package com.mengyiding.wujiu.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mengyiding.wujiu.data.repository.WujiuRepository
import com.mengyiding.wujiu.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val usageCounts: Map<Long, Int> = emptyMap(),
    val message: String? = null,
)

class CategoriesViewModel(
    private val repository: WujiuRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.observeCategories(),
        repository.observeAssets(),
        message,
    ) { categories, assets, message ->
        CategoriesUiState(
            categories = categories,
            usageCounts = assets.groupingBy { it.category.id }.eachCount(),
            message = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoriesUiState(),
    )

    fun add(name: String, onAdded: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.addCategory(name) }
                .onSuccess { onAdded() }
                .onFailure { message.value = it.message ?: "新增分类失败" }
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            runCatching {
                if (category.isBuiltIn) {
                    false
                } else if (repository.hasAssetsInCategory(category.id)) {
                    false
                } else {
                    repository.deleteCategoryIfUnused(category.id)
                }
            }
                .onSuccess { success ->
                    message.value = if (success) {
                        "已删除「${category.name}」"
                    } else {
                        "该分类仍有关联物品，无法删除"
                    }
                }
                .onFailure { message.value = it.message ?: "删除分类失败" }
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}
