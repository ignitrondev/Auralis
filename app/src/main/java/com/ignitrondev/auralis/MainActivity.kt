/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.feature.home.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AuralisTheme {
                AuralisApp()
            }
        }
    }
}

@Composable
fun AuralisApp(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        HomeScreen()
    }
}
