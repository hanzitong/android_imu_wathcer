# アーキテクチャ設計

> **初心者向け:** このドキュメントで登場する Android 固有の概念（Compose・ViewModel・Hilt・Flow など）は
> [concepts.md](concepts.md) で詳しく解説している。先にそちらを読むとこのドキュメントが理解しやすくなる。

## 概要

Android Sensor Viewer はスマートフォンの IMU・環境センサーをリアルタイム表示するアプリ。
ハードウェアアクセスから UI 描画まで、最小限の層で関心事を分離している。

---

## 層構成

アプリは **センサーパイプライン** と **姿勢パイプライン** の 2 本を並行して持つ。

```
┌─────────────────────────────────────────────────────────────────────┐
│  UI Layer                                                            │
│  SensorDashboardScreen / SensorCard / AttitudeCard / ...             │
├─────────────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                                     │
│  SensorViewModel → SensorDashboardUiState                            │
│  （基準座標系の状態 _reference も保持）                                │
├──────────────────────────────┬──────────────────────────────────────┤
│  Domain Layer（センサー）     │  Domain Layer（姿勢）                 │
│  ObserveSensorsUseCase       │  ObserveAttitudeUseCase               │
│  ObserveSensorsUseCaseImpl   │  ObserveAttitudeUseCaseImpl           │
│                              │  AttitudeMath（純粋関数群）            │
├──────────────────────────────┼──────────────────────────────────────┤
│  Source Layer（センサー）     │  Source Layer（姿勢）                 │
│  SensorDataSource            │  AttitudeDataSource                   │
│  AndroidSensorSource         │  AndroidAttitudeSource                │
├──────────────────────────────┴──────────────────────────────────────┤
│  Android SDK                                                         │
│  SensorManager / SensorEventListener                                 │
└─────────────────────────────────────────────────────────────────────┘
```

各層は直下の層にのみ依存する。Android SDK への依存は Source 層に封じ込められている。

スレッドの観点では、Source 層がセンサースレッドとメインスレッドの境界になる。
詳しくは [threading.md](threading.md) を参照。

> **補足 — 「層に分ける」とは**
> 上の層は下の層を呼び出せるが、逆（下から上）は禁止する設計。
> 例えば `AndroidSensorSource`（Source 層）が `SensorViewModel`（ViewModel 層）を呼ぶコードは書かない。
> こうすることで各層を独立してテスト・変更できる。

**Repository 層は設けない。** UseCase が直接 `SensorDataSource` を使う。
1 画面・読み取り専用のアプリにとって Repository は純粋な中間委譲にしかならないため。

**ディレクトリ構造はパッケージ名と対応させてフラットに保つ。**
`data/model/`・`domain/usecase/`・`ui/viewmodel/` などの二重ラッパーは不要なので設けない。

---

## 各層の責務

### Source 層 — `AndroidSensorSource`

`SensorManager` を Kotlin Coroutines の世界に橋渡しする唯一の場所。

```kotlin
callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) { trySend(SensorReading(...)) }
    }
    sensorManager.registerListener(listener, sensor, samplingPeriodUs)
    awaitClose { sensorManager.unregisterListener(listener) }
}
```

- `callbackFlow` により、Flow の collect 開始でリスナー登録、キャンセルで自動解除
- Android SDK の型（`SensorEvent`）はこの層から外に漏れない
- `isAvailable(type)` でハードウェアの存在チェックを担う

