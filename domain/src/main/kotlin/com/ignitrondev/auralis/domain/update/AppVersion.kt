/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.domain.update

/**
 * A semantic version such as `2.0.0` or `2.0.0-beta.2`, ordered by the SemVer 2.0 rules: a
 * pre-release is older than its release, and pre-release parts compare numerically when both are
 * numbers (`beta.2` < `beta.10`). A leading `v`, as in tags, and build metadata are ignored.
 */
class AppVersion private constructor(
    private val core: List<Int>,
    private val preRelease: List<String>,
    private val text: String,
) : Comparable<AppVersion> {

    val isPreRelease: Boolean get() = preRelease.isNotEmpty()

    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(core.size, other.core.size)) {
            val difference = core.getOrElse(i) { 0 }.compareTo(other.core.getOrElse(i) { 0 })
            if (difference != 0) return difference
        }
        if (preRelease.isEmpty() || other.preRelease.isEmpty()) {
            return other.preRelease.size.compareTo(0) - preRelease.size.compareTo(0)
        }
        for (i in 0 until minOf(preRelease.size, other.preRelease.size)) {
            val difference = comparePart(preRelease[i], other.preRelease[i])
            if (difference != 0) return difference
        }
        return preRelease.size.compareTo(other.preRelease.size)
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int =
        core.dropLastWhile { it == 0 }.hashCode() * 31 + preRelease.map { it.toIntOrNull()?.toString() ?: it }.hashCode()

    override fun toString(): String = text

    companion object {
        /** Null when [value] is not a version, for example a tag such as `nightly`. */
        fun parse(value: String): AppVersion? {
            val text = value.trim().removePrefix("v").removePrefix("V").substringBefore('+')
            val coreText = text.substringBefore('-')
            val core = coreText.split('.').map { it.toIntOrNull() ?: return null }
            if (core.isEmpty() || core.size > 4) return null
            val preRelease = if ('-' in text) text.substringAfter('-').split('.') else emptyList()
            if (preRelease.any { it.isEmpty() }) return null
            return AppVersion(core, preRelease, text)
        }

        private fun comparePart(a: String, b: String): Int {
            val numberA = a.toIntOrNull()
            val numberB = b.toIntOrNull()
            return when {
                numberA != null && numberB != null -> numberA.compareTo(numberB)
                numberA != null -> -1 // Numeric identifiers sort before alphanumeric ones.
                numberB != null -> 1
                else -> a.compareTo(b)
            }
        }
    }
}
