package com.aistudio.shreeshyamstore.pqwzkb.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.SaffronPrimary
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.TextMediumGray
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.TextMutedGray
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.TextNearBlack

/**
 * Shared popup surface for merchant selection menus. Every caller gets an
 * intentional surface and readable item colors instead of inheriting a theme
 * default that may be unreadable over the app background.
 */
@Composable
fun AppDropdownMenuSurface(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        content = content
    )
}

@Composable
fun AppDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null
) {
    val textColor = if (emphasized) SaffronPrimary else TextNearBlack
    DropdownMenuItem(
        text = {
            androidx.compose.material3.Text(
                text = text,
                color = if (enabled) textColor else TextMutedGray,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = MenuDefaults.itemColors(
            textColor = textColor,
            leadingIconColor = TextMediumGray,
            trailingIconColor = TextMediumGray
        )
    )
}
