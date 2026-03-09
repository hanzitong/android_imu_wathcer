package com.example.sensorviewer.model

/**
 * UI 層が消費するセンサー1件分の表示状態。
 *
 * @property type      センサー種別
 * @property available 端末にセンサーが物理的に存在するか
 * @property reading   最新の計測値。まだ受信していない場合は null
 */
data class UiSensorState(
    val type: SensorType,
    val available: Boolean,
    val reading: SensorReading?,
)
