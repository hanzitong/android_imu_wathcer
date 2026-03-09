package com.example.sensorviewer.usecase

import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.source.SensorDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ObserveSensorsUseCase] の実装。
 *
 * [SensorDataSource] から取得した計測値と利用可否フラグを合成し、
 * UI が直接消費できる [UiSensorState] のリストを [Flow] で返す。
 *
 * ## emit タイミング
 * `combine` を使っているため、利用可能な**全センサーが初回値を送って初めて** emit する。
 * それ以降はどれか 1 つのセンサーが更新されるたびに最新の Map を使って emit する。
 *
 * ## 将来の拡張ポイント
 * - 単位変換（例: hPa → inHg）
 * - ローパスフィルタ
 * - 異常値検知
 */
class ObserveSensorsUseCaseImpl @Inject constructor(
    private val dataSource: SensorDataSource,
) : ObserveSensorsUseCase {

    override operator fun invoke(): Flow<List<UiSensorState>> {
        // isAvailable はアプリ起動中に変化しないため、ここで一度だけ評価する。
        val availableTypes = SensorType.all.filter { dataSource.isAvailable(it) }.toSet()

        // センサーが 1 つもない端末: combine を使わず即座に全 unavailable を emit して終了。
        if (availableTypes.isEmpty()) {
            return flowOf(SensorType.all.map { UiSensorState(it, available = false, reading = null) })
        }

        // 利用可能センサーごとに「どのセンサーの値か」を Pair に包んでから combine で合流する。
        // combine は Array<Pair> を受け取るので toMap() で Map に変換してから UiSensorState を組み立てる。
        val perSensorFlows = availableTypes.map { type ->
            dataSource.observe(type).map { reading -> type to reading }
        }

        return combine(perSensorFlows) { latestPairs ->
            val latestReadings = latestPairs.toMap()
            SensorType.all.map { type ->
                UiSensorState(
                    type = type,
                    available = type in availableTypes,
                    reading = latestReadings[type],
                )
            }
        }
    }
}
