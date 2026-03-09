# Android アプリ開発の基礎概念

このドキュメントは「プログラミング経験はあるが Android アプリ開発は初めて」という人向けに、
このプロジェクトで使われている Android 固有の概念を解説する。

---

## 目次

1. [Jetpack Compose — Android の UI フレームワーク](#1-jetpack-compose--android-の-ui-フレームワーク)
2. [ViewModel — 画面の状態管理](#2-viewmodel--画面の状態管理)
3. [Coroutines と Flow — 非同期処理](#3-coroutines-と-flow--非同期処理)
4. [StateFlow — UI への状態配信](#4-stateflow--ui-への状態配信)
5. [callbackFlow — コールバックを Flow に変換する](#5-callbackflow--コールバックを-flow-に変換する)
6. [Hilt — 依存性注入（DI）フレームワーク](#6-hilt--依存性注入di-フレームワーク)
7. [sealed class / sealed interface — 閉じた型の列挙](#7-sealed-class--sealed-interface--閉じた型の列挙)
8. [Kotlin の data class — 値オブジェクト](#8-kotlin-の-data-class--値オブジェクト)

---

## 1. Jetpack Compose — Android の UI フレームワーク

### 何か

Android の UI を構築するための公式フレームワーク（2021年以降の標準）。
従来の XML レイアウトに代わり、**Kotlin のコードだけで画面を記述する**。

Web 開発の React、クロスプラットフォームの Flutter に近い考え方。

### Composable 関数

`@Composable` アノテーションを付けた関数が UI の部品になる。

```kotlin
@Composable
fun SensorCard(state: UiSensorState) {
    Card {
        Text(text = state.type.label)
    }
}
```

- 関数を呼び出す = その UI 部品を画面に配置する
- 引数が変わると Compose が差分を検出して**その部分だけ再描画**する（再コンポーズ）
- `if` や `for` で普通のコードと同じように条件分岐・繰り返しを書ける

### 再コンポーズ（Recomposition）

状態が変わったとき、Compose が変化した部分のみ `@Composable` 関数を再実行して画面を更新すること。
React の再レンダリングに相当する。

Compose は引数が「前回と同じ値か」を `equals()` で判定する。
同じなら再コンポーズをスキップしてパフォーマンスを最適化する。

### Scaffold

「ヘッダー・コンテンツ・フッター」のような画面の骨格を提供する Composable。
`TopAppBar` や `SnackbarHost` の配置を担う。

```kotlin
Scaffold(
    topBar = { TopAppBar(title = { Text("Sensor Viewer") }) },
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { innerPadding ->
    // ここにメインコンテンツ
}
```

---

## 2. ViewModel — 画面の状態管理

### 何か

画面（Activity や Composable）の**状態を保持するクラス**。
Android Jetpack のコンポーネントのひとつ。

### なぜ必要か

Android では**画面回転のたびに Activity が破棄・再生成**される。
もし Activity 自身が状態を持っていると、回転のたびにデータが消えてしまう。

ViewModel は Activity のライフサイクルとは独立して生存し、
画面回転後も同じインスタンスが再利用される。

```
画面回転
  → Activity 破棄 → Activity 再生成
  → ViewModel は生き続ける（状態が消えない）
```

### このプロジェクトでの役割

`SensorViewModel` は UseCase から得た `Flow` を `StateFlow` に変換して Composable に公開する。
Composable はその `StateFlow` を観察し、値が変わるたびに再コンポーズされる。

```kotlin
@HiltViewModel
class SensorViewModel @Inject constructor(
    observeSensors: ObserveSensorsUseCase,
) : ViewModel() {
    val uiState: StateFlow<SensorDashboardUiState> = observeSensors()
        .stateIn(viewModelScope, ...)
}
```

`@HiltViewModel` は Hilt（後述）が ViewModel のインスタンスを生成・管理することを示す。

---

## 3. Coroutines と Flow — 非同期処理

### Coroutines（コルーチン）

Kotlin の非同期処理の仕組み。`suspend` キーワードを付けた関数は、
処理を中断してスレッドを解放し、完了したら再開できる。

他の言語との対比：

| 言語 | 仕組み |
|---|---|
| JavaScript | `async / await` |
| Python | `asyncio` |
| Kotlin | `suspend` + `Coroutines` |

### Flow

`suspend` 関数が「1つの値を返す」のに対し、**Flow は複数の値を時間をかけて返す**ストリーム。

他の概念との対比：

| 概念 | 何を返すか |
|---|---|
| 通常の関数 | 値 1 つ |
| `suspend` 関数 | 値 1 つ（非同期） |
| `Flow` | 値を何度も（ストリーム） |
| RxJava の `Observable` | 同上（Kotlin では Flow が標準） |

センサーの計測値は「時間とともに繰り返し届く」ので、Flow が自然に合う。

```kotlin
// Flow を collect するとデータが流れてくる
dataSource.observe(SensorType.Accelerometer).collect { reading ->
    println(reading.values)  // 計測値が届くたびに呼ばれる
}
```

### cold Flow vs hot Flow

- **cold Flow**: `collect` を呼んだときに初めてデータ生成が始まる。誰も見ていないときは動かない
- **hot Flow**: `collect` の有無に関わらずデータが流れ続ける。`StateFlow` はこちら

このプロジェクトでは `callbackFlow` が cold、`StateFlow` が hot。

### collect と collectAsStateWithLifecycle

Composable 内で Flow を観察するには `collectAsStateWithLifecycle()` を使う。
これは Compose + ライフサイクル（画面が非表示のときは停止するなど）を自動で処理してくれる。

---

## 4. StateFlow — UI への状態配信

### 何か

「最新の値を 1 つ保持し続ける hot Flow」。

- 購読者がいなくても値を保持する
- 新しい購読者が来たとき、すぐに最新値を送る（購読した瞬間に値が得られる）
- 新しい値が来ると購読者全員に通知する

### stateIn

cold Flow を StateFlow に変換するオペレータ。

```kotlin
val uiState: StateFlow<SensorDashboardUiState> = observeSensors()
    .stateIn(
        scope = viewModelScope,          // このスコープが終了したら Flow も終了
        started = WhileSubscribed(5_000), // 購読者がいる間だけ上流を動かす
        initialValue = Loading,           // 最初の値（まだデータが来ていない状態）
    )
```

### WhileSubscribed(5_000)

`SharingStarted.WhileSubscribed` は「購読者がいる間だけ上流の Flow を動かし、
購読者がゼロになったら止める」というポリシー。

`5_000`（5秒）の猶予がなぜ必要かは [decisions.md](decisions.md) を参照。

---

## 5. callbackFlow — コールバックを Flow に変換する

### Android センサー API の問題

Android の `SensorManager` はコールバック（`SensorEventListener`）ベースの API：

```kotlin
// コールバックのせいで "センサーが届いたら○○する" しか書けない
sensorManager.registerListener(object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        // ここでしかデータを受け取れない
    }
}, sensor, samplingPeriodUs)
```

- 「センサー A と B が両方揃ったら処理する」のような合流ロジックが書きにくい
- テストで差し替えにくい
- リスナーの解除を忘れるとメモリリークになる

### callbackFlow で解決する

`callbackFlow` はコールバック API を Flow に変換するビルダー。

```kotlin
return callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySend(SensorReading(...))  // Flow に値を流す
        }
    }

    sensorManager.registerListener(listener, sensor, samplingPeriodUs)

    // collect がキャンセルされたとき（画面を閉じたとき）に自動で呼ばれる
    awaitClose { sensorManager.unregisterListener(listener) }
}
```

- `trySend()` でコールバックの値を Flow に流す
- `awaitClose` に書いたクリーンアップコードが自動実行される → リスナー解除漏れがない
- 変換後は他の Flow と同じように `combine` などで合成できる

---

## 6. Hilt — 依存性注入（DI）フレームワーク

### 依存性注入（DI）とは

クラスが依存するオブジェクト（依存性）を、クラス自身が `new` で生成するのではなく、
**外部から渡してもらう**設計パターン。

```kotlin
// DI なし: AndroidSensorSource が SensorManager を自分で取得する
class AndroidSensorSource {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
}

// DI あり: SensorManager を外から渡してもらう
class AndroidSensorSource(
    private val sensorManager: SensorManager,  // 外から渡される
)
```

DI のメリット：
- テストで偽物（Fake）に差し替えやすい
- 依存関係が引数として明示されて見通しがよい

### Hilt

Google が提供する Android 向けの DI フレームワーク。
アノテーションを付けるだけで、どのクラスにどの実装を注入するかを自動で解決してくれる。

#### 主なアノテーション

| アノテーション | 意味 |
|---|---|
| `@HiltAndroidApp` | Application クラスに付ける。Hilt を有効にする |
| `@AndroidEntryPoint` | Activity に付ける。Hilt から注入を受けられるようになる |
| `@HiltViewModel` | ViewModel に付ける。ViewModel も注入対象になる |
| `@Inject constructor(...)` | このコンストラクタで Hilt がインスタンスを生成する |
| `@Module` | 注入のルールを書くクラスに付ける |
| `@Binds` | 「インターフェース A には実装 B を使え」と指定する |
| `@Provides` | インスタンスの生成方法を手動で記述する |
| `@Singleton` | アプリ全体で 1 インスタンスを共有する |

#### このプロジェクトの設定（AppModule.kt）

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    // SensorDataSource が必要な場所には AndroidSensorSource を渡す
    @Binds @Singleton
    abstract fun bindSensorDataSource(impl: AndroidSensorSource): SensorDataSource

    // ObserveSensorsUseCase が必要な場所には ObserveSensorsUseCaseImpl を渡す
    @Binds @Singleton
    abstract fun bindObserveSensorsUseCase(impl: ObserveSensorsUseCaseImpl): ObserveSensorsUseCase

    companion object {
        // SensorManager は自動生成できないので手動で提供する
        @Provides @Singleton
        fun provideSensorManager(@ApplicationContext context: Context): SensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
}
```

`@InstallIn(SingletonComponent::class)` はアプリ起動中ずっと有効なスコープ（シングルトン）を意味する。

---

## 7. sealed class / sealed interface — 閉じた型の列挙

### 何か

「取りうる状態が完全に決まっている型」を表現する Kotlin の機能。
他言語の「代数的データ型」や「タグ付きユニオン」に相当する。

### このプロジェクトの例

```kotlin
sealed interface SensorDashboardUiState {
    data object Loading : SensorDashboardUiState   // データなし
    data class Success(val sensors: List<UiSensorState>) : SensorDashboardUiState
    data class Error(val message: String) : SensorDashboardUiState
}
```

`when` 式で使うと、コンパイラが「全ケース網羅しているか」をチェックしてくれる：

```kotlin
when (uiState) {
    is Loading -> CircularProgressIndicator()
    is Success -> SensorList(uiState.sensors)
    is Error   -> Snackbar(uiState.message)
    // else が不要。新しいサブクラスを追加すると未処理エラーがコンパイル時に出る
}
```

`sealed class SensorType` も同じ仕組みで、センサーの種類（Accelerometer・Gyroscope など）を列挙している。

---

## 8. Kotlin の data class — 値オブジェクト

### 何か

`data class` は Kotlin のクラス修飾子。以下を自動生成する：

- `equals()` / `hashCode()` — フィールドの値で比較
- `toString()` — フィールド名と値を含む文字列
- `copy()` — 一部フィールドを変えたコピーを作る

```kotlin
data class SensorReading(
    val type: SensorType,
    val values: List<Float>,
    val accuracy: Int,
    val timestampNanos: Long,
)

val r1 = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)
val r2 = SensorReading(SensorType.Accelerometer, listOf(1f, 2f, 3f), 3, 0L)

r1 == r2  // true（値が同じなので）
r1 === r2 // false（参照は別）
```

Compose の再コンポーズ最適化は `equals()` に依存するため、
センサー計測値のような「値で比較したいオブジェクト」には `data class` が適している。

### FloatArray ではなく List<Float> を使う理由

`FloatArray` はプリミティブ配列なので、Kotlin の `data class` でも `equals` が参照比較になってしまう：

```kotlin
val a = floatArrayOf(1f, 2f, 3f)
val b = floatArrayOf(1f, 2f, 3f)
a == b   // false（同じ内容でも別オブジェクトなので）

val x = listOf(1f, 2f, 3f)
val y = listOf(1f, 2f, 3f)
x == y   // true（内容で比較される）
```

`FloatArray` を使うと「値が変わっていないのに Compose が再コンポーズを実行する」バグが起きる。
詳しくは [decisions.md](decisions.md) を参照。
