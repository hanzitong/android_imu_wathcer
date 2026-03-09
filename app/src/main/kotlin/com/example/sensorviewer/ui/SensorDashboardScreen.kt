package com.example.sensorviewer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensorviewer.ui.component.SensorCard

/**
 * センサーダッシュボードのルート Composable。
 *
 * [SensorViewModel] から [SensorDashboardUiState] を collect し、
 * [SensorDashboardContent] に渡して描画する。
 * Hilt による ViewModel 注入と Snackbar の状態管理をここで行い、
 * コンテンツ描画の責務は [SensorDashboardContent] に委譲する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: SensorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Sensor Viewer") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SensorDashboardContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * UiState に応じた画面コンテンツを描画する Composable。
 *
 * [SensorDashboardScreen] から分離することで、Hilt や ViewModel を経由せず
 * 直接 [SensorDashboardUiState] を渡す Compose UI テストが可能になる。
 *
 * | UiState | 描画内容 |
 * |---------|---------|
 * | [SensorDashboardUiState.Loading] | 画面中央にプログレスインジケーター |
 * | [SensorDashboardUiState.Success] | センサーカードのスクロールリスト |
 * | [SensorDashboardUiState.Error]   | Snackbar にエラーメッセージを表示 |
 */
@Composable
internal fun SensorDashboardContent(
    uiState: SensorDashboardUiState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is SensorDashboardUiState.Loading -> LoadingContent(modifier)
        is SensorDashboardUiState.Success -> SensorList(uiState, modifier)
        is SensorDashboardUiState.Error -> {
            // LaunchedEffect のキーに message を使うことで、
            // 同じエラーが連続しても再表示されず、新しいメッセージのときだけ Snackbar が出る。
            LaunchedEffect(uiState.message) {
                snackbarHostState.showSnackbar(uiState.message)
            }
            Box(modifier = modifier.fillMaxSize())
        }
    }
}

// ── プライベート Composable ───────────────────────────────────────────────────

@Composable
private fun SensorList(
    uiState: SensorDashboardUiState.Success,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = uiState.sensors,
            // androidConstant は各センサーで一意なので安定したキーとして使える
            key = { it.type.androidConstant },
        ) { sensorState ->
            SensorCard(state = sensorState)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
