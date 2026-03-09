# 設計上の判断記録

個々の実装選択がなぜそうなっているかを記録する。

---

## `SensorReading.values` を `FloatArray` でなく `List<Float>` にする

`FloatArray` は Kotlin の `data class` における `equals` / `hashCode` の対象外（参照比較になる）。
Jetpack Compose は `StateFlow` の新旧値を `equals` で比較し、差分がなければ再コンポーズをスキップする。
`FloatArray` を使うと毎フレーム必ず再コンポーズが走り、パフォーマンスが劣化する。

```kotlin
// NG: FloatArray は参照比較になる
data class SensorReading(val values: FloatArray, ...)

// OK: List<Float> は要素ごとの値比較
data class SensorReading(val values: List<Float>, ...)
```

> **補足 — 参照比較と値比較**
> `FloatArray` は Java の配列と同じく、`==` が「同じオブジェクトか」（参照比較）になる。
> 同じ内容の配列でも別のオブジェクトなら `false` になる。
>
> ```kotlin
> floatArrayOf(1f, 2f) == floatArrayOf(1f, 2f)  // false（別オブジェクト）
> listOf(1f, 2f) == listOf(1f, 2f)               // true（内容で比較）
> ```
>
> Compose は「前回と今回の値が同じなら再描画をスキップ」という最適化を行うが、
> この判断に `equals()` を使う。`FloatArray` では常に「違う」と判定されてしまうため、
> センサー値が変わっていなくても毎フレーム再描画が走る。

---

## `SensorViewModel` に `WhileSubscribed(5_000)` を使う

`WhileSubscribed` は購読者がゼロになると上流 Flow をキャンセルする。
これによりセンサーリスナーが自動的に解除される。

ただし画面回転では一時的に Composable が再生成されるため、購読者ゼロの瞬間が生まれる。
`stopTimeoutMillis = 5_000` の猶予を設けることで、回転完了後に新しい Composable が購読を再開するまでの間、
リスナーを保持し続けセンサー再登録のコストを回避する。

> **補足 — 画面回転で何が起きるか**
> Android では画面を横にすると Activity が破棄され、縦向き用の新しい Activity が生成される。
> ViewModel はこの周期を生き延びるが、Composable（ViewModel を観察している側）は一瞬いなくなる。
>
> ```
> 回転前:  Composable(購読中) ← StateFlow ← センサーリスナー
> 回転中:  Composable(なし)   ← StateFlow ← センサーリスナー  ← WhileSubscribed がここを保持
> 回転後:  Composable(購読再開) ← StateFlow ← センサーリスナー
> ```
>
> 5 秒の猶予がない場合、「回転中」にセンサーリスナーが解除され、
> 回転後に再登録されるまでのデータが失われてしまう。

---

## UseCase をインターフェースにする

具体クラスを直接注入すると、テストで差し替えるために `open` / `abstract` が必要になり、
本番コードの設計がテスト都合に引きずられる。

インターフェースを定義することで：
- テストは Fake 実装を自由に作れる
- 将来の実装差し替え（例：BLE センサーへの切り替え）が容易になる
- 各層の依存方向が型として明示される

> **補足 — `open` vs インターフェース**
> Kotlin のクラスはデフォルトで `final`（継承不可）。
> テストで差し替えるためにクラスを継承しようとすると `open` を付けなければならない。
>
> ```kotlin
> // open を付けないとテストで継承できない（設計がテストに引きずられる例）
> open class ObserveSensorsUseCaseImpl(...) { ... }
>
> // インターフェースなら本番クラスを変えずに Fake を作れる
> interface ObserveSensorsUseCase { operator fun invoke(): Flow<...> }
> class ObserveSensorsUseCaseImpl : ObserveSensorsUseCase { ... }   // 本番
> class FakeObserveSensorsUseCase : ObserveSensorsUseCase { ... }   // テスト用
> ```

---

## Repository 層を設けない

`SensorRepository` を設けると `SensorDataSource` への純粋な中間委譲になる。
1 画面・読み取り専用のアプリでは UseCase が直接 `SensorDataSource` を使えばよく、
Repository は抽象の層を増やすだけでメリットがない。

将来、複数画面でセンサーデータを共有したり、キャッシュ・オフライン対応が必要になった段階で追加する。

> **補足 — Repository パターンとは**
> 一般的な Android アーキテクチャガイドでは「UseCase → Repository → DataSource」の 3 段構成が推奨される。
> Repository の役割は「複数の DataSource（ネットワーク・ローカルDB・センサー）を束ねてキャッシュ管理すること」。
>
> このアプリは「センサーを読むだけ・1 画面」なのでその複雑さが不要。
> 単純なものを複雑にする必要はないという判断。

