package com.example.sensorviewer.model

import android.hardware.SensorManager

/**
 * センサーの1回分の計測値。
 *
 * [values] は [SensorType.axes] と同じ順序・個数。
 * FloatArray の代わりに List<Float> を使用することで data class の equals/hashCode が正しく動作する。
 *
 * @property type           センサー種別
 * @property values         各軸の計測値。[SensorType.axes] と対応
 * @property accuracy       [SensorManager].SENSOR_STATUS_* のいずれか
 * @property timestampNanos SensorEvent.timestamp (端末起動からのナノ秒)
 */
data class SensorReading(
    val type: SensorType,
    val values: List<Float>,
    val accuracy: Int,
    val timestampNanos: Long,
) {
    companion object {
        /** 精度定数を人間可読な文字列に変換する */
        fun accuracyLabel(accuracy: Int): String = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH   -> "High"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW    -> "Low"
            SensorManager.SENSOR_STATUS_UNRELIABLE      -> "Unreliable"
            else                                        -> "Unknown"
        }
    }
}
