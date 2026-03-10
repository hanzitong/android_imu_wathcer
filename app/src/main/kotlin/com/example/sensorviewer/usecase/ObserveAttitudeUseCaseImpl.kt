package com.example.sensorviewer.usecase

import com.example.sensorviewer.model.AttitudeReading
import com.example.sensorviewer.model.UiAttitudeState
import com.example.sensorviewer.source.AttitudeDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ObserveAttitudeUseCase] の実装。
 *
 * [AttitudeDataSource] から生の回転ベクトル値を受け取り、以下の変換を順に行う:
 * 1. w 成分の再構築（一部デバイスで event.values[3] が省略されるため）
 * 2. クォータニオン → 3×3 回転行列
 * 3. 回転行列 → オイラー角 [azimuth, pitch, roll]（ラジアン）
 *
 * センサーが利用不可の場合は [UiAttitudeState](available=false) を即時 emit して終了する。
 */
class ObserveAttitudeUseCaseImpl @Inject constructor(
    private val dataSource: AttitudeDataSource,
) : ObserveAttitudeUseCase {

    override operator fun invoke(): Flow<UiAttitudeState> {
        if (!dataSource.isAvailable()) {
            return flowOf(UiAttitudeState(available = false))
        }

        return dataSource.observe().map { raw ->
            val quaternion    = extractQuaternion(raw.values)
            val rotationMatrix = quaternionToRotationMatrix(quaternion)
            val eulerAngles   = rotationMatrixToEulerAngles(rotationMatrix)

            UiAttitudeState(
                available = true,
                reading = AttitudeReading(
                    quaternion     = quaternion,
                    rotationMatrix = rotationMatrix,
                    eulerAngles    = eulerAngles,
                    accuracy       = raw.accuracy,
                    timestampNanos = raw.timestampNanos,
                ),
            )
        }
    }
}
