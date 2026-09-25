package com.kishan.billorapos.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrimaryButton(
    onPressed: (() -> Unit)?,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    elevation: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
    fullWidth: Boolean = true,
    textStyle: TextStyle? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val isButtonEnabled = enabled && onPressed != null && !isLoading
    
    val shape = RoundedCornerShape(cornerRadius)
    
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .heightIn(min = 52.dp)
            .shadow(if (isButtonEnabled) elevation else 0.dp, shape, ambientColor = PrimaryColor.copy(alpha = 0.25f), spotColor = PrimaryColor.copy(alpha = 0.25f)),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isButtonEnabled || isLoading) BilloraGradients.Primary else BilloraGradients.Disabled,
                    shape = shape
                )
                .clip(shape)
                .clickable(enabled = isButtonEnabled, role = Role.Button) { onPressed?.invoke() }
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = label,
                        color = Color.White,
                        style = textStyle ?: MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}
