package com.example.sensorviewer.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorviewer.model.UiSensorState

/**
 * 1センサー分の情報を表示する Material 3 カード。
 *
 * ```
 * ┌──────────────────────────────────┐
 * │ [センサー名]         [利用可否バッジ] │
 * │ X  0.000 m/s²                    │
 * │ Y  0.000 m/s²                    │
 * │ Z  0.000 m/s²                    │
 * └──────────────────────────────────┘
 * ```
 *
 * - [state.available] が false の場合はカードをグレーアウトして表示
 * - [state.reading] が null の場合は各軸の値を "---" で表示
 */
@Composable
fun SensorCard(
    state: UiSensorState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                SensorStatusBadge(available = state.available)
            }

            Spacer(modifier = Modifier.height(8.dp))

            state.type.axes.forEachIndexed { index, axisLabel ->
                SensorValueRow(
                    axisLabel = axisLabel,
                    value = state.reading?.values?.getOrNull(index),
                    unit = state.type.unit,
                )
            }
        }
    }
}
