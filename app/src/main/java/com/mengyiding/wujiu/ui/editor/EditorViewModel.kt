package com.mengyiding.wujiu.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mengyiding.wujiu.data.image.LocalImageStore
import com.mengyiding.wujiu.data.repository.NewAsset
import com.mengyiding.wujiu.data.repository.WujiuRepository
import com.mengyiding.wujiu.domain.model.Asset
import com.mengyiding.wujiu.util.todayLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class EditorViewModel(
    private val repository: WujiuRepository,
    private val imageStore: LocalImageStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val requestedAssetId: Long = savedStateHandle["assetId"] ?: 0L
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var originalImagePath: String? = null
    private var originalAsset: Asset? = null

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                _uiState.update { state ->
                    val selected = state.categoryId
                        ?: categories.firstOrNull { it.name == "其他" }?.id
                    state.copy(categories = categories, categoryId = selected)
                }
            }
        }
        viewModelScope.launch {
            if (requestedAssetId > 0) {
                val item = repository.getAsset(requestedAssetId)
                if (item == null) {
                    _uiState.update {
                        it.copy(isLoading = false, generalError = "物品不存在")
                    }
                } else {
                    originalAsset = item
                    originalImagePath = item.imagePath
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = true,
                            assetId = item.id,
                            name = item.name,
                            categoryId = item.categoryId,
                            priceInput = BigDecimal(item.priceCents)
                                .movePointLeft(2)
                                .stripTrailingZeros()
                                .toPlainString(),
                            purchaseDateEpochDay = item.purchaseDateEpochDay,
                            notes = item.notes,
                            imagePath = item.imagePath,
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setName(value: String) {
        _uiState.update {
            it.copy(name = value.take(40), nameError = null, isDirty = true)
        }
    }

    fun setCategory(id: Long) {
        _uiState.update {
            it.copy(categoryId = id, categoryError = null, isDirty = true)
        }
    }

    fun setPrice(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        val firstDot = filtered.indexOf('.')
        val normalized = if (firstDot < 0) {
            filtered
        } else {
            buildString {
                append(filtered.substring(0, firstDot))
                append('.')
                append(
                    filtered.substring(firstDot + 1)
                        .replace(".", "")
                        .take(2),
                )
            }.take(15)
        }
        _uiState.update {
            it.copy(priceInput = normalized, priceError = null, isDirty = true)
        }
    }

    fun setPurchaseDate(epochDay: Long) {
        _uiState.update {
            it.copy(
                purchaseDateEpochDay = epochDay,
                dateError = null,
                isDirty = true,
            )
        }
    }

    fun setNotes(value: String) {
        _uiState.update {
            it.copy(notes = value.take(200), notesError = null, isDirty = true)
        }
    }

    fun importImage(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { imageStore.import(uri) }
            }.onSuccess { path ->
                val previous = _uiState.value.imagePath
                if (previous != null && previous != originalImagePath) {
                    imageStore.delete(previous)
                }
                _uiState.update {
                    it.copy(imagePath = path, generalError = null, isDirty = true)
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(generalError = throwable.message ?: "图片处理失败")
                }
            }
        }
    }

    fun removeImage() {
        val previous = _uiState.value.imagePath ?: return
        if (previous != originalImagePath) {
            imageStore.delete(previous)
        }
        _uiState.update {
            it.copy(imagePath = null, isDirty = true)
        }
    }

    fun discardAndCleanup() {
        val current = _uiState.value.imagePath
        if (current != null && current != originalImagePath) {
            imageStore.delete(current)
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val name = state.name.trim()
        val priceCents = parsePriceCents(state.priceInput)
        val today = todayLocalDate().toEpochDay()
        val nameError = when {
            name.isEmpty() -> "请输入物品名称"
            name.length > 40 -> "名称不能超过 40 个字"
            else -> null
        }
        val priceError = when {
            state.priceInput.isBlank() -> "请输入购买价格"
            priceCents == null -> "请输入有效金额，最多两位小数"
            priceCents <= 0 -> "购买价格必须大于 0"
            else -> null
        }
        val dateError = when {
            state.purchaseDateEpochDay > today -> "购买日期不能晚于今天"
            originalAsset?.purchaseDateEpochDay != null &&
                state.purchaseDateEpochDay > today -> "购买日期不能晚于今天"
            originalAsset?.inactiveDateEpochDay != null &&
                state.purchaseDateEpochDay > originalAsset!!.inactiveDateEpochDay!! ->
                "购买日期不能晚于停用日期"
            else -> null
        }
        val categoryError = if (state.categoryId == null) "请选择分类" else null
        val notesError = if (state.notes.length > 200) "备注不能超过 200 个字" else null
        if (listOf(nameError, priceError, categoryError, dateError, notesError).any { it != null }) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    priceError = priceError,
                    categoryError = categoryError,
                    dateError = dateError,
                    notesError = notesError,
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, generalError = null) }
        viewModelScope.launch {
            val newAsset = NewAsset(
                name = name,
                categoryId = state.categoryId!!,
                priceCents = priceCents!!,
                purchaseDateEpochDay = state.purchaseDateEpochDay,
                notes = state.notes.trim(),
                imagePath = state.imagePath,
                inactiveDateEpochDay = originalAsset?.inactiveDateEpochDay,
            )
            runCatching {
                if (state.isEditing) {
                    repository.updateAsset(state.assetId, newAsset)
                } else {
                    repository.addAsset(newAsset)
                }
            }.onSuccess {
                if (originalImagePath != null && originalImagePath != state.imagePath) {
                    imageStore.delete(originalImagePath)
                }
                _uiState.update {
                    it.copy(isSaving = false, isDirty = false, isSaved = true)
                }
                onSaved()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        generalError = throwable.message ?: "保存失败，请重试",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(generalError = null) }
    }

    private fun parsePriceCents(input: String): Long? {
        if (!Regex("""^\d+(\.\d{1,2})?$""").matches(input.trim())) return null
        return runCatching {
            BigDecimal(input.trim()).movePointRight(2).longValueExact()
        }.getOrNull()
    }
}
