package com.example.sensorviewer.model

/**
 * UI 層が消費する姿勢センサーの表示状態。
 *
 * @property available 端末に TYPE_ROTATION_VECTOR センサーが存在するか
 * @property reading   最新の姿勢推定値。まだ受信していない場合は null
 */
data class UiAttitudeState(
    val available: Boolean,
    val reading: AttitudeReading? = null,
)
