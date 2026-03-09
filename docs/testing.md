# テスト戦略

> **初心者向け:** Android のテストには JVM で動くものと実機が必要なものの 2 種類がある。
> このドキュメントの末尾「[テストの基礎知識](#テストの基礎知識)」に概念の解説をまとめた。

## 方針

テストを 2 層に分け、それぞれを独立して検証する。

| 層 | テスト種別 | 実行環境 | テスト数 |
|---|---|---|---|
| ロジック層 | JVM unit test | JVM（Android 不要）| 27 |
| UI 層 | Compose instrumented test | 実機 / エミュレータ | 4 |

実機センサーを使った E2E テストは本プロジェクトのスコープ外（センサー値が非決定的なため）。

---

## ロジック層のテスト（JVM unit test）

### `SensorViewModelTest`（6 テスト）

ViewModel の状態遷移を検証する。`FakeObserveSensorsUseCase` で UseCase をスタブし、
emit タイミングと `SensorDashboardUiState` の変化が一致することを Turbine で確認する。

| テストケース | 検証内容 |
|---|---|
| 初期状態は Loading | StateFlow の initialValue |
| Success に遷移する | 正常 emit → Success |
| 連続 emit で最新値に更新される | 2 回目の emit で Success が差し替わる |
| Error に遷移する | 例外 emit → Error |
| エラーメッセージが正確に伝播する | exception.message == Error.message |
| null メッセージは \"Unknown error\" | message=null 時のフォールバック |

`MainDispatcherRule` で `Dispatchers.Main` を `StandardTestDispatcher` に置換することで
JVM 上で ViewModel を動かせる。

> **補足 — なぜ `MainDispatcherRule` が必要か**
> ViewModel の内部処理は `Dispatchers.Main`（Android のメインスレッド用のディスパッチャ）を使う。
> JVM 単体のテスト環境には Android のメインスレッドが存在しないため、
> そのまま動かすとクラッシュする。`MainDispatcherRule` は `Dispatchers.Main` を
> テスト用の偽ディスパッチャ（`StandardTestDispatcher`）に差し替えることで解決する。
>
> JUnit4 の `@get:Rule` は「テストの前後に処理を挟む」仕組み。
> `MainDispatcherRule` は「テスト開始前に差し替え、テスト終了後に元に戻す」を自動で行う。

### `ObserveSensorsUseCaseTest`（8 テスト）

UseCase のマッピング・合流ロジックを検証する。`FakeSensorDataSource` で DataSource をスタブする。

| テストケース | 検証内容 |
|---|---|
| 利用可能センサーは available=true | `isAvailable` の結果が反映される |
| 利用不可センサーは available=false / null | 同上 |
| 全センサー利用不可 → 即時 emit | `flowOf` パスの動作 |
| 出力リストの順序 | `SensorType.all` と同じ順序 |
| combine は全センサー揃うまで emit しない | `combine` のセマンティクス |
| 値更新で新しい emit が来る | 2 回目の読み取りが伝播する |
| accuracy / timestampNanos が伝播する | データの欠損チェック |
| 複数センサーが正しくマッピングされる | 3 センサー同時のマッピング確認 |

### `SensorReadingTest`（5 テスト）

`SensorReading` の純粋ロジックを検証する。Android フレームワーク不要。

| テストケース | 検証内容 |
|---|---|
| accuracyLabel の全パターン（テーブル駆動） | HIGH/MEDIUM/LOW/UNRELIABLE/-1/99 → 文字列 |
| 同じ値は等しい | `List<Float>` による値比較が機能する |
| values が異なると等しくない | フィールド equals |
| timestampNanos が異なると等しくない | フィールド equals |
| accuracy が異なると等しくない | フィールド equals |

> **補足 — テーブル駆動テスト**
> 「入力と期待値のペア」をリストで用意し、ループで全パターンを検証するスタイル。
> 個別に 6 つのテストを書く代わりに 1 つにまとめることで、追加・削除・読みやすさが向上する。
> ```kotlin
> val cases = listOf(
>     SensorManager.SENSOR_STATUS_ACCURACY_HIGH to "High",
>     -1 to "Unknown",
>     // ...
> )
> cases.forEach { (accuracy, expected) ->
>     assertEquals(expected, SensorReading.accuracyLabel(accuracy))
> }
> ```

### `SensorTypeTest`（8 テスト）

`SensorType` の構造的な正確性を検証する。

| テストケース | 検証内容 |
|---|---|
| all は 6 要素 | センサー件数の保護 |
| androidConstant に重複がない | 定数の取り違え検出 |
| label に重複がない | 同上 |
| 3 軸センサーは axes.size == 3 | Accelerometer / Gyroscope / Magnetometer |
| 単軸センサーは axes.size == 1 | Barometer / AmbientLight / Proximity |
| androidConstant が SDK 定数と一致する | コピペミス検出 |
| label が空でない | 全センサー |
| unit が空でない | 全センサー |

---

## UI 層のテスト（Compose instrumented test）

### `SensorDashboardContentTest`（4 テスト）

`SensorDashboardContent` に直接 `SensorDashboardUiState` を渡すことで、
Hilt・ViewModel を介さず描画のみを検証する。

| テストケース | 検証内容 |
|---|---|
| Loading | CircularProgressIndicator が表示される |
| Success | センサー名が表示される |
| Error | Snackbar にメッセージが表示される |
| Success（unavailable 含む） | 利用不可センサーも一覧に表示される |

> **補足 — Hilt を介さずテストできる理由**
> `SensorDashboardScreen` は `hiltViewModel()` で ViewModel を取得するため、
> テスト環境で使おうとすると Hilt のセットアップが必要になり複雑になる。
>
> そこで描画ロジックだけを持つ `SensorDashboardContent` を別の関数として分離し、
> テストでは UiState を直接渡す。これにより「この UiState のとき、この表示になるか」だけを検証できる。
> ```kotlin
> // テスト: SensorDashboardContent に直接 UiState を渡す
> composeTestRule.setContent {
>     SensorDashboardContent(uiState = SensorDashboardUiState.Loading, ...)
> }
> composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
> ```

---

## Fake 設計

### `FakeObserveSensorsUseCase`

`ObserveSensorsUseCase` インターフェースを直接実装。
`Channel<Result<List<UiSensorState>>>` を使い、正常値とエラーを同一フローで扱う。

```kotlin
fakeUseCase.emit(listOf(...))           // Success に遷移
fakeUseCase.emitError(RuntimeException) // Error に遷移（flow 内で例外が発生する）
```

`MutableSharedFlow` に対して `flow.emit(throw cause)` と書くと
`throw` が先に評価されてテストスレッドで例外が飛ぶため、
`Channel<Result<>>` + `getOrThrow()` パターンを使う。

> **補足 — Fake とは**
> テスト用に本物の実装を置き換える偽オブジェクト。
> 本物の `ObserveSensorsUseCaseImpl` はセンサーハードウェアに依存するため JVM では動かせない。
> `FakeObserveSensorsUseCase` はインターフェースを実装するだけの薄い偽物で、
> テスト側から任意のタイミングで任意の値を流せる。
>
> **Mock との違い**: Mock はライブラリが自動生成する偽物（`mockk { every { ... } }` のような記法）。
> Fake は手書きの偽物。テスト対象のロジックが複雑なときは Fake の方が意図が明確になる。

### `FakeSensorDataSource`

`SensorDataSource` を実装。
- `setAvailable(vararg types)` で `isAvailable()` の戻り値を制御
- `emit(type, reading)` で特定センサーの Flow に値を流す
- `Channel.UNLIMITED` を使うことで `emit` を suspend なしで呼べる

> **補足 — `Channel` とは**
> コルーチン間でデータを受け渡す通信路。`send()` で送信、`receive()` や `receiveAsFlow()` で受信できる。
> `Channel.UNLIMITED` は送信側をブロックしないバッファ設定。
> テストコードから `emit()` を呼ぶとき `suspend` 文脈でなくても動くので、テストが書きやすくなる。

---

## テストの実行

```bash
# ロジック層（JVM、高速）
./gradlew testDebugUnitTest

# 単一クラスのみ
./gradlew testDebugUnitTest --tests "com.example.sensorviewer.SensorViewModelTest"

# UI 層（要デバイス/エミュレータ）
./gradlew connectedAndroidTest
```

---

## テストの基礎知識

### JVM unit test vs Instrumented test

Android のテストには 2 種類ある。

| 種別 | 実行環境 | 速度 | できること |
|---|---|---|---|
| JVM unit test | PC の JVM 上 | 速い（数秒） | ロジックの検証 |
| Instrumented test | 実機またはエミュレータ | 遅い（数分） | UI・Android API の検証 |

JVM unit test は Android デバイスを必要としないため CI でも簡単に動かせる。
可能な限りロジックをここでテストし、Instrumented test は UI 描画の確認にとどめる。

プロジェクト内のディレクトリ対応：

```
app/src/
├── test/          ← JVM unit test（このプロジェクトの 27 テスト）
└── androidTest/   ← Instrumented test（このプロジェクトの 4 テスト）
```

### Turbine — Flow のテストライブラリ

Flow は「値が時間とともに流れてくる」ストリームなので、通常の `assertEquals` では検証しにくい。

Turbine は `.test { }` ブロックの中で `awaitItem()` を呼ぶことで、
Flow から値が届くのを待って検証できるライブラリ。

```kotlin
viewModel.uiState.test {
    assertEquals(Loading, awaitItem())    // 最初の値を受け取って確認

    fakeUseCase.emit(sensorList)          // 値を流す

    val state = awaitItem()               // 次の値を受け取る
    assertTrue(state is Success)

    cancelAndIgnoreRemainingEvents()      // テスト終了（残りは無視）
}
```

`awaitItem()` は値が来るまでコルーチンを一時停止して待つ。
`cancelAndIgnoreRemainingEvents()` は Flow の購読を終了し、残っている値をすべて無視する。

### runTest — コルーチンのテスト実行環境

`suspend` 関数やコルーチンを含むテストは、通常の JUnit テストでは実行できない。
`runTest { }` は コルーチンのテスト用実行環境（スコープ）を提供する関数。

```kotlin
@Test
fun `センサーデータが届くと Success になる`() = runTest {
    // この中では suspend 関数が呼べる
    useCase().test {
        val item = awaitItem()
        // ...
    }
}
```
