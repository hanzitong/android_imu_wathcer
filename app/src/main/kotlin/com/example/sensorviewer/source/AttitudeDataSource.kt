package com.example.sensorviewer.source

import kotlinx.coroutines.flow.Flow

/**
 * TYPE_ROTATION_VECTOR センサーへのアクセスを抽象化するインターフェース。
 *
 * このインターフェースは生のセンサーイベント値を返すのみ。
 * クォータニオン → 回転行列 → オイラー角への変換は UseCase 層が行う。
 */
interface AttitudeDataSource {

    /** 端末に TYPE_ROTATION_VECTOR センサーが存在するか返す */
    fun isAvailable(): Boolean

    /**
     * 生の回転ベクトル値を Flow で返す。
     *
     * @param samplingPeriodUs サンプリング間隔（SensorManager.SENSOR_DELAY_* 定数または μs 値）
     * @return [RawRotationVector] を連続して emit する cold Flow
     * @throws AttitudeNotAvailableException センサーが存在しない場合
     */
    fun observe(samplingPeriodUs: Int = android.hardware.SensorManager.SENSOR_DELAY_UI): Flow<RawRotationVector>
}

/**
 * TYPE_ROTATION_VECTOR の生センサーイベント値。
 *
 * @property values         SensorEvent.values のコピー。[x, y, z] または [x, y, z, w]。
 *                          w は一部デバイスで省略される（UseCase 側で再構築する）
 * @property accuracy       SensorManager.SENSOR_STATUS_* のいずれか
 * @property timestampNanos SensorEvent.timestamp（端末起動からのナノ秒）
 */
data class RawRotationVector(
    val values: List<Float>,
    val accuracy: Int,
    val timestampNanos: Long,
)

/** 端末に TYPE_ROTATION_VECTOR センサーが存在しない場合にスローされる例外 */
class AttitudeNotAvailableException :
    IllegalStateException("Rotation vector sensor not available on this device")
