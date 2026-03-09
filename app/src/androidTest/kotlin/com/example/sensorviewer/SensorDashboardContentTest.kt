package com.example.sensorviewer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.ui.SensorDashboardContent
import com.example.sensorviewer.ui.SensorDashboardUiState
import org.junit.Rule
import org.junit.Test

/**
 * [SensorDashboardContent] の Compose UI テスト。
 *
 * ViewModel を経由せず [SensorDashboardContent] に直接 UiState を渡すことで、
 * UI の描画ロジックのみを検証する。
 */
class SensorDashboardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Loading 状態でプログレスインジケーターが表示される`() {
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Loading,
                snackbarHostState = SnackbarHostState(),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Loading")
            .assertIsDisplayed()
    }

    @Test
    fun `Success 状態でセンサー名が表示される`() {
        val states = listOf(
            UiSensorState(
                type = SensorType.Accelerometer,
                available = true,
                reading = SensorReading(
                    type = SensorType.Accelerometer,
                    values = listOf(1f, 2f, 3f),
                    timestampNs = 0L,
                ),
            ),
        )

        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(states),
                snackbarHostState = SnackbarHostState(),
            )
        }

        composeTestRule
            .onNodeWithText(SensorType.Accelerometer.label)
            .assertIsDisplayed()
    }

    @Test
    fun `Error 状態で Snackbar にエラーメッセージが表示される`() {
        val errorMessage = "センサーエラーが発生しました"

        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Error(errorMessage),
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun `利用不可センサーも一覧に表示される`() {
        val states = SensorType.all.map { type ->
            UiSensorState(type = type, available = false, reading = null)
        }

        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(states),
                snackbarHostState = SnackbarHostState(),
            )
        }

        SensorType.all.forEach { type ->
            composeTestRule.onNodeWithText(type.label).assertIsDisplayed()
        }
    }
}
