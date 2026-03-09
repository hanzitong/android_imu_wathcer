package com.example.sensorviewer.model

import android.hardware.Sensor

/**
 * アプリがサポートするセンサーの種別。
 *
 * @property label      UI に表示するセンサー名
 * @property androidConstant [Sensor].TYPE_* 定数
 * @property unit       表示単位文字列
 * @property axes       センサー値の軸ラベル。単軸センサーは要素数 1
 */
sealed class SensorType(
    val label: String,
    val androidConstant: Int,
    val unit: String,
    val axes: List<String>,
) {
    object Accelerometer : SensorType(
        label = "Accelerometer",
        androidConstant = Sensor.TYPE_ACCELEROMETER,
        unit = "m/s²",
        axes = listOf("X", "Y", "Z"),
    )

    object Gyroscope : SensorType(
        label = "Gyroscope",
        androidConstant = Sensor.TYPE_GYROSCOPE,
        unit = "rad/s",
        axes = listOf("X", "Y", "Z"),
    )

    object Magnetometer : SensorType(
        label = "Magnetometer",
        androidConstant = Sensor.TYPE_MAGNETIC_FIELD,
        unit = "µT",
        axes = listOf("X", "Y", "Z"),
    )

    object Barometer : SensorType(
        label = "Barometer",
        androidConstant = Sensor.TYPE_PRESSURE,
        unit = "hPa",
        axes = listOf("Pressure"),
    )

    object AmbientLight : SensorType(
        label = "Ambient Light",
        androidConstant = Sensor.TYPE_LIGHT,
        unit = "lux",
        axes = listOf("Illuminance"),
    )

    object Proximity : SensorType(
        label = "Proximity",
        androidConstant = Sensor.TYPE_PROXIMITY,
        unit = "cm",
        axes = listOf("Distance"),
    )

    companion object {
        /** アプリがサポートする全センサー種別 */
        val all: List<SensorType> = listOf(
            Accelerometer,
            Gyroscope,
            Magnetometer,
            Barometer,
            AmbientLight,
            Proximity,
        )
    }
}
