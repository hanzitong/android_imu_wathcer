package com.example.sensorviewer.usecase

import com.example.sensorviewer.model.AttitudeReading
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 姿勢推定の数学変換関数群。
 *
 * Android SDK に依存しない純粋関数として切り出すことで、
 * UseCase のテストで SensorManager のモックが不要になる。
 *
 * ## 座標系
 * **右手系 ENU**（East-North-Up）固定ワールドフレーム。
 * - X 軸: 東（East）
 * - Y 軸: 北（North）
 * - Z 軸: 上（Up、鉛直上向き）
 *
 * 右手系とは「X×Y=Z（外積）」が成り立つ座標系。
 * 右手の親指を Z（上）、人差し指を X（東）に向けると、中指が Y（北）を向く。
 * Android TYPE_ROTATION_VECTOR はこの ENU 右手系に対するデバイスの回転を表す。
 *
 * ## 表記規則（ROS2 REP-103 準拠）
 * - クォータニオン: (x, y, z, w)  無次元（単位クォータニオン、ノルム = 1）
 * - 回転行列: 3×3 行優先  無次元（直交行列、各列・行のノルム = 1）
 * - オイラー角: RPY 順 [roll, pitch, yaw]  ラジアン（rad）。UI 表示時に度（°）へ変換する
 *
 * 変換式は Android SensorManager の実装に準拠。
 */

/**
 * event.values から完全なクォータニオン [x, y, z, w] を取り出す。
 *
 * TYPE_ROTATION_VECTOR の values[3]（w成分）は一部デバイスで省略される。
 * 省略された場合は単位クォータニオン条件 x²+y²+z²+w²=1 から w を再構築する。
 *
 * @param values SensorEvent.values（要素数 3 または 4 以上）
 */
internal fun extractQuaternion(values: List<Float>): List<Float> {
    val x = values[0]
    val y = values[1]
    val z = values[2]
    val w = if (values.size >= 4) {
        values[3]
    } else {
        val sq = (x * x + y * y + z * z).coerceAtMost(1f)
        sqrt(1f - sq)
    }
    return listOf(x, y, z, w)
}

/**
 * クォータニオン [x, y, z, w] から 3×3 回転行列（行優先・9要素）を計算する。
 *
 * 入力: ENU 右手系クォータニオン（無次元）
 * 出力: ENU 右手系回転行列（無次元）。各行・列のノルムは 1、行列式は +1。
 *
 * 回転行列の各要素:
 * ```
 * R = [ 1-2(y²+z²)   2(xy-wz)    2(xz+wy) ]
 *     [ 2(xy+wz)     1-2(x²+z²)  2(yz-wx) ]
 *     [ 2(xz-wy)     2(yz+wx)    1-2(x²+y²) ]
 * ```
 *
 * @param q クォータニオン [x, y, z, w]（size == 4、無次元）
 * @return 9 要素の回転行列（行優先、無次元）
 */
internal fun quaternionToRotationMatrix(q: List<Float>): List<Float> {
    val (x, y, z, w) = q
    val x2 = 2f * x * x
    val y2 = 2f * y * y
    val z2 = 2f * z * z
    val xy = 2f * x * y
    val xz = 2f * x * z
    val xw = 2f * x * w
    val yz = 2f * y * z
    val yw = 2f * y * w
    val zw = 2f * z * w
    return listOf(
        1f - y2 - z2,  xy - zw,       xz + yw,
        xy + zw,       1f - x2 - z2,  yz - xw,
        xz - yw,       yz + xw,       1f - x2 - y2,
    )
}

/**
 * 3×3 回転行列（行優先・9要素）から RPY オイラー角 [roll, pitch, yaw] を計算する。
 *
 * ROS2 REP-103 準拠の RPY（Roll-Pitch-Yaw）順。ENU 右手系・内因性回転（intrinsic）。
 * Android SensorManager.getOrientation() と同等の変換式:
 * - roll（横傾き）   = atan2(-R[6], R[8])  範囲: [-π, π] rad。正=右傾き
 * - pitch（仰俯角）  = asin(-R[7])          範囲: [-π/2, π/2] rad。正=前傾き
 * - yaw（方位角）    = atan2(R[1], R[4])    範囲: [-π, π] rad。0=北、π/2=東
 *
 * @param r 9 要素の回転行列（行優先、無次元）
 * @return [roll, pitch, yaw] ラジアン（rad）。UI 表示時は度（°）へ変換する
 */
internal fun rotationMatrixToEulerAngles(r: List<Float>): List<Float> {
    val roll  = atan2(-r[6], r[8])
    val pitch = asin(-r[7].coerceIn(-1f, 1f))
    val yaw   = atan2(r[1], r[4])
    return listOf(roll, pitch, yaw)  // RPY 順（ROS2 REP-103）
}

/**
 * 単位クォータニオンの共役（= 逆回転）を返す。
 *
 * 単位クォータニオンでは逆数 = 共役なので、x/y/z の符号を反転するだけでよい。
 *
 * @param q クォータニオン [x, y, z, w]
 * @return 共役クォータニオン [-x, -y, -z, w]
 */
internal fun quaternionConjugate(q: List<Float>): List<Float> =
    listOf(-q[0], -q[1], -q[2], q[3])

/**
 * 2つのクォータニオンのハミルトン積 q1 ⊗ q2 を計算する。
 *
 * @param q1 クォータニオン [x, y, z, w]
 * @param q2 クォータニオン [x, y, z, w]
 * @return 積クォータニオン [x, y, z, w]
 */
internal fun quaternionMultiply(q1: List<Float>, q2: List<Float>): List<Float> {
    val (x1, y1, z1, w1) = q1
    val (x2, y2, z2, w2) = q2
    return listOf(
        w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
        w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
        w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
        w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2,
    )
}

/**
 * 基準姿勢からの相対姿勢を計算する。
 *
 * q_relative = q_ref⁻¹ ⊗ q_current
 *
 * @param current   現在の姿勢
 * @param reference 基準姿勢
 * @return 基準座標系から見た現在姿勢の [AttitudeReading]
 */
internal fun computeRelativeAttitude(
    current: AttitudeReading,
    reference: AttitudeReading,
): AttitudeReading {
    val q = quaternionMultiply(quaternionConjugate(reference.quaternion), current.quaternion)
    val r = quaternionToRotationMatrix(q)
    val e = rotationMatrixToEulerAngles(r)
    return AttitudeReading(
        quaternion     = q,
        rotationMatrix = r,
        eulerAngles    = e,
        accuracy       = current.accuracy,
        timestampNanos = current.timestampNanos,
    )
}
