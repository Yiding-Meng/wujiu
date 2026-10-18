package com.mengyiding.wujiu.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mengyiding.wujiu.ui.components.CategoryIcon
import com.mengyiding.wujiu.ui.components.Pill
import com.mengyiding.wujiu.util.formatDate
import com.mengyiding.wujiu.util.todayLocalDate
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(viewModel::importImage)
    }

    LaunchedEffect(state.generalError) {
        state.generalError?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    fun requestBack() {
        if (state.isDirty) showDiscardDialog = true else onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = ::requestBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = if (state.isEditing) "编辑物品" else "添加物品",
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(
                    onClick = { viewModel.save(onSaved) },
                    enabled = !state.isSaving && !state.isLoading,
                ) {
                    Text(if (state.isSaving) "保存中" else "保存")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
            ImagePicker(
                imagePath = state.imagePath,
                onPick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemove = viewModel::removeImage,
            )
            }

            item {
            Column {
                FieldLabel("物品名称")
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：MacBook Air M4") },
                    singleLine = true,
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    shape = RoundedCornerShape(16.dp),
                )
            }
            }

            item {
            Column {
                FieldLabel("购买价格")
                OutlinedTextField(
                    value = state.priceInput,
                    onValueChange = viewModel::setPrice,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.00") },
                    prefix = { Text("¥ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = state.priceError != null,
                    supportingText = state.priceError?.let { { Text(it) } },
                    shape = RoundedCornerShape(16.dp),
                )
            }
            }

            item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FieldLabel("分类")
                    TextButton(onClick = onManageCategories) {
                        Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("管理分类")
                    }
                }
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (state.categoryId == category.id) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                )
                                .clickable { viewModel.setCategory(category.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryIcon(category, size = 26)
                            Spacer(Modifier.width(7.dp))
                            Text(category.name, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                state.categoryError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            }

            item {
            Column {
                FieldLabel("购买日期")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = formatDate(LocalDate.ofEpochDay(state.purchaseDateEpochDay)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                state.dateError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            }

            item {
            Column {
                FieldLabel("备注（可选）")
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("型号、购买渠道，或任何想记住的事") },
                    isError = state.notesError != null,
                    supportingText = state.notesError?.let { { Text(it) } },
                    shape = RoundedCornerShape(16.dp),
                )
            }
            }

            item {
            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSaving && !state.isLoading,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (state.isSaving) "保存中…" else "保存物品")
            }
            }
        }
    }

    if (showDatePicker) {
        val today = todayLocalDate()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(state.purchaseDateEpochDay)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.setPurchaseDate(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toEpochDay(),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("当前填写的内容还没有保存。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardAndCleanup()
                        onBack()
                    },
                ) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑") }
            },
        )
    }
}

@Composable
private fun ImagePicker(
    imagePath: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onPick),
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "物品照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "移除照片")
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "添加一张照片（可选）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
