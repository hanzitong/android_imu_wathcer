package com.example.sensorviewer

import app.cash.turbine.test
import com.example.sensorviewer.fake.FakeAttitudeDataSource
import com.example.sensorviewer.usecase.ObserveAttitudeUseCaseImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ObserveAttitudeUseCaseImpl] のロジックテスト。
 *
 * [FakeAttitudeDataSource] で DataSource をスタブし、以下を検証する:
 * - available フラグの伝播
 * - センサー利用不可時の即時 emit
 * - AttitudeReading の各フィールドが正しく計算・伝播されるか
 * - リアルタイム更新（値が更新されると新しい emit が来るか）
 */
class ObserveAttitudeUseCaseTest {

    private lateinit var fakeDataSource: FakeAttitudeDataSource
    private lateinit var useCase: ObserveAttitudeUseCaseImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeAttitudeDataSource()
        useCase = ObserveAttitudeUseCaseImpl(fakeDataSource)
    }

    // ── 利用可否 ──────────────────────────────────────────────────────────────

    @Test
    fun `センサーが利用不可の場合は available=false を即時 emit して終了する`() = runTest {
        // setAvailable を呼ばない → 利用不可

        useCase().test {
            val state = awaitItem()
            assertFalse(state.available)
            assertNull(state.reading)
            awaitComplete()
        }
    }

    @Test
    fun `センサーが利用可能かつ値が来ると available=true で reading が設定される`() = runTest {
        fakeDataSource.setAvailable(true)

        useCase().test {
            // 単位クォータニオン (0, 0, 0, 1) に相当する回転ベクトル値
            fakeDataSource.emit(listOf(0f, 0f, 0f, 1f))

            val state = awaitItem()
            assertTrue(state.available)
            assertNotNull(state.reading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── データの完全性 ────────────────────────────────────────────────────────

    @Test
    fun `accuracy と timestampNanos が正しく伝播する`() = runTest {
        fakeDataSource.setAvailable(true)

        useCase().test {
            fakeDataSource.emit(
                values = listOf(0f, 0f, 0f, 1f),
                accuracy = 3,
                timestampNanos = 123_456_789L,
            )

            val reading = awaitItem().reading!!
            assertEquals(3, reading.accuracy)
            assertEquals(123_456_789L, reading.timestampNanos)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quaternion が4要素で返る`() = runTest {
        fakeDataSource.setAvailable(true)

        useCase().test {
            fakeDataSource.emit(listOf(0f, 0f, 0f, 1f))

            val reading = awaitItem().reading!!
            assertEquals(4, reading.quaternion.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rotationMatrix が9要素で返る`() = runTest {
        fakeDataSource.setAvailable(true)

        useCase().test {
            fakeDataSource.emit(listOf(0f, 0f, 0f, 1f))

            val reading = awaitItem().reading!!
            assertEquals(9, reading.rotationMatrix.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `eulerAngles が3要素で返る`() = runTest {
        fakeDataSource.setAvailable(true)

        useCase().test {
            fakeDataSource.emit(listOf(0f, 0f, 0f, 1f))

            val reading = awaitItem().reading!!
            assertEquals(3, reading.eulerAngles.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── リアルタイム更新 ──────────────────────────────────────────────────────

    @Test
    fun `値が更新されると新しい emit が来る`() = runTest {
        fakeDataSource.setAvailable(true)

        useCase().test {
            fakeDataSource.emit(listOf(0f, 0f, 0f, 1f), timestampNanos = 1_000L)
            val first = awaitItem().reading!!
            assertEquals(1_000L, first.timestampNanos)

            fakeDataSource.emit(listOf(0f, 0f, 0f, 1f), timestampNanos = 2_000L)
            val second = awaitItem().reading!!
            assertEquals(2_000L, second.timestampNanos)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
