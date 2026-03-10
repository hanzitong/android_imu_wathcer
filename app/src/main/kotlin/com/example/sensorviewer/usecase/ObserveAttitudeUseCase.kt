package com.example.sensorviewer.usecase

import com.example.sensorviewer.model.UiAttitudeState
import kotlinx.coroutines.flow.Flow

/**
 * 姿勢センサーの表示状態を [Flow] で返す UseCase。
 */
interface ObserveAttitudeUseCase {
    operator fun invoke(): Flow<UiAttitudeState>
}
