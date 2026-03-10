package com.example.sensorviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorviewer.model.AttitudeReading
import com.example.sensorviewer.model.UiAttitudeState
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.usecase.ObserveAttitudeUseCase
import com.example.sensorviewer.usecase.ObserveSensorsUseCase
import com.example.sensorviewer.usecase.computeRelativeAttitude
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * センサーダッシュボード画面の ViewModel。
 *
 * [ObserveSensorsUseCase]・[ObserveAttitudeUseCase]・基準姿勢の 3 本を `combine` で合流させ、
 * Composable が購読できる [StateFlow]<[SensorDashboardUiState]> として公開する。
 */
@HiltViewModel
class SensorViewModel @Inject constructor(
    observeSensors: ObserveSensorsUseCase,
    observeAttitude: ObserveAttitudeUseCase,
) : ViewModel() {

    /** ユーザーが登録した基準姿勢。null = ENU 世界座標系をそのまま使用 */
    private val _reference = MutableStateFlow<AttitudeReading?>(null)

    val uiState: StateFlow<SensorDashboardUiState> =
        combine(observeSensors(), observeAttitude(), _reference, ::buildSuccessState)
            .catch { cause ->
                emit(SensorDashboardUiState.Error(cause.message ?: "Unknown error"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = SensorDashboardUiState.Loading,
            )

    /** 現在の世界座標系姿勢を基準として登録する */
    fun setReference() {
        _reference.value = (uiState.value as? SensorDashboardUiState.Success)
            ?.worldAttitude?.reading
    }

    /** 基準をクリアし世界座標系表示に戻す */
    fun clearReference() {
        _reference.value = null
    }
}

private fun buildSuccessState(
    sensors: List<UiSensorState>,
    rawAttitude: UiAttitudeState,
    reference: AttitudeReading?,
): SensorDashboardUiState = SensorDashboardUiState.Success(
    sensors = sensors,
    worldAttitude = rawAttitude,
    relativeAttitude = buildRelativeAttitude(rawAttitude, reference),
)

private fun buildRelativeAttitude(
    raw: UiAttitudeState,
    reference: AttitudeReading?,
): UiAttitudeState? {
    if (reference == null || raw.reading == null) return null
    return UiAttitudeState(
        available = raw.available,
        reading = computeRelativeAttitude(raw.reading, reference),
    )
}