---

## `SensorDashboardScreen` と `SensorDashboardContent` を分離する

`SensorDashboardScreen` は `hiltViewModel()` で ViewModel を取得するため、
Compose UI テストで直接使うと Hilt のセットアップが必要になる。

`SensorDashboardContent` は `SensorDashboardUiState` を引数に取る純粋な Composable であり、
テストで任意の UiState を直接渡して描画だけを検証できる。

```
SensorDashboardScreen   ← ViewModel・Hilt に依存。テストでは使わない
SensorDashboardContent  ← UiState のみに依存。Compose UI テストの対象
```

> **補足 — 純粋な Composable とは**
> 引数だけを見て描画内容が決まる Composable のこと。
> 副作用（ViewModel 取得・DI など）を持たず、同じ引数を渡せば必ず同じ見た目になる。
> テストで「この入力ならこの表示」と確認しやすい。
> React でいう「純粋コンポーネント」と同じ考え方。

---

## `UiSensorState` を `model/` に置く

`UiSensorState` は「UI 層が消費する表示状態」だが、ビジネスロジックを持たない純粋なデータクラスであり、
Domain 層の `ObserveSensorsUseCase` が生成して返す。

Domain 層のクラスが `ui/` パッケージのクラスを参照すると依存が逆転するため、
共有モデルとして `model/` に配置し双方向参照を避けている。

> **補足 — 依存の逆転**
> 「下の層が上の層を参照する」状態のこと。避けるべき設計。
>
> ```
> OK:   ui → model ← usecase    （両者が共有モデルを参照）
> NG:   ui ← usecase → ui       （usecase が ui を参照 → 逆転）
> ```
>
> `UiSensorState` を `ui/` に置くと UseCase が `ui/` パッケージを import する必要が生まれ、
> 「Domain 層が UI 層に依存する」という逆転が起きる。
> `model/` という中立な場所に置くことで双方が参照できる。

---

## エラーハンドリングを拡張関数 `toUiState()` に切り出す

ViewModel がエラーハンドリングを直接持つと、マッピングロジックと状態管理ロジックが混在して見通しが悪くなる。
`Flow<List<UiSensorState>>.toUiState()` という拡張関数に切り出すことで：

- ViewModel は `stateIn` の設定に集中できる
- エラーマッピングのロジックは独立して読める

> **補足 — Kotlin の拡張関数**
> 既存のクラスを継承せずにメソッドを追加する Kotlin の機能。
> `Flow<List<UiSensorState>>.toUiState()` は「Flow に `toUiState()` メソッドを追加する」という意味。
>
> ```kotlin
> // 拡張関数の定義
> private fun Flow<List<UiSensorState>>.toUiState(): Flow<SensorDashboardUiState> =
>     map { SensorDashboardUiState.Success(it) }
>         .catch { emit(SensorDashboardUiState.Error(...)) }
>
> // 呼び出し側（Flow のメソッドのように呼べる）
> val uiState = observeSensors().toUiState().stateIn(...)
> ```

---

## センサー追加をデータ駆動にする

`SensorType.all` という単一の正規リストを起点に、UseCase・UI がすべてこれを参照する設計にしている。
新センサーを追加するとき変更が `SensorType.kt` の 1 箇所に集約されるため、追加漏れが起きにくい。

逆に、センサーを追加した際に各層が個別に分岐を持っていると、追加漏れがサイレントなバグになりやすい。

---

## `FakeObserveSensorsUseCase` に `Channel<Result<>>` を使う

`MutableSharedFlow` でエラーを再現しようとすると、
```kotlin
flow.emit(throw cause)  // throw が先に評価されてテストスレッドで例外が飛ぶ
```
のようなバグを生みやすい。

`Channel<Result<List<UiSensorState>>>` を使い `getOrThrow()` で展開することで、
フロー内で例外を発生させる操作をテストコードから明示的に呼び出せる。

```kotlin
// テスト側
fakeUseCase.emitError(RuntimeException("error"))
// → flow 内で getOrThrow() が throw し、catch オペレータが捕捉する
```

> **補足 — `Result<T>` とは**
> Kotlin 標準ライブラリの型。「成功（値あり）」か「失敗（例外あり）」のどちらかを表す。
>
> ```kotlin
> Result.success(value)   // 成功
> Result.failure(cause)   // 失敗
>
> result.getOrThrow()     // 成功なら値を返す、失敗なら例外をスロー
> ```
>
> Channel に `Result` を流すことで「例外をデータとして扱い、Flow に乗せてから throw する」ことができる。
> こうすることで、例外がテストスレッドではなく Flow 内で発生し、`catch` オペレータが拾える。
