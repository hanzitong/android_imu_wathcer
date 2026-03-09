package com.example.sensorviewer

import android.hardware.SensorManager
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * [SensorReading] の純粋ロジックテスト。
 *
 * テスト対象:
 * - [SensorReading.accuracyLabel]: accuracy 定数 → 可読文字列への変換
 * - `equals` / `hashCode`: `List<Float>` を使っているため値比較が正しく機能することを確認
 *   （`FloatArray` では参照比較になり Compose の再コンポーズ最適化が壊れる）
 */
class SensorReadingTest {

    // ── accuracyLabel ─────────────────────────────────────────────────────────

    @Test
    fun `accuracyLabel が accuracy 定数を正しい文字列に変換する`() {
        val cases = listOf(
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH   to "High",
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM to "Medium",
            SensorManager.SENSOR_STATUS_ACCURACY_LOW    to "Low",
            SensorManager.SENSOR_STATUS_UNRELIABLE      to "Unreliable",
            -1                                          to "Unknown",
            99                                          to "Unknown",
        )
        cases.forEach { (accuracy, expected) ->
            assertEquals("accuracy=$accuracy のとき", expected, SensorReading.accuracyLabel(accuracy))
        }
    }

    // ── equals / hashCode ────────────────────────────────────────────────────

    @Test
    fun `同じ値を持つ SensorReading は等しい`() {
        val a = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
        val b = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `values が異なる SensorReading は等しくない`() {
        val a = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
        val b = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 9f), 3, 0L)
        assertNotEquals(a, b)
    }

    @Test
    fun `timestampNanos が異なる SensorReading は等しくない`() {
        val a = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
        val b = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 1L)
        assertNotEquals(a, b)
    }

    @Test
    fun `accuracy が異なる SensorReading は等しくない`() {
        val a = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
        val b = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 1, 0L)
        assertNotEquals(a, b)
    }
}
