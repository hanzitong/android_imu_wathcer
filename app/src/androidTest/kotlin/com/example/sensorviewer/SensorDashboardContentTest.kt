package com.example.sensorviewer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.sensorviewer.model.AttitudeReading
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.model.UiAttitudeState
import com.example.sensorviewer.model.UiSensorState
import com.example.sensorviewer.ui.SensorDashboardContent
import com.example.sensorviewer.ui.SensorDashboardUiState
import org.junit.Rule
import org.junit.Test

class SensorDashboardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val identityMatrix = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    private val identityAttitude = UiAttitudeState(
        available = true,
        reading = AttitudeReading(
            quaternion = listOf(0f, 0f, 0f, 1f),
            rotationMatrix = identityMatrix,
            eulerAngles = listOf(0f, 0f, 0f),
            accuracy = 3,
            timestampNanos = 0L,
        ),
    )

    // ── 既存テスト ────────────────────────────────────────────────────────────

    @Test
    fun `Loading 状態でプログレスインジケーターが表示される`() {
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Loading,
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun `Success 状態でセンサー名が表示される`() {
        val states = listOf(
            UiSensorState(
                type = SensorType.Accelerometer,
                available = true,
                reading = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L),
            ),
        )
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(sensors = states),
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithText(SensorType.Accelerometer.label).assertIsDisplayed()
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
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun `利用不可センサーも一覧に表示される`() {
        val states = SensorType.all.map { type ->
            UiSensorState(type = type, available = false, reading = null)
        }
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(sensors = states),
                snackbarHostState = SnackbarHostState(),
            )
        }
        SensorType.all.forEach { type ->
            composeTestRule.onNodeWithText(type.label).assertIsDisplayed()
        }
    }

    // ── 姿勢カード ────────────────────────────────────────────────────────────

    @Test
    fun `世界座標系カードが常に表示される`() {
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(
                    sensors = emptyList(),
                    worldAttitude = identityAttitude,
                ),
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithText("Attitude (World)").assertIsDisplayed()
    }

    @Test
    fun `基準未設定時は Set Ref ボタンが表示され Custom Reference カードは表示されない`() {
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(
                    sensors = emptyList(),
                    worldAttitude = identityAttitude,
                    relativeAttitude = null,
                ),
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithText("Set Ref").assertIsDisplayed()
        composeTestRule.onNodeWithText("Attitude (Custom Reference)").assertDoesNotExist()
    }

    @Test
    fun `基準設定後は Custom Reference カードと Reset ボタンが表示される`() {
        composeTestRule.setContent {
            SensorDashboardContent(
                uiState = SensorDashboardUiState.Success(
                    sensors = emptyList(),
                    worldAttitude = identityAttitude,
                    relativeAttitude = identityAttitude,
                ),
                snackbarHostState = SnackbarHostState(),
            )
        }
        composeTestRule.onNodeWithText("Attitude (Custom Reference)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
    }
}
