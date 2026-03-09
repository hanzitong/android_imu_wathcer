package com.example.sensorviewer.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * センサーの1軸分の値を1行で表示する Composable。
 *
 * レイアウト:
 * ```
 * X    0.123 m/s²
 * ```
 *
 * 数値部分は等幅フォントを使用し、桁が揃うようにする。
 *
 * @param axisLabel 軸ラベル ("X", "Y", "Z", "Pressure" など)
 * @param value     計測値。null の場合は "---" を表示
 * @param unit      単位文字列 ("m/s²", "hPa" など)
 * @param modifier  外部から渡される修飾子
 */
@Composable
fun SensorValueRow(
    axisLabel: String,
    value: Float?,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = axisLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value?.let { "%.3f".format(it) } ?: "---",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
