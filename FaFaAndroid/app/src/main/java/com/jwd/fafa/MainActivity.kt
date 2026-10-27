package com.jwd.fafa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jwd.fafa.feature.auth.FaFaRoot
import com.jwd.fafa.ui.theme.FaFaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FaFaTheme {
                FaFaRoot()
            }
        }
    }
}
