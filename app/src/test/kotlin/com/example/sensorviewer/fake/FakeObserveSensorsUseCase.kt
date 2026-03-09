package com.example.sensorviewer.fake

import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.usecase.ObserveSensorsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.map

/**
 * テスト用の [ObserveSensorsUseCase] フェイク。
 *
 * [emit] で正常値、[emitError] でエラーをフローに流せる。
 * `Channel<Result<>>` を使うことで、正常値とエラーを同じフローで扱う。
 */
class FakeObserveSensorsUseCase : ObserveSensorsUseCase {

    private val events = Channel<Result<List<UiSensorState>>>(Channel.UNLIMITED)

    override fun invoke(): Flow<List<UiSensorState>> =
        events.receiveAsFlow().map { it.getOrThrow() }

    suspend fun emit(states: List<UiSensorState>) {
        events.send(Result.success(states))
    }

    suspend fun emitError(cause: Throwable) {
        events.send(Result.failure(cause))
    }
}
