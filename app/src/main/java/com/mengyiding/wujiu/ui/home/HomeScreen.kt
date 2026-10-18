package com.mengyiding.wujiu.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mengyiding.wujiu.ui.components.DailyCostCard
import com.mengyiding.wujiu.ui.components.EmptyAssets
import com.mengyiding.wujiu.ui.components.Pill
import com.mengyiding.wujiu.ui.components.SectionLabel
import com.mengyiding.wujiu.ui.components.SpendCard
import com.mengyiding.wujiu.ui.components.TopIconButton
import com.mengyiding.wujiu.util.formatMoney
import com.mengyiding.wujiu.util.formatShortDate
import com.mengyiding.wujiu.util.todayLocalDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAssetClick: (Long) -> Unit,
    onAddAsset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HomeHeader(
                    assetCount = state.assets.size,
                    onAddAsset = onAddAsset,
                )
            }
            if (state.assets.isEmpty() && !state.isLoading) {
                item {
                    EmptyAssets(onAdd = onAddAsset)
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SpendCard(
                            totalSpend = formatMoney(state.totalSpendCents, showDecimals = false),
                            itemCount = state.assets.size,
                            modifier = Modifier.weight(1f),
                        )
                        DailyCostCard(
                            dailyCost = formatMoney(state.todayCost),
                            activeDays = "共 ${state.activeCount} 件使用中",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    CategoryFilters(
                        state = state,
                        onSelect = viewModel::selectCategory,
                    )
                }
                item {
                    SectionLabel(
                        title = "每件物品",
                        modifier = Modifier.padding(top = 3.dp),
                        trailing = "按日均成本 ↓",
                    )
                }
                if (state.filteredAssets.isEmpty()) {
                    item {
                        Text(
                            text = "这个分类下还没有物品",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 28.dp),
                        )
                    }
                } else {
                    items(
                        items = state.filteredAssets,
                        key = { it.id },
                    ) { asset ->
                        AssetRow(
                            asset = asset,
                            onClick = { onAssetClick(asset.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    assetCount: Int,
    onAddAsset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = formatShortDate(todayLocalDate()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "我的大件",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (assetCount == 0) "记录第一件长久之物" else "共 $assetCount 件，长期主义账本",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TopIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "添加物品",
            onClick = onAddAsset,
        )
    }
}

@Composable
private fun CategoryFilters(
    state: HomeUiState,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Pill(
                text = "全部 ${state.assets.size}",
                selected = state.selectedCategoryId == null,
                onClick = { onSelect(null) },
            )
        }
        items(state.categories, key = { it.id }) { category ->
            val count = state.assets.count { it.category.id == category.id }
            if (count > 0) {
                Pill(
                    text = "${category.name} $count",
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onSelect(category.id) },
                )
            }
        }
    }
}
