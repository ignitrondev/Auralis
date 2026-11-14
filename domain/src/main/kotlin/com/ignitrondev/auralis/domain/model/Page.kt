/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.model

/**
 * One page of a longer list.
 *
 * @property nextPageToken Opaque token for the next page, or null on the last page. Only the
 * source that issued it understands it.
 */
data class Page<T>(val items: List<T>, val nextPageToken: String?)
