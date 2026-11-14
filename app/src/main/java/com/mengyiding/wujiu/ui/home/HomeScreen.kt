package com.mengyiding.wujiu.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAssetClick: (Long) -> Unit,
    onAddAsset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = viewModel.createBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法打开导出目标文件")
                }
            }.onSuccess {
                viewModel.onBackupExported()
            }.onFailure { throwable ->
                viewModel.notifyError(throwable.message ?: "导出备份失败")
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("无法读取所选文件")
                }
            }.onSuccess { rawJson ->
                viewModel.prepareImportJson(rawJson)
            }.onFailure { throwable ->
                viewModel.notifyError(throwable.message ?: "读取备份失败")
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissStatusMessage()
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
                    onExportBackup = {
                        val stamp = todayLocalDate().toString()
                        exportBackupLauncher.launch("wujiu-backup-$stamp.json")
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
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

    state.pendingImport?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingImport,
            title = { Text("从备份恢复数据") },
            text = {
                Text(
                    "备份包含 ${backup.assets.size} 件物品、${backup.categories.size} 个分类。\n" +
                        "「追加合并」会保留现有记录并跳过重复条目；「覆盖恢复」将清空现有记录（注：JSON 备份不含本地图片原文件）。",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(replaceExisting = false) }) {
                    Text("追加合并")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = viewModel::dismissPendingImport) {
                        Text("取消")
                    }
                    TextButton(onClick = { viewModel.confirmImport(replaceExisting = true) }) {
                        Text("覆盖恢复", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }
}

@Composable
private fun HomeHeader(
    assetCount: Int,
    onAddAsset: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                TopIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "备份与恢复",
                    onClick = { menuExpanded = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("导出数据备份 (JSON)") },
                        leadingIcon = { Icon(Icons.Rounded.Upload, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onExportBackup()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("从备份恢复 (JSON)") },
                        leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onImportBackup()
                        },
                    )
                }
            }
            TopIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "添加物品",
                onClick = onAddAsset,
            )
        }
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
