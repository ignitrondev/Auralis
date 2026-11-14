/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ignitrondev.auralis.core.designsystem.theme.AuralisTheme
import com.ignitrondev.auralis.ui.AuralisApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
