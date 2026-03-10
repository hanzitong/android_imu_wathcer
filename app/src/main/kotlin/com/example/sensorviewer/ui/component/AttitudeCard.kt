package com.example.sensorviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.sensorviewer.model.AttitudeReading
import com.example.sensorviewer.model.UiAttitudeState

/**
 * 姿勢センサーデータを表示する Material 3 カード。
 *
 * ```
 * ┌──────────────────────────────────┐
 * │ Attitude               [利用可否] │
 * │                                  │
 * │ Quaternion                       │
 * │ x   0.000                        │
 * │ y   0.000                        │
 * │ z   0.000                        │
 * │ w   1.000                        │
 * │                                  │
 * │ Rotation Matrix                  │
 * │  1.000   0.000   0.000           │
 * │  0.000   1.000   0.000           │
 * │  0.000   0.000   1.000           │
 * │                                  │
 * │ Euler Angles                     │
 * │ Azimuth    0.00 °                │
 * │ Pitch      0.00 °                │
 * │ Roll       0.00 °                │
 * └──────────────────────────────────┘
 * ```
 *
 * @param title         カードのタイトル（例: "Attitude (World)", "Attitude (Custom Reference)"）
 * @param state         表示する姿勢状態
 * @param description   タイトル直下に表示する座標系・単位の補足説明。省略可
 * @param headerActions ヘッダー右端に配置するアクション（ボタンなど）。省略可
 */
@Composable
fun AttitudeCard(
    title: String,
    state: UiAttitudeState,
    modifier: Modifier = Modifier,
    description: String? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SensorStatusBadge(available = state.available)
                Spacer(modifier = Modifier.width(8.dp))
                headerActions()
            }

            QuaternionSection(state.reading)
            RotationMatrixSection(state.reading)
            EulerAnglesSection(state.reading)
        }
    }
}

@Composable
private fun QuaternionSection(reading: AttitudeReading?) {
    Text(
        text = "Quaternion  [x, y, z, w]  無次元",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    listOf("x", "y", "z", "w").forEachIndexed { index, label ->
        SensorValueRow(
            axisLabel = label,
            value = reading?.quaternion?.getOrNull(index),
            unit = "",
        )
    }
}

@Composable
private fun RotationMatrixSection(reading: AttitudeReading?) {
    Text(
        text = "Rotation Matrix  3×3  無次元",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    for (row in 0..2) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (col in 0..2) {
                val value = reading?.rotationMatrix?.getOrNull(row * 3 + col)
                Text(
                    text = value?.let { "%7.3f".format(it) } ?: "  ---  ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EulerAnglesSection(reading: AttitudeReading?) {
    Text(
        text = "Euler Angles (RPY)  [Roll, Pitch, Yaw]",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    listOf("Roll", "Pitch", "Yaw").forEachIndexed { index, label ->
        EulerAngleRow(
            axisLabel = label,
            radians = reading?.eulerAngles?.getOrNull(index),
        )
    }
}

/**
 * オイラー角の1軸分をラジアンと度の両方で表示する行。
 *
 * レイアウト:
 * ```
 * Roll    0.123 rad  (7.05°)
 * ```
 */
@Composable
private fun EulerAngleRow(axisLabel: String, radians: Float?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = axisLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = radians?.let { "%.3f".format(it) } ?: "---",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
        Text(
            text = " rad",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = radians?.let { "(%.2f°)".format(it.toDegrees()) } ?: "",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Float.toDegrees(): Float = Math.toDegrees(toDouble()).toFloat()
