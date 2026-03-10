package com.example.sensorviewer.fake

import com.example.sensorviewer.source.AttitudeDataSource
import com.example.sensorviewer.source.RawRotationVector
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * テスト用の [AttitudeDataSource] フェイク。
 *
 * - [setAvailable] で isAvailable() の戻り値を制御する
 * - [emit] で Flow に [RawRotationVector] を流す
 * - [emitError] で Flow 内に例外を発生させる（[Channel]<[Result]<>> パターン）
 */
class FakeAttitudeDataSource : AttitudeDataSource {

    private var available = false
    private val channel = Channel<Result<RawRotationVector>>(Channel.UNLIMITED)

    fun setAvailable(value: Boolean) {
        available = value
    }

    override fun isAvailable(): Boolean = available

    override fun observe(samplingPeriodUs: Int): Flow<RawRotationVector> =
        channel.receiveAsFlow().map { it.getOrThrow() }

    suspend fun emit(values: List<Float>, accuracy: Int = 3, timestampNanos: Long = 0L) {
        channel.send(Result.success(RawRotationVector(values, accuracy, timestampNanos)))
    }

    suspend fun emitError(cause: Throwable) {
        channel.send(Result.failure(cause))
    }
}
