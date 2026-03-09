package com.example.sensorviewer.fake

import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.source.SensorDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * テスト用の [SensorDataSource] フェイク。
 *
 * - [setAvailable] で isAvailable の戻り値を制御する
 * - [emit] で指定センサーの Flow に値を流す
 */
class FakeSensorDataSource : SensorDataSource {

    private val availableTypes = mutableSetOf<SensorType>()
    private val channels = SensorType.all.associateWith {
        Channel<SensorReading>(Channel.UNLIMITED)
    }

    fun setAvailable(vararg types: SensorType) {
        availableTypes.clear()
        availableTypes.addAll(types.toList())
    }

    override fun isAvailable(type: SensorType): Boolean = type in availableTypes

    override fun observe(type: SensorType, samplingPeriodUs: Int): Flow<SensorReading> =
        channels[type]?.receiveAsFlow() ?: error("Unknown SensorType: $type")

    suspend fun emit(type: SensorType, reading: SensorReading) {
        channels[type]?.send(reading)
    }
}
