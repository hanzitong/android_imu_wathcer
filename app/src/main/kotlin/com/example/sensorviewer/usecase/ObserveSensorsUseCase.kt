package com.example.sensorviewer.usecase

import com.example.sensorviewer.model.UiSensorState
import kotlinx.coroutines.flow.Flow

/**
 * 全センサーの表示状態を [Flow] で返す UseCase。
 *
 * 呼び出すと [UiSensorState] のリストを継続的に emit する cold Flow が返る。
 * リストは [com.example.sensorviewer.model.SensorType.all] の順序を保ち、
 * センサーが利用不可の場合も要素として含まれる（`available = false`）。
 *
 * ## 実装 ([ObserveSensorsUseCaseImpl]) の動作
 * 1. 利用可能センサーを [com.example.sensorviewer.source.SensorDataSource.isAvailable] で決定する
 * 2. 各センサーの Flow を `combine` で合流させ、全センサーの最新値を一括 emit する
 * 3. エラーが発生した場合は下流の `catch` で捕捉され
 *    [com.example.sensorviewer.ui.SensorDashboardUiState.Error] に変換される
 *
 * ## テスト
 * [com.example.sensorviewer.fake.FakeObserveSensorsUseCase] で差し替えることで
 * ViewModel を単体テスト可能にする。
 */
interface ObserveSensorsUseCase {
    operator fun invoke(): Flow<List<UiSensorState>>
}
