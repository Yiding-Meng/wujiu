package com.mengyiding.wujiu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mengyiding.wujiu.ui.WujiuApp
import com.mengyiding.wujiu.ui.theme.WujiuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WujiuTheme {
                WujiuApp()
            }
        }
    }
}
