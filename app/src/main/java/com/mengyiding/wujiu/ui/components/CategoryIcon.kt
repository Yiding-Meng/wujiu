package com.mengyiding.wujiu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mengyiding.wujiu.domain.model.Category
import com.mengyiding.wujiu.ui.theme.CategoryBlue
import com.mengyiding.wujiu.ui.theme.CategoryLilac
import com.mengyiding.wujiu.ui.theme.CategoryOlive
import com.mengyiding.wujiu.ui.theme.CategoryRose
import com.mengyiding.wujiu.ui.theme.CategorySage
import com.mengyiding.wujiu.ui.theme.CategorySand
import com.mengyiding.wujiu.ui.theme.Forest

data class CategoryVisual(
    val icon: ImageVector,
    val background: Color,
)

@Composable
fun CategoryIcon(
    category: Category,
    modifier: Modifier = Modifier,
    size: Int = 48,
) {
    val visual = categoryVisual(category.iconKey)
    Box(
        modifier = modifier
            .size(size.dp)
            .background(visual.background, RoundedCornerShape((size * 0.3f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = category.name,
            tint = Forest.copy(alpha = 0.82f),
            modifier = Modifier.size((size * 0.48f).dp),
        )
    }
}

fun categoryVisual(iconKey: String): CategoryVisual = when (iconKey) {
    "phone" -> CategoryVisual(Icons.Rounded.PhoneAndroid, CategorySage)
    "computer" -> CategoryVisual(Icons.Rounded.LaptopMac, CategoryBlue)
    "camera" -> CategoryVisual(Icons.Rounded.CameraAlt, CategorySand)
    "appliance" -> CategoryVisual(Icons.Rounded.Kitchen, CategoryRose)
    "transport" -> CategoryVisual(Icons.Rounded.DirectionsCar, CategoryOlive)
    "wearable" -> CategoryVisual(Icons.Rounded.Watch, CategoryLilac)
    "audio" -> CategoryVisual(Icons.Rounded.Audiotrack, CategorySage)
    else -> CategoryVisual(Icons.Rounded.DevicesOther, CategorySand)
}
