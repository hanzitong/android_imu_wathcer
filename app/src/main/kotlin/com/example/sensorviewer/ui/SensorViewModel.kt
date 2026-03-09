package com.example.sensorviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.usecase.ObserveSensorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * センサーダッシュボード画面の ViewModel。
 *
 * [ObserveSensorsUseCase] が返す `Flow<List<UiSensorState>>` を
 * Composable が直接購読できる [StateFlow]<[SensorDashboardUiState]> に変換して公開する。
 *
 * ## 画面回転への対応
 * [SharingStarted.WhileSubscribed] に 5 秒の猶予を設けることで、
 * 画面回転によって Composable が一時的に破棄されても上流 Flow（センサーリスナー）を
 * 保持し続け、再生成時の再登録コストを回避する。
 *
 * ## エラーハンドリング
 * エラーは [toUiState] の `catch` で捕捉し [SensorDashboardUiState.Error] に変換する。
 * エラー後は Flow が終了するため、UiState は Error のまま固定される。
 */
@HiltViewModel
class SensorViewModel @Inject constructor(
    observeSensors: ObserveSensorsUseCase,
) : ViewModel() {

    val uiState: StateFlow<SensorDashboardUiState> = observeSensors()
        .toUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = SensorDashboardUiState.Loading,
        )
}

// ── プライベートユーティリティ ────────────────────────────────────────────────

/**
 * `Flow<List<UiSensorState>>` を `Flow<SensorDashboardUiState>` にマッピングする拡張関数。
 *
 * - 正常値 → [SensorDashboardUiState.Success]
 * - 上流例外 → [SensorDashboardUiState.Error]（`catch` で捕捉し message を取り出す）
 *
 * ViewModel 本体のコードを薄く保つためにここに切り出している。
 */
private fun Flow<List<UiSensorState>>.toUiState(): Flow<SensorDashboardUiState> =
    map<List<UiSensorState>, SensorDashboardUiState> { SensorDashboardUiState.Success(it) }
        .catch { cause -> emit(SensorDashboardUiState.Error(cause.message ?: "Unknown error")) }
