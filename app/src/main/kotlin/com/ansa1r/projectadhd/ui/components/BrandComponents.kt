package com.ansa1r.projectadhd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.ui.theme.BrandColors

/** Settings opts into quiet surfaces without changing the shared navigation theme. */
val LocalCalmSurfaces = staticCompositionLocalOf { false }

@Composable
fun BrandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    opaque: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(2.dp, BrandColors.PurpleOutline),
        colors = ButtonDefaults.buttonColors(containerColor = BrandColors.PurpleAction.copy(alpha = if (opaque) 1f else 0.85f),
            contentColor = BrandColors.Text, disabledContainerColor = BrandColors.SurfaceVariant,
            disabledContentColor = BrandColors.Muted),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp), content = content)
}

@Composable
fun MenuCard(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandColors.PurpleSurface.copy(alpha = 0.85f), contentColor = BrandColors.Text),
        border = BorderStroke(2.dp, BrandColors.PurpleOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = BrandColors.Muted, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun BrandOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) = BrandButton(onClick, modifier, enabled, content = content)

@Composable
fun brandFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = BrandColors.PurpleSurface.copy(alpha = 0.85f),
    unfocusedContainerColor = BrandColors.PurpleSurface.copy(alpha = 0.85f),
    disabledContainerColor = BrandColors.SurfaceVariant,
    focusedBorderColor = BrandColors.PurpleOutline, unfocusedBorderColor = BrandColors.PurpleOutline,
    focusedTextColor = BrandColors.Text, unfocusedTextColor = BrandColors.Text,
    focusedLabelColor = BrandColors.Text, unfocusedLabelColor = BrandColors.Muted)
