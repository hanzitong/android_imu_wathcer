package com.example.sensorviewer

import com.example.sensorviewer.model.AttitudeReading
import com.example.sensorviewer.usecase.computeRelativeAttitude
import com.example.sensorviewer.usecase.extractQuaternion
import com.example.sensorviewer.usecase.quaternionConjugate
import com.example.sensorviewer.usecase.quaternionMultiply
import com.example.sensorviewer.usecase.quaternionToRotationMatrix
import com.example.sensorviewer.usecase.rotationMatrixToEulerAngles
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 姿勢推定の数学変換関数テスト。
 *
 * Android SDK に依存しない純粋関数のみを対象とする。
 * 浮動小数点誤差は DELTA の範囲内で許容する。
 */
class AttitudeMathTest {

    private val DELTA = 1e-5f

    // ── extractQuaternion ────────────────────────────────────────────────────

    @Test
    fun `values が4要素のとき w をそのまま使う`() {
        val q = extractQuaternion(listOf(0f, 0f, 0f, 1f))
        assertFloatEquals(1f, q[3])
    }

    @Test
    fun `values が3要素のとき w を再構築する`() {
        // 単位クォータニオン (0, 0, 0, 1) の xyz 成分だけ与える
        val q = extractQuaternion(listOf(0f, 0f, 0f))
        assertFloatEquals(1f, q[3])
    }

    @Test
    fun `w 再構築は単位クォータニオン条件を満たす`() {
        val x = 0.5f; val y = 0.5f; val z = 0.5f
        val q = extractQuaternion(listOf(x, y, z))
        val norm = q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]
        assertFloatEquals(1f, norm)
    }

    // ── quaternionToRotationMatrix ───────────────────────────────────────────

    @Test
    fun `単位クォータニオンは単位回転行列を生成する`() {
        val r = quaternionToRotationMatrix(listOf(0f, 0f, 0f, 1f))
        val identity = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        identity.forEachIndexed { i, expected ->
            assertFloatEquals(expected, r[i])
        }
    }

    @Test
    fun `回転行列は直交行列である（R × Rᵀ が単位行列）`() {
        // 任意の非自明なクォータニオンで検証
        val q = listOf(0.1f, 0.2f, 0.3f, sqrt(1f - 0.01f - 0.04f - 0.09f))
        val r = quaternionToRotationMatrix(q)

        // R × Rᵀ の対角成分が 1、非対角成分が 0
        for (i in 0..2) {
            for (j in 0..2) {
                val dot = (0..2).sumOf { k -> (r[i * 3 + k] * r[j * 3 + k]).toDouble() }.toFloat()
                assertFloatEquals(if (i == j) 1f else 0f, dot)
            }
        }
    }

    // ── rotationMatrixToEulerAngles ──────────────────────────────────────────

    @Test
    fun `単位回転行列はオイラー角がすべて0`() {
        val identity = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val euler = rotationMatrixToEulerAngles(identity)
        assertFloatEquals(0f, euler[0]) // roll
        assertFloatEquals(0f, euler[1]) // pitch
        assertFloatEquals(0f, euler[2]) // yaw
    }

    @Test
    fun `単位クォータニオンから変換したオイラー角はすべて0`() {
        val r = quaternionToRotationMatrix(listOf(0f, 0f, 0f, 1f))
        val euler = rotationMatrixToEulerAngles(r)
        assertFloatEquals(0f, euler[0])
        assertFloatEquals(0f, euler[1])
        assertFloatEquals(0f, euler[2])
    }

    @Test
    fun `yaw の範囲は -π から π`() {
        // 任意のクォータニオンで yaw が範囲内に収まることを確認
        val testQuaternions = listOf(
            listOf(0f, 0f, 0f, 1f),
            listOf(0f, 0f, sqrt(0.5f), sqrt(0.5f)),
            listOf(0.5f, 0.5f, 0.5f, 0.5f),
        )
        testQuaternions.forEach { q ->
            val r = quaternionToRotationMatrix(q)
            val euler = rotationMatrixToEulerAngles(r)
            val yaw = euler[2]  // RPY 順: [roll=0, pitch=1, yaw=2]
            assert(yaw >= -PI.toFloat() && yaw <= PI.toFloat()) {
                "yaw $yaw は [-π, π] の範囲外"
            }
        }
    }

    // ── quaternionConjugate ──────────────────────────────────────────────────

    @Test
    fun `共役クォータニオンは q ⊗ q⁻¹ が単位クォータニオンになる`() {
        val q = listOf(0.1f, 0.2f, 0.3f, sqrt(1f - 0.01f - 0.04f - 0.09f))
        val qInv = quaternionConjugate(q)
        val product = quaternionMultiply(q, qInv)
        assertFloatEquals(0f, product[0])
        assertFloatEquals(0f, product[1])
        assertFloatEquals(0f, product[2])
        assertFloatEquals(1f, product[3])
    }

    // ── computeRelativeAttitude ──────────────────────────────────────────────

    @Test
    fun `同じ姿勢を基準にすると relative は単位クォータニオンになる`() {
        val identity = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val reading = AttitudeReading(
            quaternion = listOf(0.1f, 0.2f, 0.3f, sqrt(1f - 0.01f - 0.04f - 0.09f)),
            rotationMatrix = identity,
            eulerAngles = listOf(0f, 0f, 0f),
            accuracy = 3,
            timestampNanos = 0L,
        )
        val relative = computeRelativeAttitude(current = reading, reference = reading)
        assertFloatEquals(0f, relative.quaternion[0])
        assertFloatEquals(0f, relative.quaternion[1])
        assertFloatEquals(0f, relative.quaternion[2])
        assertFloatEquals(1f, relative.quaternion[3])
    }

    @Test
    fun `relative の accuracy と timestampNanos は current から引き継がれる`() {
        val identity = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val base = AttitudeReading(
            quaternion = listOf(0f, 0f, 0f, 1f),
            rotationMatrix = identity,
            eulerAngles = listOf(0f, 0f, 0f),
            accuracy = 3,
            timestampNanos = 0L,
        )
        val current = base.copy(accuracy = 2, timestampNanos = 999L)
        val relative = computeRelativeAttitude(current = current, reference = base)
        assertEquals(2, relative.accuracy)
        assertEquals(999L, relative.timestampNanos)
    }

    @Test
    fun `relative の回転行列は直交行列である`() {
        val identity = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val ref = AttitudeReading(
            quaternion = listOf(0f, 0f, 0f, 1f),
            rotationMatrix = identity,
            eulerAngles = listOf(0f, 0f, 0f),
            accuracy = 3,
            timestampNanos = 0L,
        )
        val cur = AttitudeReading(
            quaternion = listOf(0.5f, 0.5f, 0.5f, 0.5f),
            rotationMatrix = identity,
            eulerAngles = listOf(0f, 0f, 0f),
            accuracy = 3,
            timestampNanos = 0L,
        )
        val r = computeRelativeAttitude(current = cur, reference = ref).rotationMatrix
        for (i in 0..2) {
            for (j in 0..2) {
                val dot = (0..2).sumOf { k -> (r[i * 3 + k] * r[j * 3 + k]).toDouble() }.toFloat()
                assertFloatEquals(if (i == j) 1f else 0f, dot)
            }
        }
    }

    // ── ヘルパー ─────────────────────────────────────────────────────────────

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assert(abs(expected - actual) <= DELTA) {
            "expected=$expected actual=$actual delta=$DELTA"
        }
    }
}
