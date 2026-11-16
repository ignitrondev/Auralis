/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.desktop

import androidx.compose.ui.window.application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory

fun main() {
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
    application {
        AuralisDesktopWindow(onCloseRequest = ::exitApplication)
    }
}