> **補足 — `callbackFlow` とは**
> Android の `SensorManager` はデータが来るたびにコールバック関数を呼ぶ旧来の仕組み。
> `callbackFlow` はそのコールバックを Kotlin の Flow に変換するブリッジ。
> `awaitClose { ... }` に書いたコードは「Flow の購読が終わったとき（画面を閉じたときなど）」に自動で実行される。
> これによりセンサーリスナーの解除漏れ（メモリリークの原因）を防いでいる。
> 詳しくは [concepts.md § callbackFlow](concepts.md#5-callbackflow--コールバックを-flow-に変換する)。

### Domain 層 — `ObserveSensorsUseCaseImpl`

複数センサーの Flow を合流させ、UI が直接消費できる `UiSensorState` のリストに変換する。

```
1. dataSource.isAvailable() で利用可能センサーを絞り込む
2. 利用可能センサーの observe() を combine で合流し Map に集約
3. SensorType.all の順で UiSensorState を構築して emit
```

- `SensorType.all` の定義順を維持することで表示順をここで決定する
- 将来の拡張ポイント：単位変換（hPa → inHg）、ローパスフィルタ、異常値検知

UseCase はインターフェース `ObserveSensorsUseCase` として公開されており、テストでは Fake に差し替えられる。

> **補足 — `combine` とは**
> 複数の Flow を「全員が最新値を送り終わったら、それを合わせて1つの値にして流す」オペレータ。
>
> ```
> Flow A: ──1──────3──────
> Flow B: ────2──────────4─
> combine: ────(1,2)──(3,2)──(3,4)─
> ```
>
> センサーが 3 種類あるなら、3 本の Flow が全員最初の値を送って初めて最初の emit が起きる。
> それ以降はどれか 1 本が更新されるたびに「全センサーの最新値セット」が流れてくる。

### ViewModel 層 — `SensorViewModel`

UseCase の `Flow<List<UiSensorState>>` を Composable が購読できる `StateFlow<SensorDashboardUiState>` に変換する。

```kotlin
val uiState = observeSensors()
    .map { SensorDashboardUiState.Success(it) }
    .catch { emit(SensorDashboardUiState.Error(it.message ?: "Unknown error")) }
    .stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = Loading,
    )
```

`WhileSubscribed(5_000)` により、画面回転の 5 秒間はセンサーリスナーを保持し続け、再登録コストを回避する。

> **補足 — ViewModel が必要な理由**
> Android では画面を回転させるたびに UI クラス（Activity）が破棄・再生成される。
> ViewModel はその周期から独立して生き続けるため、回転しても状態（センサー値など）が消えない。
> 詳しくは [concepts.md § ViewModel](concepts.md#2-viewmodel--画面の状態管理)。
>
> **補足 — `catch` とは**
> Flow のオペレータ。上流で例外が発生したときに捕捉して別の値に変換できる。
> ここでは例外を `SensorDashboardUiState.Error` に変換し、画面にエラー表示を出す。

### UI 層 — Composable

```
SensorDashboardScreen          ← Hilt, hiltViewModel()
    └── SensorDashboardContent ← UiState を受け取る純粋 Composable（テスト対象）
            ├── LoadingContent
            ├── SensorList
            │     └── SensorCard (センサーごと)
            │           ├── SensorStatusBadge
            │           └── SensorValueRow (軸ごと)
            └── (Error → Snackbar)
```

`SensorDashboardContent` を分離しているのは、Hilt なしで直接 UiState を渡す Compose UI テストを可能にするため。

> **補足 — Composable の再コンポーズ**
> `collectAsStateWithLifecycle()` で StateFlow を観察すると、値が変わるたびに Composable が再実行（再コンポーズ）される。
> Compose はツリー全体ではなく「変化した部分だけ」を再描画する最適化を行う。
> この最適化のために `equals()` を使うため、データクラスの設計（`List<Float>` vs `FloatArray`）が重要になる。

---

## データフロー

```
SensorManager.SensorEventListener
    ↓  onSensorChanged → trySend()
callbackFlow                             [AndroidSensorSource]
    ↓  Flow<SensorReading> × N本
combine → Flow<Map<SensorType, SensorReading>>
    ↓  map to UiSensorState list
Flow<List<UiSensorState>>               [ObserveSensorsUseCaseImpl]
    ↓  map / catch
Flow<SensorDashboardUiState>
    ↓  stateIn
StateFlow<SensorDashboardUiState>       [SensorViewModel]
    ↓  collectAsStateWithLifecycle()
Composable recomposition                [UI 層]
```

> **補足 — データフロー全体の読み方**
> センサーのデータが上から下に流れ、最終的に画面の再描画につながる仕組み。
>
> 1. Android の `SensorManager` がセンサー値をコールバックで通知する
> 2. `callbackFlow` がそれを Kotlin の Flow（ストリーム）に変換する
> 3. `combine` で複数センサーの Flow を 1 本に合流させる
> 4. `map` で UI 用のデータ形式に変換する
> 5. `stateIn` で「最新値を保持する Flow（StateFlow）」に変換する
> 6. Composable が StateFlow を監視し、変化があれば自動で再描画する

---

## 主要モデル

### `SensorType` (sealed class)

アプリがサポートする全センサーを列挙した基底型。UI 定数（ラベル・単位・軸名）と Android 定数（`Sensor.TYPE_*`）の両方を保持する。

`SensorType.all` が「アプリが扱うセンサー全体」の正規リストであり、表示順もここで決まる。

> **補足 — sealed class とは**
> 取りうる種類が完全に決まっている型を定義する Kotlin の機能。
> `when` 式と組み合わせると「全センサーを処理したか」をコンパイラがチェックしてくれる。
> 詳しくは [concepts.md § sealed class](concepts.md#7-sealed-class--sealed-interface--閉じた型の列挙)。

### `SensorReading` (data class)

センサー 1 回分の計測値。`FloatArray` ではなく `List<Float>` を使うことで、
`data class` の `equals` が正しく機能し、Compose の不要な再コンポーズをスキップできる。

### `UiSensorState` (data class)

UI 層が消費する 1 センサー分の表示状態。`SensorType`・`available: Boolean`・`SensorReading?` の合成値。
Domain 層で生成される。`model/` に置くのは Domain 層が `ui/` を参照すると依存が逆転するため。

---

## DI 構成（Hilt）

`AppModule`（`SingletonComponent`）で 2 つのインターフェースバインドを行う。

| インターフェース | 実装 |
|---|---|
| `SensorDataSource` | `AndroidSensorSource` |
| `ObserveSensorsUseCase` | `ObserveSensorsUseCaseImpl` |

`SensorManager` は `@Provides` でシステムサービスから取得して提供する。
`SensorViewModel` は `@HiltViewModel` でスコープ管理される。

> **補足 — DI（依存性注入）とは**
> クラスが必要とするオブジェクトを「自分で生成する」のではなく「外から渡してもらう」設計パターン。
> Hilt はその仕組みを自動化するフレームワークで、アノテーションを付けるだけで
> 「どのインターフェースにどの実装を使うか」を解決してくれる。
> テストでは本物の `AndroidSensorSource` の代わりに `FakeSensorDataSource` を渡すことができる。
> 詳しくは [concepts.md § Hilt](concepts.md#6-hilt--依存性注入diフレームワーク)。

---

## センサー追加手順

1. `SensorType.kt` に `object` を追加（`label`・`androidConstant`・`unit`・`axes` を指定）
2. `SensorType.all` リストに追加

以降のパイプライン（UseCase のマッピング・UI の描画）はデータ駆動のため変更不要。
