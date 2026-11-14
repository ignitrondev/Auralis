/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.core.designsystem.theme

/** Which token set to use. Dark is the default (spec section 8.2.1). */
enum class ThemeMode {
    Dark,
    Light,

    /** Follow the system dark theme setting. */
    System,
}
