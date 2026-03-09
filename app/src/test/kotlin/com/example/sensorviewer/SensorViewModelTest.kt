package com.example.sensorviewer

import app.cash.turbine.test
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.fake.FakeObserveSensorsUseCase
import com.example.sensorviewer.testutil.MainDispatcherRule
import com.example.sensorviewer.ui.SensorDashboardUiState
import com.example.sensorviewer.ui.SensorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * [SensorViewModel] の状態遷移テスト。
 *
 * [FakeObserveSensorsUseCase] で UseCase をスタブし、emit タイミングと
 * [SensorDashboardUiState] の変化が一致することを Flow（Turbine）で検証する。
 *
 * [MainDispatcherRule] により `viewModelScope`（`Dispatchers.Main.immediate` を使用）を
 * JVM 上で動作させる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeUseCase: FakeObserveSensorsUseCase
    private lateinit var viewModel: SensorViewModel

    @Before
    fun setUp() {
        fakeUseCase = FakeObserveSensorsUseCase()
        viewModel = SensorViewModel(fakeUseCase)
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
    fun `UseCase がセンサーリストを emit すると Success に遷移する`() = runTest {
        val states = listOf(
            UiSensorState(
                type = SensorType.Accelerometer,
                available = true,
                reading = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 0, 0L),
            ),
        )

        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeUseCase.emit(states)
            val success = awaitItem() as SensorDashboardUiState.Success
            assertEquals(states, success.sensors)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `連続した emit で Success の sensors が最新値に更新される`() = runTest {
        val states1 = listOf(
            UiSensorState(SensorType.Accelerometer, available = true,
                reading = SensorReading(SensorType.Accelerometer, listOf(1f, 0f, 0f), 0, 0L)),
        )
        val states2 = listOf(
            UiSensorState(SensorType.Accelerometer, available = true,
                reading = SensorReading(SensorType.Accelerometer, listOf(9f, 8f, 7f), 3, 1000L)),
        )

        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())

            fakeUseCase.emit(states1)
            val first = awaitItem() as SensorDashboardUiState.Success
            assertEquals(states1, first.sensors)

            fakeUseCase.emit(states2)
            val second = awaitItem() as SensorDashboardUiState.Success
            assertEquals(states2, second.sensors)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Error 遷移 ────────────────────────────────────────────────────────────

    @Test
    fun `UseCase がエラーを emit すると Error に遷移する`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeUseCase.emitError(RuntimeException("sensor error"))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertTrue(error.message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `例外メッセージが Error#message に正確に伝播する`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeUseCase.emitError(RuntimeException("gyroscope disconnected"))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertEquals("gyroscope disconnected", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `例外メッセージが null のとき Error#message は Unknown error になる`() = runTest {
        viewModel.uiState.test {
            assertEquals(SensorDashboardUiState.Loading, awaitItem())
            fakeUseCase.emitError(RuntimeException(null as String?))
            val error = awaitItem() as SensorDashboardUiState.Error
            assertEquals("Unknown error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
