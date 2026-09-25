package com.kishan.billorapos.core.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InputLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
