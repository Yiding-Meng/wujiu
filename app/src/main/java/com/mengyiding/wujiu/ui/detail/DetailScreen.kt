package com.mengyiding.wujiu.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mengyiding.wujiu.ui.components.CategoryIcon
import com.mengyiding.wujiu.ui.components.MetricPair
import com.mengyiding.wujiu.util.formatDate
import com.mengyiding.wujiu.util.formatMoney
import com.mengyiding.wujiu.util.todayLocalDate
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }

    if (state.notFound) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("物品不存在", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) { Text("返回") }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Row {
                IconButton(onClick = { onEdit(state.id) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        ItemHero(state)
        Spacer(Modifier.height(22.dp))

        Text(
            text = state.name,
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.category?.let {
                CategoryIcon(it, size = 25)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (state.inactiveDateEpochDay != null) {
                Text(
                    text = "已停用",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = "使用中",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        MetricPair(
            leftLabel = if (state.inactiveDateEpochDay == null) "已经使用" else "总共使用",
            leftValue = "${state.usageDays} 天",
            rightLabel = "平均每天",
            rightValue = formatMoney(state.dailyCost),
        )
        Spacer(Modifier.height(12.dp))
        MetricPair(
            leftLabel = "购买价格",
            leftValue = formatMoney(state.priceCents),
            rightLabel = "购买日期",
            rightValue = formatDate(java.time.LocalDate.ofEpochDay(state.purchaseDateEpochDay)),
        )

        if (state.notes.isNotBlank()) {
            Spacer(Modifier.height(22.dp))
            Text(
                text = "备注",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = state.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(28.dp))
        if (state.inactiveDateEpochDay == null) {
            OutlinedButton(
                onClick = { showStopDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.PauseCircleOutline, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("标记为已停用 / 已出售")
            }
        } else {
            Button(
                onClick = viewModel::resumeUsing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("恢复为使用中")
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("删除这件物品？") },
            text = { Text("删除后无法恢复，相关的本地图片也会一并清除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onDeleted)
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    if (showStopDialog) {
        StopUsingDialog(
            purchaseDateEpochDay = state.purchaseDateEpochDay,
            onDismiss = { showStopDialog = false },
            onConfirm = { date ->
                showStopDialog = false
                viewModel.stopUsing(date)
            },
        )
    }
}

@Composable
private fun ItemHero(state: DetailUiState) {
    if (state.imagePath != null) {
        AsyncImage(
            model = File(state.imagePath),
            contentDescription = state.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            state.category?.let { CategoryIcon(it, size = 96) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopUsingDialog(
    purchaseDateEpochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val today = todayLocalDate()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = today
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selected = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                return !selected.isBefore(java.time.LocalDate.ofEpochDay(purchaseDateEpochDay)) &&
                    !selected.isAfter(today)
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = state.selectedDateMillis
                        ?: today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    onConfirm(
                        Instant.ofEpochMilli(selected)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toEpochDay(),
                    )
                },
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
