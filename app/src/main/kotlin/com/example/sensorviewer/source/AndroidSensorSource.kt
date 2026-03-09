package com.example.sensorviewer.source

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * [SensorDataSource] の Android 実装。
 *
 * [SensorManager] の [SensorEventListener] を `callbackFlow` でラップし、
 * Flow の collect 開始でリスナー登録、キャンセルで自動解除する。
 */
class AndroidSensorSource @Inject constructor(
    private val sensorManager: SensorManager,
) : SensorDataSource {

    override fun isAvailable(type: SensorType): Boolean =
        sensorManager.getDefaultSensor(type.androidConstant) != null

    override fun observe(type: SensorType, samplingPeriodUs: Int): Flow<SensorReading> {
        val sensor = sensorManager.getDefaultSensor(type.androidConstant)
            ?: throw SensorNotAvailableException(type)

        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(
                        SensorReading(
                            type = type,
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
