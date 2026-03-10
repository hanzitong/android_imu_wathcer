package com.example.sensorviewer.source

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * [AttitudeDataSource] の Android 実装。
 *
 * [SensorManager] の TYPE_ROTATION_VECTOR センサーを `callbackFlow` でラップし、
 * Flow の collect 開始でリスナー登録、キャンセルで自動解除する。
 *
 * このクラスは生のセンサーイベント値を返すのみ。変換ロジックは持たない。
 * Android SDK の型（[SensorEvent]）はこのクラスの外に漏れない。
 */
class AndroidAttitudeSource @Inject constructor(
    private val sensorManager: SensorManager,
) : AttitudeDataSource {

    override fun isAvailable(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    override fun observe(samplingPeriodUs: Int): Flow<RawRotationVector> {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: throw AttitudeNotAvailableException()

        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(
                        RawRotationVector(
                            values = event.values.toList(),
                            accuracy = event.accuracy,
                            timestampNanos = event.timestamp,
                        ),
                    )
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, sensor, samplingPeriodUs)
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }
}
