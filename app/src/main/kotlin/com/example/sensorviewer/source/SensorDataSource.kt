package com.example.sensorviewer.source

import android.hardware.SensorManager
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import kotlinx.coroutines.flow.Flow

/**
 * センサーハードウェアへのアクセスを抽象化するインターフェース。
 *
 * 実装は [android.hardware.SensorManager] を [kotlinx.coroutines.flow.callbackFlow] で
 * ラップし、Flow の collect が開始／終了したタイミングでリスナーを自動登録／解除する。
 */
interface SensorDataSource {

    /**
     * 指定センサーが端末に物理的に存在するか返す。
     *
     * @param type チェック対象のセンサー種別
     */
    fun isAvailable(type: SensorType): Boolean

    /**
     * 指定センサーの計測値を Flow で返す。
     *
     * Flow を collect するとセンサーリスナーが登録され、
     * collect をキャンセルすると自動的に解除される。
     *
     * @param type              観測するセンサー種別
     * @param samplingPeriodUs  サンプリング間隔 ([SensorManager].SENSOR_DELAY_* または μs 指定)
     * @return [SensorReading] を連続して emit する cold Flow
     * @throws [SensorNotAvailableException] センサーが存在しない場合
     */
    fun observe(
        type: SensorType,
        samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI,
    ): Flow<SensorReading>
}

/** 端末に該当センサーが存在しない場合にスローされる例外 */
class SensorNotAvailableException(type: SensorType) :
    IllegalStateException("Sensor not available on this device: ${type.label}")
