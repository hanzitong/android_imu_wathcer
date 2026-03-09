package com.example.sensorviewer.ui

import com.example.sensorviewer.model.UiSensorState

/**
 * センサーダッシュボード画面の UI ステート。
 *
 * ViewModel が [kotlinx.coroutines.flow.StateFlow] で公開し、
 * Composable が collect して描画に使用する。
 */
sealed interface SensorDashboardUiState {

    /** センサーデータの初回取得待ち */
    data object Loading : SensorDashboardUiState

    /**
     * センサーデータ取得中 (正常系)
     *
     * @property sensors 表示対象のセンサー状態リスト。[com.example.sensorviewer.model.SensorType.all] の順
     */
    data class Success(
        val sensors: List<UiSensorState>,
    ) : SensorDashboardUiState

    /**
     * エラー発生
     *
     * @property message ユーザーに表示するエラーメッセージ
     */
    data class Error(
        val message: String,
    ) : SensorDashboardUiState
}
