/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.youtube

import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger

/**
 * Diagnostic messages from the YouTube integration, for a diagnostics screen or debug logs
 * (spec section 6.1). Messages never contain cookies, tokens or stream URLs: InnerTubeX redacts
 * its own values, and any URL left in the details is replaced here.
 */
fun interface ProviderLogger {
    fun log(warning: Boolean, tag: String, message: String)
}

internal fun ProviderLogger.toInnerTubeLogger(): InnerTubeLogger = InnerTubeLogger { event ->
    if (event.level == InnerTubeLogLevel.DEBUG) return@InnerTubeLogger
    val details = event.details.entries.joinToString(", ") { (key, value) ->
        "$key=${if ("://" in value) "<url>" else value}"
    }
    log(
        warning = event.level >= InnerTubeLogLevel.WARN,
        tag = event.tag,
        message = if (details.isEmpty()) event.message else "${event.message} [$details]",
    )
}
