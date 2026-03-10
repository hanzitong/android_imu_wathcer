package com.example.sensorviewer

import app.cash.turbine.test
import com.example.sensorviewer.model.SensorReading
import com.example.sensorviewer.model.SensorType
import com.example.sensorviewer.usecase.ObserveSensorsUseCaseImpl
import com.example.sensorviewer.fake.FakeSensorDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ObserveSensorsUseCaseImpl] のロジックテスト。
 *
 * [FakeSensorDataSource] で DataSource をスタブし、以下を検証する:
 * - `available` フラグの合成ロジック（`isAvailable` の結果を反映しているか）
 * - `combine` のセマンティクス（全センサーが初回値を送るまで emit しない）
 * - 出力リストの順序が [com.example.sensorviewer.model.SensorType.all] と一致するか
 * - センサーなし端末の特殊ケース（即時 emit）
 * - SensorReading のデータが欠損なく伝播するか
 */
class ObserveSensorsUseCaseTest {

    private lateinit var fakeDataSource: FakeSensorDataSource
    private lateinit var useCase: ObserveSensorsUseCaseImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeSensorDataSource()
        useCase = ObserveSensorsUseCaseImpl(fakeDataSource)
    }

    // ── available / unavailable の基本 ────────────────────────────────────────

    @Test
    fun `利用可能なセンサーは available=true で返る`() = runTest {
        fakeDataSource.setAvailable(SensorType.Accelerometer)

        useCase().test {
            val reading = SensorReading(SensorType.Accelerometer, listOf(0f, 0f, 9.8f), 0, 0L)
            fakeDataSource.emit(SensorType.Accelerometer, reading)

            val states = awaitItem()
            val accel = states.first { it.type == SensorType.Accelerometer }
            assertTrue(accel.available)
            assertEquals(reading, accel.reading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `利用不可のセンサーは available=false かつ reading=null で返る`() = runTest {
        fakeDataSource.setAvailable(SensorType.Accelerometer)

        useCase().test {
            fakeDataSource.emit(SensorType.Accelerometer,
                SensorReading(SensorType.Accelerometer, listOf(0f, 0f, 9.8f), 0, 0L))

            val states = awaitItem()
            val gyro = states.first { it.type == SensorType.Gyroscope }
            assertFalse(gyro.available)
            assertNull(gyro.reading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── センサーなし端末の特殊ケース ──────────────────────────────────────────

    @Test
    fun `全センサー利用不可の場合は即座に全て unavailable を emit する`() = runTest {
        // setAvailable を呼ばない → 全てのセンサーが利用不可

        useCase().test {
            // fakeDataSource.emit を呼ばなくても即座に値が来る
            val states = awaitItem()

            assertEquals(SensorType.all.size, states.size)
            assertTrue(states.all { !it.available })
            assertTrue(states.all { it.reading == null })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── リスト順序 ────────────────────────────────────────────────────────────

    @Test
    fun `出力リストの順序は SensorType#all と一致する`() = runTest {
        fakeDataSource.setAvailable(SensorType.Accelerometer)

        useCase().test {
            fakeDataSource.emit(SensorType.Accelerometer,
                SensorReading(SensorType.Accelerometer, listOf(0f, 0f, 0f), 0, 0L))

            val types = awaitItem().map { it.type }
            assertEquals(SensorType.all, types)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── combine のセマンティクス ───────────────────────────────────────────────

    @Test
    fun `複数センサーが全て初回値を送るまで emit しない`() = runTest {
        fakeDataSource.setAvailable(SensorType.Accelerometer, SensorType.Gyroscope)

        val accelReading = SensorReading(SensorType.Accelerometer, listOf(1f, 0f, 0f), 0, 100L)
        val gyroReading  = SensorReading(SensorType.Gyroscope,     listOf(0f, 0f, 0.1f), 3, 200L)

        useCase().test {
            // Accelerometer だけ emit した時点では combine がまだ fire しない
            fakeDataSource.emit(SensorType.Accelerometer, accelReading)
            // Gyroscope も emit して初めて combine が fire する
            fakeDataSource.emit(SensorType.Gyroscope, gyroReading)

            val states = awaitItem()
            assertEquals(accelReading, states.first { it.type == SensorType.Accelerometer }.reading)
            assertEquals(gyroReading,  states.first { it.type == SensorType.Gyroscope }.reading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── 値の更新 ──────────────────────────────────────────────────────────────

    @Test
    fun `センサー値が更新されると新しい emit が来る`() = runTest {
        fakeDataSource.setAvailable(SensorType.Accelerometer)

        val reading1 = SensorReading(SensorType.Accelerometer, listOf(0f, 0f, 9.8f), 3, 0L)
        val reading2 = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f),   3, 1_000_000L)

        useCase().test {
            fakeDataSource.emit(SensorType.Accelerometer, reading1)
            val first = awaitItem().first { it.type == SensorType.Accelerometer }
            assertEquals(reading1, first.reading)

            fakeDataSource.emit(SensorType.Accelerometer, reading2)
            val second = awaitItem().first { it.type == SensorType.Accelerometer }
            assertEquals(reading2, second.reading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── データの完全性 ────────────────────────────────────────────────────────

    @Test
    fun `SensorReading の accuracy と timestampNanos が正しく伝播する`() = runTest {
        fakeDataSource.setAvailable(SensorType.Barometer)

        val reading = SensorReading(
            type = SensorType.Barometer,
            values = listOf(1013.25f),
            accuracy = 3,
            timestampNanos = 123_456_789L,
        )

        useCase().test {
            fakeDataSource.emit(SensorType.Barometer, reading)

            val state = awaitItem().first { it.type == SensorType.Barometer }
            assertEquals(3, state.reading?.accuracy)
            assertEquals(123_456_789L, state.reading?.timestampNanos)
            assertEquals(listOf(1013.25f), state.reading?.values)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `一方のセンサーが更新されると他センサーの最新値も同じ emit に含まれる`() = runTest {
        // combine は「どれか 1 本が更新されたとき、全員の最新値を合わせて emit する」。
        // Gyroscope の値は更新していないが、Accelerometer 更新の emit に含まれ続ける。
        fakeDataSource.setAvailable(SensorType.Accelerometer, SensorType.Gyroscope)

        val accel1 = SensorReading(SensorType.Accelerometer, listOf(1f, 0f, 0f), 3, 0L)
        val gyro   = SensorReading(SensorType.Gyroscope,     listOf(0f, 1f, 0f), 3, 0L)
        val accel2 = SensorReading(SensorType.Accelerometer, listOf(2f, 0f, 0f), 3, 1_000L)

        useCase().test {
            fakeDataSource.emit(SensorType.Accelerometer, accel1)
            fakeDataSource.emit(SensorType.Gyroscope, gyro)
            awaitItem() // 初回 emit（accel1 + gyro が揃う）

            // Accelerometer だけ更新する
            fakeDataSource.emit(SensorType.Accelerometer, accel2)
            val second = awaitItem()

            // Accelerometer は accel2 に更新されている
            assertEquals(accel2, second.first { it.type == SensorType.Accelerometer }.reading)
            // Gyroscope は更新していないが、前回の値が引き続き保持されている
            assertEquals(gyro, second.first { it.type == SensorType.Gyroscope }.reading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `複数センサーの reading がそれぞれ正しいセンサーにマッピングされる`() = runTest {
        fakeDataSource.setAvailable(
            SensorType.Accelerometer,
            SensorType.Gyroscope,
            SensorType.Magnetometer,
        )

        val accelR = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
        val gyroR  = SensorReading(SensorType.Gyroscope,     listOf(4f, 5f, 6f), 2, 0L)
        val magR   = SensorReading(SensorType.Magnetometer,  listOf(7f, 8f, 9f), 1, 0L)

        useCase().test {
            fakeDataSource.emit(SensorType.Accelerometer, accelR)
            fakeDataSource.emit(SensorType.Gyroscope, gyroR)
            fakeDataSource.emit(SensorType.Magnetometer, magR)

            val states = awaitItem()
            assertEquals(accelR, states.first { it.type == SensorType.Accelerometer }.reading)
            assertEquals(gyroR,  states.first { it.type == SensorType.Gyroscope }.reading)
            assertEquals(magR,   states.first { it.type == SensorType.Magnetometer }.reading)
            // 利用不可センサーは null のまま
            assertNull(states.first { it.type == SensorType.Barometer }.reading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
