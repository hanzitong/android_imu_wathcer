package com.example.sensorviewer.fake

import com.example.sensorviewer.model.UiAttitudeState
import com.example.sensorviewer.usecase.ObserveAttitudeUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * テスト用の [ObserveAttitudeUseCase] フェイク。
 *
 * [FakeObserveSensorsUseCase] と対称な設計。
 * [Channel]<[Result]<>> パターンで正常値とエラーを同一フローで制御する。
 */
class FakeObserveAttitudeUseCase : ObserveAttitudeUseCase {

    private val channel = Channel<Result<UiAttitudeState>>(Channel.UNLIMITED)

    override fun invoke(): Flow<UiAttitudeState> =
        channel.receiveAsFlow().map { it.getOrThrow() }

    suspend fun emit(state: UiAttitudeState) {
        channel.send(Result.success(state))
    }

    suspend fun emitError(cause: Throwable) {
        channel.send(Result.failure(cause))
    }
}
