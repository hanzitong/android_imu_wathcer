package com.example.sensorviewer

import app.cash.turbine.test
import com.example.sensorviewer.model.AttitudeReading
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.model.UiAttitudeState
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.fake.FakeObserveAttitudeUseCase
import com.example.sensorviewer.fake.FakeObserveSensorsUseCase
import com.example.sensorviewer.testutil.MainDispatcherRule
import com.example.sensorviewer.ui.SensorDashboardUiState
import com.example.sensorviewer.ui.SensorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSensorsUseCase: FakeObserveSensorsUseCase
    private lateinit var fakeAttitudeUseCase: FakeObserveAttitudeUseCase
    private lateinit var viewModel: SensorViewModel

    private val defaultSensors = listOf(
        UiSensorState(
            type = SensorType.Accelerometer,
            available = true,
            reading = SensorReading(SensorType.Accelerometer, listOf(0f, 0f, 9.8f), 3, 0L),
        ),
    )
    private val identityMatrix = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    private val defaultAttitude = UiAttitudeState(
        available = true,
        reading = AttitudeReading(
            quaternion = listOf(0f, 0f, 0f, 1f),
            rotationMatrix = identityMatrix,
            eulerAngles = listOf(0f, 0f, 0f),
            accuracy = 3,
            timestampNanos = 0L,
        ),
    )

    @Before
    fun setUp() {
        fakeSensorsUseCase = FakeObserveSensorsUseCase()
        fakeAttitudeUseCase = FakeObserveAttitudeUseCase()
        viewModel = SensorViewModel(fakeSensorsUseCase, fakeAttitudeUseCase)
    }

    // ── 初期状態 ─────────────────────────────────────────────────────────────

    @Test
    fun `初期状態は Loading`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Success 遷移 ──────────────────────────────────────────────────────────

    @Test
    fun `両 UseCase が emit すると Success に遷移する`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())

            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)

            val success = awaitItem() as SensorDashboardUiState.Success
            assertEquals(defaultSensors, success.sensors)
            assertEquals(defaultAttitude, success.worldAttitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `センサー UseCase の連続 emit で sensors が最新値に更新される`() = runTest {
        val sensors2 = listOf(
            UiSensorState(SensorType.Accelerometer, available = true,
                reading = SensorReading(SensorType.Accelerometer, listOf(9f, 8f, 7f), 3, 1000L)),
        )

        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem()

            fakeSensorsUseCase.emit(sensors2)
            val second = awaitItem() as SensorDashboardUiState.Success
            assertEquals(sensors2, second.sensors)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Error 遷移 ────────────────────────────────────────────────────────────

    @Test
    fun `センサー UseCase がエラーを emit すると Error に遷移する`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emitError(RuntimeException("sensor error"))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertTrue(error.message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `姿勢 UseCase がエラーを emit すると Error に遷移する`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeAttitudeUseCase.emitError(RuntimeException("attitude error"))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertTrue(error.message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `例外メッセージが Error#message に正確に伝播する`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emitError(RuntimeException("gyroscope disconnected"))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertEquals("gyroscope disconnected", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `例外メッセージが null のとき Error#message は Unknown error になる`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emitError(RuntimeException(null as String?))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertEquals("Unknown error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── StateFlow のメモリ挙動 ────────────────────────────────────────────────

    @Test
    fun `同じ値の連続 emit では StateFlow に新しい item は来ない`() = runTest {
        // StateFlow は equals で前後を比較し、変化がなければ subscriber に emit しない。
        // SensorDashboardUiState.Success は data class なので同じフィールド値なら equals = true。
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem() // 最初の Success

            // まったく同じ sensors 値を再 emit → buildSuccessState が同じ Success を返す
            fakeSensorsUseCase.emit(defaultSensors)

            // StateFlow が dedup するため新しい item は届かない
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── WhileSubscribed のスレッド・メモリ挙動 ────────────────────────────────

    @Test
    fun `購読者がいなくなっても WhileSubscribed の猶予時間内は StateFlow が最後の値を保持する`() = runTest {
        // WhileSubscribed(5_000) は購読者ゼロになってから 5 秒間は上流を維持する。
        // StateFlow は上流が停止しても最後の値を保持し続ける。
        val collector = launch { viewModel.uiState.collect {} }
        fakeSensorsUseCase.emit(defaultSensors)
        fakeAttitudeUseCase.emit(defaultAttitude)
        advanceUntilIdle()

        val lastState = viewModel.uiState.value
        assertTrue(lastState is SensorDashboardUiState.Success)

        collector.cancel() // 購読者ゼロになる
        advanceTimeBy(4_000) // 5 秒タイムアウトの手前（4 秒経過）

        // StateFlow はまだ最後の値を保持している
        assertEquals(lastState, viewModel.uiState.value)
    }

    @Test
    fun `再購読すると StateFlow の最後の値が即座に届く（Loading には戻らない）`() = runTest {
        // StateFlow は常に最新値を 1 つ保持し、新規 subscriber に即座に replay する。
        // 画面回転後の再購読では、Loading を経由せず直前の Success が届く。
        val collector = launch { viewModel.uiState.collect {} }
        fakeSensorsUseCase.emit(defaultSensors)
        fakeAttitudeUseCase.emit(defaultAttitude)
        advanceUntilIdle()

        val successState = viewModel.uiState.value as SensorDashboardUiState.Success
        collector.cancel()

        // 再購読: StateFlow は最後の値（Success）を即座に replay する
        viewModel.uiState.test {
            val first = awaitItem()
            // Loading ではなく Success が最初に届く
            assertEquals(successState, first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── 基準座標系 ────────────────────────────────────────────────────────────

    @Test
    fun `基準設定前は relativeAttitude が null`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)

            val success = awaitItem() as SensorDashboardUiState.Success
            assertNull(success.relativeAttitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setReference 後は relativeAttitude が非 null になる`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem() // Success（基準なし）

            viewModel.setReference()
            val withRef = awaitItem() as SensorDashboardUiState.Success
            assertNotNull(withRef.relativeAttitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearReference 後は relativeAttitude が null に戻る`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem()

            viewModel.setReference()
            awaitItem() // relativeAttitude あり

            viewModel.clearReference()
            val cleared = awaitItem() as SensorDashboardUiState.Success
            assertNull(cleared.relativeAttitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setReference を Loading 中に呼んでも reference は登録されない`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())

            // Success でない状態で setReference を呼んでも何も起きない
            viewModel.setReference()

            // その後 Success に遷移しても relativeAttitude は null のまま
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)

            val success = awaitItem() as SensorDashboardUiState.Success
            assertNull(success.relativeAttitude)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setReference 後に worldAttitude が更新されると relativeAttitude も再計算される`() = runTest {
        // defaultAttitude（単位クォータニオン）とは異なる姿勢
        val rotatedAttitude = UiAttitudeState(
            available = true,
            reading = AttitudeReading(
                quaternion = listOf(0f, 0f, 0.7071f, 0.7071f), // Z 軸周り 90 度
                rotationMatrix = identityMatrix,
                eulerAngles = listOf(0f, 0f, 0f),
                accuracy = 3,
                timestampNanos = 1000L,
            ),
        )

        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem()

            viewModel.setReference() // defaultAttitude を基準に登録
            val withRef = awaitItem() as SensorDashboardUiState.Success
            val initialQ = withRef.relativeAttitude!!.reading!!.quaternion

            // worldAttitude を別の姿勢に更新
            fakeAttitudeUseCase.emit(rotatedAttitude)
            val updated = awaitItem() as SensorDashboardUiState.Success

            // worldAttitude が更新されている
            assertEquals(rotatedAttitude, updated.worldAttitude)
            // relativeAttitude も再計算されている（基準からのずれが生じる）
            val updatedQ = updated.relativeAttitude!!.reading!!.quaternion
            assertNotEquals(initialQ, updatedQ)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearReference 後に setReference を呼ぶと新しい worldAttitude が基準になる`() = runTest {
        val newAttitude = UiAttitudeState(
            available = true,
            reading = AttitudeReading(
                quaternion = listOf(0.5f, 0.5f, 0.5f, 0.5f),
                rotationMatrix = identityMatrix,
                eulerAngles = listOf(0f, 0f, 0f),
                accuracy = 3,
                timestampNanos = 9999L,
            ),
        )

        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem()

            viewModel.setReference() // 1 回目の基準登録（defaultAttitude）
            awaitItem()
            viewModel.clearReference()
            awaitItem() // relativeAttitude = null

            // 別の姿勢に更新してから再 setReference
            fakeAttitudeUseCase.emit(newAttitude)
            awaitItem()

            viewModel.setReference() // newAttitude を新しい基準に登録
            val withNewRef = awaitItem() as SensorDashboardUiState.Success

            // 自分自身（newAttitude）を基準にすると相対回転はゼロ → 単位クォータニオン
            val q = withNewRef.relativeAttitude!!.reading!!.quaternion
            val DELTA = 1e-5f
            assertTrue(kotlin.math.abs(q[3] - 1f) < DELTA)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setReference 直後の relativeAttitude の quaternion は単位クォータニオンに近い`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeSensorsUseCase.emit(defaultSensors)
            fakeAttitudeUseCase.emit(defaultAttitude)
            awaitItem()

            viewModel.setReference()
            val withRef = awaitItem() as SensorDashboardUiState.Success
            val q = withRef.relativeAttitude!!.reading!!.quaternion

            // 自分自身を基準にすると相対回転はゼロ（単位クォータニオン）
            val DELTA = 1e-5f
            assertTrue(kotlin.math.abs(q[0]) < DELTA)
            assertTrue(kotlin.math.abs(q[1]) < DELTA)
            assertTrue(kotlin.math.abs(q[2]) < DELTA)
            assertTrue(kotlin.math.abs(q[3] - 1f) < DELTA)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
