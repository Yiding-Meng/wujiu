package com.mengyiding.wujiu.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mengyiding.wujiu.WujiuApplication
import com.mengyiding.wujiu.ui.categories.CategoriesViewModel
import com.mengyiding.wujiu.ui.detail.DetailViewModel
import com.mengyiding.wujiu.ui.editor.EditorViewModel
import com.mengyiding.wujiu.ui.home.HomeViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(wujiuApplication().repository)
        }
        initializer {
            DetailViewModel(
                repository = wujiuApplication().repository,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        initializer {
            EditorViewModel(
                repository = wujiuApplication().repository,
                imageStore = wujiuApplication().imageStore,
                savedStateHandle = createSavedStateHandle(),
            )
        }
        initializer {
            CategoriesViewModel(wujiuApplication().repository)
        }
    }
}

private fun CreationExtras.wujiuApplication(): WujiuApplication =
    this[APPLICATION_KEY] as WujiuApplication
