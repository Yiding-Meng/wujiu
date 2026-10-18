package com.mengyiding.wujiu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengyiding.wujiu.ui.theme.CategoryOlive
import com.mengyiding.wujiu.ui.theme.CategorySand
import com.mengyiding.wujiu.ui.theme.Forest

@Composable
fun SpendCard(
    totalSpend: String,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(CategorySand, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "已为大件花费",
            style = MaterialTheme.typography.labelMedium,
            color = Forest.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = totalSpend,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Forest,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "共 $itemCount 件物品",
            style = MaterialTheme.typography.labelMedium,
            color = Forest.copy(alpha = 0.64f),
        )
    }
}

@Composable
fun DailyCostCard(
    dailyCost: String,
    activeDays: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(CategoryOlive, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "今天的使用成本",
            style = MaterialTheme.typography.labelMedium,
            color = Forest.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = dailyCost,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Forest,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = activeDays,
            style = MaterialTheme.typography.labelMedium,
            color = Forest.copy(alpha = 0.64f),
        )
    }
}

@Composable
fun MetricPair(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Metric(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f),
        )
        Metric(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 18.sp,
        )
    }
}
