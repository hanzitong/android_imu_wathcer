package com.example.sensorviewer.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * センサーの利用可否を小さなバッジで表示する Composable。
 *
 * - available = true  → "Available"  (primaryContainer 背景)
 * - available = false → "Unavailable" (errorContainer 背景)
 *
 * @param available センサーが端末に存在するか
 * @param modifier  外部から渡される修飾子
 */
@Composable
fun SensorStatusBadge(
    available: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (available) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (available) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = if (available) "Available" else "Unavailable",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
