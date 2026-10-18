package com.mengyiding.wujiu.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mengyiding.wujiu.di.AppViewModelProvider
import com.mengyiding.wujiu.ui.categories.CategoriesScreen
import com.mengyiding.wujiu.ui.categories.CategoriesViewModel
import com.mengyiding.wujiu.ui.detail.DetailScreen
import com.mengyiding.wujiu.ui.detail.DetailViewModel
import com.mengyiding.wujiu.ui.editor.EditorScreen
import com.mengyiding.wujiu.ui.editor.EditorViewModel
import com.mengyiding.wujiu.ui.home.HomeScreen
import com.mengyiding.wujiu.ui.home.HomeViewModel

private object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{assetId}"
    const val EDITOR = "editor?assetId={assetId}"
    const val CATEGORIES = "categories"

    fun detail(id: Long) = "detail/$id"
    fun editor(id: Long? = null) = if (id == null) "editor" else "editor?assetId=$id"
}

@Composable
fun WujiuApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(200)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                tween(220),
                initialOffset = { it / 20 },
            )
        },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = {
            fadeOut(tween(150)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                tween(200),
                targetOffset = { it / 20 },
            )
        },
    ) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
            HomeScreen(
                viewModel = vm,
                onAssetClick = { navController.navigate(Routes.detail(it)) },
                onAddAsset = { navController.navigate(Routes.editor()) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("assetId") { type = NavType.LongType }),
        ) {
            val vm: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
            DetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editor(it)) },
                onDeleted = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("assetId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) {
            val vm: EditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
            EditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set("assetSaved", true)
                    navController.popBackStack()
                },
                onManageCategories = { navController.navigate(Routes.CATEGORIES) },
            )
        }
        composable(Routes.CATEGORIES) {
            val vm: CategoriesViewModel = viewModel(factory = AppViewModelProvider.Factory)
            CategoriesScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
