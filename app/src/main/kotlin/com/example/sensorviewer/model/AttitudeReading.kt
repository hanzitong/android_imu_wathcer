package com.example.sensorviewer.model

/**
 * 姿勢推定の一回分の計測値。
 *
 * クォータニオン・回転行列・オイラー角は同一の姿勢状態の異なる表現であるため一つにまとめる。
 * 変換元は Android の TYPE_ROTATION_VECTOR（加速度計・ジャイロ・地磁気のフュージョン結果）。
 *
 * **座標系: 右手系 ENU**（East=X, North=Y, Up=Z 固定ワールドフレーム）
 *
 * @property quaternion     クォータニオン [x, y, z, w]（無次元・単位クォータニオン、ノルム = 1）
 * @property rotationMatrix 3×3 回転行列を行優先でフラット化した 9 要素リスト（無次元）
 * @property eulerAngles    RPY オイラー角 [roll, pitch, yaw] ラジアン（rad）。ROS2 REP-103 準拠・右手系 ENU 基準
 * @property accuracy       SensorManager.SENSOR_STATUS_* のいずれか
 * @property timestampNanos SensorEvent.timestamp（端末起動からのナノ秒）
 */
data class AttitudeReading(
    val quaternion: List<Float>,        // size = 4: [x, y, z, w]  無次元
    val rotationMatrix: List<Float>,    // size = 9: row-major [R00,R01,R02, R10,R11,R12, R20,R21,R22]  無次元
    val eulerAngles: List<Float>,       // size = 3: [roll, pitch, yaw]  単位: rad（UI では ° に変換）。ROS2 REP-103 RPY 順
    val accuracy: Int,
    val timestampNanos: Long,
)
