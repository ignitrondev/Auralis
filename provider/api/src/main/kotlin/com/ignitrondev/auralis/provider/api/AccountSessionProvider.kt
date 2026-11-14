/*
 * Copyright (c) 2026 IgnitronDev
 */
package com.ignitrondev.auralis.provider.api

/**
 * A signed-in session for a provider (spec section 12.1). Contains credentials: never log,
 * persist unencrypted or send anywhere except the provider. [toString] redacts every field.
 *
 * @property cookie The provider's session cookies, as one Cookie header value.
 * @property authUser Index of the account in a multi-account browser session.
 */
class ProviderSession(
    val cookie: String,
    val visitorData: String?,
    val dataSyncId: String?,
    val authUser: String = "0",
) {
    override fun toString(): String =
        "ProviderSession(cookie=<redacted>, visitorData=${if (visitorData != null) "present" else "missing"}, " +
            "dataSyncId=${if (dataSyncId != null) "present" else "missing"})"
}

/** The signed-in account as shown to the user. Only display data, no identifiers. */
data class ProviderAccount(
    val name: String,
    val handle: String?,
    val avatarUrl: String?,
)

/** A provider that can act on behalf of a signed-in user. */
interface AccountSessionProvider {
    /** Uses [session] for every following request; null signs out. */
    suspend fun applySession(session: ProviderSession?)

    /** The account the current session belongs to, or null when signed out or the session is no longer valid. */
    suspend fun currentAccount(): ProviderAccount?
}
