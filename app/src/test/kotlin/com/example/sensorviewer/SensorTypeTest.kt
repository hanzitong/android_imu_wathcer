package com.example.sensorviewer

/**
 * [com.example.sensorviewer.data.model.SensorType] の構造テスト。
 *
 * テスト対象:
 * - `SensorType.all` の件数・重複・Android 定数の正確性
 * - 各センサーの軸数ルール（3軸 / 単軸）
 * - label / unit の非空チェック
 *
 * これらは実装の誤字・コピペミス・定数の取り違えを早期に検出するためのガード。
 */

import android.hardware.Sensor
import com.example.sensorviewer.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorTypeTest {

    // ── SensorType.all の構造 ─────────────────────────────────────────────────

    @Test
    fun `all には 6 種のセンサーが含まれる`() {
        assertEquals(6, SensorType.all.size)
    }

    @Test
    fun `all に重複する androidConstant はない`() {
        val constants = SensorType.all.map { it.androidConstant }
        assertEquals(constants.distinct(), constants)
    }

    @Test
    fun `all に重複する label はない`() {
        val labels = SensorType.all.map { it.label }
        assertEquals(labels.distinct(), labels)
    }

    // ── 各センサーの axes 数 ──────────────────────────────────────────────────

    @Test
    fun `3 軸センサーは axes が 3 要素`() {
        listOf(SensorType.Accelerometer, SensorType.Gyroscope, SensorType.Magnetometer)
            .forEach { type ->
                assertEquals("${type.label} は 3 軸のはず", 3, type.axes.size)
            }
    }

    @Test
    fun `単軸センサーは axes が 1 要素`() {
        listOf(SensorType.Barometer, SensorType.AmbientLight, SensorType.Proximity)
            .forEach { type ->
                assertEquals("${type.label} は 1 軸のはず", 1, type.axes.size)
            }
    }

    // ── 各センサーの Android 定数 ─────────────────────────────────────────────

    @Test
    fun `各センサーの androidConstant が Android SDK の定数と一致する`() {
        assertEquals(Sensor.TYPE_ACCELEROMETER,   SensorType.Accelerometer.androidConstant)
        assertEquals(Sensor.TYPE_GYROSCOPE,       SensorType.Gyroscope.androidConstant)
        assertEquals(Sensor.TYPE_MAGNETIC_FIELD,  SensorType.Magnetometer.androidConstant)
        assertEquals(Sensor.TYPE_PRESSURE,        SensorType.Barometer.androidConstant)
        assertEquals(Sensor.TYPE_LIGHT,           SensorType.AmbientLight.androidConstant)
        assertEquals(Sensor.TYPE_PROXIMITY,       SensorType.Proximity.androidConstant)
    }

    // ── label / unit の基本チェック ───────────────────────────────────────────

    @Test
    fun `全センサーの label は空でない`() {
        SensorType.all.forEach { type ->
            assertTrue("${type.label} は空であるべきでない", type.label.isNotBlank())
        }
    }

    @Test
    fun `全センサーの unit は空でない`() {
        SensorType.all.forEach { type ->
            assertTrue("${type.label} の unit は空であるべきでない", type.unit.isNotBlank())
        }
    }
}
