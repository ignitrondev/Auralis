/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.repository

import com.ignitrondev.auralis.domain.model.CatalogItem

/** Public links to catalog items, for sharing. Only the item's source knows its link format. */
fun interface ShareLinks {
    /** A link anyone can open, or null when the item cannot be linked to. */
    fun link(item: CatalogItem): String?
}
