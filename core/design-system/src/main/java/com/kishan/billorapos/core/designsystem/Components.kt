package com.kishan.billorapos.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow

@Composable
fun GradientStatusChip(
    label: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun BilloraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    
    ElevatedCard(
        modifier = cardModifier.shadow(elevation, RoundedCornerShape(16.dp), ambientColor = PrimaryColor.copy(alpha = 0.15f), spotColor = PrimaryColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GradientButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    PrimaryButton(onPressed = onClick, label = label, modifier = modifier, enabled = enabled, isLoading = loading)
}

@Composable
fun BilloraSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(state) { data ->
        Snackbar(data, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(12.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface)
    }
}

@Composable
fun StockBadge(stock: Int, threshold: Int) {
    if (stock <= threshold) StatusChip(
        label = if (stock <= 0) "Out of stock" else "Low stock ($stock left)",
        containerColor = if (stock <= 0) DangerColor else WarningColor,
        contentColor = if (stock <= 0) Color.White else Slate900
    )
}

@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(24.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
