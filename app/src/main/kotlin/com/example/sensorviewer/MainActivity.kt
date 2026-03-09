package com.example.sensorviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sensorviewer.ui.SensorDashboardScreen
import com.example.sensorviewer.ui.theme.SensorViewerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * アプリのエントリーポイント。
 *
 * Hilt のインジェクションを受けるため [@AndroidEntryPoint] を付与する。
 * [SensorDashboardScreen] を [SensorViewerTheme] でラップして表示する。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorViewerTheme {
                SensorDashboardScreen()
            }
        }
    }
}
