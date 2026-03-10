# スレッドモデル

> **初心者向け:** このドキュメントでは「スレッド」「コルーチン」「ディスパッチャ」などの概念が登場する。
> まず [concepts.md § Coroutines と Flow](concepts.md#3-coroutines-と-flow--非同期処理) を読むと理解しやすい。

---

## 概要

このアプリに登場するスレッドは主に **2 種類**。

| スレッド | 用途 |
|---|---|
| **センサースレッド** | Android システムがセンサーコールバックを呼ぶ専用スレッド |
| **メインスレッド** | UI 描画・Flow 演算・ViewModel 処理 |

データはセンサースレッドで生まれ、メインスレッドで加工・表示される。
その橋渡しを `callbackFlow` と `trySend` が担う。

---

## スレッドごとの処理マップ

```
【センサースレッド（Android システム管理）】
    SensorManager がコールバックを呼ぶ
    └─ onSensorChanged(event)
           └─ trySend(RawRotationVector(...))   ← ノンブロッキング送信
                  │
                  │  Channel 経由でデータを渡す
                  ↓
【メインスレッド（Dispatchers.Main）】
    callbackFlow が Channel からデータを受け取る
    └─ Flow<RawRotationVector>
           └─ map { raw → extractQuaternion → rotationMatrix → eulerAngles }
                  │                         [ObserveAttitudeUseCaseImpl]
                  ↓
           combine(sensorsFlow, attitudeFlow, _reference)
                  └─ buildSuccessState(...)
                         └─ computeRelativeAttitude(current, reference)
                                │                  [SensorViewModel]
                                ↓
                  StateFlow<SensorDashboardUiState>
                         │
                         ↓  collectAsStateWithLifecycle()
                  Composable 再コンポーズ（画面更新）
                                                   [UI 層]
```

---

## 各処理の詳細

### 1. センサーコールバック（センサースレッド）

Android の `SensorManager` はセンサー値が更新されるたびに
`SensorEventListener.onSensorChanged()` を呼ぶ。
このコールバックは **Android システムが管理する専用スレッド** で実行される。

```kotlin
val listener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        // ← ここはセンサースレッドで実行される
        trySend(RawRotationVector(
            values = event.values.toList(),
            accuracy = event.accuracy,
            timestampNanos = event.timestamp,
        ))
    }
}
```

> **補足 — なぜ専用スレッドなのか**
> センサーは非常に高頻度（最大 1000Hz 超）でデータを生成する。
> メインスレッドで受け取ると UI の描画処理をブロックしてしまうため、
> Android はセンサーコールバックを別スレッドで呼ぶ設計になっている。
>
> このスレッドはアプリが生成するのではなく、Android システムが管理する。
> 開発者が意識する必要はないが、「onSensorChanged はメインスレッドではない」という点は重要。

---

### 2. `trySend` — スレッド間のデータ受け渡し（ノンブロッキング）

`trySend` は `callbackFlow` が内部に持つ `Channel`（キュー）にデータを送る操作。

```kotlin
trySend(RawRotationVector(...))
// ↑ センサースレッドから Channel にデータを入れる
//   Channel の反対側（Flow の collect 側）が別スレッドで取り出す
```

`trySend` は **ノンブロッキング**（送信に失敗しても待機しない）。
バッファが満杯の場合はデータを破棄して即座に戻る。

> **補足 — `trySend` vs `send`**
>
> | 関数 | 動作 | 使う場面 |
> |---|---|---|
> | `send()` | バッファが満杯なら suspend して待つ | suspend 文脈 |
> | `trySend()` | バッファが満杯なら破棄して即戻る | コールバック内（suspend 不可） |
>
> `onSensorChanged` は通常の関数（`suspend` でない）なので `send()` は呼べない。
> センサースレッドをブロックせずにデータを渡せる `trySend` を使う。
>
> センサーデータが処理しきれないほど速く来た場合は一部が破棄されるが、
> IMU データはストリームなので「最新値」が取れれば十分であり実害はほぼない。

---

### 3. `callbackFlow` — スレッドの橋渡し

`callbackFlow` は「コールバック（センサースレッド）」と「Flow（メインスレッド）」を
`Channel` を介して繋ぐブリッジ。

```
センサースレッド          Channel（キュー）          メインスレッド
onSensorChanged ──── trySend ──→ [データ] ──── collect ──→ Flow演算
```

Flow を collect する側のコルーチンは **collect を開始したコルーチンのディスパッチャ** で動く。
このアプリでは `viewModelScope`（`Dispatchers.Main`）が collect するため、
`callbackFlow` から先の処理はすべてメインスレッドになる。

> **補足 — ディスパッチャとは**
> コルーチンをどのスレッド（プール）で動かすかを決める設定。
>
> | ディスパッチャ | スレッド | 用途 |
> |---|---|---|
> | `Dispatchers.Main` | メインスレッド（1本） | UI 操作・StateFlow の更新 |
> | `Dispatchers.Default` | CPUコア数のスレッドプール | CPU 集約的な計算 |
> | `Dispatchers.IO` | 最大64本のスレッドプール | ファイル・ネットワーク待機 |
>
> `viewModelScope` は内部的に `Dispatchers.Main.immediate` を使う。
> これにより ViewModel の処理は原則メインスレッドで動く。

---

### 4. Flow 演算（メインスレッド）

`callbackFlow` 以降の `map`・`combine`・`catch` はすべてメインスレッドで実行される。

```kotlin
// ObserveAttitudeUseCaseImpl — メインスレッドで動く
dataSource.observe().map { raw ->
    val quaternion     = extractQuaternion(raw.values)      // 計算
    val rotationMatrix = quaternionToRotationMatrix(quaternion) // 計算
    val eulerAngles    = rotationMatrixToEulerAngles(rotationMatrix) // 計算
    UiAttitudeState(available = true, reading = AttitudeReading(...))
}

// SensorViewModel — メインスレッドで動く
combine(observeSensors(), observeAttitude(), _reference, ::buildSuccessState)
// buildSuccessState 内の computeRelativeAttitude もメインスレッド
```

> **補足 — なぜ計算がメインスレッドなのか**
> `viewModelScope` のデフォルトディスパッチャが `Dispatchers.Main` だから。
>
> メインスレッドは UI の描画も担当するため、重い計算を置くと画面がカクつく原因になる。
> ただし、このアプリの計算（クォータニオン→行列変換など）は浮動小数点演算が数十回程度であり、
> 通常のサンプリング間隔（`SENSOR_DELAY_UI` ≈ 60ms）では問題にならない。

---

### 5. `StateFlow` と Composable（メインスレッド）

`stateIn` で `StateFlow` に変換された後、Composable が `collectAsStateWithLifecycle()` で購読する。
値が更新されると Compose フレームワークが再コンポーズをスケジュールし、メインスレッドで画面を更新する。

```kotlin
// SensorViewModel
val uiState: StateFlow<SensorDashboardUiState> =
    combine(...).stateIn(viewModelScope, WhileSubscribed(5_000), Loading)

// SensorDashboardScreen（Composable）
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
// ↑ StateFlow の新しい値が来るたびに再コンポーズされる（メインスレッド）
```

---

## センサー更新 1 回の処理シーケンス

端末を動かしたときに何が起きるかを時系列で示す。

```
t=0ms  [センサースレッド] onSensorChanged(event) 呼ばれる
t=0ms  [センサースレッド] trySend(RawRotationVector) で Channel にデータを入れる
t=1ms  [メインスレッド]   callbackFlow が Channel からデータを取り出す
t=1ms  [メインスレッド]   map: extractQuaternion → quaternionToRotationMatrix → rotationMatrixToEulerAngles
t=1ms  [メインスレッド]   combine: sensorsFlow・attitudeFlow・_reference の最新値が揃う
t=1ms  [メインスレッド]   buildSuccessState: computeRelativeAttitude を実行
t=1ms  [メインスレッド]   StateFlow の値が更新される
t=16ms [メインスレッド]   次の Compose フレームで Composable が再コンポーズされ画面更新
```

> **補足 — Compose のフレームレートと再コンポーズ**
> Android の画面は通常 60Hz（約 16ms ごと）で更新される。
> StateFlow の値が 16ms 以内に複数回更新されても、Compose は最後の値だけを使って 1 回だけ再コンポーズする。
> センサーが 100Hz で更新されても画面は 60Hz にスロットリングされるため、
> Composable は毎フレーム再コンポーズされるとは限らない。

---

## パフォーマンス上の注意点

### 現状の構成（問題なし）

`SENSOR_DELAY_UI`（約 60ms = 約 16Hz）相当のサンプリングレートでは、
メインスレッドの計算負荷は無視できるレベル。

### 高頻度サンプリングに切り替える場合

`SENSOR_DELAY_FASTEST`（≈ 1ms = 約 1000Hz）にすると、
クォータニオン計算がメインスレッドで毎ミリ秒走ることになり、フレームドロップが起きる可能性がある。

対策として `flowOn(Dispatchers.Default)` を UseCase に追加することで、
計算をバックグラウンドスレッドに移せる。

```kotlin
// ObserveAttitudeUseCaseImpl での対応例（現在は未使用）
return dataSource.observe()
    .map { raw -> /* 計算 */ }
    .flowOn(Dispatchers.Default)  // ← map をバックグラウンドスレッドで実行
// flowOn より下流（combine・StateFlow）は引き続きメインスレッドで動く
```

> **補足 — `flowOn` の効果範囲**
> `flowOn` はそれより **上流** の演算に適用される。下流には影響しない。
>
> ```
> dataSource.observe()
>     .map { ... }        ← Dispatchers.Default で動く（flowOn の上流）
>     .flowOn(Dispatchers.Default)
>     .combine(...)       ← Dispatchers.Main で動く（flowOn の下流）
>     .stateIn(viewModelScope, ...)
> ```
>
> これにより「重い計算だけバックグラウンド、UI 更新はメインスレッド」という
> 理想的な構成を宣言的に記述できる。

---

## まとめ

| 場所 | スレッド | 理由 |
|---|---|---|
| `onSensorChanged` | センサースレッド | Android システムが管理 |
| `trySend` | センサースレッド | コールバック内なので suspend 不可 |
| `map`・`combine`（UseCase） | メインスレッド | `viewModelScope` のデフォルト |
| `buildSuccessState`・`computeRelativeAttitude` | メインスレッド | 同上 |
| `StateFlow` 更新 | メインスレッド | 同上 |
| Composable 再コンポーズ | メインスレッド | Compose フレームワークの要件 |
